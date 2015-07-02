package org.healtheheartstudy;

import android.location.Location;
import android.os.Handler;

import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.healtheheartstudy.network.GsonRequest;
import org.healtheheartstudy.network.RequestManager;

import java.util.ArrayList;
import java.util.List;

/**
 * HospitalHelper retrieves all hospitals that are within a provided range of a provided location
 * by using Google Places Search API (not a client API--need to use web API).
 */
public class HospitalHelper implements Response.Listener<PlaceSearchResult>, Response.ErrorListener {

    private static final String API_KEY = "AIzaSyALHFB5BA3emjybU9ZrPUCLDEcCyC37vjk";
    private static final String API_ROOT = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?key=" + API_KEY;

    private List<PlaceSearchResult.Place> mHospitalsInArea;
    private Listener listener;

    interface Listener {
        void onHospitalsFound(List<PlaceSearchResult.Place> hospitals);
    }

    /**
     * Launches a request to retrieve hospitals that are within the provided radius
     * away from the provided location. This is a public helper function.
     * @param location The area to find hospitals in.
     * @param radius The search radius in meters.
     * @param listener The callback for when hospitals are found.
     */
    public synchronized void findHospitalsInArea(Location location, int radius, Listener listener) {
        this.mHospitalsInArea = new ArrayList<>();
        this.listener = listener;
        findHospitalsInArea(location.getLatitude(), location.getLongitude(), radius, null);
    }

    /**
     * Makes a request to find all hospitals within the provided radius
     * of location.
     * @param lat
     * @param lng
     * @param radius
     * @param nextPageToken @Nullable. If set, we need to retrieve the next page of results
     */
    private void findHospitalsInArea(double lat, double lng, int radius, String nextPageToken) {
        // Build URL
        String requestURL = "";
        if (nextPageToken == null) {
            requestURL = API_ROOT + "&location=" + lat + "," + lng
                    + "&radius=" + radius
                    + "&types=hospital";
        } else {
            requestURL = API_ROOT + "&pagetoken=" + nextPageToken;
        }

        // Build request
        GsonRequest<PlaceSearchResult> request = new GsonRequest<>(
                requestURL,
                PlaceSearchResult.class,
                null,
                this,
                this
        );

        // Launch request
        RequestManager.getRequestQueue().add(request);
    }

    /**
     * Handles success callback for retrieving a list of nearby hospitals. Will make another request
     * if there are additional pages to retrieve.
     * @param response
     */
    @Override
    public void onResponse(final PlaceSearchResult response) {
        mHospitalsInArea.addAll(response.getPlaces());
        boolean moreResultsToFetch = !response.getNextPage().isEmpty();
        if (moreResultsToFetch) {
            // Google docs state that the nextPage won't be accessible for 'a few moments', so
            // take a 5 second nap and then request the next page
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    findHospitalsInArea(0, 0, 0, response.getNextPage());
                }
            }, 5000);
        } else {
            listener.onHospitalsFound(mHospitalsInArea);
        }
    }

    /**
     * Handles error callback for retrieving a list of nearby hospitals.
     * @param error
     */
    @Override
    public void onErrorResponse(VolleyError error) {
        // TODO: do something here
    }
}
