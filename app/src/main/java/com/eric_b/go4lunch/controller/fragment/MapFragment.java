package com.eric_b.go4lunch.controller.fragment;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.eric_b.go4lunch.R;
import com.eric_b.go4lunch.Utils.PlaceStream;
import com.eric_b.go4lunch.controller.activity.RestaurantDisplay;
import com.eric_b.go4lunch.modele.place.GooglePlacePojo;
import com.eric_b.go4lunch.modele.place.Result;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

import io.reactivex.observers.DisposableObserver;

import static android.content.Context.LOCATION_SERVICE;


public class MapFragment extends Fragment {

    // STATIC DATA FOR MAP PERMISSION

    private static final String RESTAURANT_ID = "placeId";
    private static final String USER_NAME = "UserName";
    private static final String USER_PHOTO = "UserPhoto";
    private static final String USER_RESERVED_RESTAURANT = "UserReservedRestaurant";
    private DisposableObserver<GooglePlacePojo> mPlaceDisposable;
    private GoogleMap mGoogleMap;
    private static final String TAG = MapFragment.class.getSimpleName();
    private String mGoogleApiKey;
    private String mLocation;
    private List<Result> mItems;
    private LocationManager mLocationManager;
    private String mProvider;
    private MapView mMapView;
    private View mView;
    private Marker mMarker;
    private HashMap<String, Marker> mMapMarker = new HashMap<String, Marker>();
    private HashMap<Marker, String> mMapItem = new HashMap<Marker,String>();

    public MapFragment() {
        // Required empty public constructor
    }

