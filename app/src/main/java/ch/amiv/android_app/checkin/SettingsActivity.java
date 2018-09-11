package ch.amiv.android_app.checkin;

/**
 * Author: Roger Barton, rbarton@ethz.ch
 * Date Created: 2/12/17
 */

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.math.MathUtils;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import ch.amiv.android_app.R;

public class SettingsActivity extends AppCompatActivity {
    private static String DEF_URL = "https://checkin.amiv.ethz.ch";    //NOTE: Set default value before build, also change the 'checkin_server_url' string in strings.xml to adapt UI

    //Vars for saving/reading the url from shared prefs, to allow saving between sessions. For each variable, have a key to access it and a default value
    private static SharedPreferences SHARED_PREFS;
    private static String SHARED_PREFS_KEY = "com.amivlegiscanner.app";
    private static String URL_PREF_KEY = "com.amivlegiscanner.app.serverurl";
    private static String AUTO_UPDATE_STATS_PREF_KEY = "com.amivlegiscanner.app.autorefresh";
    public static boolean DEF_AUTO_UPDATE_STATS = true;
    private static String REFRESH_FREQUENCY_KEY = "com.amivlegiscanner.app.refreshfrequency";
    public static float DEF_REFRESH_FREQUENCY = 20f;

    private EditText mUrlField;
    private CheckBox mAutoRefreshCheck;
    private EditText mRefreshFreqField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.checkin_activity_settings);

        mUrlField = findViewById(R.id.UrlField);
        mUrlField.setText(GetServerURL(getApplicationContext()));
        mAutoRefreshCheck = findViewById(R.id.autoRefreshCheck);
        mAutoRefreshCheck.setChecked(GetAutoRefresh(getApplicationContext()));
        mRefreshFreqField = findViewById(R.id.refreshFreqField);
        mRefreshFreqField.setText((Float.toString(GetRefreshFrequency(getApplicationContext()) / 1000f)));
    }

    /**
     * Saves url to Shared Prefs and returns to main activity
     */
    public void SaveSettings(View view)
    {
        SHARED_PREFS.edit().putString(URL_PREF_KEY, mUrlField.getText().toString()).apply();
        SHARED_PREFS.edit().putBoolean(AUTO_UPDATE_STATS_PREF_KEY, mAutoRefreshCheck.isChecked()).apply();
        SHARED_PREFS.edit().putFloat(REFRESH_FREQUENCY_KEY, MathUtils.clamp(Float.parseFloat(mRefreshFreqField.getText().toString()), 3f, Float.POSITIVE_INFINITY)).apply();

        ReturnToMainActivity();
    }

    /**
     * Returns the saved url, so the url is saved between sessions
     * @return URL currently set for the server
     */
    public static String GetServerURL(Context context)
    {
        if(SHARED_PREFS == null)    //if the shared prefs is not initialised and we call getString() -> crash
            SHARED_PREFS = context.getSharedPreferences(SHARED_PREFS_KEY, Context.MODE_PRIVATE);

        return SHARED_PREFS.getString(URL_PREF_KEY, DEF_URL);
    }

    /**
     * @return Returns whether auto fetching data, to update list of members from the server, is allowed
     */
    public static boolean GetAutoRefresh (Context context)
    {
        if(SHARED_PREFS == null)
            SHARED_PREFS = context.getSharedPreferences(SHARED_PREFS_KEY, Context.MODE_PRIVATE);

        return SHARED_PREFS.getBoolean(AUTO_UPDATE_STATS_PREF_KEY, DEF_AUTO_UPDATE_STATS);
    }

    /**
     * @return Returns the saved refresh frequency for getting data from the server. The value set in the settings activity
     */
    public static int GetRefreshFrequency (Context context) //returns in millisec, but is stored in sec
    {
        if(SHARED_PREFS == null)
            SHARED_PREFS = context.getSharedPreferences(SHARED_PREFS_KEY, Context.MODE_PRIVATE);

        float f = SHARED_PREFS.getFloat(REFRESH_FREQUENCY_KEY, DEF_REFRESH_FREQUENCY);
        if(f < 0)
            f = DEF_REFRESH_FREQUENCY;

        return (int)(1000 * f);
    }

    private void ReturnToMainActivity ()
    {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}
