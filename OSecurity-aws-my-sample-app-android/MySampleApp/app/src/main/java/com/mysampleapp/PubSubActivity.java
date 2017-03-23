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
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSBasicCognitoIdentityProvider;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobile.AWSMobileClient;
import com.amazonaws.mobile.content.ContentManager;
import com.amazonaws.mobile.user.IdentityManager;
import com.amazonaws.mobile.user.IdentityProvider;
import com.amazonaws.mobile.user.signin.CognitoUserPoolsSignInProvider;
import com.amazonaws.mobileconnectors.cognito.CognitoSyncManager;
import com.amazonaws.mobileconnectors.cognito.Dataset;
import com.amazonaws.mobileconnectors.cognito.DefaultSyncCallback;
import com.amazonaws.mobileconnectors.cognito.Record;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoDevice;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ChallengeContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.tokens.CognitoAccessToken;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.tokens.CognitoIdToken;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.tokens.CognitoRefreshToken;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.tokens.CognitoUserToken;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;

import com.amazonaws.services.cognitoidentityprovider.model.AuthenticationResultType;
import com.mysampleapp.demo.DemoConfiguration;
import com.mysampleapp.demo.UserSettings;
import com.mysampleapp.navigation.NavigationDrawer;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.datatype.Duration;

public class PubSubActivity extends AppCompatActivity {

    static final String LOG_TAG = PubSubActivity.class.getCanonicalName();

    // --- Constants to modify per your configuration ---


    private CognitoIdToken idToken;

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

    // Customer specific IoT endpoint
    // AWS Iot CLI describe-endpoint call returns: XXXXXXXXXX.iot.<region>.amazonaws.com,
    private static final String CUSTOMER_SPECIFIC_ENDPOINT = "a3enni6esrlrke.iot.eu-west-1.amazonaws.com";
    // Cognito pool ID. For this app, pool needs to be unauthenticated pool with
    // AWS IoT permissions.
    private static final String COGNITO_POOL_ID = "eu-west-1:b3aade10-c009-40ed-94f7-422c4134f298";

    // Region of AWS IoT
    private static final Regions MY_REGION = Regions.EU_WEST_1;


    EditText txtSubcribe;
    EditText txtTopic;
    EditText txtMessage;
    private Button mqttButton;
    TextView tvLastMessage;
    TextView tvClientId;
    TextView tvStatus;

    Button btnConnect;
    Button btnSubscribe;
    Button btnPublish;
    Button btnDisconnect;

    AWSIotMqttManager mqttManager;
    String clientId;

    AWSCredentials awsCredentials;
    CognitoCachingCredentialsProvider credentialsProvider;

    boolean isArmed = false;

    String msg;


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

        for (DemoConfiguration.DemoFeature demoFeature : DemoConfiguration.getDemoFeatureList()) {
            navigationDrawer.addDemoFeatureToMenu(demoFeature);
        }

