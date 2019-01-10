package com.eric_b.go4lunch.controller.fragment;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.bumptech.glide.Glide;
import com.eric_b.go4lunch.R;
import com.eric_b.go4lunch.Utils.CountStar;
import com.eric_b.go4lunch.Utils.Distance;
import com.eric_b.go4lunch.Utils.PlaceStream;
import com.eric_b.go4lunch.Utils.RestaurantList;
import com.eric_b.go4lunch.Utils.SPAdapter;
import com.eric_b.go4lunch.View.ListViewAdapter;
import com.eric_b.go4lunch.controller.activity.RestaurantDisplay;
import com.eric_b.go4lunch.modele.place.GooglePlacePojo;
import com.eric_b.go4lunch.modele.place.Result;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.SuccessContinuation;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.observers.DisposableObserver;
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
    private static final String SORT_BY_DISTANCE = "by_Distance";
    private static final String SORT_BY_RATE = "by_Star";
    private int mRecoverPosition = 0;
    private DisposableObserver<GooglePlacePojo> mPlaceDisposable;
    private String mLocation;
    private String mGoogleApiKey;
    private Location mLastLocation;
    private String mUserName;
    private String mUserPhoto;
    private String mUserReservedRestaurant;
    private String mSorting;
    private RestaurantList mRestaurantList;

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
        SPAdapter spAdapter = new SPAdapter(Objects.requireNonNull(getActivity()));
        mSorting = spAdapter.getSorting();
        if (mSorting.equals("")) mSorting = SORT_BY_DISTANCE;
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
        //configureRecyclerView();
        if (savedInstanceState!= null) { //recover the recyclerView position
            mRecoverPosition = savedInstanceState.getInt("POSITION");
        }
        new LoadAnswers().execute(); //load response from GoogleMap API
        return view;
    }

    private void configureRecyclerView(){
        mAdapter = new ListViewAdapter( mRestaurantList, Glide.with(this), new ListViewAdapter.PostItemListener() {
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

    private class LoadAnswers extends AsyncTask<String, String, HashMap<String, Integer>> {
        Result resulForDistance;
        GooglePlacePojo results;
        final HashMap<String, Integer> map = new HashMap<>();
        final int[] numberOfStar = new int[1];

        @Override
        protected HashMap<String, Integer> doInBackground(String... strings) {
            mRestaurantList = new RestaurantList();
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
                    String restId;
                        try {
                            for (int i = 0; i < results.getResults().size(); i++) {
                                restId = results.getResults().get(i).getPlaceId();
                                resulForDistance = results.getResults().get(i);
                                Location restaurantLocation = new Location("restaurant");
                                restaurantLocation.setLatitude(resulForDistance.getGeometry().getLocation().getLat());
                                restaurantLocation.setLongitude(resulForDistance.getGeometry().getLocation().getLng());
                                Distance distance = new Distance(mLastLocation, restaurantLocation);
                                int restdist = distance.getDistance();
                                map.put(restId,restdist);
                            }
                            //final DocumentReference docRef = db.collection("mappedRestaurant").document(entry.getKey());
                            new LoadRate(map).execute();
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }

                    }
            });
            return map;
        }

        @Override
        protected void onPostExecute(HashMap<String, Integer> map) {
            super.onPostExecute(map);


        }
        //mAdapter.updateAnswers(restaurantList);

    }


    private class LoadRate extends AsyncTask<String, String, String> {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        final int[] rating = new int[1];
        HashMap<String, Integer> map;
        public LoadRate(HashMap<String, Integer> map) {
            this.map = map;
        }

        @Override
        protected String doInBackground(String... strings) {
            final CollectionReference docRef = db.collection("mappedRestaurant");
            for (final Map.Entry<String,Integer> entry : map.entrySet()){

                docRef.document(entry.getKey()).get().onSuccessTask(new SuccessContinuation<DocumentSnapshot, Object>() {
                    @NonNull
                    @Override
                    public Task<Object> then(@android.support.annotation.Nullable DocumentSnapshot documentSnapshot) throws Exception {
                        mRestaurantList.setId(entry.getKey());
                        mRestaurantList.setDistance(entry.getValue());
                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            int numberOfStar = Math.round((Long) documentSnapshot.get("numberOfStar"));
                            int numberOfRating = Math.round((Long) documentSnapshot.get("numberOfRating"));
                            new CountStar(numberOfStar, numberOfRating);
                            rating[0] = CountStar.getcount();
                        }
                        else rating[0] = 0;
                        mRestaurantList.setRate(rating[0]);
                        if (mRestaurantList.getSize()==map.size()){

                            if (mSorting.equals(SORT_BY_RATE)) {
                                mRestaurantList.sortRestaurant("rate");
                                configureRecyclerView();
                                mAdapter.updateAnswers(mRestaurantList);
                            }
                            if (mSorting.equals(SORT_BY_DISTANCE)) {
                                mRestaurantList.sortRestaurant("distance");
                                configureRecyclerView();
                                mAdapter.updateAnswers(mRestaurantList);
                            }
                        }
                        return null;
                    }
                });
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
        }

    }


    @Override
    public void onDetach() {
        super.onDetach();
        mPlaceDisposable.dispose();
    }

    private void startRestaurantDisplayActivity(String id){
        Intent intent = new Intent(getActivity(), RestaurantDisplay.class);
        intent.putExtra(RESTAURANT_ID,id);
        startActivity(intent);
    }
}
