package ch.amiv.android_app.core;


import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Vector;

import ch.amiv.android_app.R;
import ch.amiv.android_app.events.EventDetailActivity;
import ch.amiv.android_app.events.Events;
import ch.amiv.android_app.jobs.JobDetailActivity;
import ch.amiv.android_app.util.Util;


/**
 * This is the first screen. features: drawer, pageview with bottom navigation bar and within each page a list view.
 */
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    public static MainActivity instance;



//region -  ====Variables====
    private NavigationView drawerNav;
    private TextView drawer_title;
    private TextView drawer_subtitle;

    private BottomNavigationView bottomNavigation;

    private ViewPager viewPager;
    public PagerAdapter pagerAdapter;

    /**
     * Handle what should happen when the bottom nav buttons are pressed, will change the page of the viewpager
     */
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.bottom_nav_events:
                    viewPager.setCurrentItem(ListFragment.PageType.EVENTS);
                    return true;
                /*case R.id.bottom_nav_notifications:
                    viewPager.setCurrentItem(ListFragment.PageType.NOTIFICATIONS);
                    return true;*/
                case R.id.bottom_nav_jobs:
                    viewPager.setCurrentItem(ListFragment.PageType.JOBS);
                    return true;
                /*case R.id.bottom_nav_blitz:
                    viewPager.setCurrentItem(3);
                    return true;*/
            }
            return false;
        }
    };
//endregion

//region Initialisation
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        setContentView(R.layout.core_main);

            // Create the NotificationChannel to run notifications on API 26+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                CharSequence name = "1";
                String description = "Notifications";
                int importance = NotificationManager.IMPORTANCE_DEFAULT;
                NotificationChannel channel = new NotificationChannel("1", name, importance);
                channel.setDescription(description);
                // Register the channel in the system
                NotificationManager notificationManager = getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(channel);
            }


        /*
        //Use this to set a custom taskDescription in the app overview, ie when switching apps. Can set the icon, label and color of the bar
        Resources r = getResources();
        ActivityManager.TaskDescription taskDescription = new ActivityManager.TaskDescription(r.getString(R.string.app_name),
                BitmapFactory.decodeResource(getResources(), R.drawable.ic_amiv_logo_icon_white),
                r.getColor(R.color.white));
        this.setTaskDescription(taskDescription);*/

        Toolbar toolbar = Util.SetupToolbar(this, false);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        drawerNav = findViewById(R.id.nav_view);
        drawerNav.setNavigationItemSelectedListener(this);
        drawer_title = drawerNav.getHeaderView(0).findViewById(R.id.drawer_user_title);
        drawer_subtitle = drawerNav.getHeaderView(0).findViewById(R.id.drawer_user_subtitle);

        bottomNavigation = findViewById(R.id.bottomNav);
        bottomNavigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        InitialisePageView();
        if(!Settings.LoadEvents(getApplicationContext()))
            Request.FetchEventList(getApplicationContext(), pages.get(ListFragment.PageType.EVENTS).onEventsListUpdatedCallback, null, "");
        if(!Settings.LoadJobs(getApplicationContext()))
            Request.FetchJobList(MainActivity.instance, pages.get(ListFragment.PageType.EVENTS).onJobsListUpdatedCallback, null, "");


        //fetch the user info if we are logged in, there exists a token from the previous session, should be cached.
        if(!Settings.LoadUserInfo(getApplicationContext()) || UserInfo.current._id.isEmpty() && !Settings.IsEmailOnlyLogin(getApplicationContext())) {
            Request.FetchUserData(getApplicationContext(), drawerNav, new Request.OnDataReceivedCallback() {
                @Override
                public void OnDataReceived() {
                    SetLoginUIDirty();
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
        viewPager.setOffscreenPageLimit(ListFragment.PageType.COUNT);//prevent pages being deleted when we swipe to far

        //set for the bottom nav to be updated when we swipe to change the page
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }

            @Override
            public void onPageSelected(int position) {
                //need to convert index to resource id
                if(position == ListFragment.PageType.EVENTS)
                    position = R.id.bottom_nav_events;
                /*else if (position == 1)
                    position = R.id.bottom_nav_blitz;*/
                /*else if (position == ListFragment.PageType.NOTIFICATIONS)
                    position = R.id.bottom_nav_notifications;*/
                else if (position == ListFragment.PageType.JOBS)
                    position = R.id.bottom_nav_jobs;

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
        UserInfo.LogoutUser(getApplicationContext());

        pagerAdapter.RefreshPage(ListFragment.PageType.EVENTS, true);
        SetLoginUIDirty();

        Request.FetchEventList(getApplicationContext(), pages.get(ListFragment.PageType.EVENTS).onEventsListUpdatedCallback, null, "");
    }

    /**
     * Will refresh all login related UI, use this when the user logs in/out
     */
    public void SetLoginUIDirty ()
    {
        if(Settings.IsLoggedIn(getApplicationContext())) {
            drawerNav.getMenu().findItem(R.id.nav_login)
                .setTitle(R.string.logout_title)
                .setChecked(false);
            if(UserInfo.current != null)
            {
                if(Settings.IsEmailOnlyLogin(getApplicationContext())) {
                    drawer_title.setText(UserInfo.current.email);
                    drawer_subtitle.setText(R.string.email_only_login);
                }
                else {
                    drawer_title.setText(UserInfo.current.firstname + " " + UserInfo.current.lastname);
                    drawer_subtitle.setText(UserInfo.current.email);
                }
            }
            ///else XXX
            ///  fetchuserinfo
        }
        else
        {
            drawerNav.getMenu().findItem(R.id.nav_login)
                .setTitle(R.string.login_title)
                .setChecked(false);
            drawer_title.setText(R.string.not_logged_in);
            drawer_subtitle.setText("");
        }

        MicroApp.RefreshDrawer(this);
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

    public void StartEventDetailActivity(int eventGroup, int eventIndex)
    {
        Intent intent = new Intent(this, EventDetailActivity.class);
        intent.putExtra(EventDetailActivity.LauncherExtras.EVENT_GROUP, eventGroup);
        intent.putExtra(EventDetailActivity.LauncherExtras.EVENT_INDEX, eventIndex);
        startActivityForResult(intent, 0);
    }

    public void StartJobDetailActivity(int jobGroup, int jobIndex)
    {
        Intent intent = new Intent(this, JobDetailActivity.class);
        intent.putExtra(JobDetailActivity.LauncherExtras.JOB_GROUP, jobGroup);
        intent.putExtra(JobDetailActivity.LauncherExtras.JOB_INDEX, jobIndex);
        startActivityForResult(intent, 0);
    }

    /**
     * Here we can interpret the result of the login/event detail activity, if the login was successful or not, then update accordingly
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0 && resultCode == RESULT_OK) {
            // If we are returning from the login activity and have had a successfully login, refresh the user info and login UI
            boolean refreshLogin = data.getBooleanExtra("login_success", false);
            if(refreshLogin && Settings.IsLoggedIn(getApplicationContext()))
            {
                SetLoginUIDirty();
                Request.FetchUserData(getApplicationContext(), drawerNav, new Request.OnDataReceivedCallback() {
                    @Override
                    public void OnDataReceived() {
                        SetLoginUIDirty();
                        //Update events and signups with the new userinfo
                        if(Events.eventInfos.size() > 0)
                            Request.FetchEventSignups(getApplicationContext(), pages.get(ListFragment.PageType.EVENTS).onEventsListUpdatedCallback, null, "");
                        else
                            Request.FetchEventList(getApplicationContext(), pages.get(ListFragment.PageType.EVENTS).onEventsListUpdatedCallback, null, "");
                    }
                });

            }
        }
    }

    //region =====TOOLBAR=====
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.core_main_toolbar_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Use this to add an option item (three dots on top right of actionBar)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        /*if (id == R.id.action_favorite) {
            Request.FetchEventList(getApplicationContext(), onEventsListUpdatedCallback, null);
            return true;
        }*/

        return super.onOptionsItemSelected(item);
    }
    //endregion =====END OF TOOLBAR=============


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }/* else {
            super.onBackPressed();    //main activity is the root activity, so dont call super else we leave the app
        }*/
    }
    //region ========START OF DRAWER=========

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_login) {
            if (Settings.IsLoggedIn(getApplicationContext())) {
                LogoutUser();
                return false;
            }
            else
                StartLoginActivity();
        }
        else if (id == R.id.nav_settings)
            StartSettingsActivity();
        //Note:microapp clicking is handled in MicroApp

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return false;
    }
