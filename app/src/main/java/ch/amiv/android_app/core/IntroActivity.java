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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import ch.amiv.android_app.R;
import ch.amiv.android_app.util.ui.NonSwipeableViewPager;
import ch.amiv.android_app.util.ui.EnumViewGenerator;
import ch.amiv.android_app.util.Util;

public class IntroActivity extends AppCompatActivity {

    private NonSwipeableViewPager viewPager;
    private IntroViewPagerAdapter introViewPagerAdapter;
    private LinearLayout dotsLayout;
    private TextView[] dots;
    private Button btnSkip, btnNext;
    private EditText rfidField;

    //page configs & layouts
    private int[] layouts = {
            R.layout.core_intro_slide_language,
            R.layout.core_intro_slide_info,
            R.layout.core_intro_slide_profile,
            R.layout.core_intro_slide_event_prefs,
            R.layout.core_intro_slide_pref_detail};//Used for editing an enum pref

    private static final class Page {
        private static final int LANGUAGE = 0;
        private static final int APP_INFO = 1;
        private static final int EDIT_PROFILE = 2;
        private static final int EVENT_PREF = 3;
        private static final int PREF_DETAIL = 4;
    }

    private boolean[] allowSkip = {false, false, true, false, false};
    private int[] nextText = {0, R.string.next, R.string.next, R.string.lets_go, 0};//set to 0 to hide next button

    private boolean hasLoggedIn;    //used when using back after the login page
    private String langSetIntentKey = "lang_set";

