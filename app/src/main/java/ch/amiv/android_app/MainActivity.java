package ch.amiv.android_app;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
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

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private NavigationView drawerNavigation;
    private TextView drawer_title;
    private TextView drawer_subtitle;

    private BottomNavigationView bottomNavigation;

    private ViewPager viewPager;
    private PagerAdapter pagerAdapter;

    public Requests.OnDataReceivedCallback onEventsListUpdatedCallback = new Requests.OnDataReceivedCallback() {
        @Override
        public void OnDataReceived() {
            Requests.FetchEventSignups(getApplicationContext(), onSignupsUpdatedCallback);
            pagerAdapter.RefreshCurrentList(true);
        }
    };

    private Requests.OnDataReceivedCallback onSignupsUpdatedCallback = new Requests.OnDataReceivedCallback() {
        @Override
        public void OnDataReceived() {
            pagerAdapter.RefreshCurrentList(false);
        }
    };

    /**
     * Handle what should happen when the bottom nav buttons are pressed, will change the page of the viewpager
     */
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.bottom_nav_home:
                    viewPager.setCurrentItem(0);
                    return true;
                /*case R.id.bottom_nav_blitz:
                    viewPager.setCurrentItem(1);
                    return true;*/
                case R.id.bottom_nav_events:
                    viewPager.setCurrentItem(1);
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

        InitialisePageView();

        new Settings(getApplicationContext()); //creates the settings instance, so we can store/retrieve shared preferences
        //fetch the user info if we are logged in, there exists a token from the previous session, should be cached.
        if(Settings.IsLoggedIn(getApplicationContext()) && (UserInfo.current == null || UserInfo.current.nethz.isEmpty()))
        {
            Requests.FetchUserData(getApplicationContext(), drawerNavigation, new Requests.OnDataReceivedCallback() {
                @Override
                public void OnDataReceived() {
                    SetLoginUIDirty();
                    //Fetch the event list and then the signup information for the current user
                    Requests.FetchEventList(getApplicationContext(), onEventsListUpdatedCallback, null);
                }
            });
        }

        SetLoginUIDirty();
    }

    /**
     * Creates the page view for swiping sideways to switch pages
     */
    private void InitialisePageView() {
        pagerAdapter = new PagerAdapter(getSupportFragmentManager());
        viewPager = findViewById(R.id.viewPager);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setPageTransformer(true, new DepthPageTransformer()); //used for animating

        //set for the bottom nav to be updated when we swipe to change the page
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }

            @Override
            public void onPageSelected(int position) {
                //need to convert index to resource id
                if(position == 0)
                    position = R.id.bottom_nav_home;
                /*else if (position == 1)
                    position = R.id.bottom_nav_blitz;*/
                else if (position == 1)
                    position = R.id.bottom_nav_events;

                bottomNavigation.setSelectedItemId(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) { }
        });
    }
//endregion

//region -   ====Login====
    /**
     * Log the user out, delete token. Will not log the user out of the device with the API as this is used with other amiv services as well
     */
    public void LogoutUser()
    {
        //delete session at the server and then clear the token
        Requests.DeleteCurrentSession(getApplicationContext());

        UserInfo.current = null;
        Events.ClearSignups();
        pagerAdapter.RefreshCurrentList(true);
        SetLoginUIDirty();
        System.gc();//run garbage collector explicitly to clean up user data

        Requests.FetchEventList(getApplicationContext(), onEventsListUpdatedCallback, null);
    }

    /**
     * Will refresh all login related UI, use this when the user logs in/out
     */
    public void SetLoginUIDirty ()
    {
        if(Settings.IsLoggedIn(getApplicationContext())) {
            drawerNavigation.getMenu().findItem(R.id.nav_login)
                .setTitle("Logout")
                .setChecked(false);
            if(UserInfo.current != null)
            {
                drawer_title.setText(UserInfo.current.firstname + " " + UserInfo.current.lastname);
                drawer_subtitle.setText(UserInfo.current.email);
            }
        }
        else
        {
            drawerNavigation.getMenu().findItem(R.id.nav_login)
                .setTitle("Login")
                .setChecked(false);
            drawer_title.setText("Not Logged In");
            drawer_subtitle.setText("");
        }
    }
//endregion

//region -   =====Events======

//endregion

    //=====Changing Activity====
    public void StartSettingsActivity() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    public void StartLoginActivity() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivityForResult(intent, 0);
    }

    public void StartEventDetailActivity(int eventIndex)
    {
        Intent intent = new Intent(this, EventDetailActivity.class);
        intent.putExtra("eventIndex", eventIndex);
        startActivityForResult(intent, 0);
    }

    /**
     * Here we can interpret the result of the login/event detail activity, if the login was successful or not, then update accordingly
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0) {
            if (resultCode == RESULT_OK) {
                // If we are returning from the login activity and have had a successfuly login, refresh the user info and login UI
                boolean refreshLogin = data.getBooleanExtra("login_success", false);
                if(refreshLogin && Settings.IsLoggedIn(getApplicationContext()))
                {
                    Requests.FetchUserData(getApplicationContext(), drawerNavigation, new Requests.OnDataReceivedCallback() {
                        @Override
                        public void OnDataReceived() {
                            SetLoginUIDirty();
                            //Update events and signups with the new userinfo
                            if(Events.eventInfos.size() > 0)
                                Requests.FetchEventSignups(getApplicationContext(), onSignupsUpdatedCallback);
                            else
                                Requests.FetchEventList(getApplicationContext(), onEventsListUpdatedCallback, null);
                        }
                    });

                }
            }
        }
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
        /*if (id == R.id.action_favorite) {
            Requests.FetchEventList(getApplicationContext(), onEventsListUpdatedCallback, null);
            return true;
        }*/

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

        } else if (id == R.id.nav_settings)
            StartSettingsActivity();

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
            return 2;
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

        public void RefreshCurrentList(boolean animate)
        {
            if(currentFragment != null)
                currentFragment.RefreshList(animate);
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