//endregion =====END OF DRAWER==================


//region =====START OF PAGEVIEW==============

    protected Vector<ListFragment> pages = new Vector<>(ListFragment.PageType.COUNT);
    /**
     * This will handle changing between the pages
     */
    public class PagerAdapter extends FragmentStatePagerAdapter {
        int currentPosition;

        public PagerAdapter(FragmentManager fm) {
            super(fm);
            for(int i = 0; i< ListFragment.PageType.COUNT; i++)
                pages.add(ListFragment.NewInstance(i));

            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return pages.size();
        }

        @Override
        public Fragment getItem(int position) {
            return pages.get(position);
        }

        @Override
        public int getItemPosition(Object object) {
            ListFragment fragment = (ListFragment) object;
            int position = pages.indexOf(fragment);

            if (position >= 0) {
                return position;
            } else {
                return POSITION_NONE;
            }
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            currentPosition = position;

            super.setPrimaryItem(container, position, object);
        }
/*
        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {

        }*/

        public void RefreshPage(int position, boolean animate){
            if(pages.get(position) != null)
                pages.get(position).RefreshList(animate);
            else
                Log.e("pageview", "RefreshPage(), Page does not exist will not refresh: " + position);
        }

        /**
         * Used to reconnect the link to the fragment in onresume
         */
        public void ReconnectFragment(ListFragment fragment, int position){
            try {
                pages.set(position, fragment);
            }
            catch (Exception e){
                e.printStackTrace();
            }
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