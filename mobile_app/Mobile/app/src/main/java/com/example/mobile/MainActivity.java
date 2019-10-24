package com.example.mobile;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final int ALL_PERMISSIONS_CODE = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 100;

    private FusedLocationProviderClient client;
    private boolean permissionsGranted;
    LocationCallback locationCallback;
    LocationRequest request;
    private RequestQueue queue;
    private String currentPhotoPath;
    private String encodedImage;
    private String currentData;
    private String currentID_to_be_helped;
    private String currentLectureID;
    private String id;
    private JSONArray lectures;
    private JSONObject lecture;
    private SensorManager sensorManager;
    private boolean isShake = false;
    private String result = "";
    Sensor accelerometer;

    private Button pictureRequestButton;
    private Button shakeRequestButton;

    // hy.d --start
    private AudioTrack mAudioTrack;
    private AudioRecord mAudioRecord;
    private int mRecorderBufferSize;
    private byte[] mAudioData;
    // audio argument
    private int mSampleRateInHZ = 8000; // rate
    private int mAudioFormat = AudioFormat.ENCODING_PCM_16BIT;  // format
    private int mChannelConfig = AudioFormat.CHANNEL_IN_MONO;   // channel

    private boolean isRecording = false;
    /**
     * records save
     */
    private ThreadPoolExecutor mExecutor = new ThreadPoolExecutor(2, 2, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>());

    private boolean runFlag;
    private NotificationUtils notificationUtils;
    //Handler UI modification
    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            String data = (String)msg.obj;
            Log.e("net", "get message" + msg.what);
            switch(msg.what){
                case 1:
                    // send notifications
                    notificationUtils.sendNotification("Answer Question", data);
                    break;
                case 2:
                    // open link
                    Uri uri = Uri.parse(data);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        // If shake request button is clicked, show shake request pop up

        shakeRequestButton = findViewById(R.id.shakeRequest);
        shakeRequestButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        shakeRequestPopUp();
                    }
                }
        );

        // If picture request button is clicked, show picture request pop up

        pictureRequestButton = findViewById(R.id.pictureRequest);
        pictureRequestButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        pictureRequestPopUp();
                    }
                }
        );

        client = LocationServices.getFusedLocationProviderClient(this);
        permissionsGranted = false;
        queue = Volley.newRequestQueue(this);
        currentLectureID = "-1";
        id = "1"; // GET THIS FROM LOGIN

        // get lectures from server
        getLectures();

        // get permissions
        getPermissions();

        // checks location periodically and sends "checkin"
        client.getLastLocation();
        checkAttendance();

        // check if there data for the user from the server
        CheckThread checkThread = new CheckThread(); // ADD ID OF USER
        checkThread.start();

        // hy.d --start
        notificationUtils  = new NotificationUtils(this);
        // start service
        runFlag = true;
        //startLoop();
        initData();
        final Button button = (Button)findViewById(R.id.start_btn);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isRecording){
                    button.setText("Stop Recode");
                    onClickStart();
                }else{
                    button.setText("Start Recode");
                    onClickEnd();
                }
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /*
    Get location permission for device
     */
    private void getPermissions() {

        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA};

        if (checkPermissions(this.getApplicationContext(), permissions)) {

            permissionsGranted = true;

        } else {

            ActivityCompat.requestPermissions(this,
                    permissions, ALL_PERMISSIONS_CODE);
        }
    }

    /*
    check each permission
     */
    private boolean checkPermissions(Context context, String[] permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    /*
    Handles the result of the permission request
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {

            case ALL_PERMISSIONS_CODE:

                // if permission is not granted
                if (grantResults.length > 0
                        && checkGrantResults(grantResults)) {
                    Toast exitAppToast = Toast.makeText(MainActivity.this,
                            "Need to grant permissions to use app", Toast.LENGTH_LONG);
                    exitAppToast.show();
                    finish();
                } else {
                    permissionsGranted = true;
                }
                break;
        }
    }

    /*
    check results for grantResults
     */

    private boolean checkGrantResults(int[] grantResults) {
        if (grantResults != null) {
            for (int result: grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    return true;
                }
            }
        }
        return false;
    }

    /*
    send a request to get lectures
     */

    private void getLectures() {

        String url = "http://43.240.97.26:8000/webapp/getLectures/";

        StringRequest postRequest = new StringRequest
                (Request.Method.POST, url, new Response.Listener<String>() {

                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject resp = new JSONObject(response);
                            if (resp.getString("status").equals("true")) {
                                Log.d("checkgetlectures", "received lectures");

                                try {
                                    lectures = resp.getJSONArray("lectures");
                                    Log.d("checkgetlectures", lectures.toString());
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError e) {
                        Log.d("checkgetlectures", "can't get lectures from server");
                    }
                })
        {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String>  headers = new HashMap<String, String>();
                headers.put("Content-Type", "application/x-www-form-urlencoded");

                return headers;
            }
        };

        queue.add(postRequest);
    }

    /*
    sends "check" to server every 2 seconds
     */
    class CheckThread extends Thread {

        @Override
        public void run() {

            String url = "http://43.240.97.26:8000/webapp/check/";

            while (true) {

                try {
                    CheckThread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (!currentLectureID.equals("-1")) {

                    StringRequest postRequest = new StringRequest
                            (Request.Method.POST, url, new Response.Listener<String>() {

                                @Override
                                public void onResponse(String response) {

                                    try {
                                        JSONObject resp = new JSONObject(response);

                                        if (resp.getString("status").equals("true")) {
                                            Message message = null;

                                            switch(resp.getString("type")) {

                                                case "ask_picture":
                                                    takePicturePopUp(resp.getString("ID_to_be_helped"));
                                                    break;

                                                case "picture_respond":
                                                    currentData = resp.getString("data");
                                                    pictureReceivedPopUp();
                                                    break;

                                                case "answer_question":
                                                    String answer = resp.getString("data");
                                                    message = handler.obtainMessage(1, answer);
                                                    break;

                                                case "link":
                                                    String link = resp.getString("data");
                                                    message = handler.obtainMessage(2, link);
                                                    break;
                                            }
                                        }

                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }, new Response.ErrorListener() {

                                @Override
                                public void onErrorResponse(VolleyError e) {
                                    e.getStackTrace();
                                    Toast error = Toast.makeText(MainActivity.this,
                                            "Can't send request to check", Toast.LENGTH_LONG);
                                    error.show();
                                }
                            }) {
                        @Override
                        public Map<String, String> getHeaders() throws AuthFailureError {
                            Map<String, String> headers = new HashMap<>();
                            headers.put("Content-Type", "application/x-www-form-urlencoded");

                            return headers;
                        }

                        @Override
                        public Map<String, String> getParams() {
                            Map<String, String> params = new HashMap<>();
                            try {
                                params.put("id", id); // ADD ID OF USER
                                params.put("lectureID", lecture.getString("lectureID"));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            return params;
                        }
                    };
                    queue.add(postRequest);
                }
            }
        }

    }

    /*
    Requests location and sends "checkin" to server
     */
    private void checkAttendance() {

        request = LocationRequest.create();
        request.setInterval(10000);
        request.setFastestInterval(5000);
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        try {
            if (permissionsGranted) {
                client.requestLocationUpdates(request, locationCallback = new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        Location location = locationResult.getLastLocation();
                        if (location != null) {
                            // check each lecture in lectures
                            for (int i = 0; i < lectures.length(); i++) {
                                try {
                                    lecture = lectures.getJSONObject(i);

                                    if (checkTime(lecture) && checkLocation(lecture, location)) {

                                        currentLectureID = lecture.getString("lectureID");
                                        Log.d("checklectureid", currentLectureID);

                                        String url = "http://43.240.97.26:8000/webapp/checkin/";

                                        StringRequest postRequest = new StringRequest
                                                (Request.Method.POST, url, new Response.Listener<String>() {

                                                    @Override
                                                    public void onResponse(String response) {

                                                        try {
                                                            JSONObject resp = new JSONObject(response);
                                                            if (resp.getString("status").equals("true")) {
                                                                Toast receive = Toast.makeText(MainActivity.this,
                                                                        "You are attending the lecture!", Toast.LENGTH_LONG);
                                                                receive.show();
                                                            }

                                                            if (resp.getString("status").equals("false")) {
                                                                Log.d("checkattendance", "attendance is false");
                                                            }
                                                        } catch (JSONException e) {
                                                            e.printStackTrace();
                                                        }
                                                    }
                                                }, new Response.ErrorListener() {

                                                    @Override
                                                    public void onErrorResponse(VolleyError e) {
                                                        e.getStackTrace();
                                                        Toast error = Toast.makeText(MainActivity.this,
                                                                "Can't send request to checkin", Toast.LENGTH_LONG);
                                                        error.show();
                                                    }
                                                })
                                        {
                                            @Override
                                            public Map<String, String> getHeaders() throws AuthFailureError {
                                                Map<String, String> headers = new HashMap<>();
                                                headers.put("Content-Type", "application/x-www-form-urlencoded");

                                                return headers;
                                            }

                                            @Override
                                            public Map<String,String> getParams() {
                                                Map<String, String> params = new HashMap<>();
                                                try {
                                                    params.put("id", id);
                                                    params.put("lectureID", lecture.getString("lectureID"));
                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                }

                                                return params;
                                            }
                                        };
                                        queue.add(postRequest);
                                        return;
                                    }

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        currentLectureID = "-1";
                    }
                }, null);
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    /*
    check if device is near a specific location
     */
    private boolean checkLocation(JSONObject lecture, Location location) {
        try {
            double currentLon = location.getLongitude();
            double currentLan = location.getLatitude();
            float[] distance = new float[1];
            double lectureLon = lecture.getDouble("longitude");
            double lectureLan = lecture.getDouble("latitude");
            Location.distanceBetween(lectureLan, lectureLon,
                    currentLan, currentLon, distance);

            if (distance[0] < 30) {
                Log.d("checklocation", "location is correct");
                return true;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    /*
    check if current time is between specified time and 30 minutes after that and the dates are the same
     */

    private boolean checkTime(JSONObject lecture) {
        try {
            Date currentDate = Calendar.getInstance().getTime();
            Date startDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").
                    parse(lecture.getString("dateTime"));
            Calendar cal = Calendar.getInstance();
            cal.setTime(startDate);
            cal.add(Calendar.HOUR_OF_DAY, 2);
            Date endDate = cal.getTime();

            if (currentDate.after(startDate) && currentDate.before(endDate)) {
                Log.d("checktime", "time is correct");
                return true;
            }

        } catch (JSONException | ParseException e) {
            e.printStackTrace();
        }
        Log.d("checktime", "time is incorrect");
        return false;
    }

    /*
     Pop up to request of shaking question
     */
    private void shakeRequestPopUp() {

            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Shake for question")
                    .setMessage("Would you like to notify lecturer of your question?");

            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (isShake == true){
                        sendShake("1","1"); //get it from login and lecture selection
                        Toast DiagToast = Toast.makeText(MainActivity.this,
                                "Your request have been sent", Toast.LENGTH_LONG);
                        DiagToast.show();
                    }
                    else{
                        Toast exitDiagToast = Toast.makeText(MainActivity.this,
                                "You haven't shake your phone yet", Toast.LENGTH_LONG);
                        exitDiagToast.show();
                        dialogInterface.dismiss();

                    }

                }
            });

            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });

            AlertDialog dialog = builder.create();
            dialog.show();
        }

    /*
     Pop up to request a picture
     */
    private void pictureRequestPopUp() {

        if (!currentLectureID.equals("-1")) {

            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Request a picture")
                    .setMessage("Would you like to request a picture of the whiteboard?");

            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    pictureRequest();
                }
            });

            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });

            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    /*
    send ask_help request with type ask_picture
     */
    private void pictureRequest() {

        String url = "http://43.240.97.26:8000/webapp/askhelp/";

        StringRequest postRequest = new StringRequest
                (Request.Method.POST, url, new Response.Listener<String>() {

                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject resp = new JSONObject(response);
                            if (resp.getString("status").equals("true")) {
                                Log.d("checkpicturerequest", "request for a picture has been sent");
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError e) {
                        e.getStackTrace();
                        Log.d("checkpicturerequest", "could not request for a picture");
                    }
                })
        {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/x-www-form-urlencoded");

            return headers;
            }

            @Override
            public Map<String,String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("id", id);
                params.put("lectureID", currentLectureID);
                params.put("type", "ask_picture");

                return params;
            }
        };
            queue.add(postRequest);
    }

    /*
    Pop up to ask user if they would like to help a classmate by taking a picture
     */

    private void takePicturePopUp(final String ID_to_be_helped) {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("A classmate is requesting help")
                .setMessage("Would you like to help a classmate by taking a picture of the whiteboard?");

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                sendHelp(ID_to_be_helped);
            }
        });

        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /*
    Accesses camera app to take a picture and sends upload with type picture
     */
    private void takePicture(String ID_to_be_helped) {

        if (!this.getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            Toast error = Toast.makeText(MainActivity.this,
                    "Device has no camera!", Toast.LENGTH_LONG);
            error.show();
            return;
        }

        try {
            if (permissionsGranted) {
                currentID_to_be_helped = ID_to_be_helped;
                dispatchTakePictureIntent();
            } else {
                Toast error = Toast.makeText(MainActivity.this,
                        "Do not have camera and storage permissions!", Toast.LENGTH_LONG);
                error.show();
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    /*
    create file for image
    code adapted from: https://developer.android.com/training/camera/photobasics
     */
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "Send_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();

        return image;
    }

    /*
    Open camera to take picture and save picture in a file
    code adapted from: https://developer.android.com/training/camera/photobasics
     */
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                ex.printStackTrace();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.mobile.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    /*
    send photo encoded in base64 to server once it is taken
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            encodedImage = encodeImage(currentPhotoPath);

            String url = "http://43.240.97.26:8000/webapp/upload/";

            StringRequest postRequest = new StringRequest
                    (Request.Method.POST, url, new Response.Listener<String>() {

                        @Override
                        public void onResponse(String response) {
                            try {
                                JSONObject resp = new JSONObject(response);
                                if (resp.getString("status").equals("true")) {
                                    Log.d("checksentphoto", "photo has been sent to the server");
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }, new Response.ErrorListener() {

                        @Override
                        public void onErrorResponse(VolleyError e) {
                            Log.d("checksentphoto", "photo can't be sent to the server");
                        }
                    })
            {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String>  headers = new HashMap<String, String>();
                    headers.put("Content-Type", "application/x-www-form-urlencoded");

                    return headers;
                }

                @Override
                public Map<String,String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    params.put("ID_to_be_helped", currentID_to_be_helped);
                    params.put("id", id);
                    params.put("type", "picture");
                    params.put("data", encodedImage);
                    params.put("lectureID", currentLectureID);

                    return params;
                }
            };

            queue.add(postRequest);
        }
    }

    /*
    Encode image using base64
    code adapted from: https://stackoverflow.com/a/17874349
     */
    private String encodeImage(String filePath) {
        try {
            InputStream inputStream = new FileInputStream(filePath);//You can get an inputStream using any IO API
            byte[] bytes;
            byte[] buffer = new byte[8192];
            int bytesRead;
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try {
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            bytes = output.toByteArray();
            String encodedString = Base64.encodeToString(bytes, Base64.DEFAULT);

            File image = new File(filePath);
            image.delete();

            return encodedString;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

    /*
    Pop up when an image is received
     */

    private void pictureReceivedPopUp() {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("You have received an image")
                .setMessage("Would you like to save the image?");

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                decodeImage(currentData);
            }
        });

        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /*
    Decode base64 String back to image and save image
    code adapted from: https://stackoverflow.com/a/7982964
     */
    private void decodeImage(String encodedImage) {
        byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "Receive_" + timeStamp + "_.jpg";

        File photo=new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), imageFileName);
        Log.d("Decoded", photo.toString());

        if (photo.exists()) {
            photo.delete();
        }

        try {
            FileOutputStream fos=new FileOutputStream(photo.getPath());

            fos.write(decodedString);
            fos.close();
        }
        catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    /*
    Send request to let server know that you want to help a classmate
     */
    private void sendHelp(final String ID_to_be_helped) {

        String url = "http://43.240.97.26:8000/webapp/help/";

        StringRequest postRequest = new StringRequest
                (Request.Method.POST, url, new Response.Listener<String>() {

                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject resp = new JSONObject(response);
                            if (resp.getString("status").equals("true")) {
                                takePicture(ID_to_be_helped);
                            }

                            if (resp.getString("status").equals("false")) {
                                Log.d("checkhelp", "help is false");
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError e) {
                        e.getStackTrace();
                        Toast error = Toast.makeText(MainActivity.this,
                                "Can't send help request to server", Toast.LENGTH_LONG);
                        error.show();
                    }
                })
        {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String>  headers = new HashMap<String, String>();
                headers.put("Content-Type", "application/x-www-form-urlencoded");

                return headers;
            }

            @Override
            public Map<String,String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("ID_to_be_helped", ID_to_be_helped);
                params.put("id", id);
                params.put("lectureID", currentLectureID);

                return params;
            }
        };

        queue.add(postRequest);
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void onSensorChanged(SensorEvent event) {
        // TODO Auto-generated method stub

        int sensorType = event.sensor.getType();
        // values[0]:X，values[1]：Y，values[2]：Z
        float[] values = event.values;
        //Log.d(TAG, "onSensorChanged: X:" + Math.abs(values[0]) + " Y: " + Math.abs(values[1]) + " Z: " + Math.abs(values[2]));
        if (sensorType == Sensor.TYPE_ACCELEROMETER) {
            float x = Math.abs(values[0]);
            float y = Math.abs(values[1]);
            float z = Math.abs(values[2]);
            if (x > 15 || y > 15 || z > 15){
                isShake = true;
                Toast.makeText(this, "You are shaking your phone", Toast.LENGTH_SHORT).show();

            }
        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub

    }

    // Send shaking notification to lecturer side webapp

    private void sendShake(String user, String lecture){
        final String userId = user;
        final String type = "question";
        final String lectureID = lecture;
        final String URLline = "http://43.240.97.26:8000/webapp/askhelp/";
        StringRequest stringRequest = new StringRequest(Request.Method.POST, URLline,
                new Response.Listener<String>(){
                    @Override
                    public void onResponse(String response) {
                        Toast.makeText(MainActivity.this, "Request has been received!", Toast.LENGTH_LONG).show();

                    }
                },
                new Response.ErrorListener(){
                    @Override
                    public void onErrorResponse(VolleyError error){
                        Toast.makeText(MainActivity.this, "Can't send request to ask question",Toast.LENGTH_LONG).show();
                        error.printStackTrace();
                    }
                }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/x-www-form-urlencoded");
                return headers;
            }

            @Override
            public Map<String, String> getParams(){
                Map<String, String> params = new HashMap<String, String>();
                params.put("id", userId);
                params.put("type", type);
                params.put("lectureID", lectureID);
                return params;
            }
        };
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);
    }

    // hy.d  --start
    @Override
    protected void onDestroy() {
        super.onDestroy();
        runFlag = false;
    }

    private void initData() {
        mRecorderBufferSize = AudioRecord.getMinBufferSize(mSampleRateInHZ, mChannelConfig, mAudioFormat);
        mAudioData = new byte[320];
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, mSampleRateInHZ, mChannelConfig, mAudioFormat, mRecorderBufferSize);
    }

    private void showToast(String content) {
        Toast.makeText(this, content, Toast.LENGTH_SHORT).show();
    }

    private File tmpFile;
    private File wavFile;
    void onClickStart() {
        showToast("start recoding");
        String tmpName = System.currentTimeMillis() + "_" + mSampleRateInHZ + "";
        tmpFile = createFile(tmpName + ".pcm");
        wavFile = createFile(tmpName + ".wav");

        isRecording = true;
        mAudioRecord.startRecording();
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    FileOutputStream outputStream = new FileOutputStream(tmpFile.getAbsoluteFile());
                    while (isRecording) {
                        int readSize = mAudioRecord.read(mAudioData, 0, mAudioData.length);
                        outputStream.write(mAudioData);
                    }
                    outputStream.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private File createFile(String name) {
        String dirPath = Environment.getExternalStorageDirectory().getPath() + "/AudioRecord/";
        File file = new File(dirPath);
        if (!file.exists()) {
            file.mkdirs();
        }
        String filePath = dirPath + name;
        File objFile = new File(filePath);
        if (!objFile.exists()) {
            try {
                objFile.createNewFile();
                return objFile;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * transform pcm file to wav file
     */
    private void pcmToWave(File inFileName, File outFileName) {
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long longSampleRate = mSampleRateInHZ;
        long totalDataLen = totalAudioLen + 36;
        int channels = 1;// sound channel
        long byteRate = 16 * longSampleRate * channels / 8;

        byte[] data = new byte[mRecorderBufferSize];
        try {
            in = new FileInputStream(inFileName);
            out = new FileOutputStream(outFileName);

            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;
            writeWaveFileHeader(out, totalAudioLen, totalDataLen, longSampleRate, channels, byteRate);
            while (in.read(data) != -1) {
                out.write(data);
            }

            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    
    //wave sturct
    private void writeWaveFileHeader(FileOutputStream out, long totalAudioLen, long totalDataLen, long longSampleRate, int channels, long byteRate) throws IOException {
        byte[] header = new byte[44];
        header[0] = 'R'; // RIFF
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);// data size
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';//WAVE
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        //FMT Chunk
        header[12] = 'f'; // 'fmt '
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        // PCM encode
        header[20] = 1; // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (1 * 16 / 8);
        header[33] = 0;
        header[34] = 16;
        header[35] = 0;
        //Data chunk
        header[36] = 'd';//data
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        out.write(header, 0, 44);
    }


    void onClickEnd() {
        isRecording = false;
        mAudioRecord.stop();
        try {
            pcmToWave(tmpFile, wavFile);
            NetWorkHelper.getInstance().upload(this, wavFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
