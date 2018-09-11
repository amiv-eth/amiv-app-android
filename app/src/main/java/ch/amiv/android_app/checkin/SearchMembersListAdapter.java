package ch.amiv.android_app.checkin;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import ch.amiv.android_app.R;

/**
 * Created by roger on 17/03/2018.
 * This list adapter is a simpler version of the one used in the memberListActivity, as it only has two types: member, space
 */

public class SearchMembersListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final List<MemberData> memberList;

    public static class MemberHolder extends RecyclerView.ViewHolder {
        TextView nameField;
        TextView infoField;
        TextView checkinField;
        TextView membershipField;

        public MemberHolder(View view) {
            super(view);
            nameField = view.findViewById(R.id.nameField);
            infoField = view.findViewById(R.id.infoField);
            checkinField = view.findViewById(R.id.checkinStatus);
            membershipField = view.findViewById(R.id.infoField2);
        }
    }

    public static class SpaceHolder extends RecyclerView.ViewHolder {
        View space;

        public SpaceHolder(View view) {
            super(view);
            space = view.findViewById(R.id.space);
        }
    }

    public SearchMembersListAdapter(List<MemberData> _members) {
        memberList = _members;
    }

    /**
     * This is used when creating a new UI list item. Depending on the type we use a different layout xml
     */
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = null;
        RecyclerView.ViewHolder holder = null;

        switch (viewType)
        {
            case 0: //member
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.checkin_list_item_member, parent, false);
                holder = new MemberHolder(view);
                break;
            case 1: //space
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.checkin_list_item_space, parent, false);
                holder = new SpaceHolder(view);
                break;
        }
        if(view == null)
            Log.e("recyclerView", "Unhandled viewType found, type: " + viewType);

        return holder;
    }

    /**
     * Here we map the position in the whole list to the item type, be careful with indexing and offsets
     */
    @Override
    public int getItemViewType(int position) {      //Note stat and event info use the same layout, but types are different
        if(position < memberList.size())
            return 0;   //infos header
        else
            return 1;   //Space
    }

    /**
     * This is where the data in the ui is set. Note that position is the position on screen whereas getAdapterPos is the position in the whole list
     */
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        switch (holder.getItemViewType())
        {
            case 0: //member
                MemberHolder memberHolder = (MemberHolder)holder;
                MemberData m = memberList.get(holder.getAdapterPosition());
                memberHolder.nameField.setText(m.firstname + " " + m.lastname);

                if(EventDatabase.instance.eventData.checkinType == EventData.CheckinType.Counter)
                    memberHolder.checkinField.setText(m.checkinCount);
                else if (EventDatabase.instance.eventData.checkinType == EventData.CheckinType.InOut)
                    memberHolder.checkinField.setText((m.checkedIn ? "In" : "Out"));
                else
                    memberHolder.checkinField.setText("");

                memberHolder.infoField.setText(m.GetLegiFormatted());

                if(EventDatabase.instance.eventData.eventType == EventData.EventType.GV && m.membership.length() > 1)
                    memberHolder.membershipField.setText(m.membership.substring(0,1).toUpperCase() + m.membership.substring(1));
                else
                    memberHolder.membershipField.setText("");
                break;
            case 1:
                SpaceHolder spaceHolder = (SpaceHolder)holder;
                //Add resizing of space if necessary
                break;
        }
    }

    /**
     * This is important for having the right amount of items in the list or else it will be cropped at the end
     */
    @Override
    public int getItemCount() {
        return memberList.size() + 1;  //+1 for space
    }
}
