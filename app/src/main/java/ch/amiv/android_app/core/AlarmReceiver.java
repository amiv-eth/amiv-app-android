package ch.amiv.android_app.core;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {

    // when alarm is received this function is executed
    @Override
    public void onReceive(Context context, Intent intent) {

        // event changes
        Request.FetchEventListChanges(context, new Request.OnDataReceivedCallback() {
            @Override
            public void OnDataReceived() {

            }
        }, new Request.OnDataReceivedCallback() {
            @Override
            public void OnDataReceived() {

            }
        }, "2018-05-06T10:00:00Z",false); // TODO change date here to use last checked date
                                                // use Settings.GetPref(Settings.last_change_event_check_dateKey,context)
        // job changes
        Request.FetchJobListChanges(context, new Request.OnDataReceivedCallback() {
            @Override
            public void OnDataReceived() {

            }
        }, new Request.OnDataReceivedCallback() {
            @Override
            public void OnDataReceived() {

            }                                       // TODO change date here to use last checked date
        }, "2018-05-06T10:00:00Z");  // use Settings.GetPref(Settings.last_change_check_job_dateKey,context)


    }
}
