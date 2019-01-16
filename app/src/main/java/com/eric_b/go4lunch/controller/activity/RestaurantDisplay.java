package com.eric_b.go4lunch.controller.activity;


import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.eric_b.go4lunch.R;
import com.eric_b.go4lunch.Utils.CountStar;
import com.eric_b.go4lunch.Utils.PlaceidStream;
import com.eric_b.go4lunch.Utils.SPAdapter;
import com.eric_b.go4lunch.View.RestaurantReservedAdapter;
import com.eric_b.go4lunch.api.CompagnyHelper;
import com.eric_b.go4lunch.api.MappedRestaurantHelper;
import com.eric_b.go4lunch.modele.Compagny;
import com.eric_b.go4lunch.modele.placeid.GooglePlaceidPojo;
import com.eric_b.go4lunch.modele.placeid.Photoid;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import java.util.HashMap;
import java.util.List;
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
    @BindView(R.id.starImage1)
    ImageView mStarImage1;
    @BindView(R.id.starImage2)
    ImageView mStarImage2;
    @BindView(R.id.starImage3)
    ImageView mStarImage3;
    @BindView(R.id.noImageTextView)
    TextView mNoImagetext;
    @BindView(R.id.checkButton)
    ImageView mCheckImageViewButton;
    @BindView(R.id.reserveRestaurant_RecyclerView)
    RecyclerView workmateRecyclerView;

    private static final String RESTAURANT_ID = "placeId";
    private DisposableObserver<GooglePlaceidPojo> mPlaceidDisposable;
    private String mGoogleApiKey;
    private String mName, mAdresse, mPhoneNumber, mWebSite;
    private List<Photoid> mPhoto;
    //private boolean mCheck = false;
    private String mRestaurantPlaceId;
    private FirestoreRecyclerAdapter adapter;
    private HashMap mReservedList;
    private int mNumberOfStar;
    private int mNumberOfRating;
    private String mUserName;
    private String mUserPhoto;
    private int mNumberOfWorkmate;
    private HashMap<String,HashMap<String,String>> mUserReservedList;
    private String mUserOldReserved;
    private Dialog mRateDialog;
    private Boolean mReserved = false;
    private String mUserId;
    private String mUserOldNameReserved;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant_display);
        ButterKnife.bind(this);
        mGoogleApiKey = getString(R.string.google_maps_key);
        mRestaurantPlaceId = getIntent().getStringExtra(RESTAURANT_ID);
        SPAdapter spAdapter = new SPAdapter(this);
        mUserId = spAdapter.getUserId();
        mUserName = spAdapter.getUserName();
        mUserPhoto = spAdapter.getUserPhoto();
        mUserOldReserved = spAdapter.getRestaurantReserved();
        mUserOldNameReserved = spAdapter.getRestaurantNameReserved();
        mNumberOfStar = 0;
        mNumberOfRating = 0;
        mReservedList= new HashMap<String,Pair>();
        mRateDialog = new Dialog(RestaurantDisplay.this);
        LoadUserRestaurant();

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
                    //mReserved = documentSnapshot.getBoolean("reserved");
                    mNumberOfStar = Math.round((Long) documentSnapshot.get("numberOfStar"));
                    mNumberOfRating = Math.round((Long) documentSnapshot.get("numberOfRating"));
                    mNumberOfWorkmate = Integer.valueOf((String) documentSnapshot.get("numberOfWorkmate"));
                    mReservedList = new HashMap<String,String>();
                    mReservedList.putAll((HashMap<String,HashMap<String,String>>) documentSnapshot.get("workmatesReservation"));
                    displayWorkmateList();
                    starDisplay();
                }
            }
        });
        loadDetailAnswers(mRestaurantPlaceId);
        getCurrentUserRest();
        callNumber();
        webDisplay();
        checkDisplay();
        likeButtonListener();
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
        new CountStar(mNumberOfStar, mNumberOfRating);
        switch (CountStar.getcount()) {
            case 0: {
                mStarImage1.setImageResource(R.drawable.ic_star_line);
                mStarImage2.setVisibility(View.INVISIBLE);
                mStarImage3.setVisibility(View.INVISIBLE);
            }
            break;
            case 1: {
                mStarImage1.setImageResource(R.drawable.ic_star_yellow);
                mStarImage2.setVisibility(View.INVISIBLE);
                mStarImage3.setVisibility(View.INVISIBLE);
            }
            break;
            case 2: {
                mStarImage1.setImageResource(R.drawable.ic_star_yellow);
                mStarImage2.setVisibility(View.VISIBLE);
                mStarImage3.setVisibility(View.INVISIBLE);
            }
            break;
            case 3: {
                mStarImage1.setImageResource(R.drawable.ic_star_yellow);
                mStarImage2.setVisibility(View.VISIBLE);
                mStarImage3.setVisibility(View.VISIBLE);
            }
            break;
        }
    }

    private void likeButtonListener(){
        mLikeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayDialRate();
                }
        });
    }

    private void displayDialRate(){
        mRateDialog.setContentView(R.layout.rating_dialog_box);
        ImageView closeDial = mRateDialog.findViewById(R.id.dialog_close);
        RatingBar ndOfStar = mRateDialog.findViewById(R.id.restaurant_rate_bare);
        Button dialValid = mRateDialog.findViewById(R.id.dial_button_ok);
        final int[] numberStar = new int[1];
        closeDial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRateDialog.dismiss();
            }
        });

        ndOfStar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                numberStar[0] = Math.round(rating);
            }
        });

        dialValid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mNumberOfStar = mNumberOfStar+numberStar[0];
                mNumberOfRating ++;
                updateSelectedRestaurant(mReserved,mRestaurantPlaceId, mNumberOfStar,mNumberOfRating,String.valueOf(mNumberOfWorkmate),mReservedList);
                mRateDialog.dismiss();
            }
        });
        mRateDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        mRateDialog.show();
    }

    private void checkDisplay() {
        mCheckImageViewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mReserved) {
                    mCheckImageViewButton.setImageResource(R.drawable.ic_check);
                    mReserved = false;
                    updateWorkmateUnselectedRestaurant();
                    if(mReservedList.containsKey(mUserId)) mReservedList.remove(mUserId);
                    mNumberOfWorkmate = mReservedList.size();
                    updateSelectedRestaurant(mReserved, mRestaurantPlaceId, mNumberOfStar, mNumberOfRating, String.valueOf(mNumberOfWorkmate), mReservedList);
                    SPAdapter spAdapter = new SPAdapter(getApplicationContext());
                    spAdapter.setRestaurantReserved("");
                    spAdapter.setRestaurantNameReserved("");
                    spAdapter.setRestaurantAdressReserved("");
                }
                else{
                    mCheckImageViewButton.setImageResource(R.drawable.ic_checked);
                    mReserved = true;
                    updateWorkmateSelectedRestaurant();
                    if (mReservedList==null) mReservedList = new HashMap<String,HashMap<String,String>>();
                    HashMap<String,String> pair = new HashMap<>();
                    pair.put("name",mUserName);
                    pair.put("url_photo",mUserPhoto);
                    mReservedList.put(mUserId,pair);
                    mNumberOfWorkmate = mReservedList.size();
                    if(mUserReservedList!=null)
                        deleteUserOnList();
                    updateSelectedRestaurant(mReserved,mRestaurantPlaceId, mNumberOfStar, mNumberOfRating, String.valueOf(mNumberOfWorkmate), mReservedList);
                    SPAdapter spAdapter = new SPAdapter(getApplicationContext());
                    spAdapter.setRestaurantReserved(mRestaurantPlaceId);
                    spAdapter.setRestaurantNameReserved(mName);
                    spAdapter.setRestaurantAdressReserved(mAdresse);
                }
            }});
    }

    private void deleteUserOnList() {
        mUserReservedList.remove(mUserId);
        if (mUserReservedList.size()==0) MappedRestaurantHelper.updateRestaurantReserved(mUserOldReserved,false);
        MappedRestaurantHelper.updateRestaurantWorkmateList(mUserOldReserved,mUserReservedList);
        MappedRestaurantHelper.updateRestaurantNumberOfWorkmate(mUserOldReserved, String.valueOf(mUserReservedList.size()));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
            mPlaceidDisposable.dispose();
    }

    private void getCurrentUserRest() {
        CompagnyHelper.getWorkmate(Objects.requireNonNull(this.getCurrentUser()).getUid()).addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                Compagny currentWorkmate = documentSnapshot.toObject(Compagny.class);
                if(currentWorkmate.getReservedRestaurant()!=null){
                    if(currentWorkmate.getReservedRestaurant().equals(mRestaurantPlaceId)){
                        mCheckImageViewButton.setImageResource(R.drawable.ic_checked);
                        mReserved=true;
                    }


                }
            }
        });
    }

    //  Update Workmate Selected restaurant
    private void updateWorkmateSelectedRestaurant(){
        if (this.getCurrentUser() != null) {
            CompagnyHelper.updateWorkmateRestaurant(this.mRestaurantPlaceId,this.getCurrentUser().getUid()).addOnFailureListener(this.onFailureListener());
            CompagnyHelper.updateWorkmateRestaurantName(this.mName,this.getCurrentUser().getUid()).addOnFailureListener(this.onFailureListener());
            //updateWorkmateList();
        }
    }

    //  Update Workmate Selected restaurant
    private void updateWorkmateUnselectedRestaurant(){
        if (this.getCurrentUser() != null) {
            //updateWorkmateList();
            CompagnyHelper.updateWorkmateRestaurant("",this.getCurrentUser().getUid()).addOnFailureListener(this.onFailureListener());
            CompagnyHelper.updateWorkmateRestaurantName("",this.getCurrentUser().getUid()).addOnFailureListener(this.onFailureListener());
        }
    }

    private void updateSelectedRestaurant(final Boolean reserved, final String restaurantPlaceId,final int numberOfStar,final int numberOfRating,final String numberOfWorkmate, final HashMap<String,HashMap<String,String>> workmateReservation){
        final Task<DocumentSnapshot> mappedRestaurant = MappedRestaurantHelper.getRestaurant(restaurantPlaceId);
        mappedRestaurant.addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if(Objects.requireNonNull(mappedRestaurant.getResult()).exists()){
                    MappedRestaurantHelper.updateRestaurantReserved(restaurantPlaceId,reserved);
                    MappedRestaurantHelper.updateRestaurantNumberOfWorkmate(restaurantPlaceId,numberOfWorkmate);
                    MappedRestaurantHelper.updateRestaurantWorkmateList(restaurantPlaceId,workmateReservation);
                    MappedRestaurantHelper.updateRestaurantNumberOfStar(restaurantPlaceId,numberOfStar);
                    MappedRestaurantHelper.updateRestaurantNumberOfRating(restaurantPlaceId,numberOfRating);
                }
                else{
                    MappedRestaurantHelper.createRestaurant(restaurantPlaceId, reserved, numberOfStar, numberOfRating, numberOfWorkmate, workmateReservation);
                    //MappedRestaurantHelper.updateRestaurantReserved(restaurantPlaceId,reserved);
                }
            }
        });

        SPAdapter spAdapter = new SPAdapter(this);
        spAdapter.setRestaurantReserved(restaurantPlaceId);
    }

    private void displayWorkmateList(){
        HashMap<String,HashMap<String,String>> workmateList = new HashMap<>(mReservedList);
        if(workmateList.containsKey(mUserId))workmateList.remove(mUserId);
        RestaurantReservedAdapter adapter = new RestaurantReservedAdapter(workmateList, Glide.with(getApplication()), mUserName);
        workmateRecyclerView.setAdapter(adapter);
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


    private void LoadUserRestaurant()  {
            if (!mUserOldReserved.equals("") && !mUserOldReserved.equals(null)) {
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                final DocumentReference docRef = db.collection("mappedRestaurant").document(mUserOldReserved);
                docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                        mUserReservedList  = (HashMap<String,HashMap<String,String>>) documentSnapshot.get("workmatesReservation");
                    }
                });
        }
    }



}
