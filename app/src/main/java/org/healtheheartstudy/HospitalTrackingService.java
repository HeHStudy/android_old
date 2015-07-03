package org.healtheheartstudy;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;

import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;

import org.healtheheartstudy.client.GeofenceClient;
import org.healtheheartstudy.client.LocationClient;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import timber.log.Timber;

/**
 * HospitalTrackingService encapsulates all the logic for setting up geofences around hospitals.
 *
 * It performs the following steps upon start:
 *
 * 1) retrieve user's location
 * 2) retrieve hospitals near user's location
 * 3) create geofences around the hospitals (and remove any existing geofences)
 * 4) start a timer to repeat the entire process in 24 hours
 *
 * We want tocre
 *
 */
public class HospitalTrackingService extends Service implements LocationClient.Listener, HospitalHelper.Listener {

    public static boolean IS_ALIVE = false;
    private static final long ONE_DAY_MILLIS = 1000 * 60 * 60 * 24;

    private LocationClient mLocationClient;
    private GeofenceClient mGeofenceClient;
    private HospitalHelper mHospitalHelper;
    private RemoveGeofenceBroadcastReceiver mBroadcastReceiver;

    /**
     * Resets the geofences every 24 hours to ensure that we have fresh data.
     */
    private Timer mTimer;

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

        // Register BroadcastReceiver
        mBroadcastReceiver = new RemoveGeofenceBroadcastReceiver();
        IntentFilter filter = new IntentFilter(Constants.INTENT_REMOVE_GEOFENCE);
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, filter);
    }

    public void startRefreshTimer() {
        Timber.i("Starting timer");
        if (mTimer != null) mTimer.cancel();
        mTimer = new Timer();
        mTimer.scheduleAtFixedRate(new RefreshGeofences(), ONE_DAY_MILLIS, ONE_DAY_MILLIS);
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
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        mTimer.cancel();
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
        // TODO: Can we set these to 0 for an immediate response?
        mLocationRequest.setInterval(1000 * 60);
        mLocationRequest.setFastestInterval(1000);
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
    public void onHospitalsFound(final List<PlaceSearchResult.Place> hospitals) {
        Timber.d("Hospitals found: " + hospitals.size());
        if (isGeofenceConnected) {
            // Before creating geofences, remove any existing ones
            mGeofenceClient.removeAllFences(new ResultCallback<Status>() {
                @Override
                public void onResult(Status status) {
                    if (status.isSuccess()) {
                        mGeofenceClient.createGeofences(hospitals);
                    } else {
                        // TODO: Handle error case....make another call to onHospitalsFound to retry?
                    }
                }
            });
        } else {
            // TODO: Do something here...wait for connection? (Should almost always be connected at this point)
        }
    }

    /**
     * A broadcast receiver that listens for broadcasts to remove a geofence.
     * This is triggered when a user enters a geofence.
     */
    private class RemoveGeofenceBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            // Geofences identifiers are hospital names
            String geofenceId = intent.getStringExtra(Constants.INTENT_HOSPITAL_NAME);

            // Sanity check--should never be null
            if (geofenceId != null) mGeofenceClient.removeFence(geofenceId);
            else Timber.e("geofenceId was null in onReceive()");
        }
    }

    /**
     * When a message is sent to this handler, launch a request to retrieve
     * the user's location which subsequently creates new geofences based on the user's location.
     */
    private Handler timerHandler = new Handler() {

        @Override
        public void handleMessage(Message m) {
            Timber.i("Timer triggered");
            // Retrieve location and build new geofences
            mLocationClient.connect();
        }

    };

    private class RefreshGeofences extends TimerTask {

        @Override
        public void run() {
            timerHandler.sendEmptyMessage(0);
        }

    }

}
