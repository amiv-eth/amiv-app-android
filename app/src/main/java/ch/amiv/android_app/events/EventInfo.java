package ch.amiv.android_app.events;

import android.content.res.Resources;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import ch.amiv.android_app.R;
import ch.amiv.android_app.core.Request;
import ch.amiv.android_app.util.ApiItemBase;
import ch.amiv.android_app.util.Util;

import static ch.amiv.android_app.util.Util.BuildFileUrl;

/**
 * This is all the data about one event AND the current users signup data about that event
 */
public class EventInfo extends ApiItemBase implements Serializable{
//region -   ====Variables====
    //public String _id;  //inherited from ApiItemBase
    public String _etag;
    private String title_de;
    private String title_en;
    private String catchphrase_de;
    private String catchphrase_en;
    private String description_de;
    private String description_en;
    public String location;
    public String price;
    public String priority;

    public String selection_strategy;
    public int signup_count;
    public int spots;

    public boolean allow_email_signup;
    public boolean show_website;

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
    public enum CheckinState {none, in, out}
    public CheckinState checked_in = CheckinState.none;

    public AdditField[] additional_fields;
//endregion

    public EventInfo(JSONObject json) {
        Update(json);
    }

    /**
     * Overwrite the current data
     */
    public void Update(JSONObject json)
    {
        _id             = json.optString("_id");
        _etag           = json.optString("_etag");
        title_en        = json.optString("title_en");
        title_de        = json.optString("title_de");
        description_en  = Util.ApplyStringFormatting(json.optString("description_en"));
        description_de  = Util.ApplyStringFormatting(json.optString("description_de"));
        catchphrase_en  = json.optString("catchphrase_en");
        catchphrase_de  = json.optString("catchphrase_de");
        location        = json.optString("location");
        price           = json.optString("price");
        priority        = json.optString("priority");
        try {
            additional_fields = AdditField.ParseFromJson(new JSONObject(json.getString("additional_fields")));
        } catch (JSONException e) { additional_fields = new AdditField[0]; }

        selection_strategy  = json.optString("selection_strategy");
        signup_count        = json.optInt("signup_count");
        spots               = json.optInt("spots");

        allow_email_signup  = json.optBoolean("allow_email_signup", false);
        show_website        = json.optBoolean("show_website", false);

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
        SimpleDateFormat format = Request.dateFormat;
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
     */
    public ArrayList<String[]> GetInfos(Resources r){
        if(infos != null && infos.size() > 0)
            infos.clear();

        DateFormat dateFormat = new SimpleDateFormat("dd - MMM - yyyy HH:mm", r.getConfiguration().locale);
        if(time_start != null) infos.add(new String[]{ r.getString(R.string.start_time), dateFormat.format(time_start)});
        if(time_end != null) infos.add(new String[]{ r.getString(R.string.end_time), dateFormat.format(time_end)});
        if(!price.isEmpty()) infos.add(new String[]{ r.getString(R.string.price), (price.equalsIgnoreCase("0") ? r.getString(R.string.price_free) : price + " CHF")});
        if(!location.isEmpty()) infos.add(new String[]{ r.getString(R.string.location), location});
        if(time_register_start != null) infos.add(new String[]{ r.getString(R.string.register_start), dateFormat.format(time_register_start)});
        if(time_register_end != null) infos.add(new String[]{ r.getString(R.string.register_end), dateFormat.format(time_register_end)});

        /*//DEBUG
        if(time_advertising_start != null) infos.add(new String[]{ ("Ad Start"), dateFormat.format(time_advertising_start)});
        if(time_advertising_end != null) infos.add(new String[]{ ("Ad End"), dateFormat.format(time_advertising_end)});*/

        infos.add(new String[]{ r.getString(R.string.available_places), (spots <= 0 ? "-" : "" + Math.max(0, spots - signup_count))});
        if(spots - signup_count < 0)
            infos.add(new String[]{ r.getString(R.string.waiting_list_size), "" + (signup_count - spots)});

        return infos;
    }

    public String GetTitle (Resources res)
    {
        Locale locale = res.getConfiguration().locale;
        if(res.getConfiguration().locale.equals(Locale.GERMAN) && !title_de.isEmpty())
            return title_de;
        else
            return title_en;
    }

    public String GetDescription(Resources res)
    {
        if(res.getConfiguration().locale.equals(Locale.GERMAN) && !description_de.isEmpty())
            return description_de;
        else
            return description_en;
    }

    public String GetCatchphrase(Resources res)
    {
        if(res.getConfiguration().locale.equals(Locale.GERMAN) && !catchphrase_de.isEmpty())
            return catchphrase_de;
        else
            return catchphrase_en;
    }

    public String GetPosterUrl() {
        return BuildFileUrl(poster_url);
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
