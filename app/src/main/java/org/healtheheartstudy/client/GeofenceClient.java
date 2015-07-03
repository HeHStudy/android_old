package org.healtheheartstudy.client;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

import org.healtheheartstudy.GeofenceIntentService;
import org.healtheheartstudy.HospitalTrackingService;
import org.healtheheartstudy.PlaceSearchResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import timber.log.Timber;

/**
 * GeofenceClient provides an easy-to-use interface for creating geofences. It is specifically
 * designed to build geofences around 'places' from Google Places API.
 */
public class GeofenceClient extends Client implements ResultCallback<Status> {

    private static final int GEOFENCE_RADIUS_METERS = 100;
//    private static final int GEOFENCE_LOITER_TIME_MILLIS = 1000 * 60 * 5;
    private static final int GEOFENCE_LOITER_TIME_MILLIS = 1000 * 30;
    private static final int GEOFENCE_EXPIRE_TIME_MILLIS = 1000 * 60 * 60 * 24;

    private List<Geofence> mGeofences;
    private PendingIntent mGeofencePendingIntent;
    private Context context;
    private List<PlaceSearchResult.Place> mPlaces;
    private Listener mListener;

    public interface Listener {
        void onConnected();
    }

    public GeofenceClient(Context context, Listener listener) {
        this.context = context;
        this.mListener = listener;
        mGeofences = new ArrayList<>();
    }

    public void connect() {
        connect(context, LocationServices.API);
    }

    /**
     * Handles the creation and setting of geofences.
     * @param places
     */
    public void createGeofences(List<PlaceSearchResult.Place> places) {
        Timber.d("Creating geofences");
        mPlaces = places;
        populateGeofenceList();
        LocationServices.GeofencingApi.addGeofences(
                mGoogleApiClient,
                getGeofencingRequest(),
                getGeofencePendingIntent()
        ).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(Status status) {
                if (status.isSuccess()) ((HospitalTrackingService) context).startRefreshTimer();
            }
        });
    }

    /**
     * Removes a geofence based on the request ID.
     * @param geofenceId A hospital name.
     */
    public void removeFence(String geofenceId) {
        Timber.d("Removing fence: " + geofenceId);
        LocationServices.GeofencingApi.removeGeofences(
                mGoogleApiClient,
                Arrays.asList(geofenceId)
        ).setResultCallback(this);
    }

    /**
     * Removes all geofences.
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
        Timber.d("Finished populating geofences. Number of fences add: " + mGeofences.size());
    }

    @Override
    public void onConnected(Bundle bundle) {
        mListener.onConnected();
    }

    @Override
    public void disconnect() {
        if (mGoogleApiClient.isConnected()) {
            LocationServices.GeofencingApi.removeGeofences(mGoogleApiClient, getGeofencePendingIntent());
            super.disconnect();
        }
    }

    @Override
    public void onResult(Status status) {
        if (status.isSuccess()) {
            Timber.d("Geofences status: success " + status.getStatus() +
                    " || " +
                    status.getStatusCode() +
                    " || " +
                    status.getStatusMessage());
        } else {
            Timber.e("Geofences status: failed");
        }
    }

}
