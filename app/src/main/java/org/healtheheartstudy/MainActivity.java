package org.healtheheartstudy;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AlertDialog;
import android.view.View;

import com.securepreferences.SecurePreferences;

import timber.log.Timber;


public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create geofences if this is the first time opening the app
        SharedPreferences prefs = new SecurePreferences(getApplicationContext());
        boolean firstOpen = prefs.getBoolean(Constants.KEY_FIRST_APP_OPEN, true);
        if (firstOpen) {
            // Launch hospitalization
            Intent serviceIntent = new Intent(this, HospitalizationService.class);
            serviceIntent.putExtra(Constants.KEY_SERVICE_ACTION, Constants.ACTION_CREATE_GEOFENCES);
            startService(serviceIntent);

            // Start alarm to check user's location every day
            AlarmHelper ah = new AlarmHelper(this, Constants.ACTION_CHECK_LOCATION);
            ah.setRepeating(Constants.ONE_DAY_MILLIS, Constants.ONE_DAY_MILLIS);

            prefs.edit().putBoolean(Constants.KEY_FIRST_APP_OPEN, false).apply();
        }
    }

    private void displaySurvey(String hospitalName) {
        Timber.d("displaySurvey for: " + hospitalName);
        AlertDialog builder = new AlertDialog.Builder(this)
                .setTitle("Hospital Alert")
                .setMessage("We noticed that you were near " + hospitalName + ". Are you visiting " +
                        "to treat a medical condition?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        removeSurvey();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        removeSurvey();
                    }
                })
                .setNeutralButton("Foo", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        removeSurvey();
                    }
                })
                .setCancelable(false)
                .create();
        builder.show();
    }

    /**
     * Checks if the app was opened from a notification, in which case we need to present
     * a survey to the user. We also check SharedPreferences in case the user cleared
     * the notification.
     * @param intent
     */
    private void checkForSurvey(Intent intent) {
        String hospitalName = intent.getStringExtra(Constants.KEY_HOSPITAL_NAME);
        if (hospitalName != null) {
            Timber.d("Intent was not null");
            displaySurvey(hospitalName);
        } else {
            SharedPreferences prefs = new SecurePreferences(getApplicationContext());
            hospitalName = prefs.getString(Constants.KEY_SURVEY, null);
            if (hospitalName != null) {
                Timber.d("SharedPrefs --> KEY_SURVEY was not null");
                displaySurvey(hospitalName);
            }
        }
    }

    /**
     * Removes survey from SharedPreferences.
     */
    private void removeSurvey() {
        SharedPreferences prefs = new SecurePreferences(getApplicationContext());
        prefs.edit().putString(Constants.KEY_SURVEY, null).apply();
    }

    public void restartService(View v) {
        Intent serviceIntent = new Intent(this, HospitalizationService.class);
        serviceIntent.putExtra(Constants.KEY_SERVICE_ACTION, Constants.ACTION_CREATE_GEOFENCES);
        startService(serviceIntent);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Timber.d("onNewIntent()");
        checkForSurvey(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        checkForSurvey(getIntent());
    }
}
