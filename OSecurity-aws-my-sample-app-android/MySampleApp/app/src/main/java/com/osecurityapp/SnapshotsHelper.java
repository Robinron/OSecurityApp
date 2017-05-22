package com.osecurityapp;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.ImageView;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Created by Sondre on 28.03.2017.
 */

public class SnapshotsHelper extends Activity {

    public boolean isNewImageAvailable( AmazonS3Client s3,
                                         String imageName,
                                         String bucketName ) {
        File file = new File( this.getApplicationContext().getFilesDir(),
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
    public void getRemoteImage( AmazonS3Client s3,
                                 String imageName,
                                 String bucketName ) {
        S3Object object = s3.getObject( bucketName, imageName );
        this.storeImageLocally( object.getObjectContent(), imageName );
    }

    public void storeImageLocally( InputStream stream,
                                    String imageName ) {
        FileOutputStream outputStream;
        try {
            outputStream = openFileOutput( imageName,
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

    public void displayImage( ImageView view,
                               AmazonS3Client s3,
                               String imageName,
                               String bucketName ) {
        if ( this.isNewImageAvailable( s3, imageName, bucketName ) ) {
            this.getRemoteImage( s3, imageName, bucketName );
        }

        InputStream stream = this.getLocalImage( imageName );
        view.setImageDrawable( Drawable.createFromStream( stream, "src" ) );
    }
    public InputStream getLocalImage( String imageName ) {
        try {
            return openFileInput( imageName );
        }
        catch ( FileNotFoundException exception ) {
            return null;
        }
    }

}
