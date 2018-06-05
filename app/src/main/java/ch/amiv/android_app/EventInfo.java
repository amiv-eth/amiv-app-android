package ch.amiv.android_app;

import android.util.Log;

import com.android.volley.Request;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * This is all the data about one event AND the current users signup data about that event
 */
public class EventInfo {
//region -   ====Variables====
    public String _id;
    public String title_de;
    public String title_en;
    public String catchphrase_de;
    public String catchphrase_en;
    public String description_de;
    public String description_en;
    public String location;
    public String price;
    public String priority;
    public String additional_fields;

    public String selection_strategy;
    public int signup_count;
    public int spots;

    public String allow_email_signup;
    public String show_website;

    //Media
    public String poster_url;
    public String banner_url;
    //public String img_infoscreen;
    public String thumb_url;

    //Dates
    public Date time_start;
    public Date time_end;
    public Date time_advertising_start;
    public Date time_advertising_end;
    public Date time_register_start;
    public Date time_register_end;
    public Date time_created;
    public Date time_updated;

    private ArrayList<String[]> infos = new ArrayList<>();

    //===Signup related===
    public String signup_id = "";
    public boolean accepted;
    public boolean confirmed;
    public enum CheckinState {none, in, out};
    public CheckinState checked_in = CheckinState.none;
//endregion

    public EventInfo(JSONObject json)
    {
        _id             = json.optString("_id");
        title_en        = json.optString("title_en");
        title_de        = json.optString("title_de");
        description_en  = json.optString("description_en");
        description_de  = json.optString("description_de");
        catchphrase_en  = json.optString("catchphrase_en");
        catchphrase_de  = json.optString("catchphrase_de");
        location        = json.optString("location");
        price           = json.optString("price");
        priority        = json.optString("priority");
        additional_fields = json.optString("additional_fields");

        selection_strategy  = json.optString("selection_strategy");
        signup_count        = json.optInt("signup_count");
        spots               = json.optInt("spots");

        allow_email_signup  = json.optString("allow_email_signup");
        show_website        = json.optString("show_website");

        //Add dates
        String _start = json.optString("time_start");
        String _end= json.optString("time_end");
        String advertising_start = json.optString("time_advertising_start");
        String advertising_end = json.optString("time_advertising_end");
        String register_start = json.optString("time_register_start");
        String register_end = json.optString("time_register_end");
        String _created = json.optString("_created");
        String _updated = json.optString("_updated");

        //convert dates
        SimpleDateFormat format = Requests.dateFormat;
        try {
            if(!_start.isEmpty())           time_start = format.parse(_start);
            if(!_end.isEmpty())             time_end = format.parse(_end);
            if(!advertising_start.isEmpty()) time_advertising_start = format.parse(advertising_start);
            if(!advertising_end.isEmpty())  time_advertising_end = format.parse(advertising_end);
            if(!register_start.isEmpty())   time_register_start = format.parse(register_start);
            if(!register_end.isEmpty())     time_register_end = format.parse(register_end);
            if(!_created.isEmpty())         time_created = format.parse(_created);
            if(!_updated.isEmpty())         time_updated = format.parse(_updated);
        } catch (java.text.ParseException e) {
            e.printStackTrace();
        }

        //Add Media urls, have a separate json object so need to be in a try catch
        try {
            if(json.has("img_poster"))
                poster_url = json.getJSONObject("img_poster").optString("file");
            else
                poster_url = "";

            if(json.has("img_banner"))
                banner_url = json.getJSONObject("img_banner").optString("file");
            else
                banner_url = "";
            /*
            if(json.has("img_infoscreen"))
                img_infoscreen = json.getJSONObject("img_infoscreen").optString("file");
            else
                img_infoscreen = "";*/

            if(json.has("img_thumbnail"))
                thumb_url = json.getJSONObject("img_thumbnail").optString("file");
            else
                thumb_url = "";

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void AddSignup(JSONObject json) {
        if(json.has("event") && !json.optString("event").equals(_id))
            Log.e("events", "Adding signup object to event of different id, signup event id: " + json.optString("event") + ", adding to event: " + _id);
        accepted = json.optBoolean("accepted", false);
        confirmed = json.optBoolean("confirmed", false);
        signup_id = json.optString("_id", "");

        String checkin_ = json.optString("checked_in");
        if(!checkin_.isEmpty())
        {
            if(checkin_.equalsIgnoreCase("null"))
                checked_in = CheckinState.none;
            else if(checkin_.equalsIgnoreCase("true"))
                checked_in = CheckinState.in;
            else if(checkin_.equalsIgnoreCase("false"))
                checked_in = CheckinState.out;
        }
    }

    /**
     * Choose the key value pairs to be displayed in the event info section when viewing the event in detail
     * @return
     */
    public ArrayList<String[]> GetInfos(){
        if(infos != null && infos.size() > 0)
            return infos;

        DateFormat dateFormat = new SimpleDateFormat("yyyy - MMM - dd HH:mm");
        if(time_start != null) infos.add(new String[]{"Start", dateFormat.format(time_start)});
        if(time_end != null) infos.add(new String[]{"End", dateFormat.format(time_end)});
        if(!price.isEmpty()) infos.add(new String[]{"Price", (price.equalsIgnoreCase("0") ? "Free" : price)});
        if(!location.isEmpty()) infos.add(new String[]{"Location", location});
        if(time_register_start != null) infos.add(new String[]{"Register Start", dateFormat.format(time_register_start)});
        if(time_register_end != null) infos.add(new String[]{"Register End", dateFormat.format(time_register_end)});
        infos.add(new String[]{"Available Places", (spots == 0 ? "-" : "" + Math.max(0, spots - signup_count))});
        if(spots - signup_count < 0)
            infos.add(new String[]{"Waiting List Size", "" + (signup_count - spots)});

        return infos;
    }

    public boolean IsSignedUp ()
    {
        return !signup_id.isEmpty();
    }

    public void ClearSignup() {
        signup_id = "";
        accepted = false;
        confirmed = false;
        checked_in = CheckinState.none;
    }
}
