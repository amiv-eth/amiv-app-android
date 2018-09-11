package ch.amiv.android_app.checkin;

import org.json.JSONObject;

/**
 * Created by Roger on 06-Feb-18.
 * This class stores the data that an individual member has, matches what is giver by the server (see server side project's README_API)
 */

public class MemberData {
    public String serverId;
    public boolean checkedIn;
    public String email;
    public String firstname;
    public String lastname;
    public String legi;
    private String formattedLegi;
    public String membership;   //as in membership type: ordinary extraordinary honorary
    public String nethz;
    public String checkinCount; //how often the person has been checked in

    public MemberData(JSONObject _member)
    {
        serverId    = _member.optString("_id");
        email       = _member.optString("email");
        firstname   = _member.optString("firstname");
        lastname    = _member.optString("lastname");
        legi        = _member.optString("legi");
        membership  = _member.optString("membership");
        nethz       = _member.optString("nethz");

        checkedIn   = _member.optBoolean("checked_in");
        checkinCount= _member.optString("freebies_taken");
        if(checkinCount.isEmpty())
            checkinCount = "-";
    }

    public MemberData(String _serverId, boolean _checkedIn, String _email, String _firstname, String _lastname, String _legi, String _membership, String _nethz, String _checkinCount)
    {
        serverId    = _serverId;
        checkedIn   = _checkedIn;
        email       = _email;
        firstname   = _firstname;
        lastname    = _lastname;
        legi        = _legi;
        membership  = _membership;
        nethz       = _nethz;
        checkinCount= _checkinCount;
    }

    private static final char legiFormatSeperator = '-';
    private static final char legiStandardLength = 8;
    /**
     * @return Legi formatted for reading
     */
    public String GetLegiFormatted (){
        if(!formattedLegi.isEmpty())    //cache formatted legi for faster access, e.g. when searching through list
            return formattedLegi;

        if(legi == null || legi.isEmpty())
            formattedLegi = "-";
        else if(legi.length() != legiStandardLength)
            formattedLegi = legi;
        else {
            StringBuilder s = new StringBuilder();
            s.append(legi);
            s.insert(5, legiFormatSeperator);
            s.insert(2, legiFormatSeperator);

            formattedLegi = s.toString();
        }
        return formattedLegi;
    }
}
