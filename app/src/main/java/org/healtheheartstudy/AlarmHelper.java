package org.healtheheartstudy;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * AlarmManager wrapper
 */
public class AlarmHelper {

    private Context context;
    private Intent intent;

    public AlarmHelper(Context context, String intentAction) {
        this.context = context;
        intent = new Intent(context, CustomBroadcastReceiver.class);
        intent.setAction(intentAction);
    }

    public void putExtra(String key, String value) {
        intent.putExtra(key, value);
    }

    public void set(long triggerAtMillis) {
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + triggerAtMillis, pi);
    }

    public void setRepeating(long triggerAtMillis, long intervalMillis) {
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.setRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + triggerAtMillis,
                intervalMillis,
                pi);
    }

    // Returns a date formatted in mm-dd-yyyy (e.g. 05-26-1992)
    public static String getCurrentDate() {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("MM-dd-yyyy");
        return df.format(c.getTime());
    }

}
