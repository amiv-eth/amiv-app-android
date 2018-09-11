package ch.amiv.android_app.checkin;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import ch.amiv.android_app.R;

/**
 * Created by Roger on 06-Feb-18.
 */

public class CustomListAdapter extends ArrayAdapter<Member> {
    private final Activity context;
    private final List<Member> members;

    public CustomListAdapter(Activity context, List<Member> _members){

        super(context, R.layout.checkin_list_item_member, _members);

        this.context = context;
        this.members = _members;
    }

    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View rowView = inflater.inflate(R.layout.checkin_list_item_member, null,true);

        TextView nameField = rowView.findViewById(R.id.nameField);
        TextView infoField = rowView.findViewById(R.id.infoField);
        TextView checkinField = rowView.findViewById(R.id.checkinStatus);
        TextView membershipField = rowView.findViewById(R.id.infoField2);

        Member m = members.get(position);
        nameField.setText(m.firstname + " " + m.lastname);
        infoField.setText(m.GetLegiFormatted());
        checkinField.setText((m.checkedIn ? "In" : "Out"));

        if(EventDatabase.instance != null && EventDatabase.instance.eventData.eventType == EventData.EventType.GV && m.membership.length() > 1)
            membershipField.setText(m.membership.substring(0,1).toUpperCase() + m.membership.substring(1));
        else
            membershipField.setText("");

        return rowView;

    }
}
