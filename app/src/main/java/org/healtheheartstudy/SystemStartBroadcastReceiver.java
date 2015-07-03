package org.healtheheartstudy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import timber.log.Timber;

/**
 * Created by dannypark on 7/2/15.
 */
public class SystemStartBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Timber.d("System restarted -- booting up HospitalTrackingService");
        Intent serviceIntent = new Intent(context, HospitalTrackingService.class);
        context.startService(serviceIntent);
    }

}
