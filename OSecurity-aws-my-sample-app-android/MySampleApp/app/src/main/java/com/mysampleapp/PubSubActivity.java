/**
 * Copyright 2010-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 * <p>
 * http://aws.amazon.com/apache2.0
 * <p>
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */


package com.mysampleapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;


import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobile.AWSMobileClient;
import com.amazonaws.mobile.user.IdentityManager;
import com.amazonaws.mobileconnectors.cognito.Dataset;
import com.amazonaws.mobileconnectors.cognito.DefaultSyncCallback;
import com.amazonaws.mobileconnectors.cognito.Record;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.tokens.CognitoAccessToken;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.tokens.CognitoIdToken;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.tokens.CognitoRefreshToken;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;

import com.amazonaws.services.cognitoidentityprovider.model.AuthenticationResultType;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.mysampleapp.demo.DemoConfiguration;
import com.mysampleapp.demo.HomeDemoFragment;
import com.mysampleapp.demo.UserSettings;
import com.mysampleapp.navigation.NavigationDrawer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;


public class PubSubActivity extends AppCompatActivity {

    static final String LOG_TAG = PubSubActivity.class.getCanonicalName();

    // --- Constants to modify per your configuration ---


    private CognitoIdToken idToken;

    private String firebaseToken;
    /**
     * Cognito access token.
     */
    private CognitoAccessToken accessToken;

    /**
     * Cognito refresh token.
     */
    private CognitoRefreshToken refreshToken;

    /**
     * Bundle key for saving/restoring the toolbar title.
     */
    private static final String BUNDLE_KEY_TOOLBAR_TITLE = "title";

    /**
     * The identity manager used to keep track of the current user account.
     */
    private IdentityManager identityManager;

    /**
     * The toolbar view control.
     */
    private Toolbar toolbar;


    // Customer specific IoT endpoint
    // AWS Iot CLI describe-endpoint call returns: XXXXXXXXXX.iot.<region>.amazonaws.com,
    private static final String CUSTOMER_SPECIFIC_ENDPOINT = "a3enni6esrlrke.iot.eu-west-1.amazonaws.com";
    // Cognito pool ID. For this app, pool needs to be unauthenticated pool with
    // AWS IoT permissions.
    private static final String COGNITO_POOL_ID = "eu-west-1:b3aade10-c009-40ed-94f7-422c4134f298";

    // Region of AWS IoT
    private static final Regions MY_REGION = Regions.EU_WEST_1;

    private AmazonS3Client s3;

    /**
     * Our navigation drawer class for handling navigation drawer logic.
     */
    private NavigationDrawer navigationDrawer;

    /**
     * The helper class used to toggle the left navigation drawer open and closed.
     */
    private ActionBarDrawerToggle drawerToggle;

    /**
     * Data to be passed between fragments.
     */
    private Bundle fragmentBundle;

    private Button signOutButton;


    AWSCredentials awsCredentials;
    CognitoCachingCredentialsProvider credentialsProvider;

    private VideoView vidView;

    private MediaController vidControl;




    /**
     * Initializes the Toolbar for use with the activity.
     */
    private void setupToolbar(final Bundle savedInstanceState) {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        // Set up the activity to use this toolbar. As a side effect this sets the Toolbar's title
        // to the activity's title.
        setSupportActionBar(toolbar);

        if (savedInstanceState != null) {
            // Some IDEs such as Android Studio complain about possible NPE without this check.
            assert getSupportActionBar() != null;

            // Restore the Toolbar's title.
            getSupportActionBar().setTitle(
                    savedInstanceState.getCharSequence(BUNDLE_KEY_TOOLBAR_TITLE));
        }
    }

    /**
     * Initializes the sign-in and sign-out buttons.
     */
    private void setupSignInButtons() {

        signOutButton = (Button) findViewById(R.id.button_signout);
        //signOutButton.setOnClickListener(this);

    }




    /**
     * Initializes the navigation drawer menu to allow toggling via the toolbar or swipe from the
     * side of the screen.
     */
    private void setupNavigationMenu(final Bundle savedInstanceState) {
        final DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        final ListView drawerItems = (ListView) findViewById(R.id.nav_drawer_items);

        // Create the navigation drawer.
        navigationDrawer = new NavigationDrawer(this, toolbar, drawerLayout, drawerItems,
                R.id.main_fragment_container);

        // Add navigation drawer menu items.
        // Home isn't a demo, but is fake as a demo.
        DemoConfiguration.DemoFeature home = new DemoConfiguration.DemoFeature();
        home.iconResId = R.mipmap.icon_home;
        home.titleResId = R.string.main_nav_menu_item_home;
        navigationDrawer.addDemoFeatureToMenu(home);

        DemoConfiguration.DemoFeature archive = new DemoConfiguration.DemoFeature();
        archive.iconResId = R.mipmap.user_files;
        archive.titleResId = R.string.main_nav_menu_item_archive;
        navigationDrawer.addDemoFeatureToMenu(archive);

        for (DemoConfiguration.DemoFeature demoFeature : DemoConfiguration.getDemoFeatureList()) {
            navigationDrawer.addDemoFeatureToMenu(demoFeature);
        }

        if (savedInstanceState == null) {
            // Add the home fragment to be displayed initially.
            navigationDrawer.showHome();
        }
    }



