package com.example.photosearcher;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;

import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.example.photosearcher.DL.VisionImage;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionPoint;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceContour;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    Button btnLoad, btnSearch, btnDetect;
    ImageView imgLoad, servImg;

    Uri uriLoad;

    static final int GALLERY_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseApp.initializeApp(this);

        btnLoad = (Button) findViewById(R.id.btn_load);
        imgLoad = (ImageView) findViewById(R.id.load_img);
        btnSearch = (Button) findViewById(R.id.btn_search);
        servImg = (ImageView) findViewById(R.id.bing_img);
        btnDetect = (Button) findViewById(R.id.btn_detect);

        btnLoad.setOnClickListener(v -> {
            Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
            photoPickerIntent.setType("image/*");
            startActivityForResult(photoPickerIntent, GALLERY_REQUEST);
        });

        btnSearch.setOnClickListener(v -> getSamePhoto(uriLoad));

        btnDetect.setOnClickListener(v -> {
            VisionImage visionImage = new VisionImage();
            detectFaces(visionImage.imageFromPath(this, uriLoad));
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch(requestCode) {
            case GALLERY_REQUEST:
                if(resultCode == RESULT_OK){
                    uriLoad = imageReturnedIntent.getData();
                    imgLoad.setImageURI(uriLoad);



                }
        }
    }


    private void getSamePhoto(Uri fileUri){
        // https://github.com/iPaulPro/aFileChooser/blob/master/aFileChooser/src/com/ipaulpro/afilechooser/utils/FileUtils.java
        // use the FileUtils to get the actual file by uri
        File file = FileUtils.getFile(this, fileUri);

        // create RequestBody instance from file
        RequestBody requestFile =
                RequestBody.create(
                        MediaType.parse(getContentResolver().getType(fileUri)),
                        file
                );

        // MultipartBody.Part is used to send also the actual file name
        MultipartBody.Part body =
                MultipartBody.Part.createFormData("image", file.getName(), requestFile);
        NetworkService.getInstance()
                .getJSONApi()
                .getImage(Data.KEY, body)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        Log.d("ResponseServ", "Success " + response.message());
                        try {
                            String body = response.body().string();
                            Log.d("ResponseServ", body);
                            int linkIndex = body.indexOf("webSearchUrl", 300) + 16;
                            //Log.d("ResponseServ", link + " " + body.charAt(link + 13));
                            int lastIndex = linkIndex;
                            while (lastIndex < body.length()){
                                if(body.charAt(lastIndex) == '\"'){
                                    Log.d("ResponseServ", "find");
                                    break;
                                }else{
                                    Log.d("ResponseServ", "plus1");
                                    lastIndex++;
                                }
                            }
                            String url = body.substring(linkIndex, lastIndex).replace("\\", "/");
                            Log.d("ResponseServ", url);
                            //servImg.setImageURI(Uri.parse(url));
                            Glide.with(MainActivity.this)
                                    .load(url)
                                    .into(servImg);

                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setData(Uri.parse(url));
                            startActivity(i);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        Log.d("ResponseServ", "Error" + t.getMessage());
                    }
                });
    }



    private void detectFaces(FirebaseVisionImage image) {
        // [START set_detector_options]


        FirebaseVisionFaceDetectorOptions options =
                new FirebaseVisionFaceDetectorOptions.Builder()
                        .setClassificationMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
                        .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                        .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                        .setMinFaceSize(0.15f)
                        .enableTracking()
                        .build();
        // [END set_detector_options]

        // [START get_detector]
        FirebaseVisionFaceDetector detector = FirebaseVision.getInstance()
                .getVisionFaceDetector(options);
        // [END get_detector]

        // [START run_detector]
        Task<List<FirebaseVisionFace>> result =
                detector.detectInImage(image)
                        .addOnSuccessListener(
                                new OnSuccessListener<List<FirebaseVisionFace>>() {
                                    @Override
                                    public void onSuccess(List<FirebaseVisionFace> faces) {
                                        // Task completed successfully
                                        // [START_EXCLUDE]
                                        // [START get_face_info]
                                        Log.d("MainActv", "Success");
                                        for (FirebaseVisionFace face : faces) {
                                            Rect bounds = face.getBoundingBox();
                                            float rotY = face.getHeadEulerAngleY();  // Head is rotated to the right rotY degrees
                                            float rotZ = face.getHeadEulerAngleZ();  // Head is tilted sideways rotZ degrees
                                            Log.d("MainActv", bounds.left + " " + bounds.bottom);
                                            try {
                                                cropImage(((BitmapDrawable)imgLoad.getDrawable()).getBitmap(), bounds.left,
                                                        bounds.right, bounds.top, bounds.bottom);
                                                Log.d("MainActv", "SuccessFile");
                                            } catch (IOException e) {
                                                Log.d("MainActv", "FailFile");
                                                e.printStackTrace();
                                            }
                                            // If landmark detection was enabled (mouth, ears, eyes, cheeks, and
                                            // nose available):
                                            FirebaseVisionFaceLandmark leftEar = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EAR);
                                            if (leftEar != null) {
                                                FirebaseVisionPoint leftEarPos = leftEar.getPosition();
                                            }

                                            // If classification was enabled:
                                            if (face.getSmilingProbability() != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                                                float smileProb = face.getSmilingProbability();
                                            }
                                            if (face.getRightEyeOpenProbability() != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                                                float rightEyeOpenProb = face.getRightEyeOpenProbability();
                                            }

                                            // If face tracking was enabled:
                                            if (face.getTrackingId() != FirebaseVisionFace.INVALID_ID) {
                                                int id = face.getTrackingId();
                                            }
                                        }
                                        // [END get_face_info]
                                        // [END_EXCLUDE]
                                    }
                                })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Task failed with an exception
                                        // ...
                                        Log.d("MainActv", "fail");
                                    }
                                });
        // [END run_detector]
    }

    private void faceOptionsExamples() {
        // [START mlkit_face_options_examples]
        // High-accuracy landmark detection and face classification
        FirebaseVisionFaceDetectorOptions highAccuracyOpts =
                new FirebaseVisionFaceDetectorOptions.Builder()
                        .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
                        .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                        .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                        .build();

        // Real-time contour detection of multiple faces
        FirebaseVisionFaceDetectorOptions realTimeOpts =
                new FirebaseVisionFaceDetectorOptions.Builder()
                        .setContourMode(FirebaseVisionFaceDetectorOptions.ALL_CONTOURS)
                        .build();
        // [END mlkit_face_options_examples]
    }

    private void processFaceList(List<FirebaseVisionFace> faces) {
        // [START mlkit_face_list]
        for (FirebaseVisionFace face : faces) {
            Rect bounds = face.getBoundingBox();
            float rotY = face.getHeadEulerAngleY();  // Head is rotated to the right rotY degrees
            float rotZ = face.getHeadEulerAngleZ();  // Head is tilted sideways rotZ degrees

            // If landmark detection was enabled (mouth, ears, eyes, cheeks, and
            // nose available):
            FirebaseVisionFaceLandmark leftEar = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EAR);
            if (leftEar != null) {
                FirebaseVisionPoint leftEarPos = leftEar.getPosition();
            }

            // If contour detection was enabled:
            List<FirebaseVisionPoint> leftEyeContour =
                    face.getContour(FirebaseVisionFaceContour.LEFT_EYE).getPoints();
            List<FirebaseVisionPoint> upperLipBottomContour =
                    face.getContour(FirebaseVisionFaceContour.UPPER_LIP_BOTTOM).getPoints();

            // If classification was enabled:
            if (face.getSmilingProbability() != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                float smileProb = face.getSmilingProbability();
            }
            if (face.getRightEyeOpenProbability() != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                float rightEyeOpenProb = face.getRightEyeOpenProbability();
            }

            // If face tracking was enabled:
            if (face.getTrackingId() != FirebaseVisionFace.INVALID_ID) {
                int id = face.getTrackingId();
            }
        }
        // [END mlkit_face_list]
    }


    private void cropImage(Bitmap input, int left, int right, int top, int bottom) throws IOException {
        Bitmap output = Bitmap.createBitmap(right - left, bottom - top, Bitmap.Config.RGB_565);

        for(int i = 0; i < output.getWidth(); i++){
            for (int j = 0; j < output.getHeight(); j++){
                output.setPixel(i, j, input.getPixel(left + i, top + j));
            }
        }

        servImg.setImageBitmap(output);

        String filename = "/output.jpg";

        //create a file to write bitmap data
        File f = new File(this.getCacheDir(), filename);
        f.createNewFile();

        //Convert bitmap to byte array
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        output.compress(Bitmap.CompressFormat.JPEG, 0 /*ignored for PNG*/, bos);
        byte[] bitmapdata = bos.toByteArray();

        //write the bytes in file
        FileOutputStream fos = new FileOutputStream(f);
        fos.write(bitmapdata);
        fos.flush();
        fos.close();
    }
}
