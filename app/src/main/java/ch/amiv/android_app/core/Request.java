package ch.amiv.android_app.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.util.LruCache;
import android.util.Base64;
import android.util.Log;
import android.view.View;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import ch.amiv.android_app.events.Events;
import ch.amiv.android_app.jobs.Jobs;

/**
 * A static class to do backround http requests to the amiv api. Access these requests anywhere
 * Most requests have a callback, so you can execute code when the request has returned or failed
 * See API Docs to see what requests can be done: https://api.amiv.ethz.ch/docs  or via github site https://github.com/amiv-eth/amivapi
 *
 * It is advised to test requests first with Postman or similar.
 * To see the output of the requests setup Postman as a proxy server, and then set your computer's IP as the proxy in the phones wi-fi setting for the current connection
 *
 * Generally, call a FetchX function from anywhere, which will create and format the request then use SendRequest to submit the formatted request
 * When creating your own, have a look at the override functions available for the StringRequest. Note the difference between getHeaders and getParams
 *
 * To add auth with a token, from settings see one of the functions as an example, eg. FetchEventSignups
 *
 * To load images, don't use a request directly, use a networkImageView, which will handle everything for you including caching
 *
 * Libary used for network stuff: volley, note: we use our own modified version of the libary as a git submodule
 */
public final class Request {
    private static RequestQueue requestQueue;
    private static ImageLoader imageLoader;
    private static final int MAX_CACHED_IMAGES = 75;

    public static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private static Handler callbackHandler = new Handler();

    /**
     * Used for doing callbacks when data has been received from the server
     */
    public interface OnDataReceivedCallback {
        void OnDataReceived();
    }

    /**
     * Send a created request with the requestQueue. You should use this function to send all requests. Also checks if we have an internet connection. Uses static request queue
     * @param request A volley request of generic type, eg StringRequest, JsonRequest
     * @return true if the request was sent successfully
     */
    public static boolean SendRequest(com.android.volley.Request request, Context context){
        if(!CheckConnection(context))
            return false;

        if(requestQueue == null)
            requestQueue = Volley.newRequestQueue(context);  //Adds the defined post request to the queue to be sent to the server
        requestQueue.add(request);

        return true;
    }

