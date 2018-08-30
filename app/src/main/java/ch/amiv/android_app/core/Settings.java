package ch.amiv.android_app.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Vibrator;
import android.util.Log;

import java.util.Locale;

import ch.amiv.android_app.R;

/**
 * This class is used to save settings so they can be restored in another session later.
 * Use this class to retrieve visible/hidden settings.
 * Access the settings with the according get function from the static instance.
 */
public class Settings {
    public static Settings instance;

    //public static final String API_URL = "http://192.168.1.105:5000/";
    public static final String API_URL = "https://api-dev.amiv.ethz.ch/";

    //Whether to show hidden events, where the adverts should not have started yet, should later be set by user access group
    public static final boolean showHiddenFeatures = true;

    //Vars for saving/reading the url from shared prefs, to allow saving between sessions. For each variable, have a key to access it and a default value
    private static SharedPreferences sharedPrefs;
    public static final String SHARED_PREFS_KEY = "ch.amiv.android_app";
    private static final String apiUrlPrefKey = "ch.amiv.android_app.serverurl";
    private static final String defaultApiUrl = "https://api-dev.amiv.ethz.ch";
    private static final String themeKey = "ch.amiv.android_app.theme";
    private static final boolean defaultTheme = false;  //false for light
    private static final String apiTokenKey = "ch.amiv.android_app.apitoken";
    private static final String introDoneKey = "ch.amiv.android_app.introdone";

    private static Vibrator vibrator;
    public static final class VibrateTime {
        public static final int SHORT = 50;
        public static final int NORMAL = 100;
        public static final int LONG = 250;
    }
    public static void Vibrate(int millisecs, Context context){
        if(vibrator == null)
            vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null)
            vibrator.vibrate(millisecs);
    }

    public static void CancelVibrate (){
        if(vibrator != null)
            vibrator.cancel();
    }

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

    /**
     * Note: will only check if a token exists. This token may have expired but not have been refreshed/deleted.
     * @return True if the user is logged into the api and has an access token.
     */
    public static boolean HasToken(Context context){
        CheckInitSharedPrefs(context);
        String t = sharedPrefs.getString(apiTokenKey, "");
        return !t.isEmpty();
    }

    /**
     * Will return whether the user is only loggedd in with an email, if they do not have an api login, false if current user has not be initialised
     */
    public static boolean IsEmailOnlyLogin(Context context){
        return !Settings.HasToken(context) && UserInfo.current != null && !UserInfo.current.email.isEmpty();
    }

    /**
     * Note: will only check if a token exists. This token may have expired but not have been refreshed/deleted.
     * @return True if the user is logged into the api and has an access token.
     */
    public static boolean IsLoggedIn(Context context){
        return HasToken(context) || IsEmailOnlyLogin(context);
    }


    /**
     * Will change the language, NOTE: Highly advised to restart the app/activty for changes to take effect.
     * New strings that are loaded will be in the correct language, but UI that is already created/still in memory will NOT change.
     * Returns true if the language has been changed
     */
    public static void SetLanguage(String value, Context context) {
        CheckInitSharedPrefs(context);

        sharedPrefs.edit().putString(context.getResources().getString(R.string.pref_lang_key), value).commit();//need to use commit(blocks current thread) instead of apply(multithreaded)

        //Change locale/language
        Resources res = context.getResources();
        Configuration conf = res.getConfiguration();
        conf.locale = new Locale(value);
        res.updateConfiguration(conf, res.getDisplayMetrics());
    }

    public static String GetLanguage(Context context) {
        CheckInitSharedPrefs(context);
        Resources res = context.getResources();
        return sharedPrefs.getString(res.getString(R.string.pref_lang_key), "");
    }

    public static void SetIntroDone(boolean value, Context context) {
        CheckInitSharedPrefs(context);
        sharedPrefs.edit().putBoolean(introDoneKey, value).apply();
    }

    public static boolean GetIntroDone(Context context)
    {
        CheckInitSharedPrefs(context);  //will ensure that shared prefs exists so we can edit
        return sharedPrefs.getBoolean(introDoneKey, false);
    }

    //SAMPLE get set functions for a new variable to be stored in settings
    /*public static void SetMyValue(String value, Context context) {
        CheckInitSharedPrefs(context);
        sharedPrefs.edit().putString(MY_VALUE_PREF_KEY, value).apply();
    }

    public static String GetMyValue(Context context)
    {
        CheckInitSharedPrefs(context);  //will ensure that shared prefs exists so we can edit
        return sharedPrefs.getString(MY_VALUE_PREF_KEY, DEFAULT_VALUE);
    }*/
}
