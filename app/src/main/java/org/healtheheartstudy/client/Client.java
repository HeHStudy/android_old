package org.healtheheartstudy.client;

import android.content.Context;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient;

/**
 * Client is a wrapper to easily use GoogleApiClient.
 */
public class Client implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private Context context;
    private Api api;
    protected GoogleApiClient mGoogleApiClient;

    protected synchronized void connect(Context context, Api api) {
        this.context = context;
        this.api = api;
        if (mGoogleApiClient == null || !mGoogleApiClient.isConnected()) {
            mGoogleApiClient = new GoogleApiClient.Builder(context)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(api)
                    .build();
            mGoogleApiClient.connect();
        }
    }

    public void disconnect() {
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnected(Bundle bundle) {
    }

    @Override
    public void onConnectionSuspended(int i) {
        connect(context, api);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
}
