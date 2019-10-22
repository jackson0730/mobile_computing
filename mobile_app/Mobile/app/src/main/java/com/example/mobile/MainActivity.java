package com.example.mobile;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
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
import android.widget.ImageView;
import android.widget.TextView;
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

public class MainActivity extends AppCompatActivity {

    private static final int ALL_PERMISSIONS_CODE = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 100;

    private FusedLocationProviderClient client;
    private boolean permissionsGranted;
    LocationCallback locationCallback;
    LocationRequest request;
    private RequestQueue queue;
    private String currentPhotoPath;
    private String encodedImage;
    private JSONArray lectures;

    private Button pictureRequestButton;
    private Button helpRequestButton;
    private ImageView picture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

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

        // If help request button is clicked (button only used for testing), show help request pop up
        helpRequestButton = findViewById(R.id.helpRequest);
        helpRequestButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        helpRequestPopUp();
                    }
                }
        );

        picture = findViewById(R.id.picture);

        client = LocationServices.getFusedLocationProviderClient(this);
        permissionsGranted = false;
        queue = Volley.newRequestQueue(this);

        // Get permissions
        getPermissions();

        // checks location periodically and sends "checkin"
        client.getLastLocation();
        checkAttendance();
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

        if (checkPermissions(this.getApplicationContext(), permissions) == true) {

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
                        && checkGrantResults(grantResults) == true) {
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
    Requests location and sends "checkin" to server
     */
    private void checkAttendance() {

        request = LocationRequest.create();
        request.setInterval(10000);
        request.setFastestInterval(5000);
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        try {
            if (permissionsGranted == true) {
                client.requestLocationUpdates(request, locationCallback = new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        Location location = locationResult.getLastLocation();
                        if (location != null) {
                            // CHECK LOCATION AND SEND "checkin"
                            TextView textView = findViewById(R.id.location);
                            textView.setText("latitude: " + location.getLatitude() +
                                    "\n longitude: " + location.getLongitude());
/*
                            for (int i = 0; i < lectures.length(); i++) {
                                try {
                                    JSONObject lecture = lectures.getJSONObject(i);

                                    if (checkTime(lecture) && checkLocation(lecture, location)) {

                                        String url = "http://43.240.97.26:8000/webapp/checkin";

                                        Map<String, String> params = new HashMap<>();
                                        params.put("id", "1"); // ADD ID OF USER
                                        params.put("lectureID", lecture.getString("lectureID"));
                                        JSONObject jsonParams = new JSONObject(params);

                                        JsonObjectRequest postRequest = new JsonObjectRequest
                                                (Request.Method.POST, url, jsonParams, new Response.Listener<JSONObject>() {

                                                    @Override
                                                    public void onResponse(JSONObject response) {
                                                        Toast receive = Toast.makeText(MainActivity.this,
                                                                "checkin has been received!", Toast.LENGTH_LONG);
                                                        receive.show();
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
                                        };
                                        queue.add(postRequest);
                                    }

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }*/
                        }
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
            Date currentDate = new Date();
            Date startDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").
                    parse(lecture.getString("dateTime"));
            Calendar cal = Calendar.getInstance();
            cal.setTime(startDate);
            cal.add(Calendar.MINUTE, 30);
            Date endDate = cal.getTime();

            if (currentDate.after(startDate) && currentDate.before(endDate)) {
                return true;
            }

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (ParseException e){
            e.printStackTrace();
        }
        return false;
    }

    /*
     Pop up to request a picture
     */
    private void pictureRequestPopUp() {

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

    /*
    Pop up to ask user if they would like to help a classmate by taking a picture
     */

    private void helpRequestPopUp() {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("A classmate is requesting help")
                .setMessage("Would you like to help a classmate by taking a picture of the whiteboard?");

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                takePicture();
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
    send ask_help request with type ask_picture
     */
    private void pictureRequest() {

        String url = "http://43.240.97.26:8000/webapp/askhelp";

        Map<String, String> params = new HashMap<>();
        params.put("id", "1"); // ADD ID OF USER
        params.put("type", "ask_picture");
        JSONObject jsonParams = new JSONObject(params);

        JsonObjectRequest postRequest = new JsonObjectRequest
                (Request.Method.POST, url, jsonParams, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        Toast receive = Toast.makeText(MainActivity.this,
                                "Request has been received!", Toast.LENGTH_LONG);
                        receive.show();
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError e) {
                        e.getStackTrace();
                        Toast error = Toast.makeText(MainActivity.this,
                                "Can't send request to get picture", Toast.LENGTH_LONG);
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
        };
            queue.add(postRequest);
    }

    /*
    Accesses camera app to take a picture and sends upload with type picture
     */
    private void takePicture() {

        if (this.getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) == false) {
            Toast error = Toast.makeText(MainActivity.this,
                    "Device has no camera!", Toast.LENGTH_LONG);
            error.show();
            return;
        }

        try {
            if (permissionsGranted == true) {
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
    code from: https://developer.android.com/training/camera/photobasics
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
    code from: https://developer.android.com/training/camera/photobasics
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

            String url = "http://43.240.97.26:8000/webapp/upload";

            Map<String, String> params = new HashMap<>();
            params.put("ID_to_be_helped", "2"); // ADD ID OF USER TO BE HELPED
            params.put("id", "1"); // ADD ID OF USER
            params.put("type", "picture");
            params.put("data", encodedImage);
            JSONObject jsonParams = new JSONObject(params);

            JsonObjectRequest postRequest = new JsonObjectRequest
                    (Request.Method.POST, url, jsonParams, new Response.Listener<JSONObject>() {

                        @Override
                        public void onResponse(JSONObject response) {
                            Toast receive = Toast.makeText(MainActivity.this,
                                    "Photo has been received by server!", Toast.LENGTH_LONG);
                            receive.show();
                        }
                    }, new Response.ErrorListener() {

                        @Override
                        public void onErrorResponse(VolleyError e) {
                            e.getStackTrace();
                            Toast error = Toast.makeText(MainActivity.this,
                                    "Can't send photo to server", Toast.LENGTH_LONG);
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
            };

            queue.add(postRequest);
        }
    }

    /*
    Encode image using base64
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

        return encodedString;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

    /*
    Decode base64 String back to image
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
            Log.e("PictureDemo", "Exception in photoCallback", e);
        }
    }
}
