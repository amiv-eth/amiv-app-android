package ch.amiv.android_app.util;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Vector;

import ch.amiv.android_app.core.Settings;
import ch.amiv.android_app.core.UserInfo;
import ch.amiv.android_app.events.EventInfo;
import ch.amiv.android_app.events.Events;
import ch.amiv.android_app.jobs.JobInfo;
import ch.amiv.android_app.jobs.Jobs;

import static android.content.Context.MODE_PRIVATE;

/**
 *
 */
public final class PersistentStorage {
    private static SharedPreferences prefs;
    private static final String userKey = "user";
    private static final String eventsKey = "events";
    private static final Type eventListType = new TypeToken<List<EventInfo>>() {}.getType();
    private static final String jobsKey = "jobs";
    private static final Type jobListType = new TypeToken<List<JobInfo>>() {}.getType();

    private static void Init(Context context){
        if(prefs == null)
            prefs = context.getSharedPreferences(Settings.SHARED_PREFS_KEY, MODE_PRIVATE);
    }

    public static void SaveUserInfo(Context context)
    {
        Init(context);
        SharedPreferences.Editor prefsEditor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(UserInfo.current);
        prefsEditor.putString(userKey, json);
        prefsEditor.apply();
    }

    public static boolean LoadUserInfo (Context context)
    {
        Init(context);
        if(!prefs.contains(userKey))
            return false;

        String json = prefs.getString(userKey, "");
        if(json.isEmpty())
            return false;

        try {
            UserInfo.UpdateCurrent(context, new JSONObject(json), false, true);
        }
        catch (Exception e){    //This may happen if the userinfo class changes
            e.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * Clears the stored userinfo, done in async
     * @param context
     */
    public static void ClearUser(Context context)
    {
        Init(context);
        SharedPreferences.Editor prefsEditor = prefs.edit();
        prefsEditor.putString(userKey, "");
        prefsEditor.apply();    //use commit() to run in serial
    }

    //region -   Events
    public static void SaveEvents(Context context)
    {
        Init(context);
        SharedPreferences.Editor prefsEditor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(Events.eventInfos, eventListType);
        prefsEditor.putString(eventsKey, json);
        prefsEditor.apply();
    }

    public static boolean LoadEvents (Context context)
    {
        Init(context);
        if(!prefs.contains(eventsKey))
            return false;

        String json = prefs.getString(eventsKey, "");
        if(json.isEmpty())
            return false;

        try {
            Gson gson = new Gson();
            Events.eventInfos = gson.fromJson(json, eventListType);
            Events.GenerateSortedLists(true);
        }
        catch (Exception e){    //This may happen if the userinfo class changes
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public static void ClearEvents(Context context)
    {
        Init(context);
        SharedPreferences.Editor prefsEditor = prefs.edit();
        prefsEditor.putString(eventsKey, "");
        prefsEditor.apply();
    }
//endregion

//region -   Jobs
    public static void SaveJobs(Context context)
    {
        Init(context);
        SharedPreferences.Editor prefsEditor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(Jobs.jobInfos, jobListType);
        prefsEditor.putString(jobsKey, json);
        prefsEditor.apply();
    }

    public static boolean LoadJobs (Context context)
    {
        Init(context);
        if(!prefs.contains(jobsKey))
            return false;

        String json = prefs.getString(jobsKey, "");
        if(json.isEmpty())
            return false;

        try {
            Gson gson = new Gson();
            Jobs.jobInfos = gson.fromJson(json, jobListType);
            Jobs.GenerateSortedLists(true);
        }
        catch (Exception e){    //This may happen if the userinfo class changes
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public static void ClearJobs(Context context)
    {
        Init(context);
        SharedPreferences.Editor prefsEditor = prefs.edit();
        prefsEditor.putString(jobsKey, "");
        prefsEditor.apply();
    }
//endregion
}
