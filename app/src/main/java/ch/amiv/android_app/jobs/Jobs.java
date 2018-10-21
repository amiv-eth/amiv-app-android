package ch.amiv.android_app.jobs;

import android.content.Context;

import org.json.JSONObject;

import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import ch.amiv.android_app.core.Settings;
import ch.amiv.android_app.util.ApiListBase;

/**
 * This holds all the data about the job offers similar to the events class, for more explanations see the Events class
 */
public class Jobs extends ApiListBase<JobInfo> {
    //region -   Variables
    public static final Jobs get = new Jobs();

    public boolean[] invertJobGroupSorting = new boolean[] {false, false, true};

    //Use this class to use the correct indexes for the job group for the sorted list
    public static final class JobGroup {
        public static final int SIZE          = 3;
        public static final int HIDDEN_JOBS   = 0;
        public static final int ALL_JOBS      = 1;
        public static final int PAST_JOBS     = 2;

        public static final int HIDDEN_JOBS_EXP_SIZE   = 2;
        public static final int ALL_JOBS_EXP_SIZE      = 10;
        public static final int PAST_JOBS_EXP_SIZE     = 10;
    }

    //Defines for how many days after the ad start date the new tag is visible for
    public static final int DAYS_NEW_TAG_ACTIVE = 3;

    private static Comparator<JobInfo> endDateComparator = new Comparator<JobInfo>() {
        @Override
        public int compare(JobInfo a, JobInfo b) {
            return a.time_end.compareTo(b.time_end);
        }
    };

    @Override
    public Comparator<JobInfo> GetItemComparator() {
        return endDateComparator;
    }
    //endregion

    //region -   Override functions from ApiListBase
    /**
     * @return An instance of JobInfo parsed from the json
     */
    @Override
    public JobInfo CreateItem(JSONObject json) {
        return new JobInfo(json);
    }

    /**
     * @return The amount of categories in the sorted array, 1st dim
     */
    @Override
    protected int GetSortedSize1() {
        return JobGroup.SIZE;
    }

    /**
     * @param category The 2nd dim of the sorted list
     * @return The expected initialisation size
     */
    @Override
    protected int GetSortedSize2Expected(int category) {
        if(category == JobGroup.HIDDEN_JOBS)
            return JobGroup.HIDDEN_JOBS_EXP_SIZE;
        if(category == JobGroup.ALL_JOBS)
            return JobGroup.ALL_JOBS_EXP_SIZE;
        if(category == JobGroup.PAST_JOBS)
            return JobGroup.PAST_JOBS_EXP_SIZE;
        return 0;
    }

    @Override
    public void SaveToCache(Context context){
        Settings.SaveJobs(context);
    }

    @Override
    public void LoadFromCache(Context context) {
        Settings.LoadJobs(context);
    }

    /**
     * @return The category of the sorted list the item fits in, 1st dim
     */
    @Override
    public int GetItemCategory(JobInfo item) {
        Date today = Calendar.getInstance().getTime();

        if(!item.show_website)
            return JobGroup.HIDDEN_JOBS;
        else if(item.time_end.after(today))
            return JobGroup.ALL_JOBS;
        else
            return JobGroup.PAST_JOBS;
    }
    //endregion
}
