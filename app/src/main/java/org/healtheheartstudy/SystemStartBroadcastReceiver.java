package org.healtheheartstudy;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import timber.log.Timber;

/**
 * SystemStartBroadcastReceiver is triggered when the device boots up.
 */
public class SystemStartBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Timber.d("System restarted -- booting up HospitalTrackingService");

        // Create pending intent for service
        Intent serviceIntent = new Intent(context, HospitalTrackingService.class);
        serviceIntent.putExtra(Constants.KEY_SERVICE_ACTION, Constants.VALUE_SERVICE_CREATE_GEOFENCES);
        PendingIntent pi = PendingIntent.getService(context, 0, serviceIntent, 0);

        // Set alarm
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 0, AlarmManager.INTERVAL_DAY, pi);
    }

}
