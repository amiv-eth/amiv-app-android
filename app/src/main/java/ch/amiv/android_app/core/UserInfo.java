package ch.amiv.android_app.core;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.EmptyStackException;
import java.util.zip.CheckedOutputStream;

import ch.amiv.android_app.events.Events;
import ch.amiv.android_app.util.PersistentStorage;

public class UserInfo implements Serializable{
    public static UserInfo current;

    public String _id = "";
    public String session_id = "";
    public String session_etag = "";
    public String nethz = "";
    public String legi = "";
    public String rfid = "";
    public String email = "";

    public String firstname = "";
    public String lastname = "";
    public String membership = "";
    public String gender = "";
    public String phone = "";
    public String department = "";
    public boolean send_newsletter = true;

    private UserInfo(JSONObject json, boolean isFromTokenRequest)
    {
        Update(json, isFromTokenRequest);
    }

    private UserInfo (String email){
        Update(email);
    }

    /**
     * Used when the user only logs in with an email
     * @param email_
     */
    public void Update(String email_){
        email = email_;
    }

    /**
     * Use this to update the user info with a json. Can handle partial update where old items will not be overwritten if they do not exist
     * @param isFromTokenRequest set to true when the json is from a /sessions POST request, where the user ID is "user" not "_id"
     */
    public void Update(JSONObject json, boolean isFromTokenRequest){
        try {
            //dont use optString as this will overwrite the current value to "" if it does not exist
            if (json.has(isFromTokenRequest ? "user" : "_id"))
                _id = json.getString(isFromTokenRequest ? "user" : "_id");

            if(json.has("session_etag"))
                session_etag = json.getString("session_etag");  //if from a gson saved locally
            else if(isFromTokenRequest && json.has("_etag"))
                session_etag = json.getString("_etag");


            if(json.has("session_id"))
                session_id = json.getString("session_id");
            else if(isFromTokenRequest && json.has("_id"))
                session_id = json.getString("_id");

            if (json.has("legi"))
                legi = json.getString("legi");
            if (json.has("rfid"))
                rfid = json.getString("rfid");
            if (json.has("firstname"))
                firstname = json.getString("firstname");
            if (json.has("lastname"))
                lastname = json.getString("lastname");
            if (json.has("nethz"))
                nethz = json.getString("nethz");
            if (json.has("email"))
                email = json.getString("email");

            if (json.has("membership"))
                membership = json.getString("membership");
            if (json.has("gender"))
                gender = json.getString("gender");
            if (json.has("phone"))
                phone = json.getString("phone");
            if (json.has("department"))
                department = json.getString("department");
            if (json.has("send_newsletter"))
                send_newsletter = json.getBoolean("send_newsletter");
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Will safely update the current user or create it
     * @param isFromTokenRequest This ensures the correct values are retrieved from the json. Set to true when the json is from a /sessions POST request, where the user ID is "user" not "_id" as usually
     */
    public static void UpdateCurrent(Context context, JSONObject json, boolean isFromTokenRequest, boolean isSavedInstance){
        if(current != null)
            current.Update(json, isFromTokenRequest);
        else {
            current = new UserInfo(json, isFromTokenRequest);
        }

        if(!isSavedInstance)
            PersistentStorage.SaveUserInfo(context);
    }

    public static void SetEmailOnlyLogin(Context context, String email, boolean isSavedInstance){
        if(current != null)
            current.Update(email);
        else {
            current = new UserInfo(email);
        }

        if(!isSavedInstance)
            PersistentStorage.SaveUserInfo(context);
    }

    public static void LogoutUser(Context context){
        if(Settings.IsEmailOnlyLogin(context)) {
            Settings.SetToken("", context);
        }
        else {
            //delete session at the server and then clear the token
            Requests.DeleteCurrentSession(context);
            Events.ClearSignups();
        }

        PersistentStorage.ClearUser(context);
        UserInfo.current = null;
    }
}
