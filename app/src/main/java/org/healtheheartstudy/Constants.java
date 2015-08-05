package org.healtheheartstudy;

/**
 * Created by dannypark on 7/1/15.
 */
public class Constants {

    public static final String KEY_FIRST_APP_OPEN = "first_app_open";
    public static final String KEY_SERVICE_ACTION = "service_action";
    public static final String KEY_HOSPITAL_NAME = "hospital_name";
    public static final String KEY_HOSPITAL_LAT = "hospital_lat";
    public static final String KEY_HOSPITAL_LNG = "hospital_lng";
    public static final String KEY_TRANSITION_TYPE = "transition_type";
    public static final String KEY_SURVEY = "survey";
    public static final String KEY_PREV_USER_LAT = "prev_user_lat";
    public static final String KEY_PREV_USER_LNG = "prev_user_lng";

    public static final String ACTION_CREATE_GEOFENCES = "service_create_geofences";
    public static final String ACTION_UPDATE_TRANSITION_TYPE = "update_transition_type";
    public static final String ACTION_SURVEY_ALARM = "survey_alarm";
    public static final String ACTION_CHECK_LOCATION = "check_location";

    public static final long ONE_HOUR_MILLIS = 1000 * 60 * 60;
    public static final long ONE_DAY_MILLIS = ONE_HOUR_MILLIS * 24;
    public static final int TWO_MINUTES_MILLIS = 1000 * 60 * 2;

    public static final int GEOFENCE_RADIUS_METERS = 100;
    public static final int GEOFENCE_LOITER_TIME_MILLIS = 1000 * 60 * 60 * 4;

}
