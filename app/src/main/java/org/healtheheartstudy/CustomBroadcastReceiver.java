package org.healtheheartstudy;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.securepreferences.SecurePreferences;

import timber.log.Timber;

/**
 * CustomBroadcastReceiver handles three events. The first is BOOT_COMPLETED, which is when the
 * device boots up. During this event, we need to recreate all geofences because they don't
 * persist when the device shuts off. The second event is prompted 1-hour after a user
 * leaves a hospital (after dwelling at a hospital for 4 hours). During this event, we need to
 * display a notification that asks the user a few questions about their hospital visit. The
 * third event is prompted by a daily alarm that launches our service to check if we need to
 * find new hospitals because the user moved a significant distance.
 */
public class CustomBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action != null) {
            if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
                Timber.d("BOOT COMPLETED");
                // Create geofences
                Intent serviceIntent = new Intent(context, HospitalizationService.class);
                serviceIntent.putExtra(Constants.KEY_SERVICE_ACTION, Constants.ACTION_CREATE_GEOFENCES);
                context.startService(serviceIntent);

                // Start alarm to check user's location every day
                AlarmHelper ah = new AlarmHelper(context, Constants.ACTION_CHECK_LOCATION);
                ah.setRepeating(Constants.ONE_DAY_MILLIS, Constants.ONE_DAY_MILLIS);
            } else if (action.equals(Constants.ACTION_SURVEY_ALARM)) {
                Timber.d("ALARM TRIGGERED FOR SURVEY");
                String hospitalName = intent.getStringExtra(Constants.KEY_HOSPITAL_NAME);
                buildNotification(context, hospitalName);
            } else if (action.equals(Constants.ACTION_CHECK_LOCATION)) {
                Timber.d("ALARM TRIGGERED FOR CHECK LOCATION");
                Intent serviceIntent = new Intent(context, HospitalizationService.class);
                serviceIntent.putExtra(Constants.KEY_SERVICE_ACTION, action);
                context.startService(serviceIntent);
            }
        }
    }

    private void buildNotification(Context context, String hospitalName) {
        // Need to store this in SharedPreferences in case user clears notification
        String date = AlarmHelper.getCurrentDate();
        SharedPreferences prefs = new SecurePreferences(context.getApplicationContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Constants.KEY_PERSISTENT_SURVEY_HOSPITAL, hospitalName);
        editor.putString(Constants.KEY_PERSISTENT_SURVEY_DATE, date);
        editor.apply();

        Intent displayIntent = new Intent(context, MainActivity.class);
        displayIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        displayIntent.putExtra(Constants.KEY_HOSPITAL_NAME, hospitalName);
        displayIntent.putExtra(Constants.KEY_DATE, date);
        PendingIntent displayPendingIntent = PendingIntent.getActivity(
                context,
                0,
                displayIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_media_pause)
                .setContentTitle("Health eHeart Study")
                .setContentText("We noticed that you're near a hospital and would like you to fill out a survey.")
                .setContentIntent(displayPendingIntent)
                .setAutoCancel(true)
                .setVibrate(new long[]{500, 500, 500, 500});

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(1, notifBuilder.build());
    }

}
