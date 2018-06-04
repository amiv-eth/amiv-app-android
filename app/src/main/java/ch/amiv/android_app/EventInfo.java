package ch.amiv.android_app;

import android.media.Image;
import android.net.ParseException;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventInfo {
    public String _id;
    public String title;
    public String description;
    public String catchphrase;
    public String spots;
    public String allow_email_signup;
    public String show_website;
    public String posterUrl;

    //Dates
    public Date time_register_start;
    public Date time_register_end;
    public Date time_created;
    public Date time_updated;

    private ArrayList<String[]> infos = new ArrayList<>();

    //Signup related
    public boolean accepted;
    public boolean confirmed;
    public String signup_id = "";

    /**
     * Choose the key value pairs to be displayed in the event info section when viewing the event in detail
     * @return
     */
    public ArrayList<String[]> GetInfos(){
        DateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy HH:mm");
        infos.add(new String[]{"Register Start", dateFormat.format(time_register_start)});
        infos.add(new String[]{"Register End", dateFormat.format(time_register_end)});
        infos.add(new String[]{"Available Places", spots});

        return infos;
    }

    public EventInfo(JSONObject json)
    {
        _id = json.optString("_id");
        title = json.optString("title_en");
        description = json.optString("description_en");
        catchphrase = json.optString("catchphrase_en");
        spots = json.optString("spots");
        allow_email_signup = json.optString("allow_email_signup");
        show_website = json.optString("show_website");
        String register_start = json.optString("time_register_start");
        String register_end = json.optString("time_register_end");
        String _created = json.optString("_created");
        String _updated = json.optString("_updated");
        try {
            if(json.has("img_poster"))
                posterUrl = json.getJSONObject("img_poster").optString("file");
            else
                posterUrl = "";
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //convert dates
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        try {
            time_register_start = format.parse(register_start);
            time_register_end = format.parse(register_end);
            time_created = format.parse(_created);
            time_updated = format.parse(_updated);
        } catch (java.text.ParseException e) {
            e.printStackTrace();
        }
    }

    public void AddSignup(JSONObject json) {
        accepted = json.optBoolean("accepted", false);
        confirmed = json.optBoolean("confirmed", false);
        signup_id = json.optString("_id", "");
    }
}
