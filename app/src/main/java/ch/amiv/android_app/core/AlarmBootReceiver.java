package ch.amiv.android_app.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AlarmBootReceiver extends BroadcastReceiver {


    // resets alarm if device is rebooted
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e("boot", "boot completed");
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            Notifications.set_Alarm(context);
        }
    }
}
