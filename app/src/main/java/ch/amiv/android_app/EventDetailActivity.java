package ch.amiv.android_app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;

public class EventDetailActivity extends AppCompatActivity {

    private int eventIndex = 0;

    ImageView posterImage;
    View posterMask;
    ScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);
        GetIntentData();
        InitUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        GetIntentData();
    }

    private void GetIntentData (){
        Intent intent = getIntent();
        eventIndex = intent.getIntExtra("eventIndex", 0);
    }

    private void InitUI (){
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if(eventIndex < 0 || Events.eventInfos.size() <= eventIndex) {
            Log.e("events", "invlaid event index selected during InitUI(), eventIndex: " + eventIndex + ", size" + Events.eventInfos.size() + ". Ensure that you are not clearing/overwiting the events list while viewing an event.");
            return;
        }

        TextView title = findViewById(R.id.eventTitle);
        TextView content = findViewById(R.id.eventDetail);
        scrollView = findViewById(R.id.scrollView_event);
        posterImage = findViewById(R.id.eventPoster);
        posterMask = findViewById(R.id.posterMask);

        title.setText(Events.eventInfos.get(eventIndex).title);
        content.setText(Events.eventInfos.get(eventIndex).description);

        AddRegisterDetails();

        if(Events.eventInfos.get(eventIndex).posterUrl.isEmpty())
        {
            posterImage.setVisibility(View.GONE);
            posterMask.setVisibility(View.GONE);
            findViewById(R.id.posterBg).setVisibility(View.GONE);
        }
        else
        {
            posterImage.setVisibility(View.VISIBLE);
            posterMask.setVisibility(View.VISIBLE);
            findViewById(R.id.posterBg).setVisibility(View.VISIBLE);

            //generate URL to image
            StringBuilder posterUrl = new StringBuilder();
            posterUrl.append(Events.eventInfos.get(eventIndex).posterUrl);
            if(posterUrl.charAt(0) == '/')
                posterUrl.deleteCharAt(0);
            posterUrl.insert(0, Settings.API_URL);

            Log.e("request", "image url: " + posterUrl.toString());

            //posterImage.setImageUrl(posterUrl.toString(), Requests.GetImageLoader(getApplicationContext()));

            ImageRequest posterRequest = new ImageRequest(posterUrl.toString(),
                    new Response.Listener<Bitmap>() {
                        @Override
                        public void onResponse(Bitmap bitmap) {
                            posterImage.setImageBitmap(bitmap);
                            //Will adjust the empty space/mask at the top of the scrollview so we can see the whole image
                            posterImage.post(new Runnable() {
                                @Override
                                public void run() {
                                    ViewGroup.LayoutParams layoutParams = posterMask.getLayoutParams();
                                    layoutParams.height = posterImage.getHeight();
                                    posterMask.setLayoutParams(layoutParams);
                                    //findViewById(R.id.posterBg).setLayoutParams(layoutParams);
                                }
                            });
                        }
                    }, 0, 0, ImageView.ScaleType.CENTER_INSIDE, Bitmap.Config.ARGB_8888,
                    new Response.ErrorListener() {
                        public void onErrorResponse(VolleyError error) {
                            posterImage.setImageResource(R.drawable.ic_error_white);
                        }
                    });
            Requests.SendRequest(posterRequest, getApplicationContext());
        }
    }

    private void AddRegisterDetails ()
    {
        ArrayList<String[]> infos = Events.eventInfos.get(eventIndex).GetInfos();
        LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
        for (int i = 0; i < infos.size(); i++) {
            LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.list_item_keyval, null, false);
            ((TextView) layout.findViewById(R.id.keyField  )).setText(infos.get(i)[0]);
            ((TextView) layout.findViewById(R.id.valueField)).setText(infos.get(i)[1]);

            LinearLayout linear = findViewById(R.id.register_details_list);
            linear.addView(layout);
        }
    }

    public void RegisterForEvent(View view)
    {
        if(!Requests.CheckConnection(getApplicationContext())) {
            Snackbar.make(view, "Requires Internet", Snackbar.LENGTH_LONG).show();
            return;
        }

        if(!Settings.IsLoggedIn(getApplicationContext())){
            Intent intent = new Intent(this, LoginActivity.class);
            intent.putExtra("cause", "register_event");
            startActivity(intent);
            return;
        }

        final int registerEventIndex = eventIndex;

        //Do request Token->User
        String url = Settings.API_URL + "eventsignups";
        StringRequest request = new StringRequest(Request.Method.POST, url,null, null)
        {
            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) { //Note: the parseNetworkResponse is only called if the response was successful (codes 2xx), else parseNetworkError is called.
                if(response != null) {
                    Log.e("request", "status Code: " + response.statusCode);

                    try {
                        JSONObject json = new JSONObject(new String(response.data)).getJSONObject("user");
                        if(json.has("_status") && json.getString("_status").equals("OK"))
                            Events.eventInfos.get(registerEventIndex).AddSignup(json);

                        //Log.e("request", json.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                else
                    Log.e("request", "Request returned null response.");
                return super.parseNetworkResponse(response);
            }

            @Override
            protected VolleyError parseNetworkError(final VolleyError volleyError) {  //see comments at parseNetworkResponse()
                if(volleyError != null && volleyError.networkResponse != null)
                    Log.e("request", "status code: " + volleyError.networkResponse.statusCode + "\n" + volleyError.networkResponse.data.toString());
                else
                    Log.e("request", "Request returned null response.");

                return super.parseNetworkError(volleyError);
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String,String> headers = new HashMap<String, String>();

                // Add basic auth with token
                String credentials = Settings.GetToken(getApplicationContext()) + ":";
                String auth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
                headers.put("Authorization", auth);
                return headers;
            }

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("event", Events.eventInfos.get(eventIndex)._id);
                params.put("user", UserInfo.current._id);//XXX check user exists
                return params;
            }
/*
            @Override
            public byte[] getBody() throws AuthFailureError {
                String body = "event:" + Events.eventInfos.get(eventIndex)._id + "\nuser:" + UserInfo.current._id;
                byte[] bytes = new byte[0];
                try {
                    bytes = body.getBytes("UTF-8");
                    Log.e("request", "body: " + new String(bytes, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                return bytes;
            }*/
        };

        Requests.SendRequest(request, getApplicationContext());
    }

    public void ScrollToTop (View view) {
        scrollView.fullScroll(ScrollView.FOCUS_UP);
    }
}