        if (savedInstanceState == null) {
            // Add the home fragment to be displayed initially.
            navigationDrawer.showHome();
        }
    }

    private final BroadcastReceiver settingsChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(LOG_TAG, "Received settings changed local broadcast. Update theme colors.");
            updateColor();
        }
    };

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
         btnConnect.setOnClickListener(connectClick);
         btnConnect.setEnabled(false);

         btnSubscribe = (Button) findViewById(R.id.btnSubscribe);
         btnSubscribe.setOnClickListener(subscribeClick);

         btnPublish = (Button) findViewById(R.id.btnPublish);
         btnPublish.setOnClickListener(publishClick);

         btnDisconnect = (Button) findViewById(R.id.btnDisconnect);
         btnDisconnect.setOnClickListener(disconnectClick);
         */

        mqttButton = (Button) findViewById(R.id.mqttButton);
        mqttButton.setOnClickListener(connectClick);
        mqttButton.setEnabled(false);
        mqttButton.setText("Arm");

        // MQTT client IDs are required to be unique per AWS IoT account.
        // This UUID is "practically unique" but does not _guarantee_
        // uniqueness.
        clientId = UUID.randomUUID().toString();
        // tvClientId.setText(clientId);

        // Initialize the AWS Cognito credentials provider


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

        // MQTT Client
        mqttManager = new AWSIotMqttManager(clientId, CUSTOMER_SPECIFIC_ENDPOINT);


        // The following block uses IAM user credentials for authentication with AWS IoT.
        //awsCredentials = new BasicAWSCredentials("ACCESS_KEY_CHANGE_ME", "SECRET_KEY_CHANGE_ME");
        //btnConnect.setEnabled(true);

        // The following block uses a Cognito credentials provider for authentication with AWS IoT.
        new Thread(new Runnable() {
            @Override
            public void run() {

                awsCredentials = credentialsProvider.getCredentials();


                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mqttButton.setEnabled(true);
                    }
                });
            }
        }).start();

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


    View.OnClickListener connectClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            Log.d(LOG_TAG, "clientId = " + clientId);

            //ClientConfiguration clientConfiguration = new ClientConfiguration();
            //IdentityManager identityManager = new IdentityManager(getApplicationContext(), clientConfiguration);


            try {
                mqttManager.connect(credentialsProvider, new AWSIotMqttClientStatusCallback() {
                    @Override
                    public void onStatusChanged(final AWSIotMqttClientStatus status,
                                                final Throwable throwable) {
                        Log.d(LOG_TAG, "Status = " + String.valueOf(status));

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (status == AWSIotMqttClientStatus.Connecting) {
                                    //tvStatus.setText("Connecting...");

                                } else if (status == AWSIotMqttClientStatus.Connected) {
                                    mqttButton.setClickable(false);
                                    publish();
                                    subscribe();

                                } else if (status == AWSIotMqttClientStatus.Reconnecting) {
                                    if (throwable != null) {
                                        Log.e(LOG_TAG, "Connection error.", throwable);
                                    }
                                    //tvStatus.setText("Reconnecting");
                                } else if (status == AWSIotMqttClientStatus.ConnectionLost) {
                                    if (throwable != null) {
                                        Log.e(LOG_TAG, "Connection error.", throwable);
                                        throwable.printStackTrace();
                                    }
                                    //tvStatus.setText("Disconnected");
                                } else {
                                    //tvStatus.setText("Disconnected");

                                }
                            }
                        });
                    }
                });
            } catch (final Exception e) {
                Log.e(LOG_TAG, "Connection error.", e);
                // tvStatus.setText("Error! " + e.getMessage());
            }
        }


        public void subscribe(){
                final String topic = "/osecurity/fromterminal";

                Log.d(LOG_TAG, "topic = " + topic);

                try {
                    mqttManager.subscribeToTopic(topic, AWSIotMqttQos.QOS0,
                            new AWSIotMqttNewMessageCallback() {
                                @Override
                                public void onMessageArrived(final String topic, final byte[] data) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                String message = new String(data);


                                                Log.d(LOG_TAG, "Message arrived:");
                                                Log.d(LOG_TAG, "   Topic: " + topic);
                                                Log.d(LOG_TAG, " Message: " + message);

                                                if (message.equals("armed")) {
                                                    mqttButton.setClickable(true);
                                                    mqttButton.setText("Deaktiver");
                                                } else if (message.equals("disarmed")) {
                                                    mqttButton.setClickable(true);
                                                    mqttButton.setText("Aktiver");
                                                } else if (message.equals("Terminal er online")) {
                                                    Log.d(LOG_TAG, "Terminal er online, funker");
                                                    Toast toast = Toast.makeText(getApplicationContext(), "Terminal er online!", Toast.LENGTH_SHORT);
                                                    toast.show();
                                                } else {
                                                    Log.d(LOG_TAG, "Unsupported input from Pi");
                                                }

                                            } catch (NullPointerException e) {
                                                Log.e(LOG_TAG, "NullPointerException", e);
                                            }
                                        }
                                    });
                                }
                            });
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Subscription error.", e);
                }

        };


        public boolean getArmStatus() {
            return isArmed;
        }

        public void switchArmStatus() {
            if (isArmed = true) {
                isArmed = false;
            } else if (isArmed = false) {
                isArmed = true;
            }
        }

        public void publish() {

            String fromApp = "/osecurity/fromapp";
            if (isArmed) {
                msg = "n";
                isArmed = false;
            } else if (!isArmed) {
                msg = "y";
                isArmed = true;
            }

            try {
                mqttManager.publishString(msg, fromApp, AWSIotMqttQos.QOS0);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Publish error.", e);
            }


        }

        ;

        View.OnClickListener disconnectClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    mqttManager.disconnect();
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Disconnect error.", e);
                }

            }
        };
    };
}
