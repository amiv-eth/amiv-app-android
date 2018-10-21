package ch.amiv.android_app.events;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import ch.amiv.android_app.core.Settings;
import ch.amiv.android_app.util.ApiListBase;

/**
 * This is the central place for storing information about the events, events + signups.
 * Attention! eventInfo and sorted indexes may change, the events will allways be sorted according to the adDateComparator.
 */
public class Events extends ApiListBase<EventInfo> {
    //region -   Variables
    public static Events get = new Events();

    public static boolean[] invertEventGroupSorting = new boolean[] {false, false, false, true};    //used to invert date sorting for the event groups
    public static final int DAYS_NEW_TAG_ACTIVE = 3;    //Defines for how many days after the ad start date the new tag is visible for

    /**
     * Use this class to use the correct indexes for the event group for the sorted list.
     * Sidenote: we use the term 'group' and 'category' interchangeably
     */

    public static final class EventGroup {
        public static final int SIZE            = 4;
        public static final int HIDDEN_EVENTS   = 0;
        public static final int CURRENT_EVENTS  = 1;
        public static final int CLOSED_EVENTS   = 2;
        public static final int PAST_EVENTS     = 3;

        private static final int HIDDEN_EVENTS_EXP_SIZE   = 2;
        private static final int CURRENT_EVENTS_EXP_SIZE  = 10;
        private static final int CLOSED_EVENTS_EXP_SIZE   = 5;
        private static final int PAST_EVENTS_EXP_SIZE     = 20;
    }

    private static Comparator<EventInfo> adDateComparator = new Comparator<EventInfo>() {
        @Override
        public int compare(EventInfo a, EventInfo b) {
            return b.time_advertising_start.compareTo(a.time_advertising_start);
        }
    };

    @Override
    public Comparator<EventInfo> GetItemComparator() {
        return adDateComparator;
    }
    //endregion

    //region -   Override functions from ApiListBase
    /**
     * @return An instance of EventInfo parsed from the json
     */
    @Override
    public EventInfo CreateItem(JSONObject json) {
        return new EventInfo(json);
    }

    /**
     * @return The amount of categories in the sorted array, 1st dim
     */
    @Override
    protected int GetSortedSize1() {
        return EventGroup.SIZE;
    }

    /**
     * @param category The 2nd dim of the sorted list
     * @return The expected initialisation size
     */
    @Override
    protected int GetSortedSize2Expected(int category) {
        if(category == EventGroup.HIDDEN_EVENTS)
            return EventGroup.HIDDEN_EVENTS_EXP_SIZE;
        if(category == EventGroup.CURRENT_EVENTS)
            return EventGroup.CURRENT_EVENTS_EXP_SIZE;
        if(category == EventGroup.CLOSED_EVENTS)
            return EventGroup.CLOSED_EVENTS_EXP_SIZE;
        if(category == EventGroup.PAST_EVENTS)
            return EventGroup.PAST_EVENTS_EXP_SIZE;
        return 0;
    }

    @Override
    public void SaveToCache(Context context){
        Settings.SaveEvents(context);
    }

    @Override
    public void LoadFromCache(Context context) {
        Settings.LoadEvents(context);
    }

    /**
     * @return The category of the sorted list the item fits in, 1st dim
     */
    @Override
    public int GetItemCategory(EventInfo item) {
        Date today = Calendar.getInstance().getTime();

        if(item.time_advertising_start.after(today))   //Determine which group the event is in, by date
            return EventGroup.HIDDEN_EVENTS;
        else if(item.time_register_end.after(today))
            return EventGroup.CURRENT_EVENTS;
        else if(item.time_end.after(today))
            return EventGroup.CLOSED_EVENTS;
        else
            return EventGroup.PAST_EVENTS;

    }
    //endregion


//region ===========Event Signups===========
    /**
     * Add signups to their events, use this when the data is received from the api in a jsonArray
     */
    public void AddSignupArray(JSONArray json) {
        boolean[] hasUpdatedEvent = new boolean[data.size()];

        signupLoop:
        for (int i = 0; i < json.length(); i++) {
            try {
                JSONObject signup = json.getJSONObject(i);
                if(signup.has("event")) {
                    String eventId = signup.getString("event");
                    for (int j = 0; j < data.size(); j++) {
                        if (!hasUpdatedEvent[j] && eventId.equals(data.get(j)._id)) {
                            data.get(j).AddSignup(signup);
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
    public void ClearSignups() {
        for (int i = 0; i < data.size(); i++){
            data.get(i).ClearSignup();
        }
    }
    //endregion
}
