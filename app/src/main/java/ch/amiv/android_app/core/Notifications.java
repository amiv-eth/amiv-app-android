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
import ch.amiv.android_app.util.ApiListBase;

public final class Notifications {
    public static AlarmManager alarm;
    public static Intent intent;
    public static PendingIntent pendingIntent;

    /**
     *
     * @param context
     * @return sets daily alarm to XX:XX
     */
    static void set_Alarm (Context context){

        // set alarm time in calendar
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 8);
        calendar.set(Calendar.MINUTE, 23);
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
                AlarmManager.INTERVAL_FIFTEEN_MINUTES, pendingIntent);
    }

    /**
     *
     * @param context
     * @param title title of notification
     * @param text text of notification
     * @param icon icon of notification
     * @return immediately generates notification on screen
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

        notificationManager.notify(0, mNotifyBuilder.build());

    }

    /**
     *
     * @param context
     * @param title title of notification
     * @param text text of notification
     * @param icon icon
     * @param pendingIntent pending intent to activate if notification is clicked
     * @return generates notification on screen which starts the pending activity onClick
     */
    public static void notify_pending (Context context, String title, String text, int icon, PendingIntent pendingIntent){

        NotificationManager notificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder mNotifyBuilder = new NotificationCompat.Builder(
                context,"1")
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(text)
                .setColor(context.getResources().getColor(R.color.primary)).setColorized(true)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        notificationManager.notify(0, mNotifyBuilder.build());

    }

    /**
     *
     * @param context
     * @param json JSON with all new events
     * @return event notificiation with appropriate onClick event
     */
    public static void event_notifier (Context context, JSONObject json){
        // TODO catch when projections are used
        try{

           JSONArray items =  json.getJSONArray("_items");

           if(items.length()!=0) {

               // for one new event -> notify and onClick show details
               if(items.length()==1) {

                   // get event from JSONObject and add it to events list
                   JSONObject event = items.getJSONObject(0);
                   Events.get.AddItem(event, context);

                   // refetch event list to add new event
                   String event_id = (String) event.get("_id");
                   Request.FetchEventList(context, null, null, event_id);

                   // generate pending intent to get EventDetails onClick
                   NotificationManager notificationManager = (NotificationManager) context
                           .getSystemService(Context.NOTIFICATION_SERVICE);

                   Intent notificationIntent = new Intent(context, EventDetailActivity.class);
                   notificationIntent.putExtra(ApiListBase.LauncherExtras.ITEM_ID, event_id);
                   notificationIntent.putExtra(ApiListBase.LauncherExtras.RELOAD_FIRST, true);
                   notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                   PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                           notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                   // generate notification
                   String title = (String) event.get("title_de");
                   Notifications.notify_pending(context, "New Event added", title, R.drawable.ic_amiv_logo_icon, pendingIntent);
               }
               // multiple new events -> show all titles and onClick show events main page
               else{
                   // get event id's from new events
                   String [] event_id = new String[items.length()];
                    for(int i = 0; i<items.length();i++){
                        JSONObject event = items.getJSONObject(i);
                        Events.get.AddItem(event, context); // TODO efficiency !
                        event_id[i]= (String) event.get("_id");
                    }

                    // add new events to list
                   // TODO how -> new function?
                   Request.FetchEventList(context, null, null,event_id[0]); // TODO not null !

                   // onClick start main activity
                   NotificationManager notificationManager = (NotificationManager) context
                           .getSystemService(Context.NOTIFICATION_SERVICE);

                   Intent notificationIntent = new Intent(context, MainActivity.class);
                   notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                   PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                           notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                   // build string to display all event titles
                   StringBuilder titles_list = new StringBuilder();
                   titles_list.append ((String)items.getJSONObject(0).get("title_de"));
                   for(int j = 1; j<items.length();j++){
                       titles_list.append(", ");
                       titles_list.append((String)items.getJSONObject(j).get("title_de"));
                   }
                   Notifications.notify_pending(context, "Many new Events added", titles_list.toString(), R.drawable.ic_amiv_logo_icon, pendingIntent);

               }
           }

           // sets last change check to current time
           String dat = Request.dateFormat.format(Calendar.getInstance().getTime());
           Settings.SetPref(Settings.last_change_check_dateKey,dat,context);

        }catch(JSONException ex){
            // TODO
        }
    }
}
