package ch.amiv.android_app;

import android.media.Image;

import org.json.JSONException;
import org.json.JSONObject;

public class EventInfo {
    public String _id;
    public String title;
    public String description;
    public String catchphrase;
    public String spots;
    public String allow_email_signup;
    public String show_website;
    public String time_register_end;
    public String _created;
    public String posterUrl;

    public EventInfo(JSONObject json)
    {
        _id = json.optString("_id");
        title = json.optString("title_en");
        description = json.optString("description_en");
        catchphrase = json.optString("catchphrase_en");
        spots = json.optString("spots");
        allow_email_signup = json.optString("allow_email_signup");
        show_website = json.optString("show_website");
        time_register_end = json.optString("time_register_end");
        _created = json.optString("_created");
        try {
            posterUrl = json.getJSONObject("img_poster").optString("file");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
