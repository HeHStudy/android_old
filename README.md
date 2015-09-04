<b>Testing Hospitalization</b>
<br>
<br>
1. You can add custom 'hospitals' (locations that will trigger the survey) in addHospitalForDebugging() of HospitalizationService.java. There should already be 1 custom hospital, so you can just follow that format to add as many as you'd like. 
<br><br>
2. You'll probably want to change the trigger times so you're not sitting around for 5 hours. You'll want to change <b>Constants.GEOFENCE_LOITER_TIME_MILLIS</b> (amount of time to mark user as hospitalized) and <b>Constants.SURVEY_TRIGGER_MILLIS</b> (amount of time to wait after user exits hospital before displaying survey) in Constants.java.
<br><br>
3. Install the app on your device once you've set the above variables.
3a. If this is NOT a fresh install (i.e. you previously installed the app and decided to change some variables and re-install), you'll need to press the 'RESTART SERVICE' button after installation.
<br><br>
4. Assuming you've placed a hospital where your current location is, wait for <b>GEOFENCE_LOITER_TIME_MILLIS</b> (I would wait an extra minute or two to account for the geofence setup time). This will mark the user as hospitalized (you will NOT receive any indication of this on your phone, but it will display in the Android Studio logs).
<br><br>
5. To mimic a user exiting a location, I use a third-party application called "Mock Locations" (https://play.google.com/store/apps/details?id=ru.gavrikov.mocklocations&hl=en). You need to enable 'Allow mock locations' in your phone's developer options (Settings -> Developer Options). Follow the app's instructions to mimic leaving the 'hospital'. 
<br><br>
6. Wait for <b>SURVEY_TRIGGER_MILLIS</b> and you should receive a notification that upon clicking will take you to the app and display a survey.
