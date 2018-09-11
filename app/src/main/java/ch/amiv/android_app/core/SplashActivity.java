package ch.amiv.android_app.core;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import ch.amiv.android_app.BuildConfig;

/**
 * This is the launcher/splash activity, here we choose to start the main activity or begin the setup process depending on whether the user has done the setup or not (stored in settings)
 * For the UI we use a theme with the splash_screen, using a layout file is not possible as we have to display it while loading, need to use a basic xml, no inflation
 * Theme is set in the manifest
 */
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //DEBUG to access intro
        //if(BuildConfig.DEBUG)
        //  Settings.ClearSharedPrefs(getApplicationContext());

        Intent intent;
        if(Settings.GetBoolPref(Settings.introDoneKey, getApplicationContext()))
            intent = new Intent(this, MainActivity.class);
        else
            intent = new Intent(this, IntroActivity.class);

        startActivity(intent);
        finish();
    }
}
