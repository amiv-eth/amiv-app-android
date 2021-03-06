package ch.amiv.android_app.checkin;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * This stores all event data received by /get_event_data get request. It can be accessed by the static instance. The instance should be deleted when a new session is started
 * Created by Roger on 28-Feb-18.
 */

public class EventData {
    public String serverId = "0";
    public enum EventType {NotSet, Event, PVK, GV, Counter, Unknown}
    public EventType eventType = EventType.NotSet;
    public enum CheckinType {InOut, Counter}
    public CheckinType checkinType = CheckinType.InOut;
    public String name = "";
    public String description;
    public int signupCount;
    public int spots;   //number of expected people/preregistered members
    public String startTime = "";

    public boolean Update(String _serverId, String _eventType, String _checkinType, String _name, String _description, int _signupCount, int _spots, String _startTime)
    {
        serverId = _serverId;
        name = _name;
        description = _description;
        signupCount = _signupCount;
        spots = _spots;
        startTime = _startTime;

        /*
        if(_checkinType .equalsIgnoreCase("counter"))
            checkinType = CheckinType.Counter;
        else if(_checkinType.equalsIgnoreCase("in_out"))
            checkinType = CheckinType.InOut;*/

        return SetEventType(_eventType);
    }

    public boolean SetEventType(String s)
    {
        checkinType = CheckinType.InOut;    //XXX set the checkin type in a better way, get from json or use a fkt to determine it
        if(s.equalsIgnoreCase("AMIV Events"))   //Matches the human readable string in the dropdown on the checkin login website
            eventType = EventType.Event;
        else if(s.equalsIgnoreCase("AMIV PVK"))   //May need to be adjusted to what is given
            eventType = EventType.PVK;
        else if(s.equalsIgnoreCase("AMIV General Assemblies"))
            eventType = EventType.GV;
        else if(s.equalsIgnoreCase("AMIV Freebies")) {
            eventType = EventType.Counter;
            checkinType = CheckinType.Counter;
        }
        else {
            eventType = EventType.Unknown;
            Log.d("SetEventType()", "Parsing event type unsuccessful, will attempt to imply event type else restrict functionality.");
            return false;
        }

        return true;
    }

    public List<StringPair> GetInfosAsKeyValuePairs ()
    {
        List<StringPair> list = new ArrayList<StringPair>();
        list.add(new StringPair("Event", name));
        list.add(new StringPair("Event Type", eventType.toString()));
        list.add(new StringPair("Sign-ups", "" + signupCount));
        list.add(new StringPair("Description", description));
        list.add(new StringPair("Start Time", startTime));

        return list;
    }
}
