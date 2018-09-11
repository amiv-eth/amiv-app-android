package ch.amiv.android_app.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Vibrator;
import android.util.Log;

import java.util.Locale;

import javax.xml.validation.Validator;

import ch.amiv.android_app.R;

/**
 * This class is used to save settings so they can be restored in another session later.
 * Use this class to retrieve visible/hidden settings.
 * Access the settings with the according get function from the static instance.
 */
public class Settings {

    //public static final String API_URL = "http://192.168.1.105:5000/";
    public static final String API_URL = "https://api-dev.amiv.ethz.ch/";

    //---PREF KEYS---- Vars for saving/reading the url from shared prefs, to allow saving between sessions. For each variable, have a key to access it and a default value
    private static SharedPreferences sharedPrefs;
    public static final String SHARED_PREFS_KEY = "ch.amiv.android_app";

    //Keys are always two values, (Key for shared prefs, Default value)
    //For boolean values true=1, false=0 (or anything else)
    public static final String[] apiUrlPrefKey      = {"ch.amiv.android_app.server_url", "https://api-dev.amiv.ethz.ch"};
    public static final String[] apiTokenKey        = {"ch.amiv.android_app.api_token", ""};
    public static final String[] introDoneKey       = {"ch.amiv.android_app.intro_done", "0"};

    public static final String[] foodPrefKey        = {"ch.amiv.android_app.food_pref", ""};
    public static final String[] specialFoodPrefKey = {"ch.amiv.android_app.special_food_pref", ""};
    public static final String[] sbbPrefKey         = {"ch.amiv.android_app.sbb_abo", ""};

    //---checkin
    public static final String[] recentEventPin     = {"ch.amiv.android_app.recent_event_pin", ""};

    //region ---SharedPrefs---
    /**
     * Will check that the shared prefs instance is set so we can edit/retrieve values
     */
    private static void CheckInitSharedPrefs (Context context) {
        if(sharedPrefs == null)    //if the shared prefs is not initialised and we call getString() -> crash
            sharedPrefs = context.getSharedPreferences(SHARED_PREFS_KEY, Context.MODE_PRIVATE);
    }

    /**
     * Will store the value in sharedpreferences to be restored in another session
     * @param key A string[2] in the format (prefs key, defValue), use the Settings public vars
     */
    public static void SetPref(String[] key, String value, Context context){
        CheckInitSharedPrefs(context);
        sharedPrefs.edit().putString(key[0], value).apply();
    }

    /**
     * Get the value stored in shared prefs with the given key
     * @param key A string[2] in the format (prefs key, defValue), use the Settings public vars
     */
    public static String GetPref(String[] key, Context context)
    {
        CheckInitSharedPrefs(context);
        return sharedPrefs.getString(key[0], key[1]);
    }

    //bool 'overloads'
    public static void SetBoolPref (String[] key, boolean value, Context context){
        CheckInitSharedPrefs(context);
        sharedPrefs.edit().putBoolean(key[0], value).apply();
    }

    public static boolean GetBoolPref(String[] key, Context context)
    {
        CheckInitSharedPrefs(context);
        return sharedPrefs.getBoolean(key[0], key[1].equals("1"));
    }

    //Add token functions as they are commonly used, for ease of understanding in other code
    public static void SetToken(String value, Context context){
        SetPref(apiTokenKey, value, context);
    }

    public static String GetToken(Context context){
        return GetPref(apiTokenKey, context);
    }

    /**
     * Intended more for debug, will delete all saved data, which is stored in the shared prefs
     */
    public static void ClearSharedPrefs(Context context){
        CheckInitSharedPrefs(context);
        sharedPrefs.edit().clear().commit();
    }

    /**
     * Note:only for string prefs
     * @return True if the pref is at the default value
     */
    public static boolean IsPrefDefault(String[] key, Context context){
        return GetPref(key, context).equals(key[1]);
    }

    //endregion

    //region ---Auth---
    /**
     * Note: will only check if a token exists. This token may have expired but not have been refreshed/deleted.
     * @return True if the user is logged into the api and has an access token.
     */
    public static boolean HasToken(Context context){
        String t = GetPref(apiTokenKey, context);
        return !t.isEmpty();
    }

    /**
     * Will return whether the user is only logged in with an email, if they do not have an api login, false if current user has not be initialised
     */
    public static boolean IsEmailOnlyLogin(Context context){
        return !Settings.HasToken(context) && UserInfo.current != null && !UserInfo.current.email.isEmpty();
    }

    /**
     * Note: token may have expired but not have been refreshed/deleted.
     * @return True if there is a token or an email login
     */
    public static boolean IsLoggedIn(Context context){
        return HasToken(context) || IsEmailOnlyLogin(context);
    }
    //endregion

    //region ---Language---
    /**
     * Will change the language, NOTE: Highly advised to restart the app/activity for changes to take effect.
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
    //endregion

    //region---Haptic Feedback---
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
    //endregion
}
