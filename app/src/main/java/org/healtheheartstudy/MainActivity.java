package org.healtheheartstudy;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AlertDialog;
import android.widget.Button;

import timber.log.Timber;


public class MainActivity extends ActionBarActivity {

    private Button mButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean alarmSet = prefs.getBoolean(Constants.KEY_ALARM_SET, false);
        if (!alarmSet) {
            // Create pending intent for service
            Intent serviceIntent = new Intent(this, HospitalTrackingService.class);
            serviceIntent.putExtra(Constants.KEY_SERVICE_ACTION, Constants.VALUE_SERVICE_CREATE_GEOFENCES);
            PendingIntent pi = PendingIntent.getService(this, 0, serviceIntent, 0);

            // Set alarm
            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 0, AlarmManager.INTERVAL_DAY, pi);

            prefs.edit().putBoolean(Constants.KEY_ALARM_SET, true).apply();
        }

        String hospitalName = getIntent().getStringExtra(Constants.KEY_HOSPITAL_NAME);
        if (hospitalName != null) displaySurvey(hospitalName);
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
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setCancelable(false)
                .create();
        builder.show();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Timber.d("onNewIntent()");
        String hospitalName = intent.getStringExtra(Constants.KEY_HOSPITAL_NAME);
        if (hospitalName != null) displaySurvey(hospitalName);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
