package ch.amiv.android_app.checkin;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import ch.amiv.android_app.R;

/**
 * Created by roger on 01/03/2018.
 */

public class MemberListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final List<Integer> headerList = new ArrayList<>();
    private final List<KeyValuePair> statList;
    private final List<Member> memberList;
    private final List<KeyValuePair> eventInfoList;

    /**
     * Defining our own view holder which maps the layout items to view variables which can then later be accessed, and text value set etc
     * For each item type we have to define a viewholder. This will map the layout to the variables
     */
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

    public static class KeyValueHolder extends RecyclerView.ViewHolder {
        TextView nameField;
        TextView valueField;

        public KeyValueHolder(View view) {
            super(view);
            nameField = view.findViewById(R.id.nameField);
            valueField = view.findViewById(R.id.valueField);
        }
    }

    public static class HeaderHolder extends RecyclerView.ViewHolder {
        TextView nameField;

        public HeaderHolder(View view) {
            super(view);
            nameField = view.findViewById(R.id.nameField);
        }
    }

    public static class SpaceHolder extends RecyclerView.ViewHolder {
        View space;

        public SpaceHolder(View view) {
            super(view);
            space = view.findViewById(R.id.space);
        }
    }

    public MemberListAdapter(List<Member> _members, List<KeyValuePair> _stats, List<KeyValuePair> _eventInfos) {
        headerList.add(R.string.stat_title);
        headerList.add(R.string.people_title);
        headerList.add(R.string.event_info_title);
        memberList = _members;
        statList = _stats;
        eventInfoList = _eventInfos;
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
            case 0: //header
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.checkin_list_item_header, parent, false);
                holder = new HeaderHolder(view);
                break;
            case 1: //stat
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.checkin_list_item_stat, parent, false);
                holder = new KeyValueHolder(view);
                break;
            case 2: //member
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.checkin_list_item_member, parent, false);
                holder = new MemberHolder(view);
                break;
            case 3: //info
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.checkin_list_item_stat, parent, false);
                holder = new KeyValueHolder(view);
                break;
            case 4: //space
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
        if(position == 0)
            return 0;   //header
        if(position < statList.size() +1)
            return 1;   //Stat
        else if (position == statList.size() +1)
            return 0;   //members header
        else if (position < statList.size() + memberList.size() + 2)
            return 2;   //Member
        else if(position == statList.size() + memberList.size() + 2)
            return 0;   //infos header
        else if (position < getItemCount() -1)
            return 3;   //event Info
        else
            return 4;   //Space
    }

    /**
     * This is where the data in the ui is set. Note that position is the position on screen whereas getAdapterPos is the position in the whole list
     */
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        switch (holder.getItemViewType())
        {
            case 0: //header
                HeaderHolder headerHolder = (HeaderHolder)holder;
                headerHolder.nameField.setText(headerList.get(GetHeaderIndex(holder.getAdapterPosition())));
                break;
            case 1: //stat
                KeyValueHolder statHolder = (KeyValueHolder)holder;
                KeyValuePair stat = statList.get(holder.getAdapterPosition() -1);
                statHolder.nameField.setText(stat.name);
                statHolder.valueField.setText(stat.value);
                break;
            case 2: //member
                MemberHolder memberHolder = (MemberHolder)holder;
                Member m = memberList.get(holder.getAdapterPosition() - statList.size() - 2);
                memberHolder.nameField.setText(m.firstname + " " + m.lastname);

                if(EventDatabase.instance.eventData.checkinType == EventData.CheckinType.Counter)
                    memberHolder.checkinField.setText(m.checkinCount);
                else if (EventDatabase.instance.eventData.checkinType == EventData.CheckinType.InOut)
                    memberHolder.checkinField.setText((m.checkedIn ? "In" : "Out"));
                else
                    memberHolder.checkinField.setText("");

                memberHolder.infoField.setText(m.GetLegiFormatted());

                if(EventDatabase.instance != null && EventDatabase.instance.eventData.eventType == EventData.EventType.GV && m.membership.length() > 1)
                    memberHolder.membershipField.setText(m.membership.substring(0,1).toUpperCase() + m.membership.substring(1));
                else
                    memberHolder.membershipField.setText("");
                break;
            case 3: //info
                KeyValueHolder infoHolder = (KeyValueHolder)holder;
                KeyValuePair info = eventInfoList.get(holder.getAdapterPosition() - statList.size() - memberList.size() - 3);
                infoHolder.nameField.setText(info.name);
                infoHolder.valueField.setText(info.value);
                break;
            case 4:
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
        return memberList.size() + statList.size() + eventInfoList.size() + headerList.size() + 1;  //+1 for space
    }

    /**
     * Will map the position in the whole list to the index in the header array.
     */
    private int GetHeaderIndex(int position)
    {
        if(position == 0)
            return 0;
        else if (position == statList.size() + 1)
            return 1;
        else if (position == statList.size() + memberList.size() + 2)
            return 2;

        Log.e("recyclerView", "Could not determine header position within list, at position: " + position);
        return 0;
    }
}