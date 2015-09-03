package org.healtheheartstudy;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;

import com.afollestad.materialdialogs.MaterialDialog;
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

    public void displayDummySurvey(View v) {
        displaySurvey("SOME_HOSPITAL", AlarmHelper.getCurrentDate());
    }

    private void displaySurvey(String hospitalName, String date) {
        String content = "Were you at " + hospitalName + " on " + date + " for your medical care?";
        MaterialDialog.Builder builder = new MaterialDialog.Builder(this)
                .content(content)
                .positiveText("Yes")
                .negativeText("No")
                .neutralText("I was there for another reason");
        builder.callback(
                new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        super.onPositive(dialog);
                        dialog.dismiss();
                        removeSurvey();
                    }

                    @Override
                    public void onNegative(MaterialDialog dialog) {
                        super.onNegative(dialog);
                        dialog.dismiss();
                        removeSurvey();
                    }

                    @Override
                    public void onNeutral(MaterialDialog dialog) {
                        super.onNeutral(dialog);
                        dialog.dismiss();
                        removeSurvey();
                    }
                }
        );
        builder.cancelable(false);
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
        String date = intent.getStringExtra(Constants.KEY_DATE);
        if (hospitalName != null && date != null) {
            Timber.d("Intent was not null");
            displaySurvey(hospitalName, date);
        } else {
            SharedPreferences prefs = new SecurePreferences(getApplicationContext());
            hospitalName = prefs.getString(Constants.KEY_PERSISTENT_SURVEY_HOSPITAL, null);
            date = prefs.getString(Constants.KEY_PERSISTENT_SURVEY_DATE, null);
            if (hospitalName != null) {
                Timber.d("SharedPrefs --> KEY_SURVEY was not null");
                displaySurvey(hospitalName, date);
            }
        }
    }

    /**
     * Removes survey from SharedPreferences.
     */
    private void removeSurvey() {
        SharedPreferences prefs = new SecurePreferences(getApplicationContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Constants.KEY_PERSISTENT_SURVEY_HOSPITAL, null);
        editor.putString(Constants.KEY_PERSISTENT_SURVEY_DATE, null);
        editor.apply();
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
