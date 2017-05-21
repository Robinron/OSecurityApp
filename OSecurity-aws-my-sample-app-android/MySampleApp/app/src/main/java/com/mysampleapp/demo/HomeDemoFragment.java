package com.mysampleapp.demo;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.MediaController;
import android.widget.VideoView;
import io.vov.vitamio.LibsChecker;
import io.vov.vitamio.MediaPlayer;

import java.io.FileOutputStream;


import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobile.AWSMobileClient;
import com.amazonaws.mobile.content.ContentManager;
import com.amazonaws.mobile.user.IdentityManager;
import com.amazonaws.mobile.user.IdentityProvider;
import com.amazonaws.mobile.user.signin.CognitoUserPoolsSignInProvider;
import com.amazonaws.mobileconnectors.cognito.Dataset;
import com.amazonaws.mobileconnectors.cognito.DefaultSyncCallback;
import com.amazonaws.mobileconnectors.cognito.Record;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cognitoidentityprovider.model.AuthenticationResultType;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.google.firebase.iid.FirebaseInstanceId;
import com.mysampleapp.PubSubActivity;
import com.mysampleapp.R;
import com.mysampleapp.SplashActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class HomeDemoFragment extends DemoFragmentBase implements View.OnClickListener {

    static final String LOG_TAG = PubSubActivity.class.getCanonicalName();
    private Button mqttButton, streamingButton;
    private ImageView snapshotView;
    private String firebaseToken;
    private String firebaseID;
    private Spinner streamingSpinner;
    private VideoView vidView;
    private MediaController vidControl;
    private WebView webView;
    private TextView snapshotTimestamp;
    private String snapshotTime = "";
    boolean streamOnline = false;

    //connectClick();
    //            Log.d(LOG_TAG, "BUTTON IS CLICKED!");



    // Customer specific IoT endpoint
    // AWS Iot CLI describe-endpoint call returns: XXXXXXXXXX.iot.<region>.amazonaws.com,
    private static final String CUSTOMER_SPECIFIC_ENDPOINT = "a3enni6esrlrke.iot.eu-west-1.amazonaws.com";
    // Cognito pool ID. For this app, pool needs to be unauthenticated pool with
    // AWS IoT permissions.
    private static final String COGNITO_POOL_ID = "eu-west-1:b3aade10-c009-40ed-94f7-422c4134f298";

    // Region of AWS IoT
    private static final Regions MY_REGION = Regions.EU_WEST_1;

    AWSIotMqttManager mqttManager;
    String clientId;
    private AmazonS3Client s3;
    private IdentityManager identityManager;
    private IdentityProvider identityProvider;
    private CognitoCachingCredentialsProvider credentialsProvider;

    AWSCredentials awsCredentials;


    boolean isArmed = false;

    private Toolbar toolbar;

    String msg;

    CognitoUserSession cognitoUserSession;





    /**
    public void updateColor() {
        final UserSettings userSettings = UserSettings.getInstance(getActivity());
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
                final Fragment fragment = getActivity().getSupportFragmentManager().findFragmentById(R.id.main_fragment_container);
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
            final UserSettings userSettings = UserSettings.getInstance(getActivity());
            userSettings.getDataset().synchronize(new DefaultSyncCallback() {
                @Override
                public void onSuccess(final Dataset dataset, final List<Record> updatedRecords) {
                    super.onSuccess(dataset, updatedRecords);
                    Log.d(LOG_TAG, "successfully synced user settings");
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateColor();
                        }
                    });
                }
            });
        }
    }
    */



    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_demo_home, container, false);

        mqttButton = (Button) view.findViewById(R.id.mqttButton);
        mqttButton.setOnClickListener(this);

        streamingButton = (Button) view.findViewById(R.id.streamingButton);
        streamingButton.setOnClickListener(this);

        streamingSpinner = (Spinner) view.findViewById(R.id.streamSpinner);
        streamingSpinner.setVisibility(View.GONE);
        snapshotTimestamp = (TextView) view.findViewById(R.id.snapshotTimestamp);
        snapshotView = (ImageView) view.findViewById(R.id.snapshotView);
        snapshotView.setImageResource(android.R.drawable.alert_dark_frame);
        clientId = UUID.randomUUID().toString();

        vidView = (VideoView) view.findViewById(R.id.vidView);
        vidControl = new MediaController(getActivity());


        //syncUserSettings();


        //CognitoUserPoolsSignInProvider provider = new CognitoUserPoolsSignInProvider(getContext());
        //cognitoUserSession = provider.getCognitoUserSession();

       // String idToken = cognitoUserSession.getIdToken().getJWTToken();


        //Initialize the AWS Cognito credentials provider


        identityManager = AWSMobileClient.defaultMobileClient()
                .getIdentityManager();

        identityProvider = identityManager.getCurrentIdentityProvider();

        //Firebase

        firebaseToken = FirebaseInstanceId.getInstance().getToken();
        firebaseID = FirebaseInstanceId.getInstance().getId();
        Log.d(LOG_TAG, firebaseToken);
        Log.d(LOG_TAG, firebaseID);



        s3 = new AmazonS3Client(identityManager.getCredentialsProvider());



        // MQTT Client
        mqttManager = new AWSIotMqttManager(clientId, CUSTOMER_SPECIFIC_ENDPOINT);



        //The following block uses a Cognito credentials provider for authentication with AWS IoT.
        new Thread(new Runnable() {
            @Override
            public void run() {

                awsCredentials = identityManager.getCredentialsProvider().getCredentials();


                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                    }
                });
            }
        }).start();


        s3Snapshot();

        //TODO Fix NotAuthorizedException by using "setLogins" earlier
       // AuthenticationResultType authenticationResultType = new AuthenticationResultType();


        /**
        credentialsProvider = new CognitoCachingCredentialsProvider(
                getActivity(),
                COGNITO_POOL_ID,
                MY_REGION
        );
         */

       // logins.put("eu-west-1_2F3hyifQN", idToken);

       // credentialsProvider.setLogins(logins);
        connectClick();




        /**
         * Forsøk på web-view, funker men åpner chrome for å vise youtube video
         * */


        //TODO: Refactor to be in only a part of the view instead of taking over everything


        /**
        webView.post(new Runnable() {
            @Override
            public void run() {
                //final String vidAddress = "https://youtu.be/P47bqscizJY";
                final String vidAddress = "https://www.google.no/";

                 //int width = webView.getWidth();
                 //int height = webView.getHeight();
                 //webView.loadUrl(vidAddress + "?width="+width+"&height="+height);
                webView.getSettings().setJavaScriptEnabled(true);
                webView.getSettings().setPluginState(WebSettings.PluginState.ON);
                snapshotView.setVisibility(View.GONE);
                streamingSpinner.setVisibility(View.VISIBLE);
                webView.loadUrl(vidAddress);
                webView.setWebChromeClient(new WebChromeClient());
                streamingSpinner.setVisibility(View.GONE);

            }
        }); */



        /**
         * Kode for å vise youtube video, viser bare at den ikke kan vise
         *
        vidView = (VideoView) view.findViewById(R.id.vidView);
        vidControl = new MediaController(getActivity());
        vidControl.setAnchorView(vidView);
        vidControl.setMediaPlayer(vidView);

        //String vidAddress = "https://www.youtube.com/watch?v=nnQDiGBFIXk";
        String rsp = "rtsp://r5---sn-5hne6n7e.googlevideo.com/Cj0LENy73wIaNAmk3cJBg-iaXhMYDSANFC149wFZMOCoAUIASARgoLaa5PT-7-1YigELeEpMU181VmVVR0UM/1A671C27615E13183B320B362AE41A1133BD5C42.D29F992A65901F27777307674F66F552D515799C/yt6/1/video.3gp";
        Uri vidUri = Uri.parse(rsp);

        vidView.setVideoURI(vidUri);

        vidView.setMediaController(vidControl);

        vidView.start();
         */

        /**
         * Working solution but opens in chrome with 31s lag
         *
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.ustream.tv/channel/yyK6Mm9gxRS"));
        startActivity(intent);
         */


        /**
         *


         */


        return view;
    }
    public boolean isStreamOnline() {
        return streamOnline;
    }



    public void setStreamOnline(boolean streamOnline) {
        this.streamOnline = streamOnline;
    }

    public void startStream(){
        //final String vidAddress = "http://176.34.150.29:1935/osecstream/myStream/manifest.f4m";

        /**
        //int width = webView.getWidth();
        //int height = webView.getHeight();
        //webView.loadUrl(vidAddress + "?width="+width+"&height="+height);
        snapshotView.setVisibility(View.GONE);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setPluginState(WebSettings.PluginState.ON);
        webView.loadUrl(vidAddress);
        webView.setWebChromeClient(new WebChromeClient());
        streamingSpinner.setVisibility(View.GONE);
        setStreamOnline(true);
        streamingButton.setText("Stopp stream");
         */
        snapshotView.setVisibility(View.GONE);
        streamingButton.setText("Stopp stream");

        streamingSpinner.setVisibility(View.GONE);



        vidControl.setAnchorView(vidView);
        vidControl.setMediaPlayer(vidView);
        //vidControl.setEnabled(false);
        //String vidAddress = "https://archive.org/download/ksnn_compilation_master_the_internet/ksnn_compilation_master_the_internet_512kb.mp4";
        //String vidAddress = "rtmp://1.23171047.fme.ustream.tv/ustreamVideo/23171047";
        //String vidAddress = "https://www.youtube.com/watch?v=vzojwG7OB7c";
        //String vidAddress = "https://youtu.be/xrXBZWQxk44";
        String vidAddress = "rtsp://176.34.150.29:1935/osecstream/myStream";
        //String vidAddress = "https://youtu.be/P47bqscizJY";
        Uri vidUri = Uri.parse(vidAddress);
        //vidView.setVideoURI(vidUri);
        vidView.setVideoURI(vidUri);
        vidView.setMediaController(vidControl);
        vidView.requestFocus();

        vidView.start();
        vidView.setVisibility(View.VISIBLE);
        setStreamOnline(true);



    }
    public void stopStream() {
        vidView.stopPlayback();
        vidView.setVisibility(View.GONE);
        streamingSpinner.setVisibility(View.GONE);
        snapshotView.setVisibility(View.VISIBLE);
        setStreamOnline(false);
        streamingButton.setText("Start stream");
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.mqttButton:
                Log.d(LOG_TAG, "mqtt clicked!");
                publish();
                break;
            case R.id.streamingButton:
                if (isStreamOnline() == false) {
                    streamingSpinner.setVisibility(View.VISIBLE);
                    Log.d(LOG_TAG, "Start stream clicked!");
                    startStream();
                }
                else if (isStreamOnline() == true) {
                    streamingSpinner.setVisibility(View.VISIBLE);
                    Log.d(LOG_TAG, "Stop stream clicked!");
                    stopStream();

                }

                break;
            default:
                break;
        }
    }


    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        final DemoListAdapter adapter = new DemoListAdapter(getActivity());
        adapter.addAll(DemoConfiguration.getDemoFeatureList());



        //ListView listView = (ListView) view.findViewById(android.R.id.list);
        //listView.setAdapter(adapter);
        /**
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> parent, final View view,
                                    final int position, final long id) {
                final DemoConfiguration.DemoFeature item = adapter.getItem(position);
                final AppCompatActivity activity = (AppCompatActivity) getActivity();
                if (activity != null) {
                    final Fragment fragment = DemoInstructionFragment.newInstance(item.name);

                    activity.getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.main_fragment_container, fragment, item.name)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .commit();

                    // Set the title for the fragment.
                    final ActionBar actionBar = activity.getSupportActionBar();
                    if (actionBar != null) {
                        actionBar.setTitle(item.titleResId);
                    }
                }
            }
        });
         */
    }
    //public Button getMqttButton() {
        //return mqttButton;
    //}

    private static final class DemoListAdapter extends ArrayAdapter<DemoConfiguration.DemoFeature> {
        private LayoutInflater inflater;

        public DemoListAdapter(final Context context) {
            super(context, R.layout.list_item_icon_text_with_subtitle);
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(final int position, final View convertView, final ViewGroup parent) {
            View view;
            ViewHolder holder;
            if (convertView == null) {
                view = inflater.inflate(R.layout.list_item_icon_text_with_subtitle, parent, false);
                holder = new ViewHolder();
                holder.iconImageView = (ImageView) view.findViewById(R.id.list_item_icon);
                holder.titleTextView = (TextView) view.findViewById(R.id.list_item_title);
                holder.subtitleTextView = (TextView) view.findViewById(R.id.list_item_subtitle);
                view.setTag(holder);
            } else {
                view = convertView;
                holder = (ViewHolder) convertView.getTag();
            }

            DemoConfiguration.DemoFeature item = getItem(position);
            holder.iconImageView.setImageResource(item.iconResId);
            holder.titleTextView.setText(item.titleResId);
            holder.subtitleTextView.setText(item.subtitleResId);

            return view;
        }
    }

    //TODO Encapsulate, avoid all this public shit
    public static final class ViewHolder {
        public ImageView iconImageView;
        public TextView titleTextView;
        public TextView subtitleTextView;
        public VideoView vidView;
    }
    public void s3Snapshot() {

        final StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

        StrictMode.setThreadPolicy(policy);
        //s3.getObject("latest-snapshot", "knapp.JPG");
        displayImage(snapshotView, s3, "knapp.JPG", "latest-snapshot");
    }

    //TODO 1. Må ha inn sjekk som undersøker om terminal er online når app starter
    //TODO 2. Håndtere om terminal er offline
    //Todo 3. Firebase ID må sendes SEPARAT fra armering/desarmering, bekreftet at dette virker med IoT console
    //Todo 4. Alternativ 1: Sleep / delay mellom metodekall. Alternativ 2: Sende token på oppstart (men hva om terminal er offline? Må vi se på thing shadow?)
    //Todo 5. Alternativ 3: Send MQTT når terminal startes
    public void connectClick() {


        new Thread(new Runnable() {
            @Override
            public void run() {



                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //final StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                        //StrictMode.setThreadPolicy(policy);
                        //s3.getObject("latest-snapshot", "knapp.JPG");
                        //displayImage(snapshotView, s3, "knapp.JPG", "latest-snapshot");
                        mqttButton.setEnabled(true);
                    }
                });
            }
        }).start();



        try {
            //TODO Refactore for å knytte MQTT credentials til en spesifikk bruker ved bruk av identitymanager og identitytprovider
            AuthenticationResultType authenticationResultType = new AuthenticationResultType();
            String idToken = authenticationResultType.getIdToken();


            Map<String, String> logins = new HashMap<String, String>();
            logins.put("eu-west-1_2F3hyifQN", idToken);
            credentialsProvider = new CognitoCachingCredentialsProvider(
                    getActivity(),
                    COGNITO_POOL_ID,
                    MY_REGION
            );
            credentialsProvider.setLogins(logins);

            mqttManager.connect(credentialsProvider, new AWSIotMqttClientStatusCallback() {

                @Override
                public void onStatusChanged(final AWSIotMqttClientStatus status,
                                            final Throwable throwable) {
                    Log.d(LOG_TAG, "Status = " + String.valueOf(status));

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (status == AWSIotMqttClientStatus.Connecting) {
                                //tvStatus.setText("Connecting...");

                            } else if (status == AWSIotMqttClientStatus.Connected) {
                                mqttButton.setClickable(false);
                                Log.d(LOG_TAG, credentialsProvider.getCredentials().toString());
                                subscribe();
                                checkArmStatus();
                                //TODO: Dersom terminal er offline når app connecter vil terminal ikke få firebaseID
                                publishFirebase();

                            } else if (status == AWSIotMqttClientStatus.Reconnecting) {
                                if (throwable != null) {
                                    Log.e(LOG_TAG, "Connection error.", throwable);
                                }
                                else {
                                    Log.d(LOG_TAG, "Credentials are: " + identityManager.getCredentialsProvider().getCredentials());
                                    Log.d(LOG_TAG, "User name is: " + identityManager.getUserName());
                                    if (identityManager.isUserSignedIn() == true) {
                                        Log.d(LOG_TAG, "IdentityManager checked logins, user is signed in");
                                    }
                                    else {
                                        Log.d(LOG_TAG, "Not signed in!");
                                    }
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


    public void checkArmStatus(){
        mqttManager.publishString("check", "/osecurity/fromapp", AWSIotMqttQos.QOS0);
    }

    public void publishFirebase(){
        String firebase = "/osecurity/firebase";

        mqttManager.publishString(firebaseToken, firebase, AWSIotMqttQos.QOS0);


    }


    public void subscribe(){
        final String topic = "/osecurity/fromterminal";

        Log.d(LOG_TAG, "topic = " + topic);

        try {
            mqttManager.subscribeToTopic(topic, AWSIotMqttQos.QOS0,
                    new AWSIotMqttNewMessageCallback() {
                        @Override
                        public void onMessageArrived(final String topic, final byte[] data) {
                            // Here, we create a new thread to avoid NetworkOnMainThreadException
                            getActivity().runOnUiThread(new Runnable() {
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
                                            isArmed = true;
                                        } else if (message.equals("disarmed")) {
                                            mqttButton.setClickable(true);
                                            mqttButton.setText("Aktiver");
                                            isArmed = false;
                                        } else if (message.equals("Terminal er online")) {
                                            Log.d(LOG_TAG, "Terminal er online, funker");
                                            Toast toast = Toast.makeText(getActivity(), "Terminal er online!", Toast.LENGTH_SHORT);
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

    }



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

    //TODO: Legge inn alle hardkodede variabler i ressursmappe for ryddighet
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
            //TODO Fjern en av disse (den som er gal)
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




    //View.OnClickListener handleS3 = new View.OnClickListener() {
    // @Override
    //TODO Refactor til kun å håndtere S3, mqtt tas på HomeDemoFragment


    public InputStream getLocalImage(String imageName ) {
        try {
            return getActivity().openFileInput( imageName );
        }
        catch ( FileNotFoundException exception ) {
            return null;
        }
    }


    public void displayImage( ImageView view,
                              AmazonS3Client s3,
                              String imageName,
                              String bucketName ) {
        if ( this.isNewImageAvailable( s3, imageName, bucketName ) ) {
            this.getRemoteImage( s3, imageName, bucketName );
        }

        InputStream stream = this.getLocalImage( imageName );
        view.setImageDrawable( Drawable.createFromStream( stream, "src" ) );
        GregorianCalendar calendar = new GregorianCalendar();

        snapshotTimestamp.setText("Siste stillbilde hentet " + new SimpleDateFormat("HH:mm dd.MM.yyyy").format(calendar.getTime()));
    }

    private boolean isNewImageAvailable( AmazonS3Client s3,
                                         String imageName,
                                         String bucketName ) {
        File file = new File( this.getActivity().getFilesDir(),
                imageName );
        if ( !file.exists() ) {
            return true;
        }

        ObjectMetadata metadata = s3.getObjectMetadata( bucketName,
                imageName );
        long remoteLastModified = metadata.getLastModified().getTime();

        if ( file.lastModified() < remoteLastModified ) {
            return true;
        }
        else {
            return false;
        }
    }
    private void getRemoteImage( AmazonS3Client s3,
                                 String imageName,
                                 String bucketName ) {
        S3Object object = s3.getObject( bucketName, imageName );
        this.storeImageLocally( object.getObjectContent(), imageName );
    }

    private void storeImageLocally( InputStream stream,
                                    String imageName ) {
        FileOutputStream outputStream;
        try {
            outputStream = getActivity().openFileOutput( imageName,
                    Context.MODE_PRIVATE);

            int length = 0;
            byte[] buffer = new byte[1024];
            while ( ( length = stream.read( buffer ) ) > 0 ) {
                outputStream.write( buffer, 0, length );
            }

            outputStream.close();
        }
        catch ( Exception e ) {
            Log.d( "Store Image", "Can't store image : " + e );
        }
    }


}
