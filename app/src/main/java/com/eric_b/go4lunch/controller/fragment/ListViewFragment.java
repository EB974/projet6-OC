package com.eric_b.go4lunch.controller.fragment;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.eric_b.go4lunch.R;
import com.eric_b.go4lunch.Utils.Distance;
import com.eric_b.go4lunch.Utils.PlaceStream;
import com.eric_b.go4lunch.Utils.PlaceidStream;
import com.eric_b.go4lunch.View.ListViewAdapter;
import com.eric_b.go4lunch.controller.activity.RestaurantDisplay;
import com.eric_b.go4lunch.modele.place.GooglePlacePojo;
import com.eric_b.go4lunch.modele.place.Result;
import com.eric_b.go4lunch.modele.placeid.Resultid;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.protobuf.StringValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.observers.DisposableObserver;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

import static android.content.Context.LOCATION_SERVICE;

public class ListViewFragment extends Fragment {


    private static final String TAG = ListViewFragment.class.getSimpleName();

    @BindView(R.id.restaurantRecyclerView)
    RecyclerView mRecyclerView;
    private static ListViewAdapter mAdapter;
    private static final String RESTAURANT_ID = "placeId";
    private static final String USER_NAME = "UserName";
    private static final String USER_PHOTO = "UserPhoto";
    private static final String USER_RESERVED_RESTAURANT = "UserReservedRestaurant";
    private int mRecoverPosition = 0;
    private DisposableObserver<GooglePlacePojo> mPlaceDisposable;
    private String mLocation;
    private String mGoogleApiKey;
    private Location mLastLocation;
    private String mUserName;
    private String mUserPhoto;
    private String mUserReservedRestaurant;

    public ListViewFragment() {
        // Required empty public constructor
    }


    public static ListViewFragment newInstance() {
        ListViewFragment fragment = new ListViewFragment();
        return fragment;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mGoogleApiKey = getString(R.string.google_maps_key);
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        LocationManager locationManager = (LocationManager) Objects.requireNonNull(getContext()).getSystemService(LOCATION_SERVICE);
        assert locationManager != null;
        String provider = locationManager.getBestProvider(criteria, true);
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mLastLocation = locationManager.getLastKnownLocation(provider);
        if (mLastLocation != null) {
            LatLng latLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            latLng = new LatLng(-20.8821, 55.4507);
        mLocation =  String.valueOf(latLng);
        mLocation = mLocation.substring(10,mLocation.length()-1);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list_view, container, false);
        ButterKnife.bind(this, view);
        configureRecyclerView();
        if (savedInstanceState!= null) { //recover the recyclerView position
            mRecoverPosition = savedInstanceState.getInt("POSITION");
        }
        new LoadAnswers().execute(); //load response from GoogleMap API
        return view;
    }

    private void configureRecyclerView(){
        this.mAdapter = new ListViewAdapter( new SparseArray(0), Glide.with(this), new ListViewAdapter.PostItemListener() {
            @Override
            public void onPostClick(String id) {
                startRestaurantDisplayActivity(id);
            }
        },mLastLocation, mGoogleApiKey);
        mRecyclerView.setAdapter(mAdapter);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setHasFixedSize(true);
        RecyclerView.ItemDecoration itemDecoration = new DividerItemDecoration(Objects.requireNonNull(getActivity()), DividerItemDecoration.VERTICAL);
        mRecyclerView.addItemDecoration(itemDecoration);
    }

    private class LoadAnswers extends AsyncTask<String, String, String> {
        Result resulForDistance;
        GooglePlacePojo results;

        @Override
        protected String doInBackground(String... strings) {
            mPlaceDisposable = PlaceStream.streamFetchsNearbyRestaurants(mLocation, "500", "restaurant", mGoogleApiKey).subscribeWith(new DisposableObserver<GooglePlacePojo>() {
                @Override
                public void onNext(GooglePlacePojo response) {
                    if (response.getStatus().equals("OK")) {
                        results = response;
                    }
                }
                @Override
                public void onError(Throwable e) {
                    //showErrorMessage();
                    Log.e(TAG, "onError() called with: e = [" + e + "]");
                }
                @Override
                public void onComplete() {
                    ArrayList<String> categories = new ArrayList<>();
                    HashMap<Integer, String> hmap = new HashMap<Integer, String>();
                    SparseArray<String> sparseArray = new SparseArray<String>();
                    try {
                        for (int i = 0; i < results.getResults().size(); i++) {
                            resulForDistance = results.getResults().get(i);
                            Location restaurantLocation = new Location("restaurant");
                            restaurantLocation.setLatitude(resulForDistance.getGeometry().getLocation().getLat());
                            restaurantLocation.setLongitude(resulForDistance.getGeometry().getLocation().getLng());
                            Distance distance = new Distance(mLastLocation, restaurantLocation);
                            int restdist = distance.getDistance();
                            String restId = results.getResults().get(i).getPlaceId();
                            sparseArray.put(restdist, restId);
                        }

                        // display results if one or more is found
                        //mAdapter.updateAnswers(response.getResults());
                        //i++;
                        mAdapter.updateAnswers(sparseArray);
                    }
                    catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            });

            return null;
        }

    }
    @Override
    public void onDetach() {
        super.onDetach();
        //mPlaceDisposable.dispose();
    }

    private void startRestaurantDisplayActivity(String id){
        Intent intent = new Intent(getActivity(), RestaurantDisplay.class);
        intent.putExtra(RESTAURANT_ID,id);
        startActivity(intent);
    }
}
