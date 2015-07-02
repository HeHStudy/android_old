package org.healtheheartstudy.client;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;

import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import timber.log.Timber;

/**
 * LocationClient provides an easy-to-use interface for retrieving a user's location.
 */
public class LocationClient extends Client implements LocationListener {

    private static final int TWO_MINUTES = 1000 * 60 * 2;

    private Context context;
    private Listener mListener;
    private LocationRequest request;
    private Location mCurrentLocation;

    public interface Listener {
        void onConnected();
        void onLocationChanged(Location location);
    }

    public LocationClient(Context context, Listener listener) {
        this.context = context;
        this.mListener = listener;
    }

    public void connect() {
        connect(context, LocationServices.API);
    }

    public void requestLocationUpdates(LocationRequest request) {
        this.request = request;
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                request,
                this);
    }

    public void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
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
        boolean isMuchNewer = timeDelta > TWO_MINUTES;
        boolean isMuchOlder = timeDelta < -TWO_MINUTES;
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
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            super.disconnect();
        }
    }

}