    /**
     * Will fetch the list of events from the server, note does not require an access token.
     * @param errorCallback Use this to know when an error occured to stop loading animations etc
     * @param eventId to only fetch for a specific event id add this, else set as emoty
     * @return True if the request was sent.
     */
    public static void FetchEventList(final Context context, final OnDataReceivedCallback callback, final OnDataReceivedCallback errorCallback, @NonNull final String eventId)
    {
        if(!CheckConnection(context)) {
            RunCallback(errorCallback);
            return;
        }

        String url = Settings.API_URL + "events" + (eventId.isEmpty() ?
                                                                        (UserInfo.ShowHiddenFeatures(context) ? "" : "?where={\"show_website\":true}")
                                                                        : "/" + eventId) ;
        Log.e("request", "url: " + url);

        StringRequest request = new StringRequest(com.android.volley.Request.Method.GET, url,null, null)
        {
            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) { //Note: the parseNetworkResponse is only called if the response was successful (codes 2xx), else parseNetworkError is called.
                if(response != null) {
                    Log.e("request", "fetch events status Code: " + response.statusCode);

                    try {
                        final JSONObject json = new JSONObject(new String(response.data));

                        //Update events on main thread
                        Runnable runnable = new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    if(eventId.isEmpty())
                                        Events.UpdateEventInfos(context, json.getJSONArray("_items"));
                                    else
                                        Events.UpdateSingleEvent(json, eventId);
                                    if(callback != null)
                                        callback.OnDataReceived();
                                } catch (JSONException e) {
                                    RunCallback(errorCallback);
                                    e.printStackTrace();
                                }
                            }
                        };
                        callbackHandler.post(runnable);

                        Log.e("request", new JSONObject(new String(response.data)).toString());
                    } catch (JSONException e) {
                        RunCallback(errorCallback);
                        e.printStackTrace();
                    }
                }
                else {
                    RunCallback(errorCallback);
                    Log.e("request", "Request returned null response. fetch events");
                }
                return super.parseNetworkResponse(response);
            }

            @Override
            protected VolleyError parseNetworkError(final VolleyError volleyError) {  //see comments at parseNetworkResponse()
                if(volleyError != null && volleyError.networkResponse != null)
                    Log.e("request", "status code: " + volleyError.networkResponse.statusCode + "\n" + new String(volleyError.networkResponse.data));
                else
                    Log.e("request", "Request returned null response. fetch events");

                RunCallback(errorCallback);
                return super.parseNetworkError(volleyError);
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                if(Settings.HasToken(context)) {
                    Map<String,String> headers = new HashMap<String, String>();

                    String credentials = Settings.GetToken(context) + ":";
                    String auth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
                    headers.put("Authorization", auth);

                    return headers;
                }

                return super.getHeaders();
            }
        };

        //send the request and check if it failed
        if(!Request.SendRequest(request, context))
            RunCallback(errorCallback);
    }

    /**
     * Will fetch the event signups for the current user and save them in the eventInfos list
     * @param eventId to only fetch for a specific event id add this, else set as emoty
     */
    public static void FetchEventSignups(final Context context, final OnDataReceivedCallback callback, final OnDataReceivedCallback errorCallback, @NonNull String eventId)
    {
        if(!Settings.HasToken(context) || UserInfo.current == null || UserInfo.current._id.isEmpty()) {
            RunCallback(errorCallback);
            return;
        }

        String url = Settings.API_URL + "eventsignups?where={\"user\":\"" + UserInfo.current._id + "\"" + (eventId.isEmpty() ? "" : ",\"event\":\"" + eventId + "\"") + "}";
        Log.e("request", "url: " + url);

        StringRequest request = new StringRequest(com.android.volley.Request.Method.GET, url,null, null)
        {
            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) { //Note: the parseNetworkResponse is only called if the response was successful (codes 2xx), else parseNetworkError is called.
                if(response != null) {
                    Log.e("request", "/eventsignups status Code: " + response.statusCode);

                    try {
                        JSONObject json = new JSONObject(new String(response.data));
                        if(json.has("_items")) {
                            final JSONArray signupsJson = json.getJSONArray("_items");

                            //save json data to events and run callback in main thread
                            Runnable runnable = new Runnable() {
                                @Override
                                public void run() {
                                    Events.AddSignupArray(signupsJson);
                                    if(callback != null)
                                        callback.OnDataReceived();
                                }
                            };
                            callbackHandler.post(runnable);
                        }

                        //Log.e("request", json.toString());
                    } catch (JSONException e) {
                        RunCallback(errorCallback);
                        e.printStackTrace();
                    }
                }
                else {
                    RunCallback(errorCallback);
                    Log.e("request", "Request returned null response: fetch event signups");
                }
                return super.parseNetworkResponse(response);
            }

            @Override
            protected VolleyError parseNetworkError(final VolleyError volleyError) {  //see comments at parseNetworkResponse()
                if(volleyError != null && volleyError.networkResponse != null)
                    Log.e("request", "Fetch event signups, status code: " + volleyError.networkResponse.statusCode + "\n" + new String(volleyError.networkResponse.data));
                else
                    Log.e("request", "Request returned null response: fetch event signups");

                RunCallback(errorCallback);
                return super.parseNetworkError(volleyError);
            }

            @Override
            public Map<String, String> getHeaders()  {
                Map<String,String> headers = new HashMap<String, String>();

                // Add basic auth with token
                String credentials = Settings.GetToken(context) + ":";
                String auth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
                headers.put("Authorization", auth);

                return headers;
            }
        };

        if(!Request.SendRequest(request, context))
            RunCallback(errorCallback);
    }

    public static void FetchJobList(final Context context, final OnDataReceivedCallback callback, final OnDataReceivedCallback errorCallback, @NonNull final String jobId)
    {
        if(!CheckConnection(context)) {
            RunCallback(errorCallback);
            return;
        }

        String url = Settings.API_URL + "joboffers" + (jobId.isEmpty() ?
                                                                        (UserInfo.ShowHiddenFeatures(context) ? "" : "?where={\"show_website\":true}")
                                                                        : "/" + jobId);
        Log.e("request", "url: " + url);

        StringRequest request = new StringRequest(com.android.volley.Request.Method.GET, url,null, null)
        {
            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) { //Note: the parseNetworkResponse is only called if the response was successful (codes 2xx), else parseNetworkError is called.
                if(response != null) {
                    Log.e("request", "fetch jobs status Code: " + response.statusCode);

                    try {
                        final JSONObject json = new JSONObject(new String(response.data));

                        //Update on main thread
                        Runnable runnable = new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    if(jobId.isEmpty())
                                        Jobs.UpdateJobInfos(context, json.getJSONArray("_items"));
                                    else
                                        Jobs.UpdateSingleJob(json, jobId);
                                    if(callback != null)
                                        callback.OnDataReceived();
                                } catch (JSONException e) {
                                    RunCallback(errorCallback);
                                    e.printStackTrace();
                                }
                            }
                        };
                        callbackHandler.post(runnable);

                        Log.e("request", new JSONObject(new String(response.data)).toString());
                    } catch (JSONException e) {
                        RunCallback(errorCallback);
                        e.printStackTrace();
                    }
                }
                else {
                    RunCallback(errorCallback);
                    Log.e("request", "Request returned null response. fetch jobs");
                }
                return super.parseNetworkResponse(response);
            }

            @Override
            protected VolleyError parseNetworkError(final VolleyError volleyError) {
                if(volleyError != null && volleyError.networkResponse != null)
                    Log.e("request", "status code: " + volleyError.networkResponse.statusCode + "\n" + new String(volleyError.networkResponse.data));
                else
                    Log.e("request", "Request returned null response. fetch jobs");

                RunCallback(errorCallback);
                return super.parseNetworkError(volleyError);
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                if(Settings.HasToken(context)) {
                    Map<String,String> headers = new HashMap<String, String>();

                    String credentials = Settings.GetToken(context) + ":";
                    String auth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
                    headers.put("Authorization", auth);

                    return headers;
                }

                return super.getHeaders();
            }
        };

        //send the request and check if it failed
        if(!Request.SendRequest(request, context))
            RunCallback(errorCallback);
    }

    /**
     * Will fetch the user from the api if we have an access token. ie Token -> User. Data is stored in the current userinfo (UserInfo.current). Overwrites the current user info if it exists
     */
    public static void FetchUserData(final Context context, final View view, final OnDataReceivedCallback callback)
    {
        if(!Settings.HasToken(context) || !CheckConnection(context))
            return;

        //Do request Token->User
        String url = Settings.API_URL + "sessions/" + Settings.GetToken(context) + "?embedded={\"user\":1}";
        StringRequest request = new StringRequest(com.android.volley.Request.Method.GET, url,null, null)
        {
            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) { //Note: the parseNetworkResponse is only called if the response was successful (codes 2xx), else parseNetworkError is called.
                if(response != null) {
                    Log.e("request", "status Code: " + response.statusCode);

                    try {
                        final JSONObject json = new JSONObject(new String(response.data)).getJSONObject("user");
                        callbackHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                UserInfo.UpdateCurrent(context, json, false, false);
                            }
                        });

                        if(callback != null && view != null) {
                            view.post(new Runnable() {
                                @Override
                                public void run() { callback.OnDataReceived(); }
                            });
                        }
                        //Log.e("request", json.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                else
                    Log.e("request", "Request returned null response. fetch user data");
                return super.parseNetworkResponse(response);
            }

            @Override
            protected VolleyError parseNetworkError(final VolleyError volleyError) {  //see comments at parseNetworkResponse()
                if(volleyError != null && volleyError.networkResponse != null)
                    Log.e("request", "status code: " + volleyError.networkResponse.statusCode + "\n" + new String(volleyError.networkResponse.data));
                else
                    Log.e("request", "Request returned null response. fetch user data");

                return super.parseNetworkError(volleyError);
            }

            @Override
            public Map<String, String> getHeaders()  {
                Map<String,String> headers = new HashMap<String, String>();

                // Add basic auth with token
                String credentials = Settings.GetToken(context) + ":";
                String auth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
                headers.put("Authorization", auth);

                return headers;
            }
        };

        boolean hasSent = Request.SendRequest(request, context);
    }

    /**
     * Will update the user data in the amiv api
     * XXX buffer and send request when internet is regained, and retry there is an error
     */
    public static void PatchUserData(final Context context){
        if(!Settings.HasToken(context) || !CheckConnection(context))
            return;

        //Do patch request to /user/{userId}
        String url = Settings.API_URL + "users/" + UserInfo.current._id;
        StringRequest request = new StringRequest(com.android.volley.Request.Method.PATCH, url,null, null)
        {
            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) { //Note: the parseNetworkResponse is only called if the response was successful (codes 2xx), else parseNetworkError is called.
                if(response != null) {
                    Log.e("request", "status Code: " + response.statusCode);
                }
                else
                    Log.e("request", "Request returned null response. fetch user data");
                return super.parseNetworkResponse(response);
            }

            @Override
            protected VolleyError parseNetworkError(final VolleyError volleyError) {  //see comments at parseNetworkResponse()
                if(volleyError != null && volleyError.networkResponse != null)
                    Log.e("request", "status code: " + volleyError.networkResponse.statusCode + "\n" + new String(volleyError.networkResponse.data));
                else
                    Log.e("request", "Request returned null response. fetch user data");

                return super.parseNetworkError(volleyError);
            }

            @Override
            public Map<String, String> getHeaders()  {
                Map<String,String> headers = new HashMap<String, String>();

                // Add basic auth with token
                String credentials = Settings.GetToken(context) + ":";
                String auth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
                headers.put("Authorization", auth);

                headers.put("if-match", UserInfo.current._etag);

                return headers;
            }

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String,String> params = new HashMap<String, String>();
                params.put("rfid", UserInfo.current.rfid);

                return params;
            }
        };

        boolean hasSent = Request.SendRequest(request, context);
    }

    /**
     * Will send a Delete request to delete the current session and token with it.
     * XXX When there is no internet or an error the request is not completed. the session persists on the server. need to rerun request on next time we have a connection
     */
    public static void DeleteCurrentSession(final Context context)
    {
        if(Settings.IsEmailOnlyLogin(context))
            return;

        final UserInfo user = UserInfo.current;
        final String token = Settings.GetToken(context);
        Settings.SetToken("", context);

        if(!Settings.HasToken(context) || UserInfo.current == null || UserInfo.current.session_id.isEmpty())
            return;

        //Do request Token->User
        String url = Settings.API_URL + "sessions/" + user.session_id;
        StringRequest request = new StringRequest(com.android.volley.Request.Method.DELETE, url,null, null)
        {
            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) { //Note: the parseNetworkResponse is only called if the response was successful (codes 2xx), else parseNetworkError is called.
                if(response != null) {
                    Log.e("request", "Deleted session successfully. status Code: " + response.statusCode);
                    Settings.SetToken("", context);
                }
                else
                    Log.e("request", "Request returned null response. delete session ");
                return super.parseNetworkResponse(response);
            }

            @Override
            protected VolleyError parseNetworkError(final VolleyError volleyError) {  //see comments at parseNetworkResponse()
                if(volleyError != null && volleyError.networkResponse != null)
                    Log.e("request", "delete session , status code: " + volleyError.networkResponse.statusCode + "\n" + new String(volleyError.networkResponse.data));
                else
                    Log.e("request", "delete session request returned null response.");

                return super.parseNetworkError(volleyError);
            }

            @Override
            public Map<String, String> getHeaders() {
                Map<String,String> headers = new HashMap<String, String>();

                // Add basic auth with token
                String credentials = token + ":";
                String auth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
                headers.put("Authorization", auth);

                headers.put("if-match", user.session_etag);

                return headers;
            }
        };

        Settings.SetToken("", context);
        Request.SendRequest(request, context);
    }

    /**
     * Will run the callback on the main thread, use this for easily executing callbacks, which do not have any additional code on the main thread
     * @param callback
     */
    private static void RunCallback (final OnDataReceivedCallback callback)
    {
        if(callback == null)
            return;

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                callback.OnDataReceived();
            }
        };
        callbackHandler.post(runnable);
    }

//region =====Image Loader=====
    public static ImageLoader GetImageLoader(Context context){
        if(imageLoader == null)
        {
            if(requestQueue == null)
                requestQueue = Volley.newRequestQueue(context);

            imageLoader = new ImageLoader(requestQueue, new ImageLoader.ImageCache() {
            private final LruCache<String, Bitmap> mCache = new LruCache<>(MAX_CACHED_IMAGES);
            public void putBitmap(String url, Bitmap bitmap) {
                mCache.put(url, bitmap);
            }
            public Bitmap getBitmap(String url) {
                return mCache.get(url);
            }
        });
        }

        return imageLoader;
    }

    /**
     * @return returns true if there is an active internet connection, test this before requesting something from the server
     */
    public static boolean CheckConnection(Context context)
    {
        if(context == null)//If we cannot check the connection then fallback to no internet
            return false;

        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        if(activeNetwork == null || !activeNetwork.isConnectedOrConnecting())
        {
            Log.e("postrequest", "No active internet connection");
            return false;
        }
        return true;
    }
}