package com.frman.storagefirebase;

import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.frman.storagefirebase.util.Helper;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class DownloadActivity extends AppCompatActivity implements View.OnClickListener {

    private ImageView mImageView;
    private StorageReference mImageRef;
    private TextView mTextView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);
        bindWidget();
        FirebaseStorage storage = FirebaseStorage.getInstance();

        StorageReference storageRef = storage.getReference();
        mImageRef = storageRef.child("");
    }

    private void bindWidget() {
        mImageView = (ImageView) findViewById(R.id.imageview);
        mTextView = (TextView) findViewById(R.id.textview);
        findViewById(R.id.button_download_in_memory).setOnClickListener(this);
        findViewById(R.id.button_download_in_file).setOnClickListener(this);
        findViewById(R.id.button_download_via_url).setOnClickListener(this);
        findViewById(R.id.button_get_metadata).setOnClickListener(this);
        findViewById(R.id.button_update_metadata).setOnClickListener(this);
        findViewById(R.id.button_delete_file).setOnClickListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Helper.dismissProgressDialog();
        Helper.dismisDialog();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_download_in_memory:
                downloadInMemory();
                break;
            case R.id.button_download_in_file:
                downloadInLocalFile();
                break;
            case R.id.button_download_via_url:
                downloadDataViaUrl();
                break;
            case R.id.button_get_metadata:
                getMetadata();
                break;
            case R.id.button_update_metadata:
                updateMetaData();
                break;
            case R.id.button_delete_file:
                deleteFile();
                break;
        }
    }

    private void downloadInMemory() {
        Helper.showDialog(this);
        mImageRef.getBytes(Long.MAX_VALUE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                Helper.dismisDialog();
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                mImageView.setImageBitmap(bitmap);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Helper.dismisDialog();
                mTextView.setText(String.format("Failure: %s", exception.getMessage()));
            }
        });
    }

    private void downloadInLocalFile() {
        File dir = new File(Environment.getExternalStorageDirectory() + "/photos");
        final File file = new File(dir, UUID.randomUUID().toString() + ".png");
        try {
            if (!dir.exists()) {
                dir.mkdir();
            }
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        final FileDownloadTask fileDownloadTask = mImageRef.getFile(file);
        Helper.initProgressDialog(this);
        Helper.mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                fileDownloadTask.cancel();
            }
        });
        Helper.mProgressDialog.show();

        fileDownloadTask.addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                Helper.dismissProgressDialog();
                mTextView.setText(file.getPath());
                mImageView.setImageURI(Uri.fromFile(file));
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Helper.dismissProgressDialog();
                mTextView.setText(String.format("Failure: %s", exception.getMessage()));
            }
        }).addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onProgress(FileDownloadTask.TaskSnapshot taskSnapshot) {
                int progress = (int) ((100 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount());
                Helper.setProgress(progress);
            }
        });
    }

    private void downloadDataViaUrl() {
        Helper.showDialog(this);
        mImageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                Helper.dismisDialog();
                mTextView.setText(uri.toString());
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Helper.dismisDialog();
                mTextView.setText(String.format("Failure: %s", exception.getMessage()));
            }
        });
    }

    private void getMetadata() {
        Helper.showDialog(this);
        mImageRef.getMetadata().addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
            @Override
            public void onSuccess(StorageMetadata storageMetadata) {
                Helper.dismisDialog();
                mTextView.setText(String.format("Country: %s", storageMetadata.getCustomMetadata("country")));
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Helper.dismisDialog();
                mTextView.setText(String.format("Failure: %s", exception.getMessage()));
            }
        });
    }

    private void updateMetaData() {
        Helper.showDialog(this);
        StorageMetadata metadata = new StorageMetadata.Builder().setCustomMetadata("country", "Thailand").build();
        mImageRef.updateMetadata(metadata).addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
            @Override
            public void onSuccess(StorageMetadata storageMetadata) {
                Helper.dismisDialog();
                mTextView.setText(String.format("Country: %s", storageMetadata.getCustomMetadata("country")));
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Helper.dismisDialog();
                mTextView.setText(String.format("Failure: %s", exception.getMessage()));
            }
        });
    }

    private void deleteFile() {
        Helper.showDialog(this);
        mImageRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Helper.dismisDialog();
                mImageView.setImageDrawable(null);
                mTextView.setText(String.format("%s was deleted.", mImageRef.getPath()));
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Helper.dismisDialog();
                mTextView.setText(String.format("Failure: %s", exception.getMessage()));
            }
        });
    }
}


