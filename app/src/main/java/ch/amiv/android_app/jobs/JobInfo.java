package ch.amiv.android_app.jobs;

import android.content.res.Resources;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import ch.amiv.android_app.core.Request;

import static ch.amiv.android_app.util.Util.BuildFileUrl;

public class JobInfo {
//region -   ====Variables====
    public String _id;
    public String _etag;
    private String title_de;
    private String title_en;
    private String description_de;
    private String description_en;

    public String company;
    private String logo_url;
    public String pdf_url;
    public boolean show_website;

    public Date time_end;
    public Date time_created;
    public Date time_updated;
//endregion

    public JobInfo(JSONObject json) {
        UpdateJob(json);
    }

    /**
     * Overwrite the current data
     */
    public void UpdateJob(JSONObject json)
    {
        _id             = json.optString("_id");
        _etag           = json.optString("_etag");
        title_en        = json.optString("title_en");
        title_de        = json.optString("title_de");
        description_en  = json.optString("description_en");
        description_de  = json.optString("description_de");
        company         = json.optString("company");
        show_website    = json.optBoolean("show_website", false);

        //Add dates
        String _end= json.optString("time_end");
        String _created = json.optString("_created");
        String _updated = json.optString("_updated");

        //convert dates
        SimpleDateFormat format = Request.dateFormat;
        try {
            if(!_end.isEmpty())
                time_end = format.parse(_end);
            if(!_created.isEmpty())
                time_created = format.parse(_created);
            if(!_updated.isEmpty())
                time_updated = format.parse(_updated);

        } catch (java.text.ParseException e) {
            e.printStackTrace();
        }

        //Add Media urls, have a separate json object so need to be in a try catch
        try {
            if(json.has("logo"))
                logo_url = json.getJSONObject("logo").optString("file");
            else
                logo_url = "";

            if(json.has("pdf"))
                pdf_url = json.getJSONObject("pdf").optString("file");
            else
                pdf_url = "";

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the title for the current set language
     */
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

    public String GetLogoUrl() {
        return BuildFileUrl(logo_url);
    }

    public String GetPdfUrl() {
        return BuildFileUrl(pdf_url);
    }
}
