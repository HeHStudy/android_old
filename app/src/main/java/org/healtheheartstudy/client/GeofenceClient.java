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
import org.healtheheartstudy.PlaceSearchResult;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * GeofenceClient provides an easy-to-use interface for creating geofences. It is specifically
 * designed to build geofences around 'places' from Google Places API.
 */
public class GeofenceClient extends Client implements ResultCallback<Status> {

    private static final int GEOFENCE_RADIUS_METERS = 100;

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
        mPlaces = places;
        populateGeofenceList();
        LocationServices.GeofencingApi.addGeofences(
                mGoogleApiClient,
                getGeofencingRequest(),
                getGeofencePendingIntent()
        ).setResultCallback(this);
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
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_DWELL)
                    .setLoiteringDelay(1000 * 60 * 5)
                    .build();
            mGeofences.add(gf);
        }
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
            Timber.d("Geofences successfully setup");
        } else {
            Timber.d("Geofences failed to setup");
        }
    }

}
