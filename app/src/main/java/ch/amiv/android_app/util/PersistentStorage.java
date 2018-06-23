package ch.amiv.android_app.util;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import ch.amiv.android_app.core.Settings;
import ch.amiv.android_app.core.UserInfo;
import ch.amiv.android_app.events.Events;

import static android.content.Context.MODE_PRIVATE;

/**
 *
 */
public final class PersistentStorage {
    private static SharedPreferences prefs;
    private static final String userKey= "user";

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

    public static void ClearUser(Context context)
    {
        Init(context);
        SharedPreferences.Editor prefsEditor = prefs.edit();
        prefsEditor.putString(userKey, "");
        prefsEditor.apply();
    }
}
