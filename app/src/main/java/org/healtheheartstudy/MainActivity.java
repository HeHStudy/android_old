package org.healtheheartstudy;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;


public class MainActivity extends ActionBarActivity {

    private Button mButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mButton = (Button) findViewById(R.id.main_button);
        if (MainService.IS_ALIVE) mButton.setText("Turn tracking off");
        else mButton.setText("Turn tracking on");

        String hospitalName = getIntent().getStringExtra(Constants.INTENT_HOSPITAL_NAME);
        if (hospitalName != null) displaySurvey(hospitalName);
    }

    public void buttonClick(View v) {
        if (MainService.IS_ALIVE) {
            Intent service = new Intent(this, MainService.class);
            stopService(service);
            mButton.setText("Turn tracking on");
        } else {
            Intent service = new Intent(this, MainService.class);
            startService(service);
            mButton.setText("Turn tracking off");
        }
    }

    private void displaySurvey(final String hospitalName) {
        AlertDialog builder = new AlertDialog.Builder(this)
                .setTitle("Hospital Alert")
                .setMessage("We noticed that you were near " + hospitalName + ". Are you visiting the hospital" +
                        "to treat a medical condition?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
//                        Intent intent = new Intent(Constants.INTENT_USER_SURVEY);
//                        intent.putExtra(Constants.INTENT_VISITED_HOSPITAL, true);
//                        intent.putExtra(Constants.INTENT_HOSPITAL_NAME, hospitalName);
//                        LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(intent);
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
        String hospitalName = getIntent().getStringExtra(Constants.INTENT_HOSPITAL_NAME);
        if (hospitalName != null) displaySurvey(hospitalName);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
