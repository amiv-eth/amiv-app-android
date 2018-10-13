package ch.amiv.android_app.events;

import android.content.Context;
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

import ch.amiv.android_app.core.Settings;

/**
 * This is the central place for storing information about the events, events + signups.
 * Attention! eventInfo and sortedEventInfos indexes may change, the events will allways be sorted according to the adDateComparator.
 */
public final class Events {
    public static List<EventInfo> eventInfos = new ArrayList<EventInfo>(); //This is a list of ALL events as received from the api, we will not use this directly
    public static List<List<EventInfo>> sortedEventInfos = new ArrayList<>(EventGroup.SIZE);   //A list of lists which has been sorted according to the EventGroup configuration
    public static boolean[] invertEventGroupSorting = new boolean[] {false, false, false, true};    //used to invert date sorting for the event groups

    //Use this class to use the correct indexes for the event group for the sortedEventInfos list
    public static final class EventGroup {
        public static final int SIZE            = 4;
        public static final int HIDDEN_EVENTS   = 0;
        public static final int CURRENT_EVENTS  = 1;
        public static final int CLOSED_EVENTS   = 2;
        public static final int PAST_EVENTS     = 3;
    }

    //Defines for how many days after the ad start date the new tag is visible for
    public static final int DAYS_NEW_TAG_ACTIVE = 3;

    private static Comparator<EventInfo> adDateComparator = new Comparator<EventInfo>() {
        @Override
        public int compare(EventInfo a, EventInfo b) {
            return b.time_advertising_start.compareTo(a.time_advertising_start);
        }
    };

    /**
     * This is the key function of this class. It converts a json for several events to java EventInfos
     * Update the event infos with the data received from the api. This is just for updating information about the event NOT the signup
     * @param json json array of the events.
     */
    public static void UpdateEventInfos(Context context, JSONArray json)
    {
        boolean isInitialising = eventInfos.size() == 0;

        for (int i = 0; i < json.length(); i++)
        {
            try {
                //if we are not initialising, search for the event id and then update it, else add a new one to the list. This ensures we do not lose the signup data
                JSONObject jsonEvent = json.getJSONObject(i);
                EventInfo e = new EventInfo(jsonEvent);
                if(e._id.isEmpty())
                    continue;
                if(isInitialising || !UpdateSingleEvent(jsonEvent, e._id))
                    eventInfos.add(e);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        GenerateSortedLists(isInitialising);

        Settings.SaveEvents(context);
    }

    public static void GenerateSortedLists(boolean isInitialising)
    {
        if(isInitialising){
            sortedEventInfos = new ArrayList<>(EventGroup.SIZE);
            for (int k = 0; k < EventGroup.SIZE; k++)
                sortedEventInfos.add(new ArrayList<EventInfo>());
        }
        else {
            for (int k = 0; k < sortedEventInfos.size(); k++)
                sortedEventInfos.get(k).clear();
        }

        //Sort list and update sorted list
        if(!eventInfos.isEmpty()){
            //sort so first elem has an advertising start date furthest in the future
            Collections.sort(eventInfos, adDateComparator);

            //fill in the sorted list according to the dates of the events
            for (int i = 0; i < eventInfos.size(); i++){
                AddEventToSorted(eventInfos.get(i), false);
            }
        }
    }

    /**
     * Will add the event to the sorted array. Use AddEvent to add an event, this is just used to update the sortedEventInfos list accordingly
     * @param sortAfterInsert Will resort the group after insertion, by date
     */
    private static void AddEventToSorted(EventInfo eventInfo, boolean sortAfterInsert){
        Date today = Calendar.getInstance().getTime();
        int group;

        if(eventInfo.time_advertising_start.after(today))   //Determine which group the event is in, by date
            group = EventGroup.HIDDEN_EVENTS;
        else if(eventInfo.time_register_end.after(today))
            group = EventGroup.CURRENT_EVENTS;
        else if(eventInfo.time_end.after(today))
            group = EventGroup.CLOSED_EVENTS;
        else
            group = EventGroup.PAST_EVENTS;

        if(sortedEventInfos == null || sortedEventInfos.size() == 0)
            GenerateSortedLists(true);
        else
            sortedEventInfos.get(group).add(eventInfo);

        if(sortAfterInsert) {
            Collections.sort(eventInfos, adDateComparator);
            Collections.sort(sortedEventInfos.get(group), adDateComparator);
        }
    }

    /**
     * Will add a new event to the eventInfos and sortedEventInfos, and update if the event already exists
     * @return The index of the event in eventInfos. If the event already exists it will return that index. -1 if the json is invalid, ie no _id found
     */
    public static int AddEvent(JSONObject json, Context context){
        try {
            String id = json.getString("_id");
            int index = GetEventIndexById(id);
            if(index >= 0) {
                eventInfos.get(index).UpdateEvent(json);
                return index;
            }   
        } catch (JSONException e) {
            e.printStackTrace();
            return -1;
        }

        EventInfo e = new EventInfo(json);
        eventInfos.add(e);
        AddEventToSorted(e, true);

        Settings.SaveEvents(context);

        return eventInfos.size() -1;
    }

    /**
     * Will update a given event with the id
     * @return true if the event was found and updated
     */
    public static boolean UpdateSingleEvent(JSONObject json, @NonNull String eventId){
        EventInfo event = GetEventById(eventId);

        if(event == null) return false;

        event.UpdateEvent(json);
        return true;
    }

    /**
     * Note: Try to use the sorted/eventInfos lists to access by index. Only use this if you are having issues due to the indexes changing from the lists.
     * @return The event with the corresponding _id. Returns null if the id was not found.
     */
    public static EventInfo GetEventById(String id){
        int index = GetEventIndexById(id);
        if(index >= 0)
            return eventInfos.get(index);

        return null;
    }

    /**
     *
     * @param id The event id
     * @return The index in eventInfos (unsorted list)
     */
    public static int GetEventIndexById(String id){
        if(id == null || id.isEmpty()) return -1;

        for (int i = 0; i < eventInfos.size(); ++i) {
            if(eventInfos.get(i)._id.equalsIgnoreCase(id))
                return i;
        }

        return -1;
    }

    /**
     * Add signups to their events, use this when the data is received from the api in a jsonArray
     */
    public static void AddSignupArray(JSONArray json) {
        boolean[] hasUpdatedEvent = new boolean[eventInfos.size()];

        signupLoop:
        for (int i = 0; i < json.length(); i++) {
            try {
                JSONObject signup = json.getJSONObject(i);
                if(signup.has("event")) {
                    String eventId = signup.getString("event");
                    for (int j = 0; j < eventInfos.size(); j++) {
                        if (!hasUpdatedEvent[j] && eventId.equals(eventInfos.get(j)._id)) {
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
