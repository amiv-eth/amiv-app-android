package ch.amiv.android_app.events;

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
import java.util.List;

import ch.amiv.android_app.R;
import ch.amiv.android_app.core.BaseRecyclerAdapter;
import ch.amiv.android_app.core.ListHelper;
import ch.amiv.android_app.core.MainActivity;

import static ch.amiv.android_app.core.Settings.showHiddenFeatures;

public class EventsListAdapter extends BaseRecyclerAdapter {
    private List<ListHelper.Pair> dataList = new ArrayList<>();
    private Activity activity;

    private static final class ViewType {
        private static final int HEADER      = 0;
        private static final int SPACE       = 1;
        private static final int EVENT       = 2;
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

    public EventsListAdapter(Activity activity_) {
        activity = activity_;
    }

    @Override
    public void BuildDataset ()
    {
        if(Events.sortedEvents.size() == 0)
            return;

        dataList.clear();

        int[] headers = new int[] {R.string.hidden_events_title, R.string.all_events_title, R.string.closed_events_title, R.string.past_events_title};

        //Debug: Start at 0 to show hidden events, headers will be offset though
        for (int i = (showHiddenFeatures ? 0 : 1); i < Events.EventGroup.SIZE; i++) {
            if(i < headers.length)
                dataList.add(new ListHelper.Pair(ViewType.HEADER, activity.getResources().getString(headers[i])));

            //invert order on the specified groups
            if(i >= Events.invertEventGroupSorting.length || !Events.invertEventGroupSorting[i]) {
                for (int j = 0; j < Events.sortedEvents.get(i).size(); j++) {
                    dataList.add(new ListHelper.Pair(ViewType.EVENT, new int[]{i, j}));
                }
            }
            else{
                for (int j = Events.sortedEvents.get(i).size() -1; j >= 0; j--) {
                    dataList.add(new ListHelper.Pair(ViewType.EVENT, new int[]{i, j}));
                }
            }
        }
        dataList.add(new ListHelper.Pair(ViewType.SPACE, 128));
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
                holder = new ListHelper.HeaderHolder(view);
                break;
            case ViewType.SPACE: //space
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.core_list_item_space, parent, false);
                holder = new ListHelper.SpaceHolder(view);
                break;
            case ViewType.EVENT: //event
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.events_list_item_event, parent, false);
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

        ListHelper.Pair info = dataList.get(holder.getAdapterPosition());
        Object data = info.value;

        switch (info.type)
        {
            case ViewType.HEADER: //header
                ListHelper.HeaderHolder headerHolder = (ListHelper.HeaderHolder)holder;
                headerHolder.nameField.setText((String)data);
                break;

            case ViewType.SPACE: //space
                View space = ((ListHelper.SpaceHolder)holder).space;
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
