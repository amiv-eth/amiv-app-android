package ch.amiv.android_app.jobs;

import android.content.Context;
import android.support.annotation.NonNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import ch.amiv.android_app.util.PersistentStorage;

/**
 * This holds all the data about the job offers similar to the events class, for more explanations see the Events class
 */
public class Jobs {
    public static List<JobInfo> jobInfos = new ArrayList<JobInfo>();
    public static List<List<JobInfo>> sortedJobs = new ArrayList<>(JobGroup.SIZE);
    public static boolean[] invertJobGroupSorting = new boolean[] {false, false, true};

    //Use this class to use the correct indexes for the job group for the sortedJobs list
    public static final class JobGroup {
        public static final int SIZE          = 3;
        public static final int HIDDEN_JOBS   = 0;
        public static final int ALL_JOBS      = 1;
        public static final int PAST_JOBS     = 2;
    }

    //Defines for how many days after the ad start date the new tag is visible for
    public static final int DAYS_NEW_TAG_ACTIVE = 3;

    /**
     * Update the list of job offers with a json from the api
     * @param json json array of the events.
     */
    public static void UpdateJobInfos(Context context, JSONArray json)
    {
        //initialise lists first or clear them
        boolean isInitialising = jobInfos.size() == 0;

        for (int i = 0; i < json.length(); i++)
        {
            try {
                //if we are not initialising, search for the event id and then update it, else add a new one to the list. This ensures we do not lose the signup data
                JSONObject jsonJob = json.getJSONObject(i);
                JobInfo e = new JobInfo(jsonJob);
                if(e._id.isEmpty())
                    continue;
                if(isInitialising || !UpdateSingleJob(jsonJob, e._id))
                    jobInfos.add(e);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        GenerateSortedLists(isInitialising);

        PersistentStorage.SaveJobs(context);
    }

    public static void GenerateSortedLists(boolean isInitialising)
    {
        if(isInitialising){
            for (int k = 0; k < JobGroup.SIZE; k++)
                sortedJobs.add(new ArrayList<JobInfo>());
        }
        else {
            for (int k = 0; k < sortedJobs.size(); k++)
                sortedJobs.get(k).clear();
        }

        //Sort list
        if(!jobInfos.isEmpty()){
            //sort so first elem has an end date furthest in the future
            Comparator<JobInfo> comparator;
            comparator = new Comparator<JobInfo>() {
                @Override
                public int compare(JobInfo a, JobInfo b) {
                    return a.time_end.compareTo(b.time_end);
                }
            };

            Collections.sort(jobInfos, comparator);

            Date today = Calendar.getInstance().getTime();

            //fill in the sorted list according to the dates
            for (int i = 0; i < jobInfos.size(); i++){
                if(!jobInfos.get(i).show_website)
                    sortedJobs.get(JobGroup.HIDDEN_JOBS).add(jobInfos.get(i));
                else if(jobInfos.get(i).time_end.after(today))
                    sortedJobs.get(JobGroup.ALL_JOBS).add(jobInfos.get(i));
                else
                    sortedJobs.get(JobGroup.PAST_JOBS).add(jobInfos.get(i));
            }
        }
    }

    /**
     * Will update a given event with the id
     * @param json
     * @param jobId
     * @return true if the event was found and updated
     */
    public static boolean UpdateSingleJob(JSONObject json, @NonNull String jobId){
        for (int i = 0; i < jobInfos.size(); i++){
            if(jobInfos.get(i)._id.equalsIgnoreCase(jobId)) {
                jobInfos.get(i).UpdateJob(json);
                return true;
            }
        }
        return false;
    }
}
