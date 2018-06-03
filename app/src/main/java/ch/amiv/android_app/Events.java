package ch.amiv.android_app;

import android.support.annotation.NonNull;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public final class Events {
    public static List<EventInfo> eventInfos = new ArrayList<EventInfo>();

    /**
     * Update the event infos with the data received from the api
     * @param json json array of the events.
     */
    public static void UpdateEventInfos(JSONArray json)
    {
        eventInfos.clear();
        for (int i = 0; i < json.length(); i++)
        {
            try {
                eventInfos.add(new EventInfo(json.getJSONObject(i)));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

    }
}
