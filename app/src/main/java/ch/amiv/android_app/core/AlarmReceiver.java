package ch.amiv.android_app.core;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {

    int notification_id=0;

    @Override
    public void onReceive(Context context, Intent intent) {

        //Notifications.notify(context,"Alarm","Receiver called",R.drawable.ic_amiv_logo_icon);
        Request.FetchEventListChanges(context, new Request.OnDataReceivedCallback() {
            @Override
            public void OnDataReceived() {

            }
        }, new Request.OnDataReceivedCallback() {
            @Override
            public void OnDataReceived() {

            }
        }, "2018-05-06T10:00:00Z",false); // TODO change date here to use last checked date
                                                // use Settings.GetPref(Settings.last_change_check_dateKey,context)

        // TODO test if notification needed

        // TODO enter event notifier here
        notification_id++;
    }
}
