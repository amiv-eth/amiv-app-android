package ch.amiv.android_app.core;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import ch.amiv.android_app.R;
import ch.amiv.android_app.events.EventDetailActivity;

public class AlarmReceiver extends BroadcastReceiver {

    int notification_id=0;
    String last_check_time;

    @Override
    public void onReceive(Context context, Intent intent) {

        Request.FetchEventListChanges(context, new Request.OnDataReceivedCallback() {
            @Override
            public void OnDataReceived() {

            }
        }, new Request.OnDataReceivedCallback() {
            @Override
            public void OnDataReceived() {

            }
        }, "2018-09-06T10:00:00Z");

        // TODO test if notification needed

        // TODO enter event notifier here
        notification_id++;
    }
}
