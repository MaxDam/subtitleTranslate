package com.maxdam.udemysubtitletranslator;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

public class FolderMonitoringReceiver extends WakefulBroadcastReceiver {
    
	private AlarmManager alarmMgr;
    private PendingIntent alarmIntent;
  
    @Override
    public void onReceive(Context context, Intent intent) {
    	
    	Intent service = new Intent(context, ServiceTranslate.class);
        
    	// Start the service, keeping the device awake while it is launching.
        startWakefulService(context, service);
    	//context.startService(service);
        
        Log.i("TIMER ALARM START", "TIMER ALARM START");
    }

    /**
     * set del timer alarm
     */
    public void setTimerAlarm(Context context) {
        alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, FolderMonitoringReceiver.class);
        alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        alarmMgr.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), CommonStuff.FOLDER_MONITORING_INTERVAL, alarmIntent);
    }

    /**
     * cancel del timer alarm
     */
    public void cancelTimerAlarm(Context context) {
        // If the alarm has been set, cancel it.
        if (alarmMgr!= null) {
            alarmMgr.cancel(alarmIntent);
        }
    }
}
