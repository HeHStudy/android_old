package org.healtheheartstudy.client;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;

import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.healtheheartstudy.GeofenceIntentService;
import org.healtheheartstudy.PlaceSearchResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import timber.log.Timber;

/**
 * GeofenceClient provides an easy-to-use interface for creating geofences and retrieving a user's
 * location.
 */
public class GeofenceClient extends Client implements LocationListener {

    private static final int GEOFENCE_RADIUS_METERS = 100;
//    private static final int GEOFENCE_LOITER_TIME_MILLIS = 1000 * 60 * 5;
    private static final int GEOFENCE_LOITER_TIME_MILLIS = 1000 * 30;
    private static final int GEOFENCE_EXPIRE_TIME_MILLIS = 1000 * 60 * 60 * 24;

    private static final int TWO_MINUTES_MILLIS = 1000 * 60 * 2;

    private List<Geofence> mGeofences;
    private PendingIntent mGeofencePendingIntent;
    private Context context;
    private List<PlaceSearchResult.Place> mPlaces;
    private Location mCurrentLocation;
    private Listener mListener;

    public interface Listener {
        void onConnected();
        void onLocationChanged(Location location);
    }

    public GeofenceClient(Context context, Listener listener) {
        this.context = context;
        this.mListener = listener;
        mGeofences = new ArrayList<>();
    }

    /**
     * Connects to Google API. The specfic API it connects to is passed into the constructor
     * for this class.
     */
    public void connect() {
        connect(context, LocationServices.API);
    }

    /**
     * Retrieves the user's current location.
     * @param request
     */
    public void getUserLocation(LocationRequest request) {
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                request,
                this);
    }

    /**
     * Handles the creation and setting of geofences.
     * @param places
     */
    public void createGeofences(List<PlaceSearchResult.Place> places, ResultCallback<Status> callback) {
        Timber.d("Creating geofences");
        mPlaces = places;
        populateGeofenceList();
        LocationServices.GeofencingApi.addGeofences(
                mGoogleApiClient,
                getGeofencingRequest(),
                getGeofencePendingIntent()
        ).setResultCallback(callback);
    }

    /**
     * Removes a geofence based on the request ID.
     * @param geofenceId A hospital name.
     */
    public void removeFence(String geofenceId, ResultCallback<Status> callback) {
        Timber.d("Removing fence: " + geofenceId);
        LocationServices.GeofencingApi.removeGeofences(
                mGoogleApiClient,
                Arrays.asList(geofenceId)
        ).setResultCallback(callback);
    }

    /**
     * Removes all geofences.
     */
    public void removeAllFences() {
        Timber.d("Removing all fences");
        mGeofences.clear();
        LocationServices.GeofencingApi.removeGeofences(
                mGoogleApiClient,
                getGeofencePendingIntent()
        );
    }

    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_DWELL);
        builder.addGeofences(mGeofences);
        return builder.build();
    }

    private PendingIntent getGeofencePendingIntent() {
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(context, GeofenceIntentService.class);
        mGeofencePendingIntent =
                PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return mGeofencePendingIntent;
    }

    private void populateGeofenceList() {
        for (PlaceSearchResult.Place place : mPlaces) {
            Geofence gf = new Geofence.Builder()
                    .setRequestId(place.name)
                    .setCircularRegion(
                            place.getLocation().getLatitude(),
                            place.getLocation().getLongitude(),
                            GEOFENCE_RADIUS_METERS)
                    .setExpirationDuration(GEOFENCE_EXPIRE_TIME_MILLIS)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_DWELL)
                    .setLoiteringDelay(GEOFENCE_LOITER_TIME_MILLIS)
                    .build();
            mGeofences.add(gf);
        }
        Geofence ch = new Geofence.Builder()
                .setRequestId("chapterhouse")
                .setCircularRegion(
                        39.941976,
                        -75.157326,
                        GEOFENCE_RADIUS_METERS)
                .setExpirationDuration(GEOFENCE_EXPIRE_TIME_MILLIS)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_DWELL)
                .setLoiteringDelay(GEOFENCE_LOITER_TIME_MILLIS)
                .build();
        mGeofences.add(ch);

        Geofence gym = new Geofence.Builder()
                .setRequestId("gym")
                .setCircularRegion(
                        39.948934,
                        -75.164648,
                        GEOFENCE_RADIUS_METERS)
                .setExpirationDuration(GEOFENCE_EXPIRE_TIME_MILLIS)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_DWELL)
                .setLoiteringDelay(GEOFENCE_LOITER_TIME_MILLIS)
                .build();
        mGeofences.add(gym);

        Geofence home = new Geofence.Builder()
                .setRequestId("home")
                .setCircularRegion(
                        39.934400,
                        -75.161308,
                        GEOFENCE_RADIUS_METERS)
                .setExpirationDuration(GEOFENCE_EXPIRE_TIME_MILLIS)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_DWELL)
                .setLoiteringDelay(GEOFENCE_LOITER_TIME_MILLIS)
                .build();
        mGeofences.add(home);


        Timber.d("Finished populating geofences. Number of fences add: " + mGeofences.size());
    }

    /**
     * Determines if the new location is better than the previous location. This code is taken
     * from Google docs.
     * @param newLocation
     * @return
     */
    private boolean isBetterLocation(Location newLocation) {
        if (mCurrentLocation == null) {
            return true;
        }

        long timeDelta = newLocation.getTime() - mCurrentLocation.getTime();
        boolean isMuchNewer = timeDelta > TWO_MINUTES_MILLIS;
        boolean isMuchOlder = timeDelta < -TWO_MINUTES_MILLIS;
        boolean isNewer = timeDelta > 0;

        if (isMuchNewer) {
            return true;
        } else if (isMuchOlder) {
            return false;
        }

        int accuracyDelta = (int) (newLocation.getAccuracy() - mCurrentLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isMuchLessAccurate = accuracyDelta > 200;

        boolean isSameProvider;
        if (newLocation.getProvider() == null) {
            isSameProvider = mCurrentLocation.getProvider() == null;
        } else {
            isSameProvider = newLocation.getProvider().equals(mCurrentLocation.getProvider());
        }

        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isMuchLessAccurate && isSameProvider) {
            return true;
        }

        return false;
    }

    @Override
    public void onLocationChanged(Location location) {
        Timber.d("onLocationChanged()");
        if (isBetterLocation(location)) {
            mCurrentLocation = location;
            mListener.onLocationChanged(mCurrentLocation);
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        mListener.onConnected();
    }

    @Override
    public void disconnect() {
        if (mGoogleApiClient.isConnected()) {
            super.disconnect();
        }
    }

}
