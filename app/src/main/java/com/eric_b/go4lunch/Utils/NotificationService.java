package com.eric_b.go4lunch.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.eric_b.go4lunch.R;
import com.eric_b.go4lunch.controller.activity.RestaurantDisplay;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import java.util.HashMap;
import java.util.Map;


class NotificationService extends Service {


    private static final String CHANEL_ID = "NOTIFICATION";
    private static final int NOTIFICATION_ID = 5;
    String mRestId;
    String mRestName;
    String mRestAdress;
    String mUserName;
    String mWorkmates;
    Context mContext;

    public NotificationService(){

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("ressource","notification service");
        mContext = getApplicationContext();
        SPAdapter spAdapter = new SPAdapter(mContext);
        mUserName = spAdapter.getUserName();
        mRestName = spAdapter.getRestaurantNameReserved();
        mRestAdress = spAdapter.getRestaurantAdresseReserved();
        mRestId = spAdapter.getRestaurantReserved();
        Log.d("ressource","mRestId "+mRestId);
        if (mRestId.length() > 1) workmateFound(mRestId);
        return Service.START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void workmateFound(final String id) {
        mWorkmates = "";
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        final DocumentReference docRef = db.collection("mappedRestaurant").document(id);
        docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@javax.annotation.Nullable DocumentSnapshot documentSnapshot, @javax.annotation.Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w("ressource", "Listen failed.", e);
                    return;
                }
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    HashMap<String, HashMap<String, String>> reservedList = new HashMap<>((HashMap<String, HashMap<String, String>>) documentSnapshot.get("workmatesReservation"));
                        int i=0;
                    for(Map.Entry<String, HashMap<String,String>> entry : reservedList.entrySet()) {
                        HashMap<String,String> workmate = entry.getValue();
                        i++;
                        //send colleagues' names if their number is less than 5
                        if (i<=5) {
                            if (!workmate.get("name").equals(mUserName)){
                                if (mWorkmates.length()==0)
                                    mWorkmates = workmate.get("name");
                                else {
                                    StringBuilder builder = new StringBuilder();
                                    mWorkmates =  builder.append(mWorkmates).append( ", ").append(workmate.get("name")).toString();
                                }

                            }
                        }
                    }
                    //send the number of wormate if their are more than 5
                    if(i>5) mWorkmates += " + "+i+" "+getResources().getString(R.string.workmate);
                    notificationSend(id);
                }
            }
        });
    }

    private void notificationSend(String id) {
        NotificationManager notificationManager = (NotificationManager) mContext
                .getSystemService(NOTIFICATION_SERVICE);
        Intent notificationIntent = new Intent(mContext, RestaurantDisplay.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        notificationIntent.putExtra("placeId",id);
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        //createNotification Chanel for Build version Oreo and more
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Notification";
            String description = "Include all notification";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel notificationChannel = new NotificationChannel(CHANEL_ID, name, importance);
            notificationChannel.setDescription(description);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(notificationChannel);
        }
        //set notification before send
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, CHANEL_ID);
        Log.d("ressource","mRestName "+mRestName);
        Log.d("ressource","mWorkmates "+mWorkmates);
        builder.setSmallIcon(R.drawable.notification)
                .setSound(alarmSound)
                .setContentTitle(getResources().getString(R.string.time_lunch) + mRestName)
                .setContentIntent(pendingIntent)
                .setVibrate(new long[]{200, 1000, 500, 1000, 400})
                .setAutoCancel(true);
        if (mWorkmates.length()>0)
            builder.setSmallIcon(R.drawable.notification)
                    .setContentText(getResources().getString(R.string.with) + mWorkmates);
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(mContext);

        if (mRestId.length() > 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForeground(NOTIFICATION_ID, builder.build()); //for oreo and more
                stopForeground(false);
            }
            notificationManagerCompat.notify(NOTIFICATION_ID, builder.build()); // for other build
        }else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                /* for oreo and more
                   must startForeground() after startForegroundService()
                   even if no article was found */
                startForeground(NOTIFICATION_ID, builder.build());
                stopForeground(true); // remove blank notification
            }
        }
    }

}



