package com.eric_b.go4lunch.controller.activity;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.eric_b.go4lunch.R;
import com.eric_b.go4lunch.Utils.PlaceidStream;
import com.eric_b.go4lunch.View.RestaurantReservedAdapter;
import com.eric_b.go4lunch.api.MappedRestaurantHelper;
import com.eric_b.go4lunch.api.RestaurantHelper;
import com.eric_b.go4lunch.api.UserHelper;
import com.eric_b.go4lunch.modele.MappedRestaurant;
import com.eric_b.go4lunch.modele.Restaurant;
import com.eric_b.go4lunch.modele.User;
import com.eric_b.go4lunch.modele.UserMin;
import com.eric_b.go4lunch.modele.placeid.GooglePlaceidPojo;
import com.eric_b.go4lunch.modele.placeid.Photoid;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.observers.DisposableObserver;


public class RestaurantDisplay extends BaseActivity {
    @BindView(R.id.call)
    Button mCallButton;
    @BindView(R.id.like)
    Button mLikeButton;
    @BindView(R.id.web)
    Button mWebButton;
    @BindView(R.id.restaurantImageView)
    ImageView mRestaurantImageView;
    @BindView(R.id.restaurantName)
    TextView mRestaurantName;
    @BindView(R.id.restaurantAdresse)
    TextView mRestaurantAdresse;
    @BindView(R.id.starImage)
    ImageView mStarImage;
    @BindView(R.id.noImageTextView)
    TextView mNoImagetext;
    @BindView(R.id.checkButton)
    ImageView mCheckImageViewButton;
    @BindView(R.id.reserveRestaurant_RecyclerView)
    RecyclerView workmateRecyclerView;

