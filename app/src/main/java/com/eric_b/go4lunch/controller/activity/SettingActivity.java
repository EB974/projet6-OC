package com.eric_b.go4lunch.controller.activity;

import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import com.eric_b.go4lunch.Auth.LogInActivity;
import com.eric_b.go4lunch.R;
import com.eric_b.go4lunch.utils.NotifBroadcastReceiver;
import com.eric_b.go4lunch.utils.SPAdapter;
import com.eric_b.go4lunch.api.CompagnyHelper;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import java.util.Calendar;
import java.util.Objects;

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
    private SPAdapter mSpAdapter;
    private String mSorting;
    private Boolean mNotification;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        ButterKnife.bind(this);
        mSpAdapter = new SPAdapter(this);
        mSorting = mSpAdapter.getSorting();
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
        ActionBar actionBar = getSupportActionBar();
        //mActionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        assert actionBar != null;
        actionBar.setTitle(R.string.settings_title);
        //mActionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
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
        final Dialog finalDeleteDialog = new Dialog(this);
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
                Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Intent intent = new Intent(SettingActivity.this, LogInActivity.class);
                        startActivity(intent);
                    }
                });
            }
        });
        Objects.requireNonNull(finalDeleteDialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        finalDeleteDialog.show();
    }

    private void aboutListner(){
        final Dialog finalAboutDialog = new Dialog(this);
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
                Objects.requireNonNull(finalAboutDialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
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



                if (mNotificationSwitch.isChecked()){
                    mSpAdapter.setNotification(true);


                    assert alarmManager != null;
                    alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, alarmDate.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.Notification_OK), Toast.LENGTH_LONG).show();
                }

                if (!mNotificationSwitch.isChecked()){
                    mSpAdapter.setNotification(false);
                    if (alarmManager != null) alarmManager.cancel(pendingIntent);
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.Notification_dismiss), Toast.LENGTH_LONG).show();
                }
            }

        });

    }

}
