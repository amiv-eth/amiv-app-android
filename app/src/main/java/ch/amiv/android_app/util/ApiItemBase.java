package ch.amiv.android_app.util;

import android.util.Log;

import org.json.JSONObject;

public abstract class ApiItemBase {
    public String _id;

    public abstract void Update(JSONObject json);
    public static String GetId(JSONObject json){
        return json.optString("_id");
    }
}
