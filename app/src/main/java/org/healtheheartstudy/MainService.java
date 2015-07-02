package org.healtheheartstudy;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;

import com.google.android.gms.location.LocationRequest;

import org.healtheheartstudy.client.GeofenceClient;
import org.healtheheartstudy.client.LocationClient;

import java.util.List;

import timber.log.Timber;

public class MainService extends Service implements LocationClient.Listener, HospitalHelper.Listener {

    public static boolean IS_ALIVE = false;

    private LocationClient mLocationClient;
    private GeofenceClient mGeofenceClient;
    private HospitalHelper mHospitalHelper;

    private boolean isGeofenceConnected;

    @Override
    public void onCreate() {
        super.onCreate();
        IS_ALIVE = true;

        // Create clients
        mHospitalHelper = new HospitalHelper();
        mLocationClient = new LocationClient(this, this);
        mGeofenceClient = new GeofenceClient(this, new GeofenceClient.Listener() {
            @Override
            public void onConnected() {
                isGeofenceConnected = true;
            }
        });

        // Connect clients
        mLocationClient.connect();
        mGeofenceClient.connect();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        IS_ALIVE = false;
        mLocationClient.disconnect();
        mGeofenceClient.disconnect();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onConnected() {
        // Instead of getting lastKnownLocation, request a single location update for more precision
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000 * 60 * 2);
        mLocationRequest.setFastestInterval(30000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setNumUpdates(1);
        mLocationClient.requestLocationUpdates(mLocationRequest);
    }

    @Override
    public void onLocationChanged(Location location) {
        Timber.d("Found user's location: " + location.toString());
        mHospitalHelper.findHospitalsInArea(location, 10000, this);
        mLocationClient.disconnect(); // No longer needed
    }

    @Override
    public void onHospitalsFound(List<PlaceSearchResult.Place> hospitals) {
        Timber.d("********** Found hospitals **********");
        for (PlaceSearchResult.Place hospital : hospitals) {
            Timber.d(hospital.name);
        }
        if (isGeofenceConnected) {
            mGeofenceClient.createGeofences(hospitals);
        } else {
            // TODO: Do something here...wait for connection? (Should almost always be connected at this point)
        }
    }

}