    public void updateColor() {
        final UserSettings userSettings = UserSettings.getInstance(getApplicationContext());
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(final Void... params) {
                userSettings.loadFromDataset();
                return null;
            }

            @Override
            protected void onPostExecute(final Void aVoid) {
                toolbar.setTitleTextColor(userSettings.getTitleTextColor());
                toolbar.setBackgroundColor(userSettings.getTitleBarColor());
                final Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.main_fragment_container);
                if (fragment != null) {
                    final View fragmentView = fragment.getView();
                    if (fragmentView != null) {
                        fragmentView.setBackgroundColor(userSettings.getBackgroudColor());
                    }
                }
            }
        }.execute();
    }

    private void syncUserSettings() {
        // sync only if user is signed in
        if (AWSMobileClient.defaultMobileClient().getIdentityManager().isUserSignedIn()) {
            final UserSettings userSettings = UserSettings.getInstance(getApplicationContext());
            userSettings.getDataset().synchronize(new DefaultSyncCallback() {
                @Override
                public void onSuccess(final Dataset dataset, final List<Record> updatedRecords) {
                    super.onSuccess(dataset, updatedRecords);
                    Log.d(LOG_TAG, "successfully synced user settings");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateColor();
                        }
                    });
                }
            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);





        // Obtain a reference to the mobile client. It is created in the Application class,
        // but in case a custom Application class is not used, we initialize it here if necessary.
        AWSMobileClient.initializeMobileClientIfNecessary(this);

        // Obtain a reference to the mobile client. It is created in the Application class.
        final AWSMobileClient awsMobileClient = AWSMobileClient.defaultMobileClient();

        // Obtain a reference to the identity manager.
        //identityManager = awsMobileClient.getIdentityManager();

        setupToolbar(savedInstanceState);

        setupNavigationMenu(savedInstanceState);

        //LayoutInflater inflater = this.getLayoutInflater();
        //ViewGroup viewGroup = (ViewGroup) findViewById(android.R.id.content);
        //View homeView = inflater.inflate(R.layout.fragment_demo_home , viewGroup);


        //snapshotView = (ImageView) homeView.findViewById(R.id.snapshotView);

        //snapshotView.setImageResource(android.R.drawable.alert_dark_frame);


        /**
         * Firebase subscription logic, started on runtime
         *
         */

        FirebaseMessaging.getInstance().subscribeToTopic("varsler");
        String msg = "Subscribed to topic varsler";
        Log.d(LOG_TAG, msg);

        //firebaseToken = FirebaseInstanceId.getInstance().getToken();
        //Log.d(LOG_TAG, firebaseToken);






        if (!AWSMobileClient.defaultMobileClient().getIdentityManager().isUserSignedIn()) {
            // In the case that the activity is restarted by the OS after the application
            // is killed we must redirect to the splash activity to handle the sign-in flow.
            Intent intent = new Intent(this, SplashActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return;
        }

        setupSignInButtons();
        // register settings changed receiver.
        LocalBroadcastManager.getInstance(this).registerReceiver(settingsChangedReceiver,
                new IntentFilter(UserSettings.ACTION_SETTINGS_CHANGED));
        updateColor();
        syncUserSettings();

        /**
         *

         txtSubcribe = (EditText) findViewById(R.id.txtSubcribe);
         txtTopic = (EditText) findViewById(R.id.txtTopic);
         txtMessage = (EditText) findViewById(R.id.txtMessage);

         tvLastMessage = (TextView) findViewById(R.id.tvLastMessage);
         tvClientId = (TextView) findViewById(R.id.tvClientId);
         tvStatus = (TextView) findViewById(R.id.tvStatus);

         btnConnect = (Button) findViewById(R.id.btnConnect);
         btnConnect.setOnClickListener(handleS3);
         btnConnect.setEnabled(false);

         btnSubscribe = (Button) findViewById(R.id.btnSubscribe);
         btnSubscribe.setOnClickListener(subscribeClick);

         btnPublish = (Button) findViewById(R.id.btnPublish);
         btnPublish.setOnClickListener(publishClick);

         btnDisconnect = (Button) findViewById(R.id.btnDisconnect);
         btnDisconnect.setOnClickListener(disconnectClick);
         */




        // MQTT client IDs are required to be unique per AWS IoT account.
        // This UUID is "practically unique" but does not _guarantee_
        // uniqueness.
        // tvClientId.setText(clientId);


        //TODO Fix NotAuthorizedException by using "setLogins" earlier
        AuthenticationResultType authenticationResultType = new AuthenticationResultType();
        String idToken = authenticationResultType.getIdToken();
        //String idToken = cognitoUserSession.getIdToken().getJWTToken();

        // Initialize the AWS Cognito credentials provider
        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                COGNITO_POOL_ID,
                MY_REGION
        );

        Map<String, String> logins = new HashMap<String, String>();
        logins.put("eu-west-1_2F3hyifQN", idToken);
        credentialsProvider.setLogins(logins);

        Region region = Region.getRegion(MY_REGION);




        //s3 = new AmazonS3Client(credentialsProvider);


        // The following block uses IAM user credentials for authentication with AWS IoT.
        //awsCredentials = new BasicAWSCredentials("ACCESS_KEY_CHANGE_ME", "SECRET_KEY_CHANGE_ME");
        //btnConnect.setEnabled(true);


        // The following block uses a Cognito credentials provider for authentication with AWS IoT.
        new Thread(new Runnable() {
            @Override
            public void run() {

                //@awsCredentials = credentialsProvider.getCredentials();


                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                    }
                });
            }
        }).start();


        //handleS3();

    }

    private static final class DemoListAdapter extends ArrayAdapter<DemoConfiguration.DemoFeature> {
        private LayoutInflater inflater;

        public DemoListAdapter(final Context context) {
            super(context, R.layout.list_item_icon_text_with_subtitle);
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(final int position, final View convertView, final ViewGroup parent) {
            View view;
            HomeDemoFragment.ViewHolder holder;
            if (convertView == null) {
                view = inflater.inflate(R.layout.list_item_icon_text_with_subtitle, parent, false);
                holder = new HomeDemoFragment.ViewHolder();
                holder.iconImageView = (ImageView) view.findViewById(R.id.list_item_icon);
                holder.titleTextView = (TextView) view.findViewById(R.id.list_item_title);
                holder.subtitleTextView = (TextView) view.findViewById(R.id.list_item_subtitle);
                view.setTag(holder);
            } else {
                view = convertView;
                holder = (HomeDemoFragment.ViewHolder) convertView.getTag();
            }

            DemoConfiguration.DemoFeature item = getItem(position);
            holder.iconImageView.setImageResource(item.iconResId);
            holder.titleTextView.setText(item.titleResId);
            holder.subtitleTextView.setText(item.subtitleResId);

            return view;
        }
    }






    /**
     private CognitoCachingCredentialsProvider getCredentialsProvider() {
     return credentialsProvider;
     }
     */
    /**
     private AWSCredentialsProvider initIdentity() {
     return IdentityManager.getCredentialsProvider();
     }
     */




    /**
     * Stores data to be passed between fragments.
     * @param fragmentBundle fragment data
     */
    public void setFragmentBundle(final Bundle fragmentBundle) {
        this.fragmentBundle = fragmentBundle;
    }

    /**
     * Gets data to be passed between fragments.
     * @return fragmentBundle fragment data
     */
    public Bundle getFragmentBundle() {
        return this.fragmentBundle;
    }

    public boolean onOptionsItemSelected(final MenuItem item) {
        // Handle action bar item clicks here excluding the home button.

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(final Bundle bundle) {
        super.onSaveInstanceState(bundle);
        // Save the title so it will be restored properly to match the view loaded when rotation
        // was changed or in case the activity was destroyed.
        if (toolbar != null) {
            bundle.putCharSequence(BUNDLE_KEY_TOOLBAR_TITLE, toolbar.getTitle());
        }
    }

    //@Override
    public void onClick(final View view) {
        if (view == signOutButton) {
            // The user is currently signed in with a provider. Sign out of that provider.
            identityManager.signOut();
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        }

        /**if (view == mqttButton) {
         // The user is to send a payload over MQTT
         // metodenavn(parametersomskalsendes, adresse)

         }*/
        // ... add any other button handling code here ...

    }

    private final BroadcastReceiver settingsChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(LOG_TAG, "Received settings changed local broadcast. Update theme colors.");
            updateColor();
        }
    };

    @Override
    protected void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(settingsChangedReceiver);
    }

    @Override
    public void onBackPressed() {
        final FragmentManager fragmentManager = this.getSupportFragmentManager();

        if (navigationDrawer.isDrawerOpen()) {
            navigationDrawer.closeDrawer();
            return;
        }

        if (fragmentManager.getBackStackEntryCount() == 0) {
            if (fragmentManager.findFragmentByTag(HomeDemoFragment.class.getSimpleName()) == null) {
                final Class fragmentClass = HomeDemoFragment.class;
                // if we aren't on the home fragment, navigate home.
                final Fragment fragment = Fragment.instantiate(this, fragmentClass.getName());

                fragmentManager
                        .beginTransaction()
                        .replace(R.id.main_fragment_container, fragment, fragmentClass.getSimpleName())
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .commit();

                // Set the title for the fragment.
                final ActionBar actionBar = this.getSupportActionBar();
                if (actionBar != null) {
                    actionBar.setTitle(
                            getString(R.string.app_name));
                }
                return;
            }
        }
        super.onBackPressed();
    }

}
