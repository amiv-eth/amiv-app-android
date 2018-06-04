package ch.amiv.android_app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    NavigationView drawerNavigation;
    TextView drawer_title;
    TextView drawer_subtitle;

    BottomNavigationView bottomNavigation;

    ViewPager viewPager;
    PagerAdapter pagerAdapter;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.bottom_nav_home:
                    viewPager.setCurrentItem(0);
                    return true;
                case R.id.bottom_nav_blitz:
                    viewPager.setCurrentItem(1);
                    return true;
                case R.id.bottom_nav_events:
                    viewPager.setCurrentItem(2);
                    return true;
            }
            return false;
        }
    };

//region Initialisation
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        drawerNavigation = findViewById(R.id.nav_view);
        drawerNavigation.setNavigationItemSelectedListener(this);
        drawer_title = drawerNavigation.getHeaderView(0).findViewById(R.id.drawer_user_title);
        drawer_subtitle = drawerNavigation.getHeaderView(0).findViewById(R.id.drawer_user_subtitle);

        bottomNavigation = findViewById(R.id.bottomNav);
        bottomNavigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        //Recyclerview
        InitialisePageView();

        new Settings(getApplicationContext());
        if(Settings.IsLoggedIn(getApplicationContext()) && UserInfo.current == null)
            FetchUserData();
        else
            SetLoginUIDirty();

        FetchEventList();
    }


    private void InitialisePageView() {
        pagerAdapter = new PagerAdapter(getSupportFragmentManager());
        viewPager = findViewById(R.id.viewPager);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setPageTransformer(true, new DepthPageTransformer());

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                //need to convert index to resource id
                if(position == 0)
                    position = R.id.bottom_nav_home;
                else if (position == 1)
                    position = R.id.bottom_nav_blitz;
                else if (position == 2)
                    position = R.id.bottom_nav_events;

                bottomNavigation.setSelectedItemId(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }
//endregion

    @Override
    protected void onResume() {
        super.onResume();

        // If we are returning from the login activity and have had a successfuly login, refresh the user info and login UI
        Intent intent = getIntent();
        boolean refreshLogin = intent.getBooleanExtra("login_sucess", false);
        if(refreshLogin && Settings.IsLoggedIn(getApplicationContext())){
            FetchUserData();
        }
    }

//region -   ====Login====

    /**
     * Will fetch the user from the api if we have an access token. ie Token -> User. Data is stored in the current userinfo (UserInfo.current). Overwrites the current user info if it exists
     */
    private void FetchUserData()
    {
        if(!Settings.IsLoggedIn(getApplicationContext()))
            return;

        //Do request Token->User
        String url = Settings.API_URL + "sessions/" + Settings.GetToken(getApplicationContext()) + "?embedded={\"user\":1}";
        StringRequest request = new StringRequest(Request.Method.GET, url,null, null)
        {
            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) { //Note: the parseNetworkResponse is only called if the response was successful (codes 2xx), else parseNetworkError is called.
                if(response != null) {
                    Log.e("request", "status Code: " + response.statusCode);

                    try {
                        JSONObject json = new JSONObject(new String(response.data)).getJSONObject("user");
                        UserInfo user = new UserInfo(json);
                        user.SetAsCurrent();

                        //Update UI in nav drawer
                        drawerNavigation.post(new Runnable() {    //Run updating UI on UI thread
                            public void run() {
                                SetLoginUIDirty();
                            }});

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
        };

        Requests.SendRequest(request, getApplicationContext());
    }

    /**
     * Log the user out, delete token. Will not log the user out of the device with the API as this is used with other amiv services as well
     */
    public void LogoutUser()
    {
        Settings.SetToken("", getApplicationContext());
        UserInfo.current = null;
        SetLoginUIDirty();
        System.gc();//run garbage collector explicitly to clean up user data
    }

    /**
     * Will refresh all login related UI, use this when the user logs in/out
     */
    public void SetLoginUIDirty ()
    {
        if(Settings.IsLoggedIn(getApplicationContext())) {
            if(UserInfo.current == null)
                FetchUserData();
            else {
                drawerNavigation.getMenu().findItem(R.id.nav_login).setTitle("Logout");
                drawer_title.setText(UserInfo.current.firstname + " " + UserInfo.current.lastname);
                drawer_subtitle.setText(UserInfo.current.email);
            }
        }
        else
        {
            drawerNavigation.getMenu().findItem(R.id.nav_login).setTitle("Login");
            drawer_title.setText("Not Logged In");
            drawer_subtitle.setText("");
        }
    }
//endregion

//region -   =====Events======

    /**
     * Will fetch the list of events from the server, note does not require an access token.
     */
    public void FetchEventList()
    {
        String url = Settings.API_URL + "events";
        StringRequest request = new StringRequest(Request.Method.GET, url,null, null)
        {
            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) { //Note: the parseNetworkResponse is only called if the response was successful (codes 2xx), else parseNetworkError is called.
                if(response != null) {
                    Log.e("request", "status Code: " + response.statusCode);

                    try {
                        final JSONArray eventArrayJson = new JSONObject(new String(response.data)).getJSONArray("_items");

                        //Update events on main thread
                        Handler mainHandler = new Handler(getApplicationContext().getMainLooper());
                        Runnable myRunnable = new Runnable() {
                            @Override
                            public void run() {
                                Events.UpdateEventInfos(eventArrayJson);
                                SetEventUIDirty();
                                pagerAdapter.currentFragment.RefreshList();
                            }
                        };
                        mainHandler.post(myRunnable);

                        Log.e("request", eventArrayJson.toString());
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
                if(Settings.IsLoggedIn(getApplicationContext())) {
                    Map<String,String> headers = new HashMap<String, String>();

                    String credentials = Settings.GetToken(getApplicationContext()) + ":";
                    String auth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
                    headers.put("Authorization", auth);

                    return headers;
                }

                return super.getHeaders();
            }
        };

        Requests.SendRequest(request, getApplicationContext());
    }

    /**
     * Will refresh the UI using event data
     */
    public void SetEventUIDirty()
    {
        //update the recycler view of the event page in the pageview


    }
//endregion

    //=====Changing Activity====
    public void StartSettingsActivity() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    public void StartLoginActivity() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    //region =====TOOLBAR=====
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.toolbar_menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_favorite) {
            FetchEventList();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    //endregion =====END OF TOOLBAR=============


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
    //region ========START OF DRAWER=========

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_login) {
            if(Settings.IsLoggedIn(getApplicationContext()))
                LogoutUser();
            else
                StartLoginActivity();
        } else if (id == R.id.nav_checkin) {

        } else if (id == R.id.nav_settings) {
            StartSettingsActivity();
        } else if (id == R.id.nav_dev_log) {

        } else if (id == R.id.nav_dev_request) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
    //endregion =====END OF DRAWER==================

    //region =====START OF PAGEVIEW==============

    /**
     * This will handle changing between the pages
     */
    public class PagerAdapter extends FragmentPagerAdapter {
        ListFragment currentFragment;

        public PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public Fragment getItem(int position) {
            return ListFragment.NewInstance(position);
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            if (currentFragment != object) {
                currentFragment = ((ListFragment) object);
            }

            super.setPrimaryItem(container, position, object);
        }
    }

    /**
     * Use to *animate* the transition nicely when swiping between the pages of the pagerView
     */
    public class DepthPageTransformer implements ViewPager.PageTransformer {
        private static final float MIN_SCALE = 0.75f;

        public void transformPage(@NonNull View view, float position) {     //position defines how far through the transition we are, -1= fully left, +1=fully right, 0= fully centered
            int pageWidth = view.getWidth();

            if (position < -1) { // This page is way off-screen to the left.
                view.setAlpha(0);

            } else if (position <= 0) { // Use the default slide transition when moving to the left page
                view.setAlpha(1);
                view.setTranslationX(0);
                view.setScaleX(1);
                view.setScaleY(1);

            } else if (position <= 1) {  // Fade the page out.
                view.setAlpha(1 - position);

                // Counteract the default slide transition
                view.setTranslationX(pageWidth * -position);

                // Scale the page down (between MIN_SCALE and 1)
                float scaleFactor = MIN_SCALE + (1 - MIN_SCALE) * (1 - Math.abs(position));
                view.setScaleX(scaleFactor);
                view.setScaleY(scaleFactor);
            } else {  // This page is way off-screen to the right.
                view.setAlpha(0);
            }
        }
    }
    //endregion =====END OF PAGEVIEW================
}
