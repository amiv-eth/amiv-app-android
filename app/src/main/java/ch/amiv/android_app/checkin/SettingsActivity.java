package ch.amiv.android_app.checkin;

/**
 * Author: Roger Barton, rbarton@ethz.ch
 * Date Created: 2/12/17
 */

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.math.MathUtils;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import ch.amiv.android_app.R;
import ch.amiv.android_app.core.Settings;
import ch.amiv.android_app.util.Util;

/**
 * This activity is for settings *specific* to the checkin microapp, we still use the core.Settings to store sharedPrefs/variables. Mainly handles UI
 */
public class SettingsActivity extends AppCompatActivity {
    private EditText mUrlField;
    private CheckBox mAutoRefreshCheck;
    private EditText mRefreshFreqField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.checkin_settings);
        Util.SetupToolbar(this,true);
        Util.SetWindowResizing(this,true);

        mUrlField = findViewById(R.id.UrlField);
        mAutoRefreshCheck = findViewById(R.id.autoRefreshCheck);
        mRefreshFreqField = findViewById(R.id.refreshFreqField);

        mUrlField.setText(
                Settings.GetPref(Settings.checkin_url, getApplicationContext()));
        mAutoRefreshCheck.setChecked(
                Settings.GetBoolPref(Settings.checkin_autoUpdate, getApplicationContext()));
        mRefreshFreqField.setText((Float.toString(
                Settings.GetFloatPref(Settings.checkin_refreshRate, getApplicationContext()))));
    }

    /**
     * Saves url to Shared Prefs and returns to main activity
     */
    public void SaveSettings(View view)
    {
        Settings.SetPref(Settings.checkin_url, mUrlField.getText().toString(), getApplicationContext());
        Settings.SetBoolPref(Settings.checkin_autoUpdate, mAutoRefreshCheck.isChecked(), getApplicationContext());
        Settings.SetFloatPref(Settings.checkin_refreshRate, MathUtils.clamp(Float.parseFloat(mRefreshFreqField.getText().toString()), 3f, Float.POSITIVE_INFINITY), getApplicationContext());

        ReturnToMainActivity();
    }

    /**
     * @return The refresh rate in ms, (stored in s)
     */
    public static int GetRefreshRateMillis(Context context) {
        return (int)(1000 * Settings.GetFloatPref(Settings.checkin_refreshRate, context));
    }

    private void ReturnToMainActivity ()
    {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}
