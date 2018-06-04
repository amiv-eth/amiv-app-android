package ch.amiv.android_app;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.ColorFilter;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class EventsListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final List<String> headerList = new ArrayList<String>();
    private Activity activity;
    /**
     * Defining our own view holder which maps the layout items to view variables which can then later be accessed, and text value set etc
     * For each item type we have to define a viewholder. This will map the layout to the variables
     */
    public static class EventInfoHolder extends RecyclerView.ViewHolder {
        TextView titleField;
        TextView catchphraseField;
        TextView placesField;
        ImageView statusImage;
        View.OnClickListener clickListener;

        public EventInfoHolder(View view) {
            super(view);
            titleField = view.findViewById(R.id.titleField);
            catchphraseField = view.findViewById(R.id.infoField);
            placesField = view.findViewById(R.id.places_left);
            statusImage = view.findViewById(R.id.signupStatus);
        }
    }

    public static class HeaderHolder extends RecyclerView.ViewHolder {
        TextView nameField;

        public HeaderHolder(View view) {
            super(view);
            nameField = view.findViewById(R.id.titleField);
        }
    }

    public static class SpaceHolder extends RecyclerView.ViewHolder {
        View space;

        public SpaceHolder(View view) {
            super(view);
            space = view.findViewById(R.id.space);
        }
    }

    public EventsListAdapter(Activity activity_) {
        headerList.add("New Events");
        headerList.add("Attended Events");
        activity = activity_;
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
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_header, parent, false);
                holder = new HeaderHolder(view);
                break;
            case 1: //space
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_space, parent, false);
                holder = new SpaceHolder(view);
                break;
            case 2: //event
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_event, parent, false);
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
        if(position == 0 || position == Events.eventInfos.size() + 1)
            return 0;   //header
        if(position < Events.eventInfos.size() +1)
            return 2;   //events
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
            case 0: //header
                HeaderHolder headerHolder = (HeaderHolder)holder;
                headerHolder.nameField.setText(headerList.get(GetHeaderIndex(holder.getAdapterPosition())));
                break;
            case 1: //space
                SpaceHolder spaceHolder = (SpaceHolder)holder;
                break;
            case 2: //event
                final EventInfoHolder eventInfoHolder = (EventInfoHolder)holder;
                final int eventIndex = holder.getAdapterPosition() - 1;
                final EventInfo e = Events.eventInfos.get(eventIndex);
                eventInfoHolder.titleField.setText(e.title);
                eventInfoHolder.catchphraseField.setText(e.catchphrase);
                eventInfoHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        StartEventDetailActivity(eventIndex);
                    }
                });

                if(e.accepted && e.confirmed){ //change status of event depending on signup state
                    eventInfoHolder.statusImage.setVisibility(View.VISIBLE);
                    eventInfoHolder.placesField.setVisibility(View.GONE);
                    eventInfoHolder.statusImage.setImageDrawable(Resources.getSystem().getDrawable(R.drawable.ic_check, activity.getTheme()));
                    eventInfoHolder.statusImage.setColorFilter(Resources.getSystem().getColor(R.color.colorYellow, activity.getTheme()));
                }
                else if (e.accepted) {
                    eventInfoHolder.statusImage.setVisibility(View.VISIBLE);
                    eventInfoHolder.placesField.setVisibility(View.GONE);
                    eventInfoHolder.statusImage.setImageDrawable(Resources.getSystem().getDrawable(R.drawable.ic_pending, activity.getTheme()));
                    eventInfoHolder.statusImage.setColorFilter(Resources.getSystem().getColor(R.color.colorGreen, activity.getTheme()));
                }
                else {
                    eventInfoHolder.statusImage.setVisibility(View.GONE);
                    eventInfoHolder.placesField.setVisibility(View.VISIBLE);
                    eventInfoHolder.placesField.setText(e.spots);
                }
                break;
                /*XXXXX Change color of status image, Test status showing correctly for event, whether we are signed up or not, pending. Hide register button once registered, show registered.
                Test registering to event
                Fix event image loading from api
                Sort events upcoming and attended events, hide unattended old eventsa
                return to main activity once registered??*/
        }
    }

    /**
     * This is important for having the right amount of items in the list or else it will be cropped at the end
     */
    @Override
    public int getItemCount() {
        return headerList.size() + Events.eventInfos.size() + 1;  //+1 for space
    }

    /**
     * Will map the position in the whole list to the index in the header array.
     */
    private int GetHeaderIndex(int position)
    {
        if(position == 0)
            return 0;
        if(position == Events.eventInfos.size() +1)
            return 1;

        Log.e("recyclerView", "Could not determine header position within list, at position: " + position);
        return 0;
    }

    public void StartEventDetailActivity(int eventIndex) {
        Intent intent = new Intent(activity, EventDetailActivity.class);
        intent.putExtra("eventIndex", eventIndex);
        activity.startActivity(intent);
    }
}
