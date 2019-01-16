package com.eric_b.go4lunch.controller.activity;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.eric_b.go4lunch.LogInActivity;
import com.eric_b.go4lunch.R;
import com.eric_b.go4lunch.Utils.NotifBroadcastReceiver;
import com.eric_b.go4lunch.Utils.SPAdapter;
import com.eric_b.go4lunch.api.CompagnyHelper;
import com.eric_b.go4lunch.controller.fragment.ListViewFragment;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;


public class SettingActivity extends AppCompatActivity {


    @BindView(R.id.setting_activity_toolbar)
    Toolbar mToolbar;
    @BindView(R.id.radiogroup_sorting)
    RadioGroup mRadioGroup;
    @BindView(R.id.radioButton_distance)
    RadioButton mRadioButtonDistance;
    @BindView(R.id.radioButton_rating)
    RadioButton mRadioButtonRate;
    @BindView(R.id.notification_switch)
    Switch mNotificationSwitch;
    @BindView(R.id.delete_button)
    Button mDeleteUserButton;
    @BindView(R.id.about_textview)
    TextView mAboutTextView;

    private static final String SORT_BY_DISTANCE = "by_Distance";
    private static final String SORT_BY_RATE = "by_Star";
    private ActionBar mActionBar;
    private SPAdapter mSpAdapter;
    private String mSorting;
    private String mRestName;
    private String mRestAdress;
    private String mRestId;
    private Boolean mNotification;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        ButterKnife.bind(this);
        mSpAdapter = new SPAdapter(this);
        mSorting = mSpAdapter.getSorting();
        mRestId = mSpAdapter.getRestaurantReserved();
        mRestAdress = mSpAdapter.getRestaurantAdresseReserved();
        mRestName = mSpAdapter.getRestaurantNameReserved();
        mNotification = mSpAdapter.getNotification();
        configureToolBar();
        configureSorting();
        deleteUserAcountListener();
        notificationListener();
        aboutListner();
    }

    // Configure Toolbar
    private void configureToolBar() {
        setSupportActionBar(mToolbar);
        mActionBar = getSupportActionBar();
        //mActionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        assert mActionBar != null;
        mActionBar.setTitle(R.string.settings_title);
        //mActionBar.setDisplayShowHomeEnabled(true);
        mActionBar.setDisplayHomeAsUpEnabled(true);
    }

    private void configureSorting(){
        if(mSorting.equals(SORT_BY_DISTANCE)){
            mRadioButtonDistance.setChecked(true);
            mRadioButtonRate.setChecked(false);
        }
        if(mSorting.equals(SORT_BY_RATE)){
            mRadioButtonDistance.setChecked(false);
            mRadioButtonRate.setChecked(true);
        }
        // When radio group "Sort" checked change.
        mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                int checkedRadioId = group.getCheckedRadioButtonId();
                if(checkedRadioId== R.id.radioButton_distance) {
                    mSpAdapter.setSorting(SORT_BY_DISTANCE);
                } else if(checkedRadioId== R.id.radioButton_rating ) {
                    mSpAdapter.setSorting(SORT_BY_RATE);
                }
                Intent intent = new Intent();
                setResult(RESULT_OK, intent);
            }
        });
    }
    
    private void deleteUserAcountListener(){
        mDeleteUserButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteDialog();
            }
        });
    }

    private void deleteDialog() {
        Dialog deleteDialog = new Dialog(this);
        final Dialog finalDeleteDialog = deleteDialog;
        finalDeleteDialog.setContentView(R.layout.delete_dialog_box);
        Button dialValid = finalDeleteDialog.findViewById(R.id.dial_button_ok);
        Button dialCancel = finalDeleteDialog.findViewById(R.id.dial_button_cancel);
        dialCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finalDeleteDialog.dismiss();
            }
        });
        dialValid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String uid = mSpAdapter.getUserId();
                CompagnyHelper.deleteWorkmate(uid);
                FirebaseAuth.getInstance().getCurrentUser().delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Intent intent = new Intent(SettingActivity.this, LogInActivity.class);
                        startActivity(intent);
                    }
                });
            }
        });
        finalDeleteDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        finalDeleteDialog.show();
    }

    private void aboutListner(){
        Dialog aboutDialog = new Dialog(this);
        final Dialog finalAboutDialog = aboutDialog;
        finalAboutDialog.setContentView(R.layout.about_dialog_box);
        mAboutTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button dialOk = finalAboutDialog.findViewById(R.id.about_button_ok);
                dialOk.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        finalAboutDialog.dismiss();
                    }
                });
                finalAboutDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                finalAboutDialog.show();
            }
        });

    }

    private void notificationListener(){
        if(mNotification) mNotificationSwitch.setChecked(true);
        else mNotificationSwitch.setChecked(false);


        mNotificationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                Intent alarmIntent = new Intent(SettingActivity.this, NotifBroadcastReceiver.class);
                alarmIntent.putExtra("restName",mSpAdapter.getRestaurantNameReserved());
                alarmIntent.putExtra("restAdress",mSpAdapter.getRestaurantAdresseReserved());
                alarmIntent.putExtra("restId",mSpAdapter.getRestaurantReserved());

                PendingIntent pendingIntent = PendingIntent.getBroadcast(SettingActivity.this, 234, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                Calendar alarmDate = Calendar.getInstance();
                alarmDate.setTimeInMillis(System.currentTimeMillis());
                alarmDate.set(Calendar.HOUR_OF_DAY, 12);
                alarmDate.set(Calendar.MINUTE, 0);
                alarmDate.set(Calendar.SECOND, 0);
                // cancel already scheduled reminders
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy MMM dd HH:mm:ss");

                if (mNotificationSwitch.isChecked()){
                    mSpAdapter.setNotification(true);
                    // Enable a receiver

                    Log.d("ressource","hour "+sdf.format(alarmDate.getTime()));
                    assert alarmManager != null;
                    alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, alarmDate.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
                    Toast.makeText(getApplicationContext(), "Notification OK", Toast.LENGTH_LONG).show();
                }

                if (!mNotificationSwitch.isChecked()){
                    mSpAdapter.setNotification(false);
                    if (alarmManager != null) alarmManager.cancel(pendingIntent);
                    Toast.makeText(getApplicationContext(), "Notification dismiss", Toast.LENGTH_LONG).show();
                }
            }

        });

    }

}
