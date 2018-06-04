package ch.amiv.android_app;

import org.json.JSONException;
import org.json.JSONObject;

public class UserInfo {
    public static UserInfo current;

    public String _id = "";
    public String firstname = "";
    public String lastname = "";
    public String nethz = "";
    public String email = "";
    public String membership = "";
    public String gender = "";

    public UserInfo (JSONObject json)
    {
        try {
            if (json.has("_id"))
                _id = json.getString("_id");
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
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void SetAsCurrent(){
        current = this;
    }
}
