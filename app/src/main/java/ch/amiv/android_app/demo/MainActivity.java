package ch.amiv.android_app.demo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import ch.amiv.android_app.R;
import ch.amiv.android_app.util.Util;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.demo_activity_main);

        Util.SetupToolbar(this, true);

    /*UNCOMMENT WHEN READING


        //To access details of the user use
        String firstname = UserInfo.current.firstname;

        //To access events use
        EventInfo eventInfo = Events.sortedEvents.get(0).get(1);//sorted into groups by date
        eventInfo = Events.eventInfos.get(0);//unsorted list

        //To Send a request to the api or elsewhere see core.Request class for examples. Basic structure is as below
        //START of request
        if(!Request.CheckConnection(getApplicationContext())) {
            //return;
        }

        //build url string
        String url = Settings.API_URL + "my/url/extension";

        //create our http request with the volley libary, we will override the methods to customise our request and set values
        StringRequest request = new StringRequest(Request.Method.GET, url,null, null)
        {

            //====Adding data to be sent to the request====
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {   //Here we add our data to the header, can also add the body in getParams, use Crl+O to see possible override functions
                Map<String,String> headers = new HashMap<String, String>();

                //This adds basic auth using our token if it exists, this can be retrieved from core.Settings
                if(Settings.HasToken(getApplicationContext())) {
                    String credentials = Settings.GetToken(getApplicationContext()) + ":";
                    String auth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
                    headers.put("Authorization", auth);
                }

                //Add other headers

                return headers;
            }

            @Override
            protected Map<String, String> getParams() throws AuthFailureError { //Add key value pairs to the body of the request
                Map<String,String> body = new HashMap<String, String>();

                //Add body key values here using body.put(key, value);

                return body;
            }

            //====Handling of the response of the request
            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) { //this is called if we have received a response from the server with a successful status code, eg 2xx, not 4xx
                if(response != null) {
                    Log.e("request", "fetch demo data status Code: " + response.statusCode);

                    try {
                        final JSONObject json = new JSONObject(new String(response.data));  //get data as json so we can use getString, optString, getJSONArray etc

                        //
                        Runnable runnable = new Runnable() {
                            @Override
                            public void run() {
                                //use the data to update values here, this is executed on the main thread
                            }
                        };
                        Handler handler = new Handler();
                        handler.post(runnable);

                        Log.e("request", new JSONObject(new String(response.data)).toString());
                    } catch (JSONException e) {
                        //Add your error handling here, usually a callback. This happens when we have received a json or other data which was not as expected
                        e.printStackTrace();
                    }
                }
                else {
                    //Add your error handling here, usually a callback
                    Log.e("request", "Request returned null response: fetch demo data");
                }
                return super.parseNetworkResponse(response);
            }

            @Override
            protected VolleyError parseNetworkError(final VolleyError volleyError) {    //This is called when we receive an error, either locally or the server returns an error status code, 4xx or 5xx
                //Always check that we have a non null network response, it is often null when there is no internet
                if(volleyError != null && volleyError.networkResponse != null)
                    Log.e("request", "status code: " + volleyError.networkResponse.statusCode + "\n" + new String(volleyError.networkResponse.data));
                else
                    Log.e("request", "Request returned null response: fetch demo data");
                return super.parseNetworkError(volleyError);
            }
        };

        //send the request and check if it failed, this will use a request queue and handle internet connection issues
        boolean hasSent = Request.SendRequest(request, getApplicationContext());

        //See the Request class for real examples, using Crl+B can be useful to jump to the definition, Crl+F for searching the whole project for a function
        //END of request
*/
    }
}
