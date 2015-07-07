package org.healtheheartstudy;

import java.util.ArrayList;
import java.util.List;

/**
 * PlaceSearchResult is a subset of Places from Google Places API. It is molded to our needs.
 */
public class PlaceSearchResult {

    private String status;
    private List<Place> results;
    private List<String> html_attributions;
    private String next_page_token;

    PlaceSearchResult() {
        status = "";
        results = new ArrayList<>();
        html_attributions = new ArrayList<>();
        next_page_token = "";
    }

    public String getStatus() {
        return status;
    }

    public List<Place> getPlaces() {
        return results;
    }

    public String getNextPage() {
        return next_page_token;
    }

    public class Place {

        public Geometry geometry;
        public String icon;
        public String name;
        public String place_id;
        public String scope;
        public List<String> types;
        public String vicinity;

        Place() {
            geometry = new Geometry();
            icon = "";
            name = "";
            place_id = "";
            scope = "";
            types = new ArrayList<>();
            vicinity = "";
        }

        public android.location.Location getLocation() {
            android.location.Location location = new android.location.Location("");
            location.setLatitude(geometry.location.lat);
            location.setLongitude(geometry.location.lng);
            return location;
        }

        class Geometry {
            Location location;
            Geometry() {
                location = new Location();
            }
        }

        class Location {
            double lat;
            double lng;
            Location() {
                lat = 0.0;
                lng = 0.0;
            }
        }

    }

}
