package com.example.mobile;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;

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
import androidx.core.content.ContextCompat;

import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_REQUEST_CODE = 1;

    private FusedLocationProviderClient client;
    private boolean locationPermissionGranted;
    LocationCallback locationCallback;
    LocationRequest request;
    private RequestQueue queue;

    private Button pictureButton;

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

        // If picture button is clicked, show picture pop up
        pictureButton = findViewById(R.id.picture);
        pictureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        picturePopUp();
                    }
                }
        );

        client = LocationServices.getFusedLocationProviderClient(this);
        locationPermissionGranted = false;
        queue = Volley.newRequestQueue(this);

        // Get location permission
        getLocationPermission();

        // checks location periodically and sends "checkin"
        checkLocation();
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
    private void getLocationPermission() {

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            locationPermissionGranted = true;

        } else {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }
    }

    /*
    Handles the result of the permission request
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case LOCATION_REQUEST_CODE: {

                // if permission is not granted
                if (grantResults.length > 0
                        && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast exitAppToast = Toast.makeText(MainActivity.this,
                            "Need to grant location permissions to use app", Toast.LENGTH_LONG);
                    exitAppToast.show();
                    finish();
                }
                return;
            }
        }
    }

    /*
    Requests location and sends "checkin" to server
     */
    private void checkLocation() {

        request = LocationRequest.create();
        request.setInterval(10000);
        request.setFastestInterval(5000);
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        try {
            if (locationPermissionGranted == true) {
                client.requestLocationUpdates(request, locationCallback = new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        Location location = locationResult.getLastLocation();
                        if (location != null) {
                            // CHECK LOCATION AND SEND "checkin"
                            TextView textView = findViewById(R.id.location);
                            textView.setText("latitude: " + location.getLatitude() +
                                    "\n longitude: " + location.getLongitude());
                        }
                    }
                }, null);
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    /*
     Pop up to request a picture
     */
    private void picturePopUp() {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Request a picture")
                .setMessage("Would you like to request a picture of the whiteboard?");

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // SEND "askhelp"
                String url = "GET URL";

                Map<String, String> params = new HashMap<>();
                params.put("id", "1"); // ADD ID OF USER
                params.put("type", "ask_picture");
                JSONObject jsonParams = new JSONObject(params);

                JsonObjectRequest postRequest = new JsonObjectRequest
                        (Request.Method.POST, url, jsonParams, new Response.Listener<JSONObject>() {

                            @Override
                            public void onResponse(JSONObject response) {
                                // RECEIVE JSON OBJECT
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
                        });

                queue.add(postRequest);
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
