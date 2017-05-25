package com.osecurityapp.contentproviders;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.LinearLayout;


import com.amazonaws.mobile.AWSMobileClient;
import com.amazonaws.mobile.user.IdentityManager;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.osecurityapp.PubSubActivity;
import com.osecurityapp.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

import static android.R.attr.button;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ArchiveFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ArchiveFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ArchiveFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    static final String LOG_TAG = ArchiveFragment.class.getCanonicalName();
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private Button btn;
    private AmazonS3Client s3;
    private IdentityManager identityManager;
    private TextView snapshotTimestamp;
    private String snapshotTime = "";
    private ImageView snapshotView;
    private GridLayout gridLayout;


    private OnFragmentInteractionListener mListener;

    public ArchiveFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ArchiveFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ArchiveFragment newInstance(String param1, String param2) {
        ArchiveFragment fragment = new ArchiveFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_archive, container, false);
        gridLayout = (GridLayout) view.findViewById(R.id.imageLayout);
        int total = 20;
        int column = 2;
        int row = total / column;
        gridLayout.setColumnCount(column);
        gridLayout.setRowCount(row + 1);



        identityManager = AWSMobileClient.defaultMobileClient()
                .getIdentityManager();

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState){
        s3 = new AmazonS3Client(identityManager.getCredentialsProvider());

        loopArchive();
    }




    public void loopArchive(){
        for(int i=0;i<20;i++)
        {
            ImageView image = new ImageView(getActivity());
            image.setLayoutParams(new LinearLayout.LayoutParams(500, 500));
            //image.setMaxHeight(500);
            //image.setMaxWidth(500);
            image.setPadding(50, 0, 0, 0);
            displayImage(image, s3, "knapp.JPG", "latest-snapshot");
            // Adds the view to the layout
            GregorianCalendar calendar = new GregorianCalendar();
            ObjectMetadata metadata = s3.getObjectMetadata("latest-snapshot", "knapp.JPG");
            long lastModified = metadata.getLastModified().getTime();
            String strLong = Long.toString(lastModified);
            TextView snapshotTimestamp = new TextView(getActivity());
            snapshotTimestamp.setText("Bilde tatt: " + String.valueOf(lastModified));
            //LinearLayout linLayout = new LinearLayout(getActivity());

            //linLayout.addView(image);
            //linLayout.addView(snapshotTimestamp);
            gridLayout.addView(image);
            //gridLayout.addView(image);
            //gridLayout.addView(snapshotTimestamp);
        }
    }




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

        //TODO: Gjøre klart så timestamp kommer på arkiv
        //snapshotTimestamp.setText("Siste stillbilde hentet " + new SimpleDateFormat("HH:mm dd.MM.yyyy").format(calendar.getTime()));
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

    public void s3Snapshot() {

        final StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

        StrictMode.setThreadPolicy(policy);
        //s3.getObject("latest-snapshot", "knapp.JPG");
        displayImage(snapshotView, s3, "knapp.JPG", "latest-snapshot");
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            //throw new RuntimeException(context.toString()
             //       + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
