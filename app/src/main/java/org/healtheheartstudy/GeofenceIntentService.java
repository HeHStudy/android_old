package org.healtheheartstudy;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * GeofenceIntentService is invoked when the user enters a geofence.
 */
public class GeofenceIntentService extends IntentService {

    public GeofenceIntentService() {
        super("GeofenceIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        GeofencingEvent gEvent = GeofencingEvent.fromIntent(intent);
        if (gEvent.hasError()) {
            Timber.e("Geofence error code: " + gEvent.getErrorCode());
        } else if (gEvent.getGeofenceTransition() == Geofence.GEOFENCE_TRANSITION_DWELL) {
            List<Geofence> places = gEvent.getTriggeringGeofences();

            // Build notification
            buildNotification(places);
        } else {
            Timber.e("Geofences were triggered with wrong transitions");
        }
    }


    private void buildNotification(List<Geofence> geofences) {
        // Create the intent extra which contains the geofence(s) name(s) that were triggered
        String intentExtra;
        if (geofences.size() > 0) {
            List geofenceNames = new ArrayList();
            for (Geofence g : geofences) {
                geofenceNames.add(g.getRequestId());
            }
            intentExtra = TextUtils.join(",", geofenceNames);
        } else {
            Geofence geofence = geofences.get(0);
            intentExtra = geofence.getRequestId();
        }

        // Create intent to launch when notification is selected. If the activity is already
        // running, bring it to the top and finish all other activities
        Intent displayIntent = new Intent(this, MainActivity.class);
        displayIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        displayIntent.putExtra(Constants.INTENT_HOSPITAL_NAME, intentExtra);
        PendingIntent displayPendingIntent = PendingIntent.getActivity(
                this,
                0,
                displayIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_media_pause)
                .setContentTitle("Health eHeart")
                .setContentText("We noticed that you're near a hospital and we wanted to make sure you're OK!")
                .setContentIntent(displayPendingIntent)
                .setAutoCancel(true)
                .setVibrate(new long[]{500, 500, 500, 500});

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(1, notifBuilder.build());

        // Send broadcast to service to remove geofence
        Intent removeFenceIntent = new Intent(Constants.INTENT_REMOVE_GEOFENCE);
        removeFenceIntent.putExtra(Constants.INTENT_HOSPITAL_NAME, intentExtra);
        LocalBroadcastManager.getInstance(this).sendBroadcast(removeFenceIntent);
    }

}
