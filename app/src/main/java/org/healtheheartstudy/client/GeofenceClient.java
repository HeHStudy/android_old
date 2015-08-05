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

import org.healtheheartstudy.Constants;
import org.healtheheartstudy.GeofenceIntentService;
import org.healtheheartstudy.PlaceSearchResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import timber.log.Timber;

/**
 * GeofenceClient provides an interface for creating geofences and retrieving a user's
 * location.
 */
public class GeofenceClient extends Client implements LocationListener {

    private List<Geofence> mGeofences;
    private PendingIntent mGeofencePendingIntent;
    private Context context;
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
    public void createGeofences(List<PlaceSearchResult.Place> places, int transitionType,
                                ResultCallback<Status> callback) {
        Timber.d("Creating geofences");
        populateGeofenceList(transitionType, places);
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

    /**
     * Removes all geofences with a success/error callback.
     * @param callback
     */
    public void removeAllFences(ResultCallback<Status> callback) {
        Timber.d("Removing all fences");
        mGeofences.clear();
        LocationServices.GeofencingApi.removeGeofences(
                mGoogleApiClient,
                getGeofencePendingIntent()
        ).setResultCallback(callback);
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

    /**
     * Creates an array of geofences
     * @param transitionType The transition type for the trigger.
     */
    private void populateGeofenceList(int transitionType, List<PlaceSearchResult.Place> places) {
        for (PlaceSearchResult.Place place : places) {
            Geofence.Builder builder = new Geofence.Builder()
                    .setRequestId(place.name)
                    .setCircularRegion(
                            place.getLocation().getLatitude(),
                            place.getLocation().getLongitude(),
                            Constants.GEOFENCE_RADIUS_METERS
                    )
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(transitionType);

            // Set loiter time if it is a DWELL trigger
            if (transitionType == Geofence.GEOFENCE_TRANSITION_DWELL) {
                builder.setLoiteringDelay(Constants.GEOFENCE_LOITER_TIME_MILLIS);
            }

            mGeofences.add(builder.build());
        }
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
        boolean isMuchNewer = timeDelta > Constants.TWO_MINUTES_MILLIS;
        boolean isMuchOlder = timeDelta < -Constants.TWO_MINUTES_MILLIS;
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
