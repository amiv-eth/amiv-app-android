package ch.amiv.android_app.checkin;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Roger on 13-Feb-18.
 *
 * Used for getting/sending data from/to the server, if it is required in several activities.
 */

public final class ServerRequests {
    private static String ON_SUBMIT_PIN_URL_EXT = "/checkpin";
    private static String ON_SUBMIT_LEGI_URL_EXT = "/mutate";
    private static String GET_DATA_URL_EXT = "/checkin_update_data";
    public static RequestQueue requestQueue;

    public interface OnDataReceivedCallback {  //used for doing callbacks when the memberDB has been updated
        void OnDataReceived();
    }

    public interface OnCheckPinReceivedCallback {  //used for doing callbacks when the memberDB has been updated
        void OnStringReceived(boolean validResponse, int statusCode, String data);
    }

    public interface OnJsonReceivedCallback {  //used for doing callbacks when the memberDB has been updated
        void OnJsonReceived(int statusCode, JSONObject data);
        void OnStringReceived(int statusCode, String data);
    }

    /**
     * Call this to check with the server if a pin is valid
     * @param callback A function to execute when the response has been received
     */
    public static void CheckPin (final Context context, final OnCheckPinReceivedCallback callback)
    {

        if(!CheckConnection(context))
            return;

        Log.e("postrequest", "Params sent: pin=" + MainActivity.CurrentPin + ", URL used: " + SettingsActivity.GetServerURL(context) + ON_SUBMIT_PIN_URL_EXT);

        StringRequest postRequest = new StringRequest(Request.Method.POST, SettingsActivity.GetServerURL(context) + ON_SUBMIT_PIN_URL_EXT
                , new Response.Listener<String>() { @Override public void onResponse(String response){} }
                , new Response.ErrorListener() { @Override public void onErrorResponse(VolleyError error){} })
        {
            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) { //Note: the parseNetworkResponse is only called if the response was successful (codes 2xx), else parseNetworkError is called.
                if(response != null)
                    callback.OnStringReceived(true, response.statusCode, new String(response.data));
                else
                    callback.OnStringReceived(false, 400, "");
                return super.parseNetworkResponse(response);
            }

            @Override
            protected VolleyError parseNetworkError(final VolleyError volleyError) {  //see comments at parseNetworkResponse()
                if(volleyError != null && volleyError.networkResponse != null)
                    callback.OnStringReceived(true, volleyError.networkResponse.statusCode, new String(volleyError.networkResponse.data));
                else
                    callback.OnStringReceived(false, 400, "");

                return super.parseNetworkError(volleyError);
            }

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>(); //Set the parameters being sent
                params.put("pin", MainActivity.CurrentPin);

                return params;
            }
        };

        if(requestQueue == null)
            requestQueue = Volley.newRequestQueue(context);  //Adds the defined post request to the queue to be sent to the server
        requestQueue.add(postRequest);

    }

    public static void CheckLegi(final Context context, final OnJsonReceivedCallback callback, final String legi, final boolean isCheckingIn)
    {
        //Note: server will send a Json if the response is valid, ie the person has been checked in, else a string. This is to get the member type. Yet we still need to do a stringRequest
        StringRequest req = new StringRequest(Request.Method.POST, SettingsActivity.GetServerURL(context) + ON_SUBMIT_LEGI_URL_EXT
                , new Response.Listener<String>() { @Override public void onResponse(String response) {}}       //initialise with empty response listeners as we will handle the response in the parseNetworkResponse and parseNetworkError functions
                , new Response.ErrorListener() {@Override public void onErrorResponse(VolleyError error) {}})
        {
            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) { //Note: the parseNetworkResponse is only called if the response was successful (codes 2xx), else parseNetworkError is called.
                if(callback != null && response != null && !response.toString().isEmpty()) {
                    Log.e("json", "mutate json received: " + new String(response.data));
                    try {
                        callback.OnJsonReceived(response.statusCode, new JSONObject(new String(response.data)));
                    }
                    catch (JSONException e) {
                        Log.e("json", "Error creating Json from string: " + e.toString());
                    }
                }

                return super.parseNetworkResponse(response);
            }

            @Override
            protected VolleyError parseNetworkError(VolleyError ve) {  //see comments at parseNetworkResponse()
                if(ve != null && ve.networkResponse != null) {
                    Log.e("postrequest", "parseNetworkError Response from server for JSON data: " + ve.networkResponse.statusCode + " with text: " + new String(ve.networkResponse.data) + " on event pin: " + MainActivity.CurrentPin);
                    callback.OnStringReceived(ve.networkResponse.statusCode, new String(ve.networkResponse.data));
                }

                return super.parseNetworkError(ve);
            }

            @Override
            protected Map<String, String> getParams() { //Adding the parameters to be sent to the server, with forms. Do not change strings as they match with the server scripts!`

                Map<String, String> params = new HashMap<String, String>(); //Parameters being sent to server in POST
                params.put("pin", MainActivity.CurrentPin);
                params.put("info", legi);
                params.put("checkmode", (isCheckingIn ? "in" : "out"));

                return params;
            }
        };

        if(requestQueue == null)
            requestQueue = Volley.newRequestQueue(context);  //Adds the defined post request to the queue to be sent to the server
        requestQueue.add(req);
    }

    //-----Updating Stats-----
    /**
     * Will Get the list of people for the event from the server, with stats, and save to the static memberDatabase. Note data may still be null!
     */
    public static void UpdateMemberDB (final Context context, final OnDataReceivedCallback callback)
    {
        if(!CheckConnection(context))
            return;

        Log.e("postrequest", "Params sent: pin=" + MainActivity.CurrentPin + ", URL used: " + SettingsActivity.GetServerURL(context) + GET_DATA_URL_EXT);

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, SettingsActivity.GetServerURL(context) + GET_DATA_URL_EXT,
                (JSONObject) null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                // Parsing json object response and save to the static memberDB, parse each part in a separate try catch, so we get as much info as possible
                //1. Update List of People
                if (EventDatabase.instance == null) {
                    new EventDatabase();
                    Log.e("postrequest", "Cancelled parsing json as assumed the scanning session was ended.");
                    return;
                }
                if(response == null) {
                    Log.e("get_event_info", "Response received is null.");
                    return;
                }

                try {
                    Log.e("postrequest", "response length:" + response.length() + "   keys: " + (response.keys().hasNext() ? response.keys().next() : "no keys found") + "   content: " + response.toString());

                    if (response.has("signups"))
                        EventDatabase.instance.UpdateMemberData(response.getJSONArray("signups"));
                    else
                        Log.e("postrequest", "Unable to update signups: not found in json");
                } catch (JSONException e) {
                    Log.e("postrequest", "Error parsing received JsonObject in get Signups: " + e.toString());
                    e.printStackTrace();
                }

                //2. Update Event Infos
                boolean hasEventInfos = response.has("eventinfos");   //If the json does not have the eventinfos or we cannot parse the event type then imply the event type from the stats
                if (hasEventInfos) {
                    try {
                        JSONObject eventInfo = response.getJSONObject("eventinfos");
                        //Log.e("postrequest", "event infos content:" + eventInfo.toString());

                        hasEventInfos = EventDatabase.instance.eventData.Update(eventInfo.getString("_id"), eventInfo.optString("event_type"), eventInfo.optString("checkin_type"), eventInfo.getString("title"),
                                eventInfo.optString("description"), eventInfo.getInt("signup_count"), eventInfo.getInt("spots"), eventInfo.getString("time_start"));
                    } catch(JSONException e){
                        Log.e("postrequest", "Error parsing received JsonObject in get EventInfos: " + e.toString());
                        e.printStackTrace();
                    }
                }

                //3. Update Stats
                if (response.has("statistics")) {
                    try {
                        JSONArray stats = response.getJSONArray("statistics");

                        EventDatabase.instance.UpdateStats(stats, hasEventInfos);
                    } catch (JSONException e) {
                        Log.e("postrequest", "Error parsing received JsonObject in get Statistics: " + e.toString());
                        e.printStackTrace();
                    }
                }

                if(callback != null)
                    callback.OnDataReceived();    //Ensure to call callback
            }
        }, new Response.ErrorListener() { @Override public void onErrorResponse(VolleyError error) {}})
        {
            @Override
            protected VolleyError parseNetworkError(VolleyError volleyError) {  //see comments at parseNetworkResponse()
                if(volleyError != null && volleyError.networkResponse != null)
                    Log.e("postrequest", "parseNetworkError Response from server for JSON data: " + volleyError.networkResponse.statusCode + " with text: " + new String(volleyError.networkResponse.data) + " on event pin: " + MainActivity.CurrentPin);
                return super.parseNetworkError(volleyError);
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("pin", MainActivity.CurrentPin);
                return headers;
            }
        };

        if(requestQueue == null)
            requestQueue = Volley.newRequestQueue(context);  //Adds the defined post request to the queue to be sent to the server
        requestQueue.add(req);
    }

    /**         // Note: Is currently commented out as the server side is giving us the eventData as part of the /checking_update_data json, however may change to be separate request, so it is only sent at the start of a session
     * Will update the event data, only call this when entering a session
     * @param callback Use this callback for executing code when the data has been received
     */
    /*public static void UpdateEventData(Context context, final OnDataReceivedCallback callback)
    {
        if(!CheckConnection(context))
            return;

        Log.e("postrequest", "Params sent: pin=" + MainActivity.CurrentPin + ", URL used: " + SettingsActivity.GetServerURL(context) + "/get_event_data");

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, SettingsActivity.GetServerURL(context) + "/get_event_data",
                (JSONObject) null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.e("postrequest", "response length:" + response.length() + "   first key: " + response.keys().next() + "   content: " + response.toString());

                //Note the constuctor will set this as the static instance
                if(response.has("eventinfos"))
                    new EventData(response.optString("event_type"), response.optString("event_title")/*, response.optString("startDate"), response.optString("endDate")*///); //! Check keys with server project !
/*
                if(callback != null)
                    callback.OnDataReceived();    //Ensure to call callback
            }
        }, new Response.ErrorListener() { @Override public void onErrorResponse(VolleyError error) {}})
        {
            @Override
            protected VolleyError parseNetworkError(VolleyError volleyError) {  //see comments at parseNetworkResponse()
                if(volleyError != null)
                    Log.e("postrequest", "parseNetworkError Response from server for JSON data: " + volleyError.networkResponse.statusCode + " with text: " + new String(volleyError.networkResponse.data) + " on event pin: " + MainActivity.CurrentPin);
                return super.parseNetworkError(volleyError);
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("pin", MainActivity.CurrentPin);
                return headers;
            }
        };

        if(requestQueue == null)
            requestQueue = Volley.newRequestQueue(context);  //Adds the defined post request to the queue to be sent to the server
        requestQueue.add(req);
    }*/

    /**
     * @return returns true if there is an active internet connection, test this before requesting something from the server
     */
    public static boolean CheckConnection(Context context)
    {
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