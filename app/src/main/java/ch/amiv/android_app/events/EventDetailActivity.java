package ch.amiv.android_app.events;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import ch.amiv.android_app.R;
import ch.amiv.android_app.core.LoginActivity;
import ch.amiv.android_app.core.Request;
import ch.amiv.android_app.core.Settings;
import ch.amiv.android_app.core.UserInfo;
import ch.amiv.android_app.util.PersistentStorage;
import ch.amiv.android_app.util.Util;

/**
 * The activity/screen used for showing a selected event in detail.
 * This mainly displays stored info about the event, eg description and also fetches more such as images. Also handles registering for and event and the possible outcomes
 *
 * To launch this activity, you need to provide an event to view. You need to provide this as an intent extra (use intent.putExtra()):
 * - Provide eventGroup == -1, eventGroup == index in Events.eventInfos (unsorted list)
 * - Provide eventGroup == group in Events.sortedEventInfos, eventGroup == index in Events.sortedEventInfos (sorted list)
 * - Provide eventId == any valid event id
 *
 * If the event is not found, the activity finishes and returns to the calling activity.
 */
public class EventDetailActivity extends AppCompatActivity {
    /**
     * A constant class to easily set extras for launching the EventDetailActivity
     */
    public static final class LauncherExtras {
        public static final String EVENT_GROUP = "eventGroup";
        public static final String EVENT_INDEX = "eventIndex";
        public static final String EVENT_ID = "eventId";
        public static final String LOAD_EVENTS = "loadEvents";
    }

    private EventInfo event;

    private ImageView posterImage;
    private ImageView posterMask;
    private View posterBg;
    private ProgressBar posterProgress; //Note:posterProgress bar is specific for the image loading, whereas swipeRefreshLayout (swipe down to refresh) is for reloading the whole event
                                        //There should be a progress bar for each image as these can take longer to load
    private ScrollView scrollView;
    private Button registerButton;

    private Intent responseIntent = new Intent();   //used for sending back a result to the calling activity, used for telling MainActivity if the login has changed

    private SwipeRefreshLayout swipeRefreshLayout;
    private Request.OnDataReceivedCallback cancelRefreshCallback = new Request.OnDataReceivedCallback() {
        @Override
        public void OnDataReceived() {
            swipeRefreshLayout.setRefreshing(false);
        }
    };

    public Request.OnDataReceivedCallback onEventsListUpdatedCallback = new Request.OnDataReceivedCallback() {
        @Override
        public void OnDataReceived() {
            SetUIDirty(true, false);
            Request.FetchEventSignups(getApplicationContext(), onSignupsUpdatedCallback, cancelRefreshCallback, event._id);
            LoadEventImage(true);
        }
    };

    private Request.OnDataReceivedCallback onSignupsUpdatedCallback = new Request.OnDataReceivedCallback() {
        @Override
        public void OnDataReceived() {
            SetUIDirty(true, true);
            swipeRefreshLayout.setRefreshing(false);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        GetIntentData();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.events_detail);
        InitUI();
    }

    //When returning from login activity refresh data and register the user, then apply the response to the UI
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0 && resultCode == RESULT_OK) {
            // If we are returning from the login activity and have had a successfully login, refresh the user info and login UI
            boolean refreshLogin = data.getBooleanExtra("login_success", false);
            if(refreshLogin && Settings.IsLoggedIn(getApplicationContext()))
            {
                if(Settings.IsEmailOnlyLogin(getApplicationContext()))
                    Snackbar.make(posterImage, R.string.requires_login, Snackbar.LENGTH_SHORT).show();
                else {
                    Request.FetchEventSignups(getApplicationContext(), new Request.OnDataReceivedCallback() {
                        @Override
                        public void OnDataReceived() {
                            UpdateRegisterButton();
                        }
                    }, null, "");
                    RegisterForEvent(null);
                }

                responseIntent.putExtra("login_success", refreshLogin);
                ScrollToBottom(null);
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
        ReturnToCallingActivity(true);
    }

    private void ReturnToCallingActivity (boolean success){
        setResult(success ? RESULT_OK : RESULT_CANCELED, responseIntent);
        finish();
    }

    /**
     * This will retrieve the eventIndexes to display, is only set when we originate from the MainActivity, where the int is added to the intent.
     */
    private void GetIntentData (){
        Intent intent = getIntent();

        if(intent.getBooleanExtra(LauncherExtras.LOAD_EVENTS, false))
            PersistentStorage.LoadEvents(getApplicationContext());

        if(intent.hasExtra(LauncherExtras.EVENT_GROUP) && intent.hasExtra(LauncherExtras.EVENT_INDEX))
        {
            int eventGroup = intent.getIntExtra(LauncherExtras.EVENT_GROUP, 0);
            int eventIndex = intent.getIntExtra(LauncherExtras.EVENT_INDEX, 0);
            if(eventGroup == -1)
                event = Events.eventInfos.get(eventIndex);
            else
                event = Events.sortedEventInfos.get(eventGroup).get(eventIndex);

            if (event == null)
                Log.e("events", "invalid event index selected during InitUI(), (groupIndex, eventIndex): (" + eventGroup + "," + eventIndex + "), total event size" + Events.eventInfos.size() + ". Ensure that you are not clearing/overwriting the events list while viewing an event. Returning to calling activity...");
        }
        else if(intent.hasExtra(LauncherExtras.EVENT_ID))
        {
            event = Events.GetById(intent.getStringExtra(LauncherExtras.EVENT_ID));

            if(event == null)
                Log.e("events", "No event found from eventId=" + intent.getStringExtra(LauncherExtras.EVENT_ID) + " in intent, have you used intent.putStringExtra. Returning to calling activity...");
        }

        if(event == null)
            ReturnToCallingActivity(false);
    }

    /**
     * This initialises UI variables and sets up various UI elements
     */
    private void InitUI (){
        Util.SetupToolbar(this, true);

        //Check that we have been given an event that exists else return to the calling activity
        if(event == null) return;

        //Link up variables with UI elements from the layout xml
        scrollView = findViewById(R.id.scrollView);
        posterProgress = findViewById(R.id.progressBar);
        posterImage = findViewById(R.id.eventPoster);
        posterMask = findViewById(R.id.posterMask);
        posterBg = findViewById(R.id.posterBg);
        registerButton = findViewById(R.id.registerButton);

        InitSwipeRefreshUI();

        SetUIDirty(false, false);
    }

    //Setup swipe down to refresh, adds the amiv logo and rotate animation
    private void InitSwipeRefreshUI()
    {
        //Set on refresh functionality
        swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {    //This sets what function is called when we swipe down to refresh
            @Override
            public void onRefresh() {
                OnSwipeRefreshed();

                try {
                    Field f = swipeRefreshLayout.getClass().getDeclaredField("mCircleView");
                    f.setAccessible(true);
                    ImageView img = (ImageView)f.get(swipeRefreshLayout);

                    RotateAnimation rotate = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                    rotate.setRepeatMode(Animation.INFINITE);
                    rotate.setDuration(1000);
                    rotate.setInterpolator(new LinearInterpolator());
                    img.startAnimation(rotate);
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        });

        //Set Image of swipe refresh
        try {
            Field f = swipeRefreshLayout.getClass().getDeclaredField("mCircleView");
            f.setAccessible(true);
            ImageView img = (ImageView)f.get(swipeRefreshLayout);
            img.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_amiv_logo_icon_bordered, null));
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void OnSwipeRefreshed(){
        if(event == null) return;

        Request.FetchEventList(getApplicationContext(), onEventsListUpdatedCallback, cancelRefreshCallback, event._id);
    }

    public void SetUIDirty(boolean isRefreshing, boolean signupUpdated)
    {
        if(!signupUpdated) {
            ((TextView) findViewById(R.id.eventTitle)).setText(event.GetTitle(getResources()));
            ((TextView) findViewById(R.id.eventDetail)).setText(event.GetDescription(getResources()));
            LoadEventImage(isRefreshing);
            AddRegisterInfos();
        }

        UpdateRegisterButton();
    }

    /**
     * Will load the poster for the event if it exists and handle progress bars etc. Can call this again to refresh the poster
     */
    private void LoadEventImage (boolean isRefreshing)
    {
        //Image loading and masking. the posterMask is a small arrow image but we use the layout margin to add some transparent 'padding' to the top of the scrollview
        if(event.poster_url.isEmpty() || !Request.CheckConnection(getApplicationContext()))
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

            //Send a request for the image, note we can also use a NetworkImageView, but have less control then
            ImageRequest posterRequest = new ImageRequest(event.GetPosterUrl(),
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
                                }
                            });
                        }
                    }, 0, 0, ImageView.ScaleType.CENTER_INSIDE, Bitmap.Config.ARGB_8888,
                    new Response.ErrorListener() {
                        public void onErrorResponse(VolleyError error) {
                            Snackbar.make(posterImage, R.string.error_image_load, Snackbar.LENGTH_SHORT).show();
                            posterProgress.setVisibility(View.GONE);
                        }
                    });

            if(!Request.SendRequest(posterRequest, getApplicationContext())){
                //Only enter here if the request was not sent, usually because of missing internet
                posterProgress.setVisibility(View.GONE);
            }
        }
    }

    /**
     * This creates the short list under Infos, to display specific details. We get which details to display from the EventInfo
     */
    private void AddRegisterInfos()
    {
        LinearLayout linear = findViewById(R.id.register_details_list);
        linear.removeAllViews();

        ArrayList<String[]> infos = event.GetInfos(getResources());
        LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
        for (int i = 0; i < infos.size(); i++) {
            //Create a view from the xml and then add it as a child of the listview
            LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.core_main_list_item_keyval, linear, false);
            ((TextView) layout.findViewById(R.id.keyField  )).setText(infos.get(i)[0]);
            ((TextView) layout.findViewById(R.id.valueField)).setText(infos.get(i)[1]);

            linear.addView(layout);
        }
    }

    /**
     * Will send a request to register the user for the event the activity is showing
     */
    public void RegisterForEvent(View view)
    {
        if(!Request.CheckConnection(getApplicationContext())) {
            Snackbar.make(view, R.string.requires_internet, Snackbar.LENGTH_LONG).show();
            return;
        }

        //Redirect to the login page first
        if(!Settings.HasToken(getApplicationContext()) && !event.allow_email_signup){
            Intent intent = new Intent(this, LoginActivity.class);
            intent.putExtra("cause", "register_event");
            startActivityForResult(intent, 0);
            return;
        }

        String url = Settings.API_URL + "eventsignups";

        StringRequest request = new StringRequest(com.android.volley.Request.Method.POST, url,null, null)
        {
            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) { //Note: the parseNetworkResponse is only called if the response was successful (codes 2xx), else parseNetworkError is called.
                if(response != null) {
                    Log.e("request", "status Code: " + response.statusCode);

                    try {
                        Log.e("request", new String(response.data));
                        JSONObject json = new JSONObject(new String(response.data));
                        event.AddSignup(json);  //Register signup

                        //Fetch event signup object again for this event id
                        Request.FetchEventSignups(getApplicationContext(), new Request.OnDataReceivedCallback() {
                            @Override
                            public void OnDataReceived() {
                                UpdateRegisterButton();
                            }
                        }, null, event._id);

                        //Interpret notification to show from the signup
                        int notification = 0;
                        if(event.accepted) {
                            if(event.confirmed)
                                notification = R.string.register_success;
                            else
                                notification = R.string.register_success_confirm_required;
                            Settings.Vibrate(Settings.VibrateTime.NORMAL, getApplicationContext());
                        } else
                            notification = R.string.added_to_waiting_list;
                        Snackbar.make(scrollView, notification, Snackbar.LENGTH_LONG).show();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    Log.e("request", "Request returned null response.");
                    Snackbar.make(scrollView, R.string.snack_error_retry, Snackbar.LENGTH_SHORT).show();
                }
                return super.parseNetworkResponse(response);
            }

            @Override
            protected VolleyError parseNetworkError(final VolleyError volleyError) {  //see comments at parseNetworkResponse()
                if(volleyError != null && volleyError.networkResponse != null) {
                    Log.e("request", "status code: " + volleyError.networkResponse.statusCode + "\n" + new String(volleyError.networkResponse.data));
                    if(volleyError.networkResponse.statusCode == 422) {
                        Snackbar.make(scrollView, R.string.already_registered, Snackbar.LENGTH_SHORT).show();
                    }
                    else if(volleyError.networkResponse.statusCode == 403)
                        Snackbar.make(scrollView, R.string.not_authorised, Snackbar.LENGTH_SHORT).show();
                }
                else
                    Log.e("request", "Request returned null response.");

                return super.parseNetworkError(volleyError);
            }

            @Override
            public Map<String, String> getHeaders()  {
                Map<String,String> headers = new HashMap<String, String>();

                // Add basic auth with token
                if(Settings.HasToken(getApplicationContext())) {
                    String credentials = Settings.GetToken(getApplicationContext()) + ":";
                    String auth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
                    headers.put("Authorization", auth);
                }
                return headers;
            }

            @Override
            protected Map<String, String> getParams()  {
                Map<String, String> params = new HashMap<String, String>();
                params.put("event", event._id);
                if(Settings.IsEmailOnlyLogin(getApplicationContext()))
                    params.put("email", UserInfo.current.email);
                else
                    params.put("user", UserInfo.current._id);

                //encode signup form values
                /* Add when fully tested XXX
                SharedPreferences prefs = getSharedPreferences(Settings.SHARED_PREFS_KEY, MODE_PRIVATE);
                Gson gson = new Gson();
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("food_preference", prefs.getString(getResources().getString(R.string.pref_food_key), ""));
                jsonObject.addProperty("sbb_abo", prefs.getString(getResources().getString(R.string.pref_sbb_key), ""));
                params.put("additional_fields", jsonObject.getAsString());
                */
                return params;
            }
        };

        Request.SendRequest(request, getApplicationContext());
    }

    /**
     * Will set the register button appearance accordingly to the dates of the event and whether the user is signed up
     */
    private void UpdateRegisterButton() {

        if (event.IsSignedUp()) {
            registerButton.setEnabled(false);
            registerButton.setText(R.string.already_registered);
            return;
        }

        Date today = Calendar.getInstance().getTime();
        if(event.time_register_start.before(today))
        {
            if(event.time_register_end.after(today))
            {
                registerButton.setEnabled(true);
                if(Settings.IsEmailOnlyLogin(getApplicationContext()) && !event.allow_email_signup)
                    registerButton.setText(R.string.requires_login);
                else
                    registerButton.setText(R.string.register_title);
            }
            else
            {
                registerButton.setEnabled(false);
                registerButton.setText(R.string.registration_closed);
            }
        }
        else {
            registerButton.setEnabled(false);
            registerButton.setText(R.string.register_soon);
        }
    }

    public void ScrollToTop (View view) {
        scrollView.fullScroll(ScrollView.FOCUS_UP);
    }

    public void ScrollToBottom (View view) {
        scrollView.fullScroll(ScrollView.FOCUS_DOWN);
    }
}
