package com.eric_b.go4lunch.View;

import android.content.Context;
import android.location.Location;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.request.RequestOptions;
import com.eric_b.go4lunch.R;
import com.eric_b.go4lunch.Utils.CalculateOpeningHours;
import com.eric_b.go4lunch.Utils.CountStar;
import com.eric_b.go4lunch.Utils.Distance;
import com.eric_b.go4lunch.Utils.PlaceidStream;
import com.eric_b.go4lunch.Utils.RestaurantList;
import com.eric_b.go4lunch.api.MappedRestaurantHelper;
import com.eric_b.go4lunch.modele.placeid.GooglePlaceidPojo;
import com.eric_b.go4lunch.modele.placeid.OpeningHoursid;
import com.eric_b.go4lunch.modele.placeid.Photoid;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.protobuf.StringValue;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

import javax.annotation.Nullable;

import io.reactivex.observers.DisposableObserver;


public class ListViewAdapter extends RecyclerView.Adapter<ListViewAdapter.ViewHolder> {


    //private List<Result> mItems;
    private RestaurantList mItems;
    private PostItemListener mItemListener;
    private RequestManager mGlide;
    private String mKey;
    private String mPhotoReference;
    private String mDetailId;
    private OpeningHoursid mOpeningHoursid;
    private ArrayList mList;
    private Location mLocation;


    public ListViewAdapter(RestaurantList results, RequestManager glide, PostItemListener itemListener, Location usrLocation, String key) {
        mItemListener = itemListener;
        mItems = results;
        mGlide = glide;
        mLocation = usrLocation;
        mKey = key;
        mList = results.getList();
        Log.d("ressource","mList "+mList);
        Log.d("ressource","results "+results);
    }

    public interface Listeners {
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        PostItemListener mItemListener;
        TextView mRestaurantName, mRestaurantAdresse, mRestaurantOpenHour, mRestaurantDistance, mNbOfWorkmate;
        ImageView mIcWorkmate, mRestaurantPhoto, mStar01, mStar02, mStar03;


        ViewHolder(View restaurantView, PostItemListener postItemListener) {
            super(restaurantView);
            mRestaurantName = restaurantView.findViewById(R.id.restaurantName);
            mRestaurantAdresse = restaurantView.findViewById(R.id.restaurantAdresse);
            mRestaurantOpenHour = restaurantView.findViewById(R.id.openHour);
            mRestaurantDistance = restaurantView.findViewById(R.id.distance);
            mNbOfWorkmate = restaurantView.findViewById(R.id.nbOfWorkmates);
            mIcWorkmate = restaurantView.findViewById(R.id.ic_workmate);
            mRestaurantPhoto = restaurantView.findViewById(R.id.restaurantPhoto);
            mStar01 = restaurantView.findViewById(R.id.star01);
            mStar02 = restaurantView.findViewById(R.id.star02);
            mStar03 = restaurantView.findViewById(R.id.star03);
            this.mItemListener = postItemListener;
            restaurantView.setOnClickListener(this);
            //CREATE VIEW HOLDER AND INFLATING ITS XML LAYOUT
        }


        @Override
        public void onClick(View view) {
            String restaurantId = getResults(getAdapterPosition());
            this.mItemListener.onPostClick(restaurantId);
            notifyDataSetChanged();
        }
    }


    @NonNull
    @Override
    public ListViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View postView = inflater.inflate(R.layout.restaurant_item, parent, false);
        return new ViewHolder(postView, this.mItemListener);
    }

    @Override
    public void onBindViewHolder(@NonNull final ListViewAdapter.ViewHolder holder, int position) {
        final String restaurantId = mItems.getId((int)mList.get(position));
        final int restaurantDistance = mItems.getDistance((int)mList.get(position));
        final int restaurantRate = mItems.getRate((int)mList.get(position));
        Log.d("ressource","restaurantId "+restaurantId);
        Log.d("ressource","restaurantDistance "+restaurantDistance);
        Log.d("ressource","restaurantRate "+restaurantRate);
        //String detailId = item.getPlaceId();
        //final Location restaurantLocation = new Location("restaurant");

        DisposableObserver<GooglePlaceidPojo> mPlaceidDisposable = PlaceidStream.streamFetchsDetailRestaurants(restaurantId, "name,formatted_address,photo,formatted_phone_number,website,opening_hours,id", "restaurant", mKey).subscribeWith(new DisposableObserver<GooglePlaceidPojo>() {

            @Override
            public void onNext(GooglePlaceidPojo placeidPojo) {
                try {
                    Photoid photoId = placeidPojo.getResult().getPhotos().get(0);
                    mPhotoReference = photoId.getPhotoReference();
                    mOpeningHoursid = placeidPojo.getResult().getOpeningHours();
                } catch (Throwable e) {
                    e.printStackTrace();
                }


                CalculateOpeningHours calcOpeningHours = new CalculateOpeningHours(mOpeningHoursid);
                holder.mRestaurantOpenHour.setText(calcOpeningHours.getDate());
                holder.mRestaurantDistance.setText(String.format("%sm", restaurantDistance));
                try {
                    String photoheight = "70";
                    String photowidth = "70";
                    String photoUrl = "https://maps.googleapis.com/maps/api/place/photo?photoreference=" + mPhotoReference + "&sensor=false&maxheight=" + photoheight + "&maxwidth=" + photowidth + "&key=" + mKey;
                    mGlide.load(photoUrl)
                            .apply(RequestOptions.centerCropTransform())
                            .into(holder.mRestaurantPhoto);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                switch (restaurantRate) {
                    case 0: {
                        holder.mStar01.setImageResource(R.drawable.ic_star_line);
                        holder.mStar02.setVisibility(View.INVISIBLE);
                        holder.mStar03.setVisibility(View.INVISIBLE);
                    }
                    break;
                    case 1: {
                        holder.mStar01.setImageResource(R.drawable.ic_star_yellow);
                        holder.mStar02.setVisibility(View.INVISIBLE);
                        holder.mStar03.setVisibility(View.INVISIBLE);
                    }
                    break;
                    case 2: {
                        holder.mStar01.setImageResource(R.drawable.ic_star_yellow);
                        holder.mStar02.setVisibility(View.VISIBLE);
                        holder.mStar03.setVisibility(View.INVISIBLE);
                    }
                    break;
                    case 3: {
                        holder.mStar01.setImageResource(R.drawable.ic_star_yellow);
                        holder.mStar02.setVisibility(View.VISIBLE);
                        holder.mStar03.setVisibility(View.VISIBLE);
                    }
                    break;
                }

                holder.mRestaurantName.setText(placeidPojo.getResult().getName());
                holder.mRestaurantAdresse.setText(placeidPojo.getResult().getFormattedAddress());
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }

        });


        //holder.mRestaurantName.setText(item.getName());
        //holder.mRestaurantAdresse.setText(item.getFormattedAddress());
        //holder.mRestaurantOpenHour.setText(castOpeningHours[0].getDate());
    }


    @Override
    public int getItemCount() {
        return mList.size();
    }

    public void updateAnswers(RestaurantList restaurantList) {
        mItems = restaurantList;
        //mItems = items;
        notifyDataSetChanged();
    }

    private String getResults(int adapterPosition) {
        return (String) mItems.getId(adapterPosition);
    }

    public interface PostItemListener {
        void onPostClick(String url);
    }

    private void starDisplay(int numberOfStar, int numberOfRating) {

    }
}


