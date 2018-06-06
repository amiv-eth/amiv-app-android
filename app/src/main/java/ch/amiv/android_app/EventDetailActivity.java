package ch.amiv.android_app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * The activity/screen used for showing a selected event in detail.
 * This mainly displays stored info about the event, eg description and also fetches more such as images. Also handles registering for and event and the possible outcomes
 */
public class EventDetailActivity extends AppCompatActivity {

    private int eventIndex = 0;

    private ImageView posterImage;
    private ImageView posterMask;
    private View posterBg;
    private ProgressBar posterProgress; //Note:posterProgress bar is specific for the image loading, whereas swipeRefreshLayout (swipe down to refresh) is for reloading the whole event
                                        //There should be a progress bar for each image as these can take longer to load
    private ScrollView scrollView;
    private Button registerButton;

    private Intent responseIntent = new Intent();   //used for sending back a result to the calling activity, used for telling MainActivity if the login has changed

    private SwipeRefreshLayout swipeRefreshLayout;
    private Requests.OnDataReceivedCallback cancelRefreshCallback = new Requests.OnDataReceivedCallback() {
        @Override
        public void OnDataReceived() {
            swipeRefreshLayout.setRefreshing(false);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);
        GetIntentData();
        InitUI();
    }

    //When returning from login activity refresh data and register the user, then apply the response to the UI
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0 && resultCode == RESULT_OK) {
            // If we are returning from the login activity and have had a successfuly login, refresh the user info and login UI
            boolean refreshLogin = data.getBooleanExtra("login_success", false);
            if(refreshLogin && Settings.IsLoggedIn(getApplicationContext()))
            {
                Requests.FetchEventSignups(getApplicationContext(), new Requests.OnDataReceivedCallback() {
                    @Override
                    public void OnDataReceived() {
                        UpdateRegisterButton();
                    }
                });
                responseIntent.putExtra("login_success", refreshLogin);
                ScrollToBottom(null);
                RegisterForEvent(null);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        GetIntentData();
    }

    @Override
    public void onBackPressed() {
        //return to the calling activity with the set response in the intent, such as whether the login state has changed
        setResult(RESULT_OK, responseIntent);
        finish();
    }

    /**
     * This will retrieve the eventIndex to display, is only set when we originate from the MainActivity, where the int is added to the intent.
     */
    private void GetIntentData (){
        if(eventIndex == 0) {
            Intent intent = getIntent();
            eventIndex = intent.getIntExtra("eventIndex", 0);
        }
    }

    /**
     * This initialises UI variables and sets up various UI elements
     */
    private void InitUI (){
        //Set up toolbar and back button
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Check that we have been given an event that exists else return to the calling activity
        if(eventIndex < 0 || Events.eventInfos.size() <= eventIndex) {
            Log.e("events", "invlaid event index selected during InitUI(), eventIndex: " + eventIndex + ", size" + Events.eventInfos.size() + ". Ensure that you are not clearing/overwiting the events list while viewing an event.");
            onBackPressed();
            return;
        }

        //Link up variables with UI elements from the layout xml
        ((TextView) findViewById(R.id.eventTitle)).setText(Events.eventInfos.get(eventIndex).title_en);
        ((TextView)findViewById(R.id.eventDetail)).setText(Events.eventInfos.get(eventIndex).description_en);
        scrollView = findViewById(R.id.scrollView_event);
        posterProgress = findViewById(R.id.progressBar);
        posterImage = findViewById(R.id.eventPoster);
        posterMask = findViewById(R.id.posterMask);
        posterBg = findViewById(R.id.posterBg);
        registerButton = findViewById(R.id.registerButton);

        swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {    //This sets what function is called when we swipe down to refresh
            @Override
            public void onRefresh() {
                LoadEventImage(true);   //XXX fetch this specific event from the server again
            }
        });

        AddRegisterInfos();
        UpdateRegisterButton();

        LoadEventImage(false);
    }

    /**
     * Will load the poster for the event if it exists and handle progress bars etc. Can call this again to refresh the poster
     * @param isRefreshing
     */
    private void LoadEventImage (boolean isRefreshing)
    {
        //Image loading and masking. the posterMask is a small arrow image but we use the layout margin to add some transparent 'padding' to the top of the scrollview
        if(Events.eventInfos.get(eventIndex).poster_url.isEmpty() || !Requests.CheckConnection(getApplicationContext()))
        {
            //Hide the image and mask if there is no poster linked with the event or we have no internet
            if(!isRefreshing) {
                posterImage.setVisibility(View.GONE);
                posterMask.setVisibility(View.GONE);
                posterBg.setVisibility(View.GONE);
                posterProgress.setVisibility(View.GONE);
            }
        }
        else
        {
            if(!isRefreshing || posterImage.getDrawable() == null) {
                swipeRefreshLayout.setRefreshing(false);
                posterProgress.setVisibility(View.VISIBLE);
                posterImage.setVisibility(View.VISIBLE);
                posterBg.setVisibility(View.VISIBLE);

                //Will set height of mask to be the height of the constraint layout(parent of the posterImage) this is likely to be the closest match, to reduce snapping once the image is loaded
                    posterMask.post(new Runnable() {    //need to run one frame later so the layouts are correctly initialised so we can retrieve the height of the parent.
                        @Override
                        public void run() {
                            posterMask.setVisibility(View.VISIBLE);
                            ViewGroup.MarginLayoutParams maskParams = (ViewGroup.MarginLayoutParams) posterMask.getLayoutParams();
                            maskParams.topMargin = ((View) posterImage.getParent()).getMeasuredHeight() - posterMask.getHeight();
                            posterMask.requestLayout();
                        }
                    });
            }

            //generate URL for the poster
            StringBuilder posterUrl = new StringBuilder();
            posterUrl.append(Events.eventInfos.get(eventIndex).poster_url); //To show the banner instead change this variable
            if(posterUrl.charAt(0) == '/')
                posterUrl.deleteCharAt(0);
            posterUrl.insert(0, Settings.API_URL);

            //Log.e("request", "Event image url: " + posterUrl.toString());

            //Send a request for the image, note we can also use a NetworkImageView, but have less control then
            ImageRequest posterRequest = new ImageRequest(posterUrl.toString(),
                    new Response.Listener<Bitmap>() {
                        @Override
                        public void onResponse(final Bitmap bitmap) {
                            posterImage.setImageBitmap(bitmap); //Apply image downloaded

                            //Will adjust the empty space/mask at the top of the scrollview so we can see the whole image
                            posterImage.post(new Runnable() {
                                @Override
                                public void run() {
                                    //Set background height
                                    ViewGroup.LayoutParams bgParams = posterBg.getLayoutParams();
                                    bgParams.height = posterImage.getHeight();
                                    posterBg.setLayoutParams(bgParams);
                                    posterBg.setVisibility(View.VISIBLE);

                                    //Set mask height
                                    ViewGroup.MarginLayoutParams maskParams = (ViewGroup.MarginLayoutParams) posterMask.getLayoutParams();
                                    maskParams.topMargin = posterImage.getHeight() - posterMask.getLayoutParams().height;
                                    posterMask.requestLayout();
                                    posterMask.setVisibility(View.VISIBLE);

                                    posterProgress.setVisibility(View.GONE);
                                    swipeRefreshLayout.setRefreshing(false);
                                }
                            });
                        }
                    }, 0, 0, ImageView.ScaleType.CENTER_INSIDE, Bitmap.Config.ARGB_8888,
                    new Response.ErrorListener() {
                        public void onErrorResponse(VolleyError error) {
                            Snackbar.make(posterImage, "Error loading image", Snackbar.LENGTH_SHORT).show();
                            posterProgress.setVisibility(View.GONE);
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    });

            if(!Requests.SendRequest(posterRequest, getApplicationContext())){
                //Only enter here if the request was not sent, usually because of missing internet
                posterProgress.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
            }
        }

        swipeRefreshLayout.setRefreshing(false);
    }

    /**
     * This creates the short list under Infos, to display specific details. We get which details to dispkay from the EventInfo
     */
    private void AddRegisterInfos()
    {
        LinearLayout linear = findViewById(R.id.register_details_list);
        linear.removeAllViews();

        ArrayList<String[]> infos = Events.eventInfos.get(eventIndex).GetInfos();
        LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
        for (int i = 0; i < infos.size(); i++) {
            //Create a view from the xml and then add it as a child of the listview
            LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.list_item_keyval, linear, false);
            ((TextView) layout.findViewById(R.id.keyField  )).setText(infos.get(i)[0]);
            ((TextView) layout.findViewById(R.id.valueField)).setText(infos.get(i)[1]);

            linear.addView(layout);
        }
    }

    /**
     * Will send a request to register the user for the event the activity is showing
     * @param view
     */
    public void RegisterForEvent(View view)
    {
        if(!Requests.CheckConnection(getApplicationContext())) {
            Snackbar.make(view, "Requires Internet", Snackbar.LENGTH_LONG).show();
            return;
        }

        //Redirect to the login page first
        if(!Settings.IsLoggedIn(getApplicationContext())){
            Intent intent = new Intent(this, LoginActivity.class);
            intent.putExtra("cause", "register_event");
            startActivityForResult(intent, 0);
            return;
        }

        final int registerEventIndex = eventIndex;  //declare final so it does not change
        String url = Settings.API_URL + "eventsignups";

        StringRequest request = new StringRequest(Request.Method.POST, url,null, null)
        {
            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) { //Note: the parseNetworkResponse is only called if the response was successful (codes 2xx), else parseNetworkError is called.
                if(response != null) {
                    Log.e("request", "status Code: " + response.statusCode);

                    try {
                        Log.e("request", new String(response.data));
                        JSONObject json = new JSONObject(new String(response.data));
                        Events.eventInfos.get(registerEventIndex).AddSignup(json);  //Register signup

                        //We need to fetch signups again as the response for registering for an event is not a complete signup object
                        Requests.FetchEventSignups(getApplicationContext(), new Requests.OnDataReceivedCallback() {
                            @Override
                            public void OnDataReceived() {
                                UpdateRegisterButton();
                            }
                        });

                        //Interpret notification to show from the signup
                        String notification = "";
                        if(Events.eventInfos.get(eventIndex).accepted) {
                            if(Events.eventInfos.get(eventIndex).confirmed)
                                notification = "Successfully Registered";
                            else
                                notification = "Registered, please confirm with mail";
                        } else
                            notification = "Added to Waiting List";
                        Snackbar.make(scrollView, notification, Snackbar.LENGTH_LONG).show();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    Log.e("request", "Request returned null response.");
                    Snackbar.make(scrollView, "Error occured, please try again", Snackbar.LENGTH_SHORT).show();
                }
                return super.parseNetworkResponse(response);
            }

            @Override
            protected VolleyError parseNetworkError(final VolleyError volleyError) {  //see comments at parseNetworkResponse()
                if(volleyError != null && volleyError.networkResponse != null) {
                    Log.e("request", "status code: " + volleyError.networkResponse.statusCode + "\n" + new String(volleyError.networkResponse.data));
                    if(volleyError.networkResponse.statusCode == 422) {
                        Snackbar.make(scrollView, "Already Registered", Snackbar.LENGTH_SHORT).show();
                    }
                }
                else
                    Log.e("request", "Request returned null response.");

                return super.parseNetworkError(volleyError);
            }

            @Override
            public Map<String, String> getHeaders()  {
                Map<String,String> headers = new HashMap<String, String>();

                // Add basic auth with token
                String credentials = Settings.GetToken(getApplicationContext()) + ":";
                String auth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
                headers.put("Authorization", auth);
                return headers;
            }

            @Override
            protected Map<String, String> getParams()  {
                Map<String, String> params = new HashMap<String, String>();
                params.put("event", Events.eventInfos.get(eventIndex)._id);
                params.put("user", UserInfo.current._id);
                return params;
            }
        };

        Requests.SendRequest(request, getApplicationContext());
    }

    private void UpdateRegisterButton() {
        if (Events.eventInfos.get(eventIndex).IsSignedUp()) {
            registerButton.setEnabled(false);
            registerButton.setText("Already Registered");
        } else {
            registerButton.setEnabled(true);
            registerButton.setText("Register");
        }
    }

    public void ScrollToTop (View view) {
        scrollView.fullScroll(ScrollView.FOCUS_UP);
    }

    public void ScrollToBottom (View view) {
        scrollView.fullScroll(ScrollView.FOCUS_DOWN);
    }
}
