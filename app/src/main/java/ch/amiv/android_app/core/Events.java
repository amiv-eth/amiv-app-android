package ch.amiv.android_app.core;

import android.support.annotation.NonNull;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * This is the central place for storing information about the events, events + signups.
 */
public final class Events {
    public static List<EventInfo> eventInfos = new ArrayList<EventInfo>(); //This is a list of ALL events as received from the api, we will not use this directly
    public static List<List<EventInfo>> sortedEvents = new ArrayList<List<EventInfo>>(4);   //A list of lists which has been sorted according to the EventGroup configuration
    public static boolean[] invertEventGroupSorting = new boolean[] {false, false, false, true};    //used to invert date sorting for the event groups

    //Use this class to use the correct indexes for the event group for the sortedEvents list
    public static final class EventGroup {
        public static final int HIDDEN_EVENTS   = 0;
        public static final int ALL_EVENTS      = 1;
        public static final int CLOSED_EVENTS   = 2;
        public static final int PAST_EVENTS     = 3;
    }

    //Defines for how many days after the ad start date the new tag is visible for
    public static final int DAYS_NEW_TAG_ACTIVE = 3;

    /**
     * Update the event infos with the data received from the api. This is just for updating information about the event NOT the signup
     * @param json json array of the events.
     */
    public static void UpdateEventInfos(JSONArray json)
    {
        eventInfos.clear();
        sortedEvents.clear();

        for (int i = 0; i < json.length(); i++)
        {
            try {
                eventInfos.add(new EventInfo(json.getJSONObject(i)));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if(!eventInfos.isEmpty()){
            //sort so first elem has an advertising start date furthest in the future
            Comparator<EventInfo> comparator;
            comparator = new Comparator<EventInfo>() {
                @Override
                public int compare(EventInfo a, EventInfo b) {
                    return b.time_advertising_start.compareTo(a.time_advertising_start);
                }
            };

            Collections.sort(eventInfos, comparator);

            Date today = Calendar.getInstance().getTime();
            sortedEvents.add(new ArrayList<EventInfo>());
            sortedEvents.add(new ArrayList<EventInfo>());
            sortedEvents.add(new ArrayList<EventInfo>());
            sortedEvents.add(new ArrayList<EventInfo>());

            //fill in the sorted list according to the dates of the events
            for (int i = 0; i < eventInfos.size(); i++){
                if(eventInfos.get(i).time_advertising_start.after(today))
                    sortedEvents.get(EventGroup.HIDDEN_EVENTS).add(eventInfos.get(i));
                else if(eventInfos.get(i).time_register_end.after(today))
                    sortedEvents.get(EventGroup.ALL_EVENTS).add(eventInfos.get(i));
                else if(eventInfos.get(i).time_end.after(today))
                    sortedEvents.get(EventGroup.CLOSED_EVENTS).add(eventInfos.get(i));
                else
                    sortedEvents.get(EventGroup.PAST_EVENTS).add(eventInfos.get(i));
            }
        }
    }

    /**
     * Do not use this to create a single event, will update a given event with the id
     * @param json
     * @param eventId
     */
    public static void UpdateSingleEvent(JSONObject json, @NonNull String eventId){
        for (int i = 0; i < eventInfos.size(); i++){
            if(eventInfos.get(i)._id.equalsIgnoreCase(eventId))
                eventInfos.get(i).UpdateEvent(json);
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