    private static final String RESTAURANT_ID = "placeId";
    private static final String USER_NAME = "UserName";
    private static final String USER_PHOTO = "UserPhoto";
    private DisposableObserver<GooglePlaceidPojo> mPlaceidDisposable;
    private String mGoogleApiKey;
    private String mName, mAdresse, mPhoneNumber, mWebSite;
    private List<Photoid> mPhoto;
    private String mStarColor = "line";
    private boolean mCheck = false;
    private String mRestaurantPlaceId;
    private FirestoreRecyclerAdapter adapter;
    private int mNumbStar;
    private HashMap mReservedList;
    private int mNumberOfStar;
    private String mUserName;
    private String mUserPhoto;
    private RecyclerView mRecyclerView;
    private RestaurantReservedAdapter mAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant_display);
        ButterKnife.bind(this);
        mGoogleApiKey = getString(R.string.google_maps_key);
        mRestaurantPlaceId = getIntent().getStringExtra(RESTAURANT_ID);
        mUserName = getIntent().getStringExtra(USER_NAME);
        mUserPhoto = getIntent().getStringExtra(USER_PHOTO);
        mNumberOfStar = 0;
        mReservedList= new HashMap<String,String>();
        UserMin userMin = new UserMin();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        final DocumentReference docRef = db.collection("mappedRestaurant").document(mRestaurantPlaceId);
        docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w("ressource", "Listen failed.", e);
                    return;
                }
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    mNumberOfStar = Math.round((Long) documentSnapshot.get("numberOfStar"));
                    mReservedList= new HashMap<String,String>();
                    mReservedList.putAll((HashMap<String,String>) documentSnapshot.get("workmatesReservation"));
                    displayWorkmateList();
                }
            }
        });
        loadDetailAnswers(mRestaurantPlaceId);
        getCurrentUserRest();
        callNumber();
        webDisplay();
        starDisplay();
        checkDisplay();
        //getWorkmateList();
    }


    private void loadDetailAnswers(String detailId) {
        //Log.d("Ressource", "detailId "+detailId);
        this.mPlaceidDisposable = PlaceidStream.streamFetchsDetailRestaurants(detailId, "name,formatted_address,photo,formatted_phone_number,website", "restaurant", mGoogleApiKey).subscribeWith(new DisposableObserver<GooglePlaceidPojo>() {
            @Override
            public void onNext(GooglePlaceidPojo placeidResponce) {
                if (placeidResponce.getStatus().equals("OK")) {
                    mName = placeidResponce.getResult().getName();
                    mAdresse = placeidResponce.getResult().getFormattedAddress();
                    mPhoneNumber = placeidResponce.getResult().getFormattedPhoneNumber();
                    mWebSite = placeidResponce.getResult().getWebsite();
                    mPhoto = placeidResponce.getResult().getPhotos();
                    adapter();
                    photoDisplay();

                }
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onComplete() {
            }
        });
    }

    private void adapter() {
        mRestaurantName.setText(mName);
        mRestaurantAdresse.setText(mAdresse);
        if (mPhoneNumber == null) {
            mCallButton.setEnabled(false);
            mCallButton.setTextColor(getResources().getColor(R.color.grey_icon));
        }
        if (mWebSite == null) {
            mWebButton.setEnabled(false);
            mWebButton.setTextColor(getResources().getColor(R.color.grey_icon));
        }
    }

    private void callNumber() {
        mCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Intent callIntent = new Intent(Intent.ACTION_CALL);
                callIntent.setData(Uri.parse("tel:" + mPhoneNumber));
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                startActivity(callIntent);

            }
        });
    }

    private void photoDisplay () {
        Photoid photo = mPhoto.get(0);

        try {
            String photoheight = "403";
            String photowidth = "302";
            String photoUrl = "https://maps.googleapis.com/maps/api/place/photo?photoreference="+photo.getPhotoReference()+"&sensor=false&maxheight="+photoheight+"&maxwidth="+photowidth+"&key="+getString(R.string.google_maps_key);
            Glide.with(RestaurantDisplay.this)
                    .load(photoUrl)
                    .into(mRestaurantImageView);
            mNoImagetext.setText("");
        } catch (Throwable e) {
            e.printStackTrace();
            mRestaurantImageView.setImageResource(R.drawable.no_image);
            mNoImagetext.setText(getString(R.string.no_image));
        }
    }

    private void webDisplay(){
        mWebButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(mWebSite));
                startActivity(browserIntent);
            }
        });
    }

    private void starDisplay() {
        mLikeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mStarColor.equals("yellow")) {
                    mStarImage.setImageResource(R.drawable.ic_star_yellow);
                    mStarColor = "yellow";
                    mNumberOfStar++;
                    MappedRestaurantHelper.updateRestaurantNumberOfStar(mRestaurantPlaceId,mNumberOfStar);
                }
            else{
                    mStarImage.setImageResource(R.drawable.ic_star_line);
                    mStarColor = "line";
                    if (mNumberOfStar>0){
                        mNumberOfStar--;
                        MappedRestaurantHelper.updateRestaurantNumberOfStar(mRestaurantPlaceId,mNumberOfStar);
                    }
                }
        }});
    }

    private void checkDisplay() {
        mCheckImageViewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCheck) {
                    mCheckImageViewButton.setImageResource(R.drawable.ic_check);
                    mCheck = false;
                    updateUserUnselectedRestaurant();
                    if(mReservedList.containsKey(mUserName)) mReservedList.remove(mUserName);
                    updateSelectedRestaurant(false, mRestaurantPlaceId, mNumberOfStar, mReservedList);
                }
                else{
                    mCheckImageViewButton.setImageResource(R.drawable.ic_checked);
                    mCheck = true;
                    updateUserSelectedRestaurant();
                    mReservedList.put(mUserName,mUserPhoto);
                    updateSelectedRestaurant(true,mRestaurantPlaceId, mNumberOfStar, mReservedList);
                }
            }});
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
            mPlaceidDisposable.dispose();
    }

    private void getCurrentUserRest() {
        UserHelper.getUser(Objects.requireNonNull(this.getCurrentUser()).getUid()).addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                User currentUser = documentSnapshot.toObject(User.class);
                if(currentUser.getReservedRestaurant()!=null){
                    if(currentUser.getReservedRestaurant().equals(mRestaurantPlaceId)){
                        mCheckImageViewButton.setImageResource(R.drawable.ic_checked);
                        mCheck=true;
                    }


                }
            }
        });
    }

    //  Update User Selected restaurant
    private void updateUserSelectedRestaurant(){
        if (this.getCurrentUser() != null) {
            UserHelper.updateUserRestaurant(this.mRestaurantPlaceId,this.getCurrentUser().getUid()).addOnFailureListener(this.onFailureListener());
            UserMin userMin= new UserMin(this.getCurrentUser().getUid());
            //updateWorkmateList();
        }
    }

    //  Update User Selected restaurant
    private void updateUserUnselectedRestaurant(){
        if (this.getCurrentUser() != null) {
            //updateWorkmateList();
            UserHelper.updateUserRestaurant("",this.getCurrentUser().getUid()).addOnFailureListener(this.onFailureListener());

        }
    }

    private void updateSelectedRestaurant(final Boolean reserved, final String restaurantPlaceId,final int numberOfStar,final HashMap<String,String> workmateReservation){
        final Task<DocumentSnapshot> mappedRestaurant = MappedRestaurantHelper.getRestaurant(restaurantPlaceId);
        mappedRestaurant.addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if(Objects.requireNonNull(mappedRestaurant.getResult()).exists()){
                    MappedRestaurantHelper.updateRestaurantReserved(restaurantPlaceId,reserved);
                    MappedRestaurantHelper.updateRestaurantWorkmateList(restaurantPlaceId,workmateReservation);
                }
                else{
                    MappedRestaurantHelper.createRestaurant(restaurantPlaceId, reserved, numberOfStar,workmateReservation);
                    //MappedRestaurantHelper.updateRestaurantReserved(restaurantPlaceId,reserved);
                }
            }
        });
    }

    private void displayWorkmateList(){
        HashMap<String, String> workmateList = new HashMap<>(mReservedList);
        if(workmateList.containsKey(mUserName))workmateList.remove(mUserName);
        mAdapter = new RestaurantReservedAdapter(workmateList, Glide.with(this),mUserName);
        workmateRecyclerView.setAdapter(mAdapter);
        workmateRecyclerView.setLayoutManager(new LinearLayoutManager(this));

    }

    @Override
    public void onStart() {
        super.onStart();
        //adapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        //adapter.stopListening();
    }
}
