package ch.amiv.android_app.core;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

public class AlarmService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public int onStartCommand (Intent intent, int flags,int start_id){
        super.onStartCommand(intent,flags,start_id);
        Notifications.set_Alarm(getApplicationContext());
        return START_STICKY;
    }
}
