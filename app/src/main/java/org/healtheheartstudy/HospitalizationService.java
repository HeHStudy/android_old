package org.healtheheartstudy;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.IBinder;

import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationRequest;
import com.securepreferences.SecurePreferences;

import org.healtheheartstudy.client.GeofenceClient;

import java.util.Arrays;
import java.util.List;

import timber.log.Timber;

/**
 * HospitalizationService is started to perform one of three tasks:
 *
 * a. Build geofences around nearby hospitals
 *      1) Retrieve the user's location
 *      2) Retrieve hospitals near user's location
 *      3) Remove any existing geofences
 *      4) Create geofences around the hospitals
 * b. Update geofence transition triggers
 * c. Check if user has moved a significant distance, in which case we need to
 *    find new hospitals in the area (perform steps a2 - a4). This check is
 *    scheduled to occur daily.
 *
 * After the service performs a task, it is shut down so that no additional resources are consumed.
 * Although the service shuts down, the geofences will continue to be tracked in the background.
 */
public class HospitalizationService extends Service implements
        HospitalHelper.Listener,
        GeofenceClient.Listener,
        ResultCallback<Status> {

    private GeofenceClient mGeofenceClient;
    private HospitalHelper mHospitalHelper;
    private Intent mIntent;
    private String action;

    @Override
    public void onCreate() {
        super.onCreate();
        Timber.d("onCreate");
        mHospitalHelper = new HospitalHelper();
        mGeofenceClient = new GeofenceClient(this, this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Timber.d("onStartCommand");
        mIntent = intent;
        mGeofenceClient.connect();
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Notifies the service when the google api client has connected.
     */
    @Override
    public void onConnected() {
        Timber.d("onConnected");
        action = mIntent.getStringExtra(Constants.KEY_SERVICE_ACTION);
        if (action.equals(Constants.ACTION_CREATE_GEOFENCES) || action.equals(Constants.ACTION_CHECK_LOCATION)) {
            Timber.d("CREATE_GEOFENCES or CHECK_LOCATION");
            // Instead of getting lastKnownLocation, request a single location update for more precision
            LocationRequest mLocationRequest = new LocationRequest();
            mLocationRequest.setInterval(1000 * 60);
            mLocationRequest.setFastestInterval(1000);
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            mLocationRequest.setNumUpdates(1);
            mGeofenceClient.getUserLocation(mLocationRequest);
        } else if (action.equals(Constants.ACTION_UPDATE_TRANSITION_TYPE)) {
            // Retrieve extras
            String hospitalName = mIntent.getStringExtra(Constants.KEY_HOSPITAL_NAME);
            Double lat = mIntent.getDoubleExtra(Constants.KEY_HOSPITAL_LAT, 0);
            Double lng = mIntent.getDoubleExtra(Constants.KEY_HOSPITAL_LNG, 0);
            final int transitionTrigger = mIntent.getIntExtra(Constants.KEY_TRANSITION_TYPE, 0);

            // Create place with extras data
            PlaceSearchResult psr = new PlaceSearchResult();
            final PlaceSearchResult.Place hospital = psr.new Place();
            hospital.name = hospitalName;
            hospital.setLocation(lat, lng);

            // Update transition trigger
            int newTransitionTrigger = 0;
            if (transitionTrigger == Geofence.GEOFENCE_TRANSITION_DWELL) {
                newTransitionTrigger = Geofence.GEOFENCE_TRANSITION_EXIT;
                Timber.d("DWELL was triggered -- now updating transition to EXIT");
            } else if (transitionTrigger == Geofence.GEOFENCE_TRANSITION_EXIT){
                newTransitionTrigger = Geofence.GEOFENCE_TRANSITION_DWELL;
                Timber.d("EXIT was triggered -- now updating transition to DWELL");
            }
            mGeofenceClient.createGeofences(Arrays.asList(hospital),
                    newTransitionTrigger,
                    this);
        }
    }

    /**
     * Serves as a callback that receives the user's current location. We search for hospitals
     * with this location. The action will specify if we should immediately search for hospitals
     * or if we need to check if the current location is far from the previous location, in which
     * case we need to search for new hospitals in the new area.
     *
     * @param location
     */
    @Override
    public void onLocationChanged(Location location) {
        Timber.d("Found user's location: " + location.toString());
        boolean stopService = false;
        SharedPreferences prefs = new SecurePreferences(this);

        if (action.equals(Constants.ACTION_CREATE_GEOFENCES)) {
            mHospitalHelper.findHospitalsInArea(location, HospitalHelper.DEFAULT_SEARCH_RADIUS, this);
        } else if (action.equals(Constants.ACTION_CHECK_LOCATION)) {
            // Get previous location
            double prevLat = Double.longBitsToDouble(prefs.getLong(Constants.KEY_PREV_USER_LAT, 0));
            double prevLng = Double.longBitsToDouble(prefs.getLong(Constants.KEY_PREV_USER_LNG, 0));

            // Get distance between locations
            Location prevLocation = new Location("");
            prevLocation.setLatitude(prevLat);
            prevLocation.setLongitude(prevLng);
            float distance = location.distanceTo(prevLocation);

            if (distance > HospitalHelper.DEFAULT_SEARCH_RADIUS) {
                // Find new hospitals to track because the user moved a significant distance
                mHospitalHelper.findHospitalsInArea(location, HospitalHelper.DEFAULT_SEARCH_RADIUS, this);
            } else {
                stopService = true;
            }
        }

        // Save current location to SP
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(Constants.KEY_PREV_USER_LAT, Double.doubleToRawLongBits(location.getLatitude()));
        editor.putLong(Constants.KEY_PREV_USER_LNG, Double.doubleToRawLongBits(location.getLongitude()));
        editor.apply();

        if (stopService) {
            mGeofenceClient.disconnect();
            stopSelf();
        }
    }

    @Override
    public void onHospitalsFound(final List<PlaceSearchResult.Place> hospitals) {
        Timber.d("Hospitals found: " + hospitals.size());
        // First remove any existing geofences and then create geofences around the hospitals
        mGeofenceClient.removeAllFences();
        mGeofenceClient.createGeofences(hospitals, Geofence.GEOFENCE_TRANSITION_DWELL, this);
    }

    /**
     * Notifies the service when a Geofence API call has finished. We only care about the callbacks
     * for creating geofences and removing a geofence, because it lets us know the service can
     * be stopped.
     * @param status
     */
    @Override
    public void onResult(Status status) {
        if (status.isSuccess()) {
            mGeofenceClient.disconnect();
            stopSelf();
        } else {
            // TODO: Handle failure case
        }
    }

}
