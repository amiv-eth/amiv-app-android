package ch.amiv.android_app;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.net.ConnectException;

/**
 * This class is used to save settings so they can be resoted in another session later.
 * Use this class to retrieve visible/hidden settings.
 * Access the settings with the according get function from the static instance.
 */
public class Settings {
    public static Settings instance;

    //Vars for saving/reading the url from shared prefs, to allow saving between sessions. For each variable, have a key to access it and a default value
    private static SharedPreferences sharedPrefs;
    private static final String SHARED_PREFS_KEY = "ch.amiv.android_app";
    private static final String apiUrlPrefKey = "ch.amiv.android_app.serverurl";
    private static final String defaultApiUrl = "https://api-dev.amiv.ethz.ch";
    private static final String themeKey = "ch.amiv.android_app.theme";
    private static final boolean defaultTheme = false;  //flase for light
    private static final String apiTokenKey = "ch.amiv.android_app.apitoken";


    /*
     * This constructor will set the instance created as the statically accessible instance, which can be accessed anywhere
     * This means we can just create a settings instance to initialise this.
     */
    public Settings(Context context) {
        if(instance != null) {
            Log.d("settings", "A Settings instance already exists. Will use existing instance.");
            return;
        }
        instance = this;
        CheckInitSharedPrefs(context);
    }

    /**
     * Will check that the shared prefs instance is set so we can edit/retrieve values
     */
    private static void CheckInitSharedPrefs (Context context) {
        if(sharedPrefs == null)    //if the shared prefs is not initialised and we call getString() -> crash
            sharedPrefs = context.getSharedPreferences(SHARED_PREFS_KEY, Context.MODE_PRIVATE);
    }


    //==========Get/Set Settings===============
    /**
     * Will store the value in sharedpreferences to be restored in another session
     */
    public static void SetApiURL(String value, Context context) {
        CheckInitSharedPrefs(context);
        sharedPrefs.edit().putString(apiUrlPrefKey, value).apply();
    }

    /**
     * Returns the saved url, so the url is saved between sessions
     */
    public static String GetApiURL(Context context)
    {
        CheckInitSharedPrefs(context);
        return sharedPrefs.getString(apiUrlPrefKey, defaultApiUrl);
    }

    //Access Token
    public static void SetToken(String value, Context context) {
        CheckInitSharedPrefs(context);
        sharedPrefs.edit().putString(apiTokenKey, value).apply();
    }

    public static String GetToken(Context context) {
        CheckInitSharedPrefs(context);
        return sharedPrefs.getString(apiTokenKey, "");
    }


    // Theme
    public static void SetIsDarkTheme(boolean value, Context context) {
        CheckInitSharedPrefs(context);
        sharedPrefs.edit().putBoolean(themeKey, value).apply();
    }

    public static boolean GetIsDarkTheme(Context context) {
        CheckInitSharedPrefs(context);
        return sharedPrefs.getBoolean(themeKey, defaultTheme);
    }



    //SAMPLE get set functions for a new variable to be stored in settings
    /*public static void SetMyValue(String value) {
        sharedPrefs.edit().putString(MY_VALUE_PREF_KEY, value).apply();
    }

    public static String GetMyValue(Context context)
    {
        CheckInitSharedPrefs(context);  //will ensure that shared prefs exists so we can edit
        return sharedPrefs.getString(MY_VALUE_PREF_KEY, DEFAULT_VALUE);
    }*/
}
