package ch.amiv.android_app.core;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import ch.amiv.android_app.R;

public class EventsListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<Pair> dataList = new ArrayList<>();
    private Activity activity;

    //Whether to show hidden events, where the adverts should not have started yet, should later be set by user access group
    private boolean showHidden = true;

    private static final class ViewType {
        private static final int HEADER      = 0;
        private static final int SPACE       = 1;
        private static final int EVENT       = 2;
    }

    private class Pair {
        public int type;
        public Object value;

        private Pair(int type, Object value) {
            this.type = type;
            this.value = value;
        }
    }
    /**
     * Defining our own view holder which maps the layout items to view variables which can then later be accessed, and text value set etc
     * For each item type we have to define a viewholder. This will map the layout to the variables
     */
    private class EventInfoHolder extends RecyclerView.ViewHolder {
        TextView titleField;
        TextView catchphraseField;
        TextView placesField;
        TextView newTag;
        ImageView statusImage;

        private EventInfoHolder(View view) {
            super(view);
            titleField = view.findViewById(R.id.titleField);
            catchphraseField = view.findViewById(R.id.infoField);
            placesField = view.findViewById(R.id.places_left);
            newTag = view.findViewById(R.id.newTag);
            statusImage = view.findViewById(R.id.signupStatus);
        }
    }

    private class HeaderHolder extends RecyclerView.ViewHolder {
        TextView nameField;

        private HeaderHolder(View view) {
            super(view);
            nameField = view.findViewById(R.id.titleField);
        }
    }

    private class SpaceHolder extends RecyclerView.ViewHolder {
        View space;

        private SpaceHolder(View view) {
            super(view);
            space = view.findViewById(R.id.space);
        }
    }

    public EventsListAdapter(Activity activity_) {
        activity = activity_;
    }

    public void RefreshData(){
        BuildDataset();
        notifyDataSetChanged();
    }

    public void BuildDataset ()
    {
        dataList.clear();

        List<Integer> headers = new ArrayList<>();
        if(showHidden)
            headers.add(R.string.hidden_events_title);
        headers.add(R.string.all_events_title);
        headers.add(R.string.closed_events_title);
        headers.add(R.string.past_events_title);

        //Debug: Start at 0 to show hidden events, headers will be offset though
        for (int i = (showHidden ? 0 : 1); i < Events.sortedEvents.size(); i++) {
            if(i < headers.size())
                dataList.add(new Pair(ViewType.HEADER, activity.getResources().getString(headers.get(i))));

            //invert order on the specified groups
            if(i >= Events.invertEventGroupSorting.length || !Events.invertEventGroupSorting[i]) {
                for (int j = 0; j < Events.sortedEvents.get(i).size(); j++) {
                    dataList.add(new Pair(ViewType.EVENT, new int[]{i, j}));
                }
            }
            else{
                for (int j = Events.sortedEvents.get(i).size() -1; j >= 0; j--) {
                    dataList.add(new Pair(ViewType.EVENT, new int[]{i, j}));
                }
            }
        }
        dataList.add(new Pair(ViewType.SPACE, 128));
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
            case ViewType.HEADER: //header
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.core_list_item_header, parent, false);
                holder = new HeaderHolder(view);
                break;
            case ViewType.SPACE: //space
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.core_list_item_space, parent, false);
                holder = new SpaceHolder(view);
                break;
            case ViewType.EVENT: //event
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.core_list_item_event, parent, false);
                holder = new EventInfoHolder(view);
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
        return dataList.get(position).type;
    }

    /**
     * This is where the data in the ui is set. Note that position is the position on screen whereas getAdapterPos is the position in the whole list
     */
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int screenPosition) {    //NOTE: screenPosition supplied is position on screen not in the list!, use holder.getAdapterPosition() indstead
        if(activity == null || holder == null)
            return;

        Pair info = dataList.get(holder.getAdapterPosition());
        Object data = info.value;

        switch (info.type)
        {
            case ViewType.HEADER: //header
                HeaderHolder headerHolder = (HeaderHolder)holder;
                //headerHolder.nameField.setText(headerList.get(GetHeaderIndex(holder.getAdapterPosition())));
                headerHolder.nameField.setText((String)data);
                break;

            case ViewType.SPACE: //space
                View space = ((SpaceHolder)holder).space;
                ViewGroup.LayoutParams params = space.getLayoutParams();
                params.height = (int)data;
                space.setLayoutParams(params);
                break;

            case ViewType.EVENT: //event
                int[] indexes = (int[])data;
                final int eventGroup = indexes[0];
                final int eventIndex = indexes[1];
                final EventInfo e = Events.sortedEvents.get(eventGroup).get(eventIndex);
                final EventInfoHolder eventInfoHolder = (EventInfoHolder)holder;

                eventInfoHolder.titleField.setText(e.GetTitle(activity.getResources()));
                eventInfoHolder.catchphraseField.setText(e.GetCatchphrase(activity.getResources()));

                //Showing "new" tag if within a certain number of days of the ad start date
                Calendar cal = Calendar.getInstance();
                cal.setTime(e.time_advertising_start);
                cal.add(Calendar.DAY_OF_YEAR, Events.DAYS_NEW_TAG_ACTIVE);
                if (eventGroup == Events.EventGroup.ALL_EVENTS && cal.getTime().after(Calendar.getInstance().getTime()))
                    eventInfoHolder.newTag.setVisibility(View.VISIBLE);
                else
                    eventInfoHolder.newTag.setVisibility(View.GONE);

                eventInfoHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        StartEventDetailActivity(eventGroup, eventIndex);
                    }
                });

                if(e.accepted && e.confirmed){ //change status of event depending on signup state
                    eventInfoHolder.statusImage.setVisibility(View.VISIBLE);
                    eventInfoHolder.placesField.setVisibility(View.GONE);
                    eventInfoHolder.statusImage.setImageDrawable(activity.getResources().getDrawable(R.drawable.ic_check, activity.getTheme()));
                    eventInfoHolder.statusImage.setColorFilter(activity.getResources().getColor(R.color.colorGreen, activity.getTheme()));
                }
                else if (e.accepted || e.confirmed || e.IsSignedUp()) {
                    eventInfoHolder.statusImage.setVisibility(View.VISIBLE);
                    eventInfoHolder.placesField.setVisibility(View.GONE);
                    eventInfoHolder.statusImage.setImageDrawable(activity.getResources().getDrawable(R.drawable.ic_pending, activity.getTheme()));
                    eventInfoHolder.statusImage.setColorFilter(activity.getResources().getColor(R.color.colorYellow, activity.getTheme()));
                }
                else {
                    eventInfoHolder.statusImage.setVisibility(View.GONE);
                    eventInfoHolder.placesField.setVisibility(View.VISIBLE);
                    eventInfoHolder.placesField.setText("" + Math.max(0, e.spots - e.signup_count));
                }
                break;
        }
    }

    /**
     * This is important for having the right amount of items in the list or else it will be cropped at the end
     */
    @Override
    public int getItemCount() {
        return dataList.size();
    }

    private void StartEventDetailActivity(int eventGroup, int eventIndex) {
        ((MainActivity)activity).StartEventDetailActivity(eventGroup, eventIndex);
    }
}
