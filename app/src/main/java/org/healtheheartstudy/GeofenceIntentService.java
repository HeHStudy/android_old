package org.healtheheartstudy;

import android.app.IntentService;
import android.content.Intent;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.List;

import timber.log.Timber;

/**
 * GeofenceIntentService is invoked when a geofence is triggered.
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
        } else {
            // Retrieve geofence info
            List<Geofence> places = gEvent.getTriggeringGeofences();
            String placeName = places.get(0).getRequestId();
            Double lat = gEvent.getTriggeringLocation().getLatitude();
            Double lng = gEvent.getTriggeringLocation().getLongitude();
            int transitionType = gEvent.getGeofenceTransition();

            // Launch service to update geofence transition trigger
            Intent serviceIntent = new Intent(this, HospitalizationService.class);
            serviceIntent.putExtra(Constants.KEY_SERVICE_ACTION, Constants.ACTION_UPDATE_TRANSITION_TYPE);
            serviceIntent.putExtra(Constants.KEY_TRANSITION_TYPE, transitionType);
            serviceIntent.putExtra(Constants.KEY_HOSPITAL_NAME, placeName);
            serviceIntent.putExtra(Constants.KEY_HOSPITAL_LAT, lat);
            serviceIntent.putExtra(Constants.KEY_HOSPITAL_LNG, lng);
            startService(serviceIntent);

            // If EXIT triggered, we need to set an alarm that will display the survey in 1 hour
            if (transitionType == Geofence.GEOFENCE_TRANSITION_EXIT) {
                Timber.d("EXIT was triggered. Starting timer now");
                AlarmHelper ah = new AlarmHelper(this, Constants.ACTION_SURVEY_ALARM);
                ah.putExtra(Constants.KEY_HOSPITAL_NAME, placeName);
                ah.set(AlarmHelper.ONE_HOUR);
            }
        }
    }
}