    public static MapFragment newInstance() {
        MapFragment fragment = new MapFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mGoogleApiKey = getString(R.string.google_maps_key);
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_map, container, false);
        {
            mMapView = mView.findViewById(R.id.mapView);
            mMapView.onCreate(savedInstanceState);
            mMapView.onResume();
            try {
                MapsInitializer.initialize(Objects.requireNonNull(getActivity()).getApplicationContext());
            } catch (Exception e) {
                e.printStackTrace();
            }
            mMapView.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap mMap) {
                    mGoogleMap = mMap;
                    if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    mGoogleMap.setMyLocationEnabled(true);
                    // For showing location
                    Criteria criteria = new Criteria();
                    criteria.setAccuracy(Criteria.ACCURACY_FINE);
                    mLocationManager = (LocationManager) getContext().getSystemService(LOCATION_SERVICE);
                    assert mLocationManager != null;
                    mProvider = mLocationManager.getBestProvider(criteria, true);
                    boolean success = mGoogleMap.setMapStyle(new MapStyleOptions(getResources()
                            .getString(R.string.style_json)));

                    if (!success) {
                        Log.e(TAG, "Style parsing failed.");
                    }

                    // get last know location before display Map
                    if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    Location lastLocation = mLocationManager.getLastKnownLocation(mProvider);
                    if (lastLocation != null) {
                        LatLng latLng = new LatLng(-20.8821, 55.4507);
                        //setPosition(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()));
                        setPosition(latLng);
                    }

                    // listen location update
                    mLocationManager.requestLocationUpdates(mProvider, 60000, 150, new LocationListener() {

                        @Override
                        public void onLocationChanged(Location location) {
                            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                            latLng = new LatLng(-20.8821, 55.4507);
                            setPosition(latLng);
                            loadAnswers();
                        }

                        @Override
                        public void onStatusChanged(String provider, int status, Bundle extras) {

                        }

                        @Override
                        public void onProviderEnabled(String provider) {
                        }

                        @Override
                        public void onProviderDisabled(String provider) {

                        }
                    });

                }
            });
            return mView;
        }
    }

    private void setPosition(LatLng latLng) {
        CameraPosition cameraPosition = new CameraPosition.Builder().target(latLng).zoom(16).build();
        mGoogleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        mLocation = String.valueOf(latLng);
        mLocation = mLocation.substring(10, mLocation.length() - 1);
    }


    @Override
    public void onDetach() {
        super.onDetach();
        mPlaceDisposable.dispose();
        mGoogleMap = null;
        mLocationManager = null;
    }

    private void loadAnswers() {
        mPlaceDisposable = PlaceStream.streamFetchsNearbyRestaurants(mLocation, "500", "restaurant", mGoogleApiKey).subscribeWith(new DisposableObserver<GooglePlacePojo>() {
            @Override
            public void onNext(GooglePlacePojo response) {
                if (response.getStatus().equals("OK")) {
                    // display results if one or more is found
                    PlaceAdapter(response.getResults());
                }
            }

            @Override
            public void onError(Throwable e) {
                //showErrorMessage();
                Log.e(TAG, "onError() called with: e = [" + e + "]");
            }

            @Override
            public void onComplete() {
            }
        });

    }


    private void PlaceAdapter(List<Result> results) {
        //final Marker marker;
        mItems = results;
        for (int i = 0; i < results.size(); i++) {
            final Result item = mItems.get(i);
            Double latitude = item.getGeometry().getLocation().getLat();
            Double longitude = item.getGeometry().getLocation().getLng();
            final LatLng latLng = new LatLng(latitude, longitude);
            final String restaurantName = item.getName();
            final String restaurantId = item.getPlaceId();

            putMarker(R.drawable.ic_restaurant_red, latLng, restaurantName, restaurantId);
            //String markerId = mMarker.getId();
            //int id = Integer.valueOf(markerId.substring(1, markerId.length()));
            mMapMarker.put(item.getPlaceId(),mMarker);
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            final DocumentReference docRef = db.collection("mappedRestaurant").document(item.getPlaceId());
            docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
                @Override
                public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);
                        return;
                    }
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        //Log.d("ressource", "Current restaurant "+documentSnapshot.getId()+" data: " + documentSnapshot.getData().values());
                        mMarker = mMapMarker.get(documentSnapshot.getId());
                        //String markerId = marker.getId();
                        //int id = Integer.valueOf(markerId.substring(1, markerId.length()));
                        if(documentSnapshot.getBoolean("reserved")){
                            mMarker.remove();
                            putMarker(R.drawable.ic_restaurant_green, latLng, restaurantName, restaurantId);
                            mMapMarker.put(item.getPlaceId(),mMarker);
                        }
                        if(!documentSnapshot.getBoolean("reserved")){
                            mMarker.remove();
                            putMarker(R.drawable.ic_restaurant_red, latLng, restaurantName, restaurantId);
                            mMapMarker.put(item.getPlaceId(),mMarker);
                        }
                    } else {


                    }

                }
            });
        }
        // Set a listener for marker click.
        //mGoogleMap.setOnMarkerClickListener(this);
        //this listener is listening the events that you click on the title of the map marker

        mGoogleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {

            @Override
            public boolean onMarkerClick(Marker marker) {
                String placeId = mMapItem.get(marker);
                String markerId = marker.getId();
                int i = Integer.valueOf(markerId.substring(1, markerId.length()));
                //Result item = mItems.get(i);
                //String id = mMap.get(marker);
                //Log.d("ressource", "Listener marker id: "+markerId+" placeId: "+placeId);
                //startRestaurantDisplayActivity(item.getPlaceId());
                //Log.d("ressource", "mUserName02 "+currentUser.getUserName()+" -- mUserPhoto02 "+currentUser.getUserPhoto());
                startRestaurantDisplayActivity(placeId);
                return false;
            }
        });


    }

    private void putMarker(int icRestaurant, LatLng latLng,String restaurantName, String restaurantId) {

        mMarker = mGoogleMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title(restaurantName)
                .icon(BitmapDescriptorFactory.fromResource(icRestaurant)));
        mMapItem.put(mMarker,restaurantId);
        //Log.d("ressource","put id: "+mMarker.getId()+"  rest name: "+restaurantName);
    }

    private void startRestaurantDisplayActivity(String id){
        Intent intent = new Intent(getActivity(), RestaurantDisplay.class);
        intent.putExtra(RESTAURANT_ID,id);
        startActivity(intent);
    }

}
