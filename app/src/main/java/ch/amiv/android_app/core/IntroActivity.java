package ch.amiv.android_app.core;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import ch.amiv.android_app.R;
import ch.amiv.android_app.ui.NonSwipeableViewPager;

public class IntroActivity extends AppCompatActivity {

    private NonSwipeableViewPager viewPager;
    private IntroViewPagerAdapter introViewPagerAdapter;
    private LinearLayout dotsLayout;
    private TextView[] dots;
    private Button btnSkip, btnNext;

    //page configs & layouts
    private int[] layouts = {
            R.layout.core_intro_slide_language,
            R.layout.core_intro_slide_info,
            R.layout.core_intro_slide_profile,
            R.layout.core_intro_slide_event_prefs};
    private boolean[] allowSkip = {false, false, true, false};
    private int[] nextText = {0, R.string.next, 0, R.string.lets_go};//set to 0 to hide next button

    private boolean hasLoggedIn;    //used when using back after the login page
    private String langSetIntentKey = "lang_set";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.core_activity_intro);

        viewPager = findViewById(R.id.viewPager);
        dotsLayout = findViewById(R.id.layoutDots);
        btnSkip = findViewById(R.id.buttonSkip);
        btnNext = findViewById(R.id.buttonNext);

        boolean hasSetLang = false;
        Intent intent = getIntent();
        if(intent != null)
            hasSetLang = intent.getBooleanExtra(langSetIntentKey, false);

        //init ui elements
        introViewPagerAdapter = new IntroViewPagerAdapter();
        viewPager.setAdapter(introViewPagerAdapter);
        viewPager.addOnPageChangeListener(viewPagerPageChangeListener);
        viewPager.setOffscreenPageLimit(0);

        btnSkip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NextPage(false);
            }
        });

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NextPage(true);
            }
        });

        if(hasSetLang)
            NextPage(true);
        else
            RefreshPageUI(0);
    }

    @Override
    public void onBackPressed() {
        int currentPos = viewPager.getCurrentItem();
        if(currentPos == 2) {
            StartLoginActivity(true);
        }
        else if(currentPos == 3){
            if(hasLoggedIn)
                viewPager.setCurrentItem(2);
            else
                StartLoginActivity(true);
        }
        else if(currentPos > 0)
            viewPager.setCurrentItem(currentPos -1);

        //Dont call super.onBackPressed as we will otherwise leave the app
    }

    /**
     * Will set the buttons and dots correctly for the page given
     */
    private void RefreshPageUI(int currentPage){
        RefreshPageDots(currentPage);

        btnSkip.setVisibility(allowSkip[currentPage] ? View.VISIBLE : View.INVISIBLE);
        btnNext.setVisibility(nextText[currentPage] != 0 ? View.VISIBLE : View.INVISIBLE);
        if(nextText[currentPage] != 0)
            btnNext.setText(getString(nextText[currentPage]));
    }

    /**
     * Will update the page dots at the bottom, to indicate which page we are on. note: the views are deleted and recreated
     */
    private void RefreshPageDots(int currentPage) {
        dots = new TextView[layouts.length +1]; //+1 for login acitivty

        int inactive = ContextCompat.getColor(this, R.color.darkGrey);
        dotsLayout.removeAllViews();
        for (int i = 0; i < dots.length; i++) {
            dots[i] = new TextView(this);
            dots[i].setText(Html.fromHtml("&#8226;"));
            dots[i].setTextSize(35);
            dots[i].setTextColor(inactive);
            dotsLayout.addView(dots[i]);
        }

        if (dots.length > 0)//change indexes for login activity
            dots[currentPage < 2 ? currentPage : currentPage +1].setTextColor(ContextCompat.getColor(this, R.color.lightGrey));
    }

    private void NextPage(boolean success){
        int nextPos = viewPager.getCurrentItem() + 1;
        if (nextPos == 2)//go to login first
            StartLoginActivity(false);
        else if (nextPos < layouts.length) {
            viewPager.setCurrentItem(nextPos);
        } else {
            StartMainAcivity();
        }
    }

    //  Callbacks for when the page changes to refresh the progress dots
    ViewPager.OnPageChangeListener viewPagerPageChangeListener = new ViewPager.OnPageChangeListener() {

        @Override
        public void onPageSelected(int position) {
            RefreshPageUI(position);
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {}

        @Override
        public void onPageScrollStateChanged(int arg0) {}
    };

    /**
     * View pager adapter
     */
    public class IntroViewPagerAdapter extends PagerAdapter {
        private LayoutInflater layoutInflater;

        public IntroViewPagerAdapter() {
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View view = layoutInflater.inflate(layouts[position], container, false);
            container.addView(view);

            return view;
        }

        @Override
        public int getCount() {
            return layouts.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object obj) {
            return view == obj;
        }


        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            View view = (View) object;
            container.removeView(view);
        }
    }

    /**
     * This is called when we return from the login activity
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 0) {  //skip the profile page if we are not logged in
            hasLoggedIn = data.getBooleanExtra("login_success", false);
            if(hasLoggedIn)
                hasLoggedIn = Settings.HasToken(getApplicationContext());//check if user is only logged in by mail, or if we have no user profile to edit

            if(resultCode == RESULT_OK)
                viewPager.setCurrentItem(hasLoggedIn ? 2 : 3);
            else
                viewPager.setCurrentItem(1);//means the user canceled using back, show the previous page
        }
    }

    private void StartLoginActivity(boolean logoutFirst) {
        if(logoutFirst)
            UserInfo.LogoutUser(getApplicationContext());
        startActivityForResult(new Intent(this, LoginActivity.class), 0);
    }

    private void StartMainAcivity() {
        Settings.SetIntroDone(true, this);
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
    //endregion

    //region----Functionality specific to a page----
    //region---Language Page---
    public void SetLanguageEN(View view){
        ApplyLanguage(0);
    }

    public void SetLanguageDE(View view){
        ApplyLanguage(1);
    }

    /**
     * @param langIndex According to the language pref array (pref_lang_list_entries) in strings.xml
     */
    private void ApplyLanguage(int langIndex){
        Settings.SetLanguage(getResources().getStringArray(R.array.pref_lang_list_entries)[langIndex], getApplicationContext());

        //restart activty and set extra to skip language page
        Intent intent = getIntent();
        intent.putExtra(langSetIntentKey, true);
        finish();
        startActivity(intent);
    }
    //endregion

    //region---Profile---

    //endregion
    //endregion
}
