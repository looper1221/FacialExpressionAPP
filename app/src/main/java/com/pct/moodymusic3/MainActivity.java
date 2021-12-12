package com.pct.moodymusic3;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.FaceDetector;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Objects;

import Retrofit.IUploadAPI;
import Retrofit.RetrofitClient;
import Utils.Common;
import Utils.ProgressRequestBody;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private static final int PICK_FILE_REQUEST = 1000;

    IUploadAPI mService;
    Uri selectedFileUri;
    Button loadButton;
    ImageView imageView;

    ProgressDialog dialog;

    private IUploadAPI getAPIUpload(){
        return RetrofitClient.getClient().create(IUploadAPI.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Dexter.withActivity(this)
                .withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        Toast.makeText(MainActivity.this, "Permission Granted !", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(MainActivity.this, "Should Accept to upload Image !", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                        Toast.makeText(MainActivity.this, "", Toast.LENGTH_SHORT).show();
                    }
                }).check();

        loadButton = findViewById(R.id.load_button);
        imageView = findViewById(R.id.image_view);

        mService = getAPIUpload();

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseFile();
            }
        });

        loadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    uploadFile();
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK){
            if (requestCode == PICK_FILE_REQUEST){
                if (data != null){
                    selectedFileUri = data.getData();
                    if (selectedFileUri != null && !selectedFileUri.getPath().isEmpty()){
                        imageView.setImageURI(selectedFileUri);
                        System.out.println("URI1::" + selectedFileUri);
                    }else {
                        Toast.makeText(this, "File not found !", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    private void uploadFile() throws URISyntaxException {
        if (selectedFileUri != null) {
            dialog = new ProgressDialog(MainActivity.this);
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setMessage("Uploading...");
            dialog.setMax(100);
            dialog.setCanceledOnTouchOutside(false);
            dialog.setIndeterminate(false);
            dialog.show();

            File file = null;
            try {
                System.out.println(this + ":::" + selectedFileUri.getPath());
                String path = selectedFileUri.getPath();

//                System.out.println(Common.getFilePath(this, selectedFileUri));
               File root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
               // file = new File(Common.getFilePath(this, selectedFileUri));
//                path.mkdirs();
                if (path.startsWith("/document/raw:/storage/emulated/0")){
                     path = path.replaceFirst("/document/raw:/storage/emulated/0","");
                }
                System.out.println("paaaath:" + path);
                file = new File(root, path);
//                System.out.println(path + "" + selectedFileUri.getPath());

            } finally {

            }
            if (file != null) {
                final ProgressRequestBody requestBody = new ProgressRequestBody(file, this);
                final MultipartBody.Part body = MultipartBody.Part.createFormData("image", file.getName(), requestBody);
                System.out.println("img name::" + file.getName());
                System.out.println("body::" + body.headers());
                System.out.println("RequestBody::" + requestBody.contentType());
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mService.uploadFile(body)
                                .enqueue(new Callback<String>() {

                                    @Override
                                    public void onResponse(Call<String> call, Response<String> response) {
                                        dialog.dismiss();
                                        System.out.println("Response Body::" + response.body());
                                        String image_processed_link = new StringBuilder("http://10.0.2.2:5000/" +
                                                response.body().replace("\"", "")).toString();
                                        System.out.println("link:" + image_processed_link);
                                        Picasso.get().load(image_processed_link)
                                                .into(imageView);
                                        Toast.makeText(MainActivity.this, "Detected", Toast.LENGTH_SHORT).show();
                                    }

                                    @Override
                                    public void onFailure(Call<String> call, Throwable t) {
                                        dialog.dismiss();
                                        Toast.makeText(MainActivity.this, t.getMessage() , Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                }).start();

            }
        }
        else {
            Toast.makeText(this, "File in null", Toast.LENGTH_SHORT).show();
        }
    }




    private void chooseFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_FILE_REQUEST);
    }



//    @Override
//    public void onActivityResult(int requestCode, int resultCode, Intent requestData) {
//        super.onActivityResult(requestCode, resultCode, requestData);
//        Uri uri = null;
//        if(requestCode == READ_CODE_REQUEST && requestCode == Activity.RESULT_OK){
//            try {
//                uri = requestData.getData();
//                getBitmapFromUri(uri);
//            }catch (IOException e){
//
//            }
//        }
//    }
//
//    private void getBitmapFromUri(Uri uri) throws FileNotFoundException {
//        ParcelFileDescriptor parcelFileDescriptor = getContentResolver().openFileDescriptor(uri, "r");
//        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
//        showImage(fileDescriptor);
//    }

//    private void showImage(FileDescriptor fileDescriptor) {
//        // load Image
//        imageView = findViewById(R.id.imgview);
//        BitmapFactory.Options options = new BitmapFactory.Options();
//        options.inMutable = true;
//
//        // Create Paint object to draw square
//        Bitmap myBitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);
//        Paint paint = new Paint();
//        paint.setStrokeWidth(5);
//        paint.setColor(Color.RED);
//        paint.setStyle(Paint.Style.STROKE);
//
//        // Create Canvas to draw RECT.
//        Bitmap tempBitmap = Bitmap.createBitmap(myBitmap.getWidth(),
//                myBitmap.getHeight(), Bitmap.Config.RGB_565);
//        Canvas tempCanvas = new Canvas(tempBitmap);
//        tempBitmap.drawBitmap(myBitmap,0 ,0, null);
//
//        // Face Detector
//        FaceDetector faceDetector = new FaceDetector.Builder(getApplicationContext()).setTrackingEnabled(false).build();
//
//        if(!faceDetector.isOperational()){
//            new AlertDialog.Builder(getApplicationContext()).setMessage("Couldn't set up");
//            return;
//        }
//
//        // detect faces
//        Frame frame = new Frame.builder()setBitmap(myBitmap).build();
//
//    }
}