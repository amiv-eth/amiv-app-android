package ch.amiv.android_app.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Vibrator;
import android.util.Pair;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Locale;

import ch.amiv.android_app.R;
import ch.amiv.android_app.events.EventInfo;
import ch.amiv.android_app.events.Events;
import ch.amiv.android_app.jobs.JobInfo;
import ch.amiv.android_app.jobs.Jobs;

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
    public static final String[] apiUrlPrefKey      = {"core.server_url", "https://api-dev.amiv.ethz.ch"};
    public static final String[] apiTokenKey        = {"core.api_token", ""};
    public static final String[] introDoneKey       = {"core.intro_done", "0"};

    public static final String[] foodPrefKey        = {"core.food_pref", ""};
    public static final String[] specialFoodPrefKey = {"core.special_food_pref", ""};
    public static final String[] sbbPrefKey         = {"core.sbb_abo", ""};

    //Storing of larger data
    public static final String[] userInfoKey        = {"core.user_info", ""};
    public static final String[] eventInfoKey       = {"events.event_infos", ""};
    public static final String[] jobInfoKey         = {"jobs.job_infos", ""};

    //region---Check-in----
    public static final String[] recentEventPin     = {"checkin.recent_event_pin", ""};
    public static final String[] checkin_url        = {"checkin.serverurl", "https://checkin.amiv.ethz.ch"};
    //public static final String[] checkin_url      = {"checkin.serverurl", "https://checkin-dev.amiv.ethz.ch"};//DEBUG
    public static final String[] checkin_autoUpdate = {"checkin.autorefresh", "1"};
    public static final Pair<String, Float> checkin_refreshRate = new Pair<>("checkin.refreshfrequency", 20f);
    //endregion

    //last changes check
    public static final String[] last_change_check_dateKey = {"core.notifications_date", "1979-02-19T10:00:00Z"};

    //region ---SharedPrefs---
    /**
     * Will check that the shared prefs instance is set so we can edit/retrieve values
     */
    private static void CheckInitSharedPrefs (Context context) {
        if(sharedPrefs == null)    //if the shared prefs is not initialised and we call getString() -> crash
            sharedPrefs = context.getSharedPreferences(SHARED_PREFS_KEY, Context.MODE_PRIVATE);
    }

    public static boolean HasKey(String[] key, Context context){
        CheckInitSharedPrefs(context);
        return sharedPrefs.contains(key[0]);
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

    // Sidenote: Float prefs need to use a pair instead of a string[], so we have a float for the default value
    public static boolean HasKey(Pair<String, Float> key, Context context){
        CheckInitSharedPrefs(context);
        return sharedPrefs.contains(key.first);
    }

    public static void SetFloatPref (Pair<String, Float> key, float value, Context context){
        CheckInitSharedPrefs(context);
        sharedPrefs.edit().putFloat(key.first, value).apply();
    }

    public static float GetFloatPref(Pair<String, Float> key, Context context)
    {
        CheckInitSharedPrefs(context);
        return sharedPrefs.getFloat(key.first, key.second);
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
        return !HasToken(context) && UserInfo.current != null && !UserInfo.current.email.isEmpty();
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

    //============Storing User,Event,Job Infos===============
    //region user,events,jobs
    private static final Type eventListType = new TypeToken<List<EventInfo>>() {}.getType();
    private static final Type jobListType = new TypeToken<List<JobInfo>>() {}.getType();

    private static boolean hasLoadedUser = false;
    private static boolean hasLoadedEvents = false;
    private static boolean hasLoadedJobs = false;

    public static void SaveUserInfo(Context context)
    {
        Gson gson = new Gson();
        String json = gson.toJson(UserInfo.current);
        SetPref(userInfoKey, json, context);
    }

    public static boolean LoadUserInfo (Context context)
    {
        if(hasLoadedUser || !HasKey(userInfoKey, context)) return false;

        String json = GetPref(userInfoKey, context);
        if(json.isEmpty())
            return false;

        try {
            UserInfo.UpdateCurrent(context, new JSONObject(json), false, true);
        }
        catch (Exception e){    //This may happen if the userinfo class changes
            e.printStackTrace();
            return false;
        }

        hasLoadedUser = true;
        return true;
    }

    /**
     * Clears the stored userinfo, done in async
     */
    public static void ClearUser(Context context) {
        SetPref(userInfoKey, "", context);
    }

    //region -   Events
    public static void SaveEvents(Context context)
    {
        Gson gson = new Gson();
        String json = gson.toJson(Events.get.data, eventListType);
        SetPref(eventInfoKey, json, context);
    }

    public static boolean LoadEvents (Context context)
    {
        if(hasLoadedEvents || !HasKey(eventInfoKey, context)) return false;

        String json = GetPref(eventInfoKey, context);
        if(json.isEmpty())
            return false;

        try {
            Gson gson = new Gson();
            Events.get.data = gson.fromJson(json, eventListType);
            Events.get.GenerateSortedLists(true);
        }
        catch (Exception e){    //This may happen if the userinfo class changes
            e.printStackTrace();
            return false;
        }

        hasLoadedEvents = true;
        return true;
    }

    public static void ClearEvents(Context context) {
        SetPref(eventInfoKey, "", context);
    }
//endregion

    //region -   Jobs
    public static void SaveJobs(Context context)
    {
        Gson gson = new Gson();
        String json = gson.toJson(Jobs.get.data, jobListType);
        SetPref(jobInfoKey, json, context);
    }

    public static boolean LoadJobs (Context context)
    {
        if(hasLoadedJobs || !HasKey(jobInfoKey, context)) return false;

        String json = GetPref(jobInfoKey, context);
        if(json.isEmpty())
            return false;

        try {
            Gson gson = new Gson();
            Jobs.get.data = gson.fromJson(json, jobListType);
            Jobs.get.GenerateSortedLists(true);
        }
        catch (Exception e){    //This may happen if the userinfo class changes
            e.printStackTrace();
            return false;
        }

        hasLoadedJobs = true;
        return true;
    }

    public static void ClearJobs(Context context) {
        SetPref(jobInfoKey, "", context);
    }
//endregion
    ///endregion
}
