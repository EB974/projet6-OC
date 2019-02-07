package com.eric_b.go4lunch.utils;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;


public class NotifBroadcastReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("ressource","broadcast");
        Intent serviceIntent = new Intent(context, NotificationService.class);
        serviceIntent.putExtra("restName",intent.getStringExtra("restName"));
        serviceIntent.putExtra("restAdress",intent.getStringExtra("restAdress"));
        serviceIntent.putExtra("restId",intent.getStringExtra("restId"));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent); // for build on Oreo and sup
        } else {
            context.startService(serviceIntent);
        }

    }

}
