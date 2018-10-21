package ch.amiv.android_app.core;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

import ch.amiv.android_app.events.Events;

public class UserInfo implements Serializable{
    public static UserInfo current;

    public String _id = "";
    public String _etag = "";
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
            if (json.has("_etag"))
                _etag = json.getString("_etag");

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
            Settings.SaveUserInfo(context);
    }

    public static void SetEmailOnlyLogin(Context context, String email, boolean isSavedInstance){
        if(current != null)
            current.Update(email);
        else {
            current = new UserInfo(email);
        }

        if(!isSavedInstance)
            Settings.SaveUserInfo(context);
    }

    /**
     * Will set the rfid if it is valid. Valid if it has 6 chars
     * @return If the given rfid was valid/different to the current
     */
    public boolean SetRFID (String newRFID){
        if(newRFID == null || newRFID.length() != 6 || newRFID.equalsIgnoreCase(UserInfo.current.rfid))
            return false;

        rfid = newRFID;
        return true;
    }

    public static void LogoutUser(Context context){
        if(Settings.IsEmailOnlyLogin(context)) {
            Settings.SetToken("", context);
        }
        else {
            //delete session at the server and then clear the token
            Request.DeleteCurrentSession(context);
            Events.get.ClearSignups();
        }

        Settings.ClearUser(context);
        UserInfo.current = null;
    }

    //region ---Access Level---
    /**
     * Use this to determine features only accessible to certain users
     */
    public static final class AccessLevel{
        public static final int EVERYONE = 0;
        public static final int EMAIL = 1; //email only login or greater
        public static final int LOGIN = 2;  //anyone with a login
        public static final int ADMIN = 3;  //anyone in the admin user group

        //Other API user groups
        public static final int CHECKIN = 4; //anyone in the checkin user group
    }

    /**
     * @return If the current user is in the accessgroup
     */
    public static boolean IsAuthorised(int accessLevel, Context context){
        //if(BuildConfig.DEBUG) return true;
        //if(UserInfo.current.email.equals("dev")) return true;

        switch (accessLevel){
            case AccessLevel.EMAIL:
                return Settings.IsLoggedIn(context);
            case AccessLevel.LOGIN:
                return Settings.HasToken(context);

            //XXX request to see if user is in API user group
            case AccessLevel.ADMIN:
                return true;
            case AccessLevel.CHECKIN:
                return true;
        }

        return false;
    }

    /**
     * @return True if hidden features should be shown
     */
    public static boolean ShowHiddenFeatures(Context context){
        return IsAuthorised(AccessLevel.ADMIN, context);
    }

    //endregion
}
