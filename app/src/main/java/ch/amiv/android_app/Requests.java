package ch.amiv.android_app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.constraint.ConstraintLayout;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.widget.ImageView;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class Requests {
    private static String ON_SUBMIT_PIN_URL_EXT = "/checkpin";
    private static String ON_SUBMIT_LEGI_URL_EXT = "/mutate";
    private static String GET_DATA_URL_EXT = "/checkin_update_data";
    private static RequestQueue requestQueue;
    private static ImageLoader imageLoader;

    //=======CALLBACK INTERFACES========

    /**
     * Used for doing callbacks when the memberDB has been updated
     */
    public interface OnDataReceivedCallback {
        void OnDataReceived(int statusCode, String data);
    }

    public interface OnCheckPinReceivedCallback {  //used for doing callbacks when the memberDB has been updated
        void OnStringReceived(boolean validResponse, int statusCode, String data);
    }

    public interface OnJsonReceivedCallback {  //used for doing callbacks when the memberDB has been updated
        void OnJsonReceived(int statusCode, JSONObject data);
        void OnStringReceived (int statusCode, String data);
    }
    //====END OF CALLBACKS======

    public enum ServerTarget {None, API, Checkin}
    /**
     * Send a created request with the requestQueue
     * @param request A volley request of generic type, eg StringRequest, JsonRequest
     */
    public static boolean SendRequest(Request request, Context context){
        if(!CheckConnection(context))
            return false;

        if(requestQueue == null)
            requestQueue = Volley.newRequestQueue(context);  //Adds the defined post request to the queue to be sent to the server
        requestQueue.add(request);

        return true;
    }

    /**
     * Will replace illegal characters correctly
     * @param url
     * @return
     */
    public static String EncodeUrl(String url)
    {
        String encodedUrl = "";
        try {
            encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return "";
        }
        return encodedUrl;
    }

    /**
     *
     * @param context
     * @param callback
     * @return If the request was sucessfuly sent, may be false if there is not connction
     */
    public static boolean Request(Request.Method requestMethod, final Context context, final ServerTarget serverTarget, final String url_, final HashMap<String, String> params, final HashMap<String, String> header, final HashMap<String, String> body, final OnJsonReceivedCallback callback){
        if(!CheckConnection(context))
            return false;

        //create URL
        String url = "";
        switch (serverTarget) {
            case None:
                break;
            case API:
                url += Settings.API_URL;
                break;
            case Checkin:
                break;
            default:
                break;
        }
        url += url_;

        String encodedUrl = "";
        try {
             encodedUrl = URLEncoder.encode(url, java.nio.charset.StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return false;
        }

        StringRequest request = new StringRequest(requestMethod.hashCode(), encodedUrl,
                /*new Response.Listener<String>() { @Override public void onResponse(String response){} },
                new Response.ErrorListener() { @Override public void onErrorResponse(VolleyError error){} }*/
                null, null)
        {
            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) { //Note: the parseNetworkResponse is only called if the response was successful (codes 2xx), else parseNetworkError is called.
                if(response != null)
                    callback.OnStringReceived(response.statusCode, new String(response.data));
                else
                    callback.OnStringReceived(400, "");
                return super.parseNetworkResponse(response);
            }

            @Override
            protected VolleyError parseNetworkError(final VolleyError volleyError) {  //see comments at parseNetworkResponse()
                if(volleyError != null && volleyError.networkResponse != null)
                    callback.OnStringReceived(volleyError.networkResponse.statusCode, new String(volleyError.networkResponse.data));
                else
                    callback.OnStringReceived(400, "");

                return super.parseNetworkError(volleyError);
            }

            //==Adding the content==
            @Override
            protected Map<String, String> getParams() {
                if(Settings.IsLoggedIn(context))
                    params.put("token", Settings.GetToken(context));
                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return header;
            }

            @Override
            public byte[] getBody() throws AuthFailureError {
                StringBuilder tmp = new StringBuilder();
                Iterator it = body.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry pair = (Map.Entry)it.next();
                    tmp.append(pair.getKey()).append(":").append(pair.getValue()).append("\n");
                    it.remove();
                }
                return tmp.toString().getBytes();
            }

        };

        return SendRequest(request, context);
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }

//region =====Image Loader=====
    public static ImageLoader GetImageLoader(Context context){
        if(imageLoader == null)
        {
            if(requestQueue == null)
                requestQueue = Volley.newRequestQueue(context);

            imageLoader = new ImageLoader(requestQueue, new ImageLoader.ImageCache() {
            private final LruCache<String, Bitmap> mCache = new LruCache<String, Bitmap>(10);
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