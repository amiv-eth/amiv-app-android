package ch.amiv.android_app.core;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * This is the central place for storing information about the events, events + signups.
 */
public final class Events {
    public static List<EventInfo> eventInfos = new ArrayList<EventInfo>();

    /**
     * Update the event infos with the data received from the api. This is just for updating information about the event NOT the signup
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

    /**
     * Add signups to their events, use this when the data is received from the api in a jsonArray
     * @param json
     */
    public static void AddSignupArray(JSONArray json) {
        boolean[] hasUpdatedEvent = new boolean[eventInfos.size()];

        signupLoop:
        for (int i = 0; i < json.length(); i++) {
            try {
                JSONObject signup = json.getJSONObject(i);
                if(signup.has("event")) {
                    String event = signup.getString("event");
                    for (int j = 0; j < eventInfos.size(); j++) {
                        if (!hasUpdatedEvent[j] && event.equals(eventInfos.get(j)._id)) {
                            Events.eventInfos.get(j).AddSignup(signup);
                            hasUpdatedEvent[j] = true;
                            continue signupLoop;
                        }
                    }
                    Log.e("events", "Received signup for event that does not exist locally, event id:" + signup.getString("event"));
                }
            }
            catch (JSONException e){
                e.printStackTrace();
            }
        }
    }

    /**
     * Will clear the signup data for each event, use this when changing user
     */
    public static void ClearSignups() {
        for (int i = 0; i < eventInfos.size(); i++){
            eventInfos.get(i).ClearSignup();
        }
    }
}
