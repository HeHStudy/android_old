package org.healtheheartstudy;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;

import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;

import org.healtheheartstudy.client.GeofenceClient;

import java.util.List;

import timber.log.Timber;

/**
 * HospitalTrackingService performs two tasks:
 *
 * a. Build geofences around nearby hospitals
 *      1) Retrieve the user's location
 *      2) Retrieve hospitals near user's location
 *      3) Remove any existing geofences
 *      4) Create geofences around the hospitals
 *
 * b. Remove a geofence after it has been triggered
 *
 * An alarm will prompt the service will perform task (a) every 24 hours. This is to ensure that
 * we account for any major location changes from the user and to (re)create any geofences that
 * may have been triggered.
 *
 * Triggered geofences are removed for 1 day because it is unlikely that a user visits a hospital
 * multiple times in a day, and we don't want to annoy the user.
 *
 * After the service performs a task, it is shut down so that no additional resources are consumed.
 * Although the service shuts down, the geofences will continue to be tracked in the background.
 */
public class HospitalTrackingService extends Service implements
        HospitalHelper.Listener,
        GeofenceClient.Listener,
        ResultCallback<Status> {

    private GeofenceClient mGeofenceClient;
    private HospitalHelper mHospitalHelper;
    private Intent mIntent;

    @Override
    public void onCreate() {
        super.onCreate();
        mHospitalHelper = new HospitalHelper();
        mGeofenceClient = new GeofenceClient(this, this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
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
     * Notifies the service when the google api client has connected. From here we either retrieve
     * the user's location to build geofences or remove a geofence.
     */
    @Override
    public void onConnected() {
        String action = mIntent.getStringExtra(Constants.KEY_SERVICE_ACTION);
        if (action.equals(Constants.VALUE_SERVICE_CREATE_GEOFENCES)) {
            // Instead of getting lastKnownLocation, request a single location update for more precision
            LocationRequest mLocationRequest = new LocationRequest();
            // TODO: Can we set these to 0 for an immediate response?
            mLocationRequest.setInterval(1000 * 60);
            mLocationRequest.setFastestInterval(1000);
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            mLocationRequest.setNumUpdates(1);
            mGeofenceClient.getUserLocation(mLocationRequest);
        } else if (action.equals(Constants.VALUE_SERVICE_REMOVE_GEOFENCES)) {
            String fenceToRemove = mIntent.getStringExtra(Constants.KEY_HOSPITAL_NAME);
            mGeofenceClient.removeFence(fenceToRemove, this);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Timber.d("Found user's location: " + location.toString());
        mHospitalHelper.findHospitalsInArea(location, 10000, this);
    }

    @Override
    public void onHospitalsFound(final List<PlaceSearchResult.Place> hospitals) {
        Timber.d("Hospitals found: " + hospitals.size());
        // First remove any existing geofences and then create geofences around the hospitals
        mGeofenceClient.removeAllFences();
        mGeofenceClient.createGeofences(hospitals, this);
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
