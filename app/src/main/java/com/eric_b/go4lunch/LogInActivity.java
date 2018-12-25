package com.eric_b.go4lunch;

import android.content.Intent;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.eric_b.go4lunch.api.CompagnyHelper;
import com.eric_b.go4lunch.controller.activity.BaseActivity;
import com.eric_b.go4lunch.controller.activity.LunchActivity;
import com.firebase.ui.auth.AuthUI;
import java.util.Collections;
import java.util.Objects;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import butterknife.BindView;
import butterknife.ButterKnife;


public class LogInActivity extends BaseActivity {

    @BindView(R.id.facebook_button_login) RelativeLayout mFacebookLogin;
    @BindView(R.id.google_button_login) RelativeLayout mGoogleLogin;
    @BindView(R.id.activity_log_in_coordinator_layout) CoordinatorLayout mCoordinatorLayout;


    //FOR DATA
    private static final int RC_SIGN_IN = 100;


    @Override
    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);
        ButterKnife.bind(this);
        if (getCurrentUser()!= null){
            startLunchActivity();
        } else {
            facebookSignIn();
            googleSignIn();
        }
    }


    private void facebookSignIn() {
        mFacebookLogin.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                    signIn("Facebook");

            }
        });
    }

    private void googleSignIn() {
        mGoogleLogin.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                signIn("Google");
            }
        });
    }

    @Nullable
    protected FirebaseUser getCurrentUser(){ return FirebaseAuth.getInstance().getCurrentUser();}

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Handle SignIn Activity response on activity result
        this.handleResponseAfterSignIn(requestCode, resultCode, data);
    }

    private void signIn(String provider) {
        AuthUI.IdpConfig providerIdp = null;
        switch (provider){
            case "Google":{
                 providerIdp = new AuthUI.IdpConfig.GoogleBuilder()
                    .build();
            }
            break;
            case "Facebook": {
                 providerIdp = new AuthUI.IdpConfig.FacebookBuilder()
                    .setPermissions(Collections.singletonList("user_friends"))
                    .build();
            }
            break;
        }
        // Create and launch sign-in intent
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setTheme(R.style.LoginTheme)
                        .setAvailableProviders(Collections.singletonList(providerIdp))
                        .build(),
                RC_SIGN_IN);
    }


    // Show Snack Bar with a message

    private void showSnackBar(CoordinatorLayout coordinatorLayout, String message){
        // Create the Snackbar

        LayoutInflater inflater = getLayoutInflater();
        Snackbar snackbar = Snackbar.make(coordinatorLayout, message, Snackbar.LENGTH_LONG);
        Snackbar.SnackbarLayout layout = (Snackbar.SnackbarLayout) snackbar.getView();
        // get snackbar view
        View viewSnackbar = snackbar.getView();
        // get textview inside snackbar view
        TextView textSnackbar = viewSnackbar.findViewById(android.support.design.R.id.snackbar_text);
        // set text to center
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
            textSnackbar.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        else
            textSnackbar.setGravity(Gravity.CENTER_HORIZONTAL);
        // Show the Snackbar
        snackbar.show();
        //Snackbar.make(coordinatorLayout, message, Snackbar.LENGTH_LONG).show();
    }


    //  Method that handles response after SignIn Activity close
    private void handleResponseAfterSignIn(int requestCode, int resultCode, Intent data){
        IdpResponse response = IdpResponse.fromResultIntent(data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK){
                this.createUserInFirestore();
                startLunchActivity();
            }
            if (resultCode != RESULT_OK) {
                assert response != null;
                Log.d("SignIn","response "+Objects.requireNonNull(response.getError()).getErrorCode());
                // If errors
                if (response == null) {
                    showSnackBar(this.mCoordinatorLayout, getString(R.string.error_authentication_canceled));
                } else if (response.getError().getErrorCode()== ErrorCodes.NO_NETWORK) {
                    showSnackBar(this.mCoordinatorLayout, getString(R.string.error_no_internet));
                } else if (response.getError().getErrorCode() == ErrorCodes.UNKNOWN_ERROR) {
                    showSnackBar(this.mCoordinatorLayout, getString(R.string.error_unknown_error));
                }
            }
        }
    }

    // --------------------
    // REST REQUEST
    // --------------------

    // 1 - Http request that create user in firestore
    private void createUserInFirestore(){

        if (this.getCurrentUser() != null){
            String urlPicture = (this.getCurrentUser().getPhotoUrl() != null) ? this.getCurrentUser().getPhotoUrl().toString() : null;
            String username = this.getCurrentUser().getDisplayName();
            String uid = this.getCurrentUser().getUid();
            CompagnyHelper.createWorkmate(uid, username, urlPicture,"","").addOnFailureListener(this.onFailureListener());
        }
    }


    private void startLunchActivity(){
        Intent intent = new Intent(this, LunchActivity.class);
        startActivity(intent);
    }
}