    private View.OnClickListener onNextClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (viewPager.getCurrentItem() == 2)//If we press next on the profile page, submit new data
                UpdateProfile();
            NextPage(true);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.core_intro);

        viewPager = findViewById(R.id.viewPager);
        dotsLayout = findViewById(R.id.layoutDots);
        btnSkip = findViewById(R.id.buttonSkip);
        btnNext = findViewById(R.id.buttonNext);
        rfidField = findViewById(R.id.rfidField);

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

        btnNext.setOnClickListener(onNextClick);

        if(hasSetLang)
            NextPage(true);
        else
            RefreshPageUI(0);
    }

    @Override
    public void onBackPressed() {
        int currentPos = viewPager.getCurrentItem();
        if(currentPos == Page.EDIT_PROFILE) {
            StartLoginActivity(true);
        }
        else if(currentPos == Page.EVENT_PREF){
            if(hasLoggedIn)
                SetPage(Page.EDIT_PROFILE, true);
            else
                StartLoginActivity(true);
        }
        else if(currentPos > 0)
            SetPage(currentPos -1, true);

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
        if(currentPage > layouts.length -2)//pref detail view
            return;

        dots = new TextView[layouts.length]; //+1 for login acitivty, -1 for pref detail

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
            dots[currentPage < Page.EDIT_PROFILE ? currentPage : currentPage +1].setTextColor(ContextCompat.getColor(this, R.color.lightGrey));
    }

    private void NextPage(boolean success){
        int currentPos = viewPager.getCurrentItem();
        if(currentPos == Page.EDIT_PROFILE)
            Util.HideKeyboard(this);

        if (currentPos == Page.APP_INFO)//go to login first
            StartLoginActivity(false);
        else if (currentPos == Page.EVENT_PREF)
            StartMainAcivity();
        else if (currentPos +1 < layouts.length)
            SetPage(currentPos +1, true);

    }

    private void SetPage(int page, boolean setupPage){
        viewPager.setCurrentItem(page);//This actually changes the page, otherwise we are just setting up the new page

        if(!setupPage)
            return;

        //Setup Next Page
        if(page == Page.EVENT_PREF){//Setup pref values
            TextView foodLabel = findViewById(R.id.foodPrefText);
            String foodValue = Settings.GetPref(Settings.foodPrefKey, getApplicationContext());
            if(foodLabel != null) {
                if(foodValue.isEmpty())
                foodLabel.setText(R.string.tap_to_set);
                else
                    foodLabel.setText(foodValue);
            }

            TextView sbbLabel = findViewById(R.id.sbbPrefText);
            String sbbValue = Settings.GetPref(Settings.sbbPrefKey, getApplicationContext());
            if(sbbLabel != null) {
                if(sbbValue.isEmpty())
                    sbbLabel.setText(R.string.tap_to_set);
                else
                    sbbLabel.setText(sbbValue);
            }
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

            if(resultCode == RESULT_OK) {
                if(hasLoggedIn){
                    SetPage(Page.EDIT_PROFILE, true);
                    RefreshPageUI(Page.EDIT_PROFILE);
                    btnNext.setVisibility(View.INVISIBLE);
                    Request.FetchUserData(getApplicationContext(), viewPager, new Request.OnDataReceivedCallback() {
                        @Override
                        public void OnDataReceived() {
                            SetProfileUI();
                        }
                    });
                }
                else
                    SetPage(Page.EVENT_PREF, true);

            }
            else
                SetPage(Page.APP_INFO, true);//means the user canceled using back, show the previous page
        }
    }

    private void StartLoginActivity(boolean logoutFirst) {
        if(logoutFirst)
            UserInfo.LogoutUser(getApplicationContext());
        startActivityForResult(new Intent(this, LoginActivity.class), 0);
    }

    private void StartMainAcivity() {
        Settings.SetBoolPref(Settings.introDoneKey, true, this);
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
    /**
     * Use this to fill/show in the profile text fields once the userInfo has been received
     */
    private void SetProfileUI(){
        findViewById(R.id.loadingCircle).setVisibility(View.GONE);
        findViewById(R.id.scrollView).setVisibility(View.VISIBLE);
        RefreshPageUI(Page.EDIT_PROFILE);

        //fill in values for the profile fields
        if(rfidField == null)
            rfidField = findViewById(R.id.rfidField);
        rfidField.setText(UserInfo.current.rfid);
    }

    /**
     * Use this to update the profile info and submit to the server
     */
    private void UpdateProfile(){
        if(UserInfo.current.SetRFID(rfidField.getText().toString()))
            Request.PatchUserData(getApplicationContext());
    }
    //endregion

    //region---Prefs---
    /**
     * This changes the page to edit the food pref, UI content is generated using EnumViewGenerator
     */
    public void EditFoodPrefs(View view){
        SetPage(Page.PREF_DETAIL, true);

        final EnumViewGenerator.OnButtonIndexClicked onClick = new EnumViewGenerator.OnButtonIndexClicked() {
            @Override
            public void OnClick(int enumIndex) {//use length-1 when other is used
                if(enumIndex < 0)
                    return;
                SetPage(Page.EVENT_PREF, false);

                String value = getResources().getStringArray(R.array.pref_food_list_values)[enumIndex];
                Settings.SetPref(Settings.foodPrefKey, value, getApplicationContext());
                if(enumIndex == getResources().getStringArray(R.array.pref_food_list_values).length -1 && findViewById(R.id.otherField) != null) {
                    Settings.SetPref(Settings.specialFoodPrefKey, ((EditText)findViewById(R.id.otherField)).getText().toString(), getApplicationContext());
                }

                TextView label = findViewById(R.id.foodPrefText);
                if(label != null)
                    label.setText(getResources().getStringArray(R.array.pref_food_list_values)[enumIndex]);

                btnNext.setOnClickListener(onNextClick);//reset next button
            }
        };

        EnumViewGenerator.InitialiseEnumList(this, R.string.pref_food_title, onClick, getResources().getStringArray(R.array.pref_food_list_values), true);

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClick.OnClick(getResources().getStringArray(R.array.pref_food_list_values).length -1);
            }
        });
    }

    public void EditSBBPrefs(View view){
        SetPage(Page.PREF_DETAIL, true);

        final EnumViewGenerator.OnButtonIndexClicked onClick = new EnumViewGenerator.OnButtonIndexClicked() {
            @Override
            public void OnClick(int enumIndex) {//use length-1 when other is used
                if(enumIndex < 0)
                    return;
                SetPage(Page.EVENT_PREF, false);

                String value = getResources().getStringArray(R.array.pref_sbb_list_values)[enumIndex];
                Settings.SetPref(Settings.sbbPrefKey, value, getApplicationContext());

                TextView label = findViewById(R.id.sbbPrefText);
                if(label != null)
                    label.setText(getResources().getStringArray(R.array.pref_sbb_list_values)[enumIndex]);

                btnNext.setOnClickListener(onNextClick);
            }
        };

        EnumViewGenerator.InitialiseEnumList(this, R.string.pref_sbb_title, onClick, getResources().getStringArray(R.array.pref_sbb_list_values), false);

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClick.OnClick(getResources().getStringArray(R.array.pref_sbb_list_values).length -1);
            }
        });
    }
    //endregion
    //endregion
}
