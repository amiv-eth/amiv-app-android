package ch.amiv.android_app;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.ListFragment;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
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
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    NavigationView navigationView;
    TextView drawer_title;
    TextView drawer_subtitle;

    RecyclerView mRecylerView;
    RecyclerView.Adapter mRecylcerAdaper;
    RecyclerView.LayoutManager mRecyclerLayoutAdapter;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    return true;
                case R.id.navigation_dashboard:
                    return true;
                case R.id.navigation_notifications:
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

        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        drawer_title = navigationView.getHeaderView(0).findViewById(R.id.drawer_user_title);
        drawer_subtitle = navigationView.getHeaderView(0).findViewById(R.id.drawer_user_subtitle);

        BottomNavigationView navigation = findViewById(R.id.bottomNav);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        //Recyclerview
        InitialisePageView();

        new Settings(getApplicationContext());
        if(Settings.IsLoggedIn() && UserInfo.current == null)
            FetchUserData();
        else
            SetLoginUIDirty();
    }


    private void InitialisePageView() {
        MyAdapter pagerAdapter = new MyAdapter(getSupportFragmentManager());
        ViewPager viewPager = findViewById(R.id.viewPager);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setPageTransformer(true, new DepthPageTransformer());

        /*mRecylerView = findViewById(R.id.recyclerView);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecylerView.setHasFixedSize(true);

        // use a linear layout manager
        mRecyclerLayoutAdapter = new LinearLayoutManager(this);
        mRecylerView.setLayoutManager(mRecyclerLayoutAdapter);

        // specify an adapter (see also next example)
        mRecylcerAdaper = new MemberListAdapter(EventDatabase.instance.members, EventDatabase.instance.stats, EventDatabase.instance.eventData.GetInfosAsKeyValuePairs());
        mRecylerView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(this, R.anim.layout_anim_falldown));
        mRecylerView.setAdapter(mRecylcerAdaper);*/
    }
//endregion

    @Override
    protected void onResume() {
        super.onResume();

        // If we are returning from the login activity and have had a successfuly login, refresh the user info and login UI
        Intent intent = getIntent();
        boolean refreshLogin = intent.getBooleanExtra("login_sucess", false);
        if(refreshLogin && Settings.IsLoggedIn()){
            FetchUserData();
        }
    }

//region Login

    /**
     * Will fetch the user from the api if we have an access token. ie Token -> User. Data is stored in the current userinfo (UserInfo.current). Overwrites the current user info if it exists
     */
    private void FetchUserData()
    {
        if(!Settings.IsLoggedIn())
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
                        navigationView.post(new Runnable() {    //Run updating UI on UI thread
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
        if(Settings.IsLoggedIn()) {
            if(UserInfo.current == null)
                FetchUserData();
            else {
                navigationView.getMenu().findItem(R.id.nav_login).setTitle("Logout");
                drawer_title.setText(UserInfo.current.firstname + " " + UserInfo.current.lastname);
                drawer_subtitle.setText(UserInfo.current.email);
            }
        }
        else
        {
            navigationView.getMenu().findItem(R.id.nav_login).setTitle("Login");
            drawer_title.setText("Not Logged In");
            drawer_subtitle.setText("");
        }
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

    //=====TOOLBAR=====
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.toolbar_menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_favorite) {
            Toast.makeText(MainActivity.this, "Action clicked", Toast.LENGTH_LONG).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    //=====END OF TOOLBAR=============


    //========START OF DRAWER=========
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_login) {
            if(Settings.IsLoggedIn())
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
    //=====END OF DRAWER==================

    //=====START OF PAGEVIEW==============
    public static class MyAdapter extends FragmentPagerAdapter {
        public MyAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public Fragment getItem(int position) {
            return ArrayListFragment.newInstance(position);
        }
    }

    /**
     * An examply fragment, the central view in MainActivity, for showing a list, should be replaced by a standard fragment with a custom recyclerView, create one different class for different views
     */
    public static class ArrayListFragment extends ListFragment {
        int mNum;

        /**
         * Create a new instance of CountingFragment, providing "num"
         * as an argument.
         */
        static ArrayListFragment newInstance(int num) {
            ArrayListFragment f = new ArrayListFragment();

            // Supply num input as an argument.
            Bundle args = new Bundle();
            args.putInt("num", num);
            f.setArguments(args);

            return f;
        }

        /**
         * When creating, retrieve this instance's number from its arguments.
         */
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mNum = getArguments() != null ? getArguments().getInt("num") : 1;
        }

        /**
         * The Fragment's UI is just a simple text view showing its
         * instance number.
         */
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.main_page1, container, false);
            View tv = v.findViewById(R.id.text);
            ((TextView)tv).setText("Fragment #" + mNum);
            return v;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            setListAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, new String[]{"A", "B", "C"}));
        }

        @Override
        public void onListItemClick(ListView l, View v, int position, long id) {
            Log.i("FragmentList", "Item clicked: " + id);
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
    //=====END OF PAGEVIEW================
}
