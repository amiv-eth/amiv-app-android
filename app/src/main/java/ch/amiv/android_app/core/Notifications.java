package ch.amiv.android_app.core;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

import ch.amiv.android_app.R;
import ch.amiv.android_app.events.EventDetailActivity;
import ch.amiv.android_app.events.Events;
import ch.amiv.android_app.jobs.JobDetailActivity;
import ch.amiv.android_app.jobs.Jobs;

public final class Notifications {
    public static AlarmManager alarm;
    public static Intent intent;
    public static PendingIntent pendingIntent;

    /**
     *
     * @param context
     * output: sets daily alarm to XX:XX
     */
    static void set_Alarm (Context context){

        // set alarm time in calendar
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 8);
        calendar.set(Calendar.MINUTE, 10);
        calendar.set(Calendar.SECOND, 0);

        // pending intent to activate activity when notification is clicked
        intent = new Intent(context, AlarmReceiver.class);

        pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT); // updates intent if it already exists

        // set alarm manager
        alarm = (AlarmManager) context
                .getSystemService(context.ALARM_SERVICE);

        alarm.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, pendingIntent);
    }

    /**
     *
     * @param context
     * @param title title of notification
     * @param text text of notification
     * @param icon icon of notification
     * output: immediately generates notification on screen
     */
    public static void notify (Context context, String title, String text, int icon){

        NotificationManager notificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder mNotifyBuilder = new NotificationCompat.Builder(
                context,"1")
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true);

        notificationManager.notify(2, mNotifyBuilder.build()); // id 2

    }

    /**
     *
     * @param context
     * @param title title of notification
     * @param text text of notification
     * @param icon icon
     * @param pendingIntent pending intent to activate if notification is clicked
     * @param id 0 event notifications 1 job notifications 2 other
     * output: generates notification on screen which starts the pending activity onClick
     */
    public static void notify_pending (Context context, String title, String text, int icon, PendingIntent pendingIntent, int id){

        NotificationManager notificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder mNotifyBuilder = new NotificationCompat.Builder(
                context,"1")
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(text)
                .setColor(context.getResources().getColor(R.color.primary)).setColorized(true)
                .setAutoCancel(true)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(pendingIntent);

        notificationManager.notify(id, mNotifyBuilder.build());

    }

    /**
     *
     * @param context
     * @param json JSON with all new events
     * output: event notificiation with appropriate onClick event
     */
    public static void event_notifier (Context context, JSONObject json){
        // TODO catch when projections are used
            try{

               JSONArray items =  json.getJSONArray("_items");
                // if length  == 0 -> change last change time and don't send notifiactions
               if(items.length()!=0) {

                   // for one new event -> notify and onClick show details
                   // multiple new events -> show all titles and onClick show events main page
                   //else{
                       // get event id's from new events
                       String [] event_id = new String[items.length()];
                       int [] event_index = new int[items.length()];
                        for(int i = 0; i<items.length();i++){
                            JSONObject event = items.getJSONObject(i);
                            event_index[i]=Events.AddEvent(event, context); // TODO efficiency !
                            event_id[i]= (String) event.get("_id");
                        }
                        // add new events to list
                       // TODO how -> new function?
                       Request.FetchEventList(context, null, null,event_id[0]); // TODO not null !

                       NotificationManager notificationManager = (NotificationManager) context
                               .getSystemService(Context.NOTIFICATION_SERVICE);

                       // generate Intent based on how many changes there are
                       Intent notificationIntent;
                       if(items.length()> 1) {
                           notificationIntent = new Intent(context, MainActivity.class);
                           notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                       }
                       else{
                           notificationIntent = new Intent(context, EventDetailActivity.class);
                           notificationIntent.putExtra(EventDetailActivity.LauncherExtras.EVENT_ID, event_id);
                           notificationIntent.putExtra(EventDetailActivity.LauncherExtras.LOAD_EVENTS, true);
                           notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                       }

                       PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                               notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                       // build string to display all event titles
                       StringBuilder titles_list = new StringBuilder();
                       titles_list.append (Events.eventInfos.get(event_index[0]).GetTitle(context.getResources()));
                       for(int j = 1; j<items.length();j++){
                           titles_list.append(", ");
                           titles_list.append(Events.eventInfos.get(event_index[j]).GetTitle(context.getResources()));
                       }

                       // set notification title depending on how many changes there are
                       String title;
                       if(items.length()>1){
                           title = context.getString(R.string.multipleEventsadded);
                       }
                       else{
                           title = context.getString(R.string.singleEventadded);
                       }
                       Notifications.notify_pending(context, title, titles_list.toString(), R.drawable.ic_amiv_logo_icon, pendingIntent,0);

               }



               // sets last change check to current time
               String dat = Request.dateFormat.format(Calendar.getInstance().getTime());
               Settings.SetPref(Settings.last_change_check_event_dateKey,dat,context);

            }catch(JSONException ex){
                // TODO
            }






    }

    public static void jobs_notifier (Context context, JSONObject json){
        // TODO catch when projections are used
        try{

            JSONArray items =  json.getJSONArray("_items");

            if(items.length()!=0) {

                // for one new job -> notify and onClick show details
                // multiple new jobs -> show all jobs and onClick show events main page

                    // get job id's from new events
                    String [] job_id = new String[items.length()];
                    // TODO add single job
                    for(int i = 0; i<items.length();i++){
                        JSONObject job = items.getJSONObject(i);
                        job_id[i]= (String) job.get("_id");
                    }
                    Jobs.UpdateJobInfos(context, items); // TODO efficiency !

                    // add new jobs to list
                    // TODO how -> new function?
                    Request.FetchJobList(context, null, null,job_id[0]); // TODO not null !

                    // onClick start main activity
                    NotificationManager notificationManager = (NotificationManager) context
                            .getSystemService(Context.NOTIFICATION_SERVICE);

                    Intent notificationIntent;
                    if(items.length()>1) {
                        notificationIntent = new Intent(context, MainActivity.class); // TODO go to jobs tab instead of
                        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    }
                    else{
                        notificationIntent = new Intent(context, JobDetailActivity.class);
                        notificationIntent.putExtra(EventDetailActivity.LauncherExtras.EVENT_ID, job_id);
                        notificationIntent.putExtra(EventDetailActivity.LauncherExtras.LOAD_EVENTS, true);
                        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    }

                    PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, // TODO is this correct ?
                            notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                    // build string to display all event titles
                    StringBuilder titles_list = new StringBuilder();
                    titles_list.append((String)items.getJSONObject(0).get("company"));
                    titles_list.append(" - ");
                    titles_list.append ((String)items.getJSONObject(0).get("title_de")); // TODO access job via job index
                    for(int j = 1; j<items.length();j++){
                        titles_list.append(", ");
                        titles_list.append((String)items.getJSONObject(j).get("company"));
                        titles_list.append(" - ");
                        titles_list.append((String)items.getJSONObject(j).get("title_de"));
                    }
                    String title;
                    if(items.length()>1){
                       title = context.getString(R.string.new_jobs_notification);
                    }
                    else {
                        title = context.getString(R.string.new_job_notification);
                    }
                    Notifications.notify_pending(context,title , titles_list.toString(), R.drawable.ic_amiv_logo_icon, pendingIntent,1);


            }

            // sets last change check to current time
            String dat = Request.dateFormat.format(Calendar.getInstance().getTime());
            Settings.SetPref(Settings.last_change_check_job_dateKey,dat,context);

        }catch(JSONException ex){
            // TODO
        }

    }




}
