package ch.amiv.android_app.core;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.icu.text.DateFormat;
import android.icu.text.SimpleDateFormat;
import android.support.v4.app.NotificationCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Date;

import ch.amiv.android_app.R;
import ch.amiv.android_app.events.EventDetailActivity;

public final class Notifications {
    static AlarmManager alarm;
    static Intent intent;
    static PendingIntent pendingIntent;

    /**
     *
     * @param context
     * @return sets daily alarm to 18:00
     */
    static void set_Alarm (Context context){
        Calendar calendar = Calendar.getInstance();

        calendar.set(Calendar.HOUR_OF_DAY, 2);
        calendar.set(Calendar.MINUTE, 50);
        calendar.set(Calendar.SECOND, 0);

        intent = new Intent(context, AlarmReceiver.class);

        pendingIntent = PendingIntent.getBroadcast(
               context, 0, intent,
              PendingIntent.FLAG_UPDATE_CURRENT); // updates intent if it already exists

        alarm = (AlarmManager) context
                .getSystemService(context.ALARM_SERVICE);

        alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, pendingIntent);

    }

    static boolean is_alarm_set (){

        AlarmManager.AlarmClockInfo ala = alarm.getNextAlarmClock();
        if(ala!= null)
        return true;

        return false;
    }

    /**
     *
     * @param context
     * @param title title of notification
     * @param text text of notification
     * @param icon icon
     * @return generates notification on screen
     */
    // not working with Android 8.0
    public static void notify (Context context, String title, String text, int icon){

        NotificationManager notificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder mNotifyBuilder = new NotificationCompat.Builder(
                context,"1").setSmallIcon(icon)                             // channel id for compatibility with newer android versions
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true);
        notificationManager.notify(0, mNotifyBuilder.build());

    }

    /**
     *
     * @param context
     * @param title title of notification
     * @param text text of notification
     * @param icon icon
     * @param pendingIntent generates notification on screen which starts the pending activity onClick
     */
    public static void notify_pending (Context context, String title, String text, int icon, PendingIntent pendingIntent){

        NotificationManager notificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder mNotifyBuilder = new NotificationCompat.Builder(
                context,"1").setSmallIcon(icon)                             // channel id for compatibility with newer android versions
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true).setContentIntent(pendingIntent);
        notificationManager.notify(0, mNotifyBuilder.build());

    }

    /**
     *
     * @param context
     */

    // TODO
    public static void event_notifier (Context context, JSONObject json){
        if(json == null) {
            NotificationManager notificationManager = (NotificationManager) context
                    .getSystemService(Context.NOTIFICATION_SERVICE);

            Intent notificationIntent = new Intent(context, EventDetailActivity.class);
            notificationIntent.putExtra("eventGroup", 1);
            notificationIntent.putExtra("eventIndex", 0);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                    notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            Notifications.notify_pending(context, "New Event", "take a look", R.drawable.ic_amiv_logo_icon, pendingIntent);
        }
        // TODO catch when projections are used
        // TODO catch if no new events
        else{
            try{
               JSONArray items =  json.getJSONArray("_items");
               if(items.length()!=0) {
                   JSONObject event = items.getJSONObject(0);
                   String event_id = (String) event.get("title_en");
                   NotificationManager notificationManager = (NotificationManager) context
                           .getSystemService(Context.NOTIFICATION_SERVICE);

                   Intent notificationIntent = new Intent(context, EventDetailActivity.class);
                   notificationIntent.putExtra("eventGroup", 1);
                   notificationIntent.putExtra("eventIndex", 0);
                   notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                   PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                           notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                   Notifications.notify_pending(context, "New Event", event_id, R.drawable.ic_amiv_logo_icon, pendingIntent);

               }
               String dat = Request.dateFormat.format(Calendar.getInstance().getTime());
               Settings.SetPref(Settings.last_change_check_dateKey,dat,context);
               //notify(context,"Change time",Settings.GetPref(Settings.last_change_check_dateKey,context),R.drawable.ic_amiv_logo_icon);

            }catch(JSONException ex){
                //RunCallback(errorCallback);
                //e.printStackTrace();
            }


        }



    }




}
