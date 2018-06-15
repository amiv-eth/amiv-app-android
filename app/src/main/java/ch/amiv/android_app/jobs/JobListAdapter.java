package ch.amiv.android_app.jobs;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import ch.amiv.android_app.R;
import ch.amiv.android_app.core.BaseRecyclerAdapter;
import ch.amiv.android_app.core.ListHelper;
import ch.amiv.android_app.core.MainActivity;
import ch.amiv.android_app.core.Requests;

public class JobListAdapter extends BaseRecyclerAdapter {
    private List<ListHelper.Pair> dataList = new ArrayList<>();
    private Activity activity;

    //Whether to show hidden jobs, where the adverts should not have started yet, should later be set by user access group
    private boolean showHidden = true;

    private static final class ViewType {
        private static final int HEADER      = 0;
        private static final int SPACE       = 1;
        private static final int JOB         = 2;
    }

    /**
     * Defining our own view holder which maps the layout items to view variables which can then later be accessed, and text value set etc
     * For each item type we have to define a viewholder. This will map the layout to the variables
     */
    private class JobInfoHolder extends RecyclerView.ViewHolder {
        TextView titleField;
        TextView companyField;
        TextView newTag;
        NetworkImageView logoImage;

        private JobInfoHolder(View view) {
            super(view);
            titleField = view.findViewById(R.id.titleField);
            companyField = view.findViewById(R.id.infoField);
            newTag = view.findViewById(R.id.newTag);
            logoImage = view.findViewById(R.id.companyLogo);
        }
    }

    public JobListAdapter(Activity activity_) {
        activity = activity_;
    }

    @Override
    public void BuildDataset ()
    {
        dataList.clear();

        List<Integer> headers = new ArrayList<>();
        if(showHidden)
            headers.add(R.string.hidden_jobs_title);
        headers.add(R.string.all_jobs_title);
        headers.add(R.string.past_jobs_title);

        for (int i = (showHidden ? 0 : 1); i < Jobs.sortedJobs.size(); i++) {
            if(i < headers.size())
                dataList.add(new ListHelper.Pair(JobListAdapter.ViewType.HEADER, activity.getResources().getString(headers.get(i))));

            //invert order on the specified groups
            if(i >= Jobs.invertJobGroupSorting.length || !Jobs.invertJobGroupSorting[i]) {
                for (int j = 0; j < Jobs.sortedJobs.get(i).size(); j++) {
                    dataList.add(new ListHelper.Pair(ViewType.JOB, new int[]{i, j}));
                }
            }
            else{
                for (int j = Jobs.sortedJobs.get(i).size() -1; j >= 0; j--) {
                    dataList.add(new ListHelper.Pair(ViewType.JOB, new int[]{i, j}));
                }
            }
        }
        dataList.add(new ListHelper.Pair(JobListAdapter.ViewType.SPACE, 128));
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
            case ViewType.JOB: //jobs
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.jobs_list_item_job, parent, false);
                holder = new JobInfoHolder(view);
                break;
        }
        if(view == null)
            Log.e("recyclerView", "Job List, Unhandled viewType found, type: " + viewType);

        return holder;
    }

    /**
     * Here we map the position in the whole list to the item type, be careful with indexing and offsets
     */
    @Override
    public int getItemViewType(int position) {      //Note stat and job info use the same layout, but types are different
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

            case ViewType.JOB: //job
                int[] indexes = (int[])data;
                final int jobGroup = indexes[0];
                final int jobIndex = indexes[1];
                final JobInfo j = Jobs.sortedJobs.get(jobGroup).get(jobIndex);
                final JobInfoHolder jobInfoHolder = (JobInfoHolder)holder;

                jobInfoHolder.titleField.setText(j.GetTitle(activity.getResources()));
                jobInfoHolder.companyField.setText(j.company);

                jobInfoHolder.logoImage.setImageUrl(j.GetLogoUrl(), Requests.GetImageLoader(activity.getApplicationContext()));

                //Showing "new" tag if within a certain number of days of the ad start date
                Calendar cal = Calendar.getInstance();
                cal.setTime(j.time_created);
                cal.add(Calendar.DAY_OF_YEAR, Jobs.DAYS_NEW_TAG_ACTIVE);
                if (jobGroup == Jobs.JobGroup.ALL_JOBS && cal.getTime().after(Calendar.getInstance().getTime()))
                    jobInfoHolder.newTag.setVisibility(View.VISIBLE);
                else
                    jobInfoHolder.newTag.setVisibility(View.GONE);

                jobInfoHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        StartJobDetailActivity(jobGroup, jobIndex);
                    }
                });
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

    private void StartJobDetailActivity(int jobGroup, int jobIndex) {
        ((MainActivity)activity).StartJobDetailActivity(jobGroup, jobIndex);
    }
}
