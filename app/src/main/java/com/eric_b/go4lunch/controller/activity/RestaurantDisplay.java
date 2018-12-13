package com.eric_b.go4lunch.controller.activity;


import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.eric_b.go4lunch.R;
import com.eric_b.go4lunch.Utils.PlaceidStream;
import com.eric_b.go4lunch.api.MappedRestaurantHelper;
import com.eric_b.go4lunch.api.UserHelper;
import com.eric_b.go4lunch.modele.User;
import com.eric_b.go4lunch.modele.placeid.GooglePlaceidPojo;
import com.eric_b.go4lunch.modele.placeid.Photoid;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.observers.DisposableObserver;
import pub.devrel.easypermissions.EasyPermissions;


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

    private static final String RESTAURANT_ID = "placeId";
    private DisposableObserver<GooglePlaceidPojo> mPlaceidDisposable;
    private String mGoogleApiKey;
    private String mName, mAdresse, mPhoneNumber, mWebSite;
    private List<Photoid> mPhoto;
    private String mStarColor = "line";
    private boolean mCheck = false;
    private String mRestaurantPlaceId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant_display);
        ButterKnife.bind(this);
        mGoogleApiKey = getString(R.string.google_maps_key);
        mRestaurantPlaceId = getIntent().getStringExtra(RESTAURANT_ID);
        loadDetailAnswers(mRestaurantPlaceId);
        getCurrentUserRest();
        callNumber();
        webDisplay();
        starDisplay();
        checkDisplay();
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
                }
            else{
                    mStarImage.setImageResource(R.drawable.ic_star_line);
                    mStarColor = "line";}
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
                    updateSelectedRestaurant(false, mRestaurantPlaceId);
                }
                else{
                    mCheckImageViewButton.setImageResource(R.drawable.ic_checked);
                    mCheck = true;
                    updateUserSelectedRestaurant();
                    updateSelectedRestaurant(true,mRestaurantPlaceId);
                }
            }});
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
            mPlaceidDisposable.dispose();
    }

    private void getCurrentUserRest() {
        UserHelper.getUser(this.getCurrentUser().getUid()).addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
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
        }
    }

    //  Update User Selected restaurant
    private void updateUserUnselectedRestaurant(){
        if (this.getCurrentUser() != null) {
            UserHelper.updateUserRestaurant("",this.getCurrentUser().getUid()).addOnFailureListener(this.onFailureListener());
        }
    }

    private void updateSelectedRestaurant(final Boolean reserved, final String restaurantPlaceId){
        final Task<DocumentSnapshot> mappedRestaurant = MappedRestaurantHelper.getRestaurantReseved(restaurantPlaceId);
        mappedRestaurant.addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if(mappedRestaurant.getResult().exists())
                    MappedRestaurantHelper.updateRestaurantId(restaurantPlaceId,reserved);
                else {
                    MappedRestaurantHelper.createRestaurant(restaurantPlaceId, reserved);
                    MappedRestaurantHelper.updateRestaurantId(restaurantPlaceId,reserved);
                }
            }
        });

    }

}
