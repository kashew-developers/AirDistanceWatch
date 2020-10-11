package in.kashewdevelopers.airdistance_watch;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import android.graphics.Color;
import android.os.Bundle;

import androidx.wear.widget.SwipeDismissFrameLayout;

import android.support.wearable.activity.WearableActivity;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.Toast;

import in.kashewdevelopers.airdistance_watch.databinding.ActivityMapsBinding;

@SuppressWarnings("deprecation")
public class MapsActivity extends WearableActivity implements OnMapReadyCallback {

    ActivityMapsBinding binding;

    public Marker sourceMarker;
    public Marker destinationMarker;
    public Polyline distanceLine;
    private GoogleMap mMap;

    LatLngToPlaceTask.OnTaskCompleteListener latLngToPlaceTaskListener;

    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setAmbientEnabled();

        setLatLngToPlaceTaskListener();

        binding.distanceMsg.setVisibility(View.GONE);

        // Enables the Swipe-To-Dismiss Gesture via the root layout (SwipeDismissFrameLayout).
        // Swipe-To-Dismiss is a standard pattern in Wear for closing an app and needs to be
        // manually enabled for any Google Maps Activity. For more information, review our docs:
        // https://developer.android.com/training/wearables/ui/exit.html
        binding.swipeDismissRootContainer.addCallback(new SwipeDismissFrameLayout.Callback() {
            @Override
            public void onDismissed(SwipeDismissFrameLayout layout) {
                // Hides view before exit to avoid stutter.
                layout.setVisibility(View.GONE);
                finish();
            }
        });

        // Adjusts margins to account for the system window insets when they become available.
        binding.swipeDismissRootContainer.setOnApplyWindowInsetsListener(
                new View.OnApplyWindowInsetsListener() {
                    @Override
                    public WindowInsets onApplyWindowInsets(View view, WindowInsets insets) {
                        insets = binding.swipeDismissRootContainer.onApplyWindowInsets(insets);

                        FrameLayout.LayoutParams params =
                                (FrameLayout.LayoutParams) binding.mapContainer.getLayoutParams();

                        // Sets Wearable insets to FrameLayout container holding map as margins
                        params.setMargins(
                                insets.getSystemWindowInsetLeft(),
                                insets.getSystemWindowInsetTop(),
                                insets.getSystemWindowInsetRight(),
                                insets.getSystemWindowInsetBottom());
                        binding.mapContainer.setLayoutParams(params);

                        return insets;
                    }
                });

        // Obtain the MapFragment and set the async listener to be notified when the map is ready.
        MapFragment mapFragment =
                (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        showInstruction();

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                setMarker(latLng);
            }
        });

        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {
            }

            @Override
            public void onMarkerDrag(Marker marker) {
            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                if (marker.equals(sourceMarker)) {
                    marker.setTitle(getString(R.string.source));
                    initiatePlaceNameRetrieval(marker.getPosition(), getString(R.string.source));
                } else if (marker.equals(destinationMarker)) {
                    marker.setTitle(getString(R.string.destination));
                    initiatePlaceNameRetrieval(marker.getPosition(), getString(R.string.destination));
                }
                showDistance();
            }
        });

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                marker.showInfoWindow();
                return true;
            }
        });
    }


    public void setLatLngToPlaceTaskListener() {
        latLngToPlaceTaskListener = new LatLngToPlaceTask.OnTaskCompleteListener() {
            @Override
            public void onTaskCompleteListener(LatLng coordinates, String placeName, String placeType) {
                if (placeType.equals(getString(R.string.source))) {
                    if (sourceMarker.getPosition().equals(coordinates)) {
                        sourceMarker.setTitle(placeName);
                        sourceMarker.showInfoWindow();
                    }
                } else if (placeType.equals(getString(R.string.destination))) {
                    if (destinationMarker.getPosition().equals(coordinates)) {
                        destinationMarker.setTitle(placeName);
                        destinationMarker.showInfoWindow();
                    }
                }
            }
        };
    }


    // map elements
    public void setMarker(LatLng latLng) {
        if (sourceMarker == null || sourceMarker.isInfoWindowShown()) {
            setSourceMarker(latLng);
        } else if (destinationMarker == null || destinationMarker.isInfoWindowShown()) {
            setDestinationMarker(latLng);
        }
        showDistance();
    }

    public void setSourceMarker(LatLng latLng) {
        if (sourceMarker == null) {
            sourceMarker = mMap.addMarker(new MarkerOptions().position(latLng));
        }

        sourceMarker.setPosition(latLng);
        sourceMarker.setTitle(getString(R.string.source));
        sourceMarker.setDraggable(true);

        initiatePlaceNameRetrieval(latLng, getString(R.string.source));
    }

    public void setDestinationMarker(LatLng latLng) {
        if (destinationMarker == null) {
            destinationMarker = mMap.addMarker(new MarkerOptions().position(latLng));
        }

        destinationMarker.setPosition(latLng);
        destinationMarker.setTitle(getString(R.string.destination));
        destinationMarker.setDraggable(true);

        initiatePlaceNameRetrieval(latLng, getString(R.string.destination));
    }


    // functionality
    public void showInstruction() {
        Toast instructionToast = Toast.makeText(this, getString(R.string.instructions), Toast.LENGTH_LONG);
        instructionToast.setGravity(Gravity.CENTER, 0, 0);
        instructionToast.show();
    }

    private double getDistanceInKm() {
        // haversine formula
        double earthRadius = 6378.137; // Radius of earth at equator in KM
        double diffLat = sourceMarker.getPosition().latitude * Math.PI / 180
                - destinationMarker.getPosition().latitude * Math.PI / 180;
        double diffLon = sourceMarker.getPosition().longitude * Math.PI / 180
                - destinationMarker.getPosition().longitude * Math.PI / 180;
        double a = Math.sin(diffLat / 2) * Math.sin(diffLat / 2) +
                Math.cos(sourceMarker.getPosition().latitude * Math.PI / 180) *
                        Math.cos(destinationMarker.getPosition().latitude * Math.PI / 180) *
                        Math.sin(diffLon / 2) * Math.sin(diffLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return c * earthRadius;
    }

    private void showDistance() {
        if (sourceMarker == null || destinationMarker == null)
            return;

        if (distanceLine != null)
            distanceLine.remove();

        distanceLine = mMap.addPolyline(new PolylineOptions()
                .add(sourceMarker.getPosition(), destinationMarker.getPosition())
                .width(5)
                .color(Color.RED));

        handleDistanceUnit(getDistanceInKm());
    }

    public void handleDistanceUnit(double distanceInKm) {
        String unit = "Km";
        double distance;

        if (distanceInKm < 1) {
            distance = distanceInKm * 1000;
            unit = (distance > 1) ? "Meters" : "Meter";
        } else {
            distance = distanceInKm;
        }

        String formatted = getString(R.string.distance_msg, distance, unit);
        binding.distanceMsg.setText(formatted);
        binding.distanceMsg.setVisibility(View.VISIBLE);
    }

    public void initiatePlaceNameRetrieval(LatLng coordinate, String placeType) {
        new LatLngToPlaceTask(this, placeType)
                .setTaskListener(latLngToPlaceTaskListener)
                .execute(coordinate);
    }

}