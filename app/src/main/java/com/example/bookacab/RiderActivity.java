package com.example.bookacab;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.List;

public class RiderActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    LocationManager locationManager;
    LocationListener locationListener;
    Button callCabButton;
    Boolean requestActive = false;
    Handler handler = new Handler();
    TextView infoTextView;

    public void checkForUpdates(){
        final ParseQuery<ParseObject> query = ParseQuery.getQuery("Request");
        query.whereEqualTo("username",ParseUser.getCurrentUser().getUsername());
        query.whereExists("driverUsername");
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if(e==null&&objects.size()>0){

                    ParseQuery<ParseUser> query1 = ParseUser.getQuery();
                    query1.whereEqualTo("username",objects.get(0).getString("driverUsername"));
                    query.findInBackground(new FindCallback<ParseObject>() {
                        @Override
                        public void done(List<ParseObject> objects, ParseException e) {
                            if(e == null&&objects.size()>0){
                                ParseGeoPoint driverLocation = objects.get(0).getParseGeoPoint("location");

                                if (ActivityCompat.checkSelfPermission(RiderActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ) {
                                    Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                                    if(lastKnownLocation!=null){
                                        ParseGeoPoint userLocation = new ParseGeoPoint(lastKnownLocation.getLatitude(),lastKnownLocation.getLongitude());

                                        Double distance = userLocation.distanceInKilometersTo(driverLocation);
                                        if(distance<0.1){
                                            infoTextView.setText("Your driver is here");
                                            mMap.clear();

                                            LatLng driverLocationLatLng = new LatLng(driverLocation.getLatitude(),driverLocation.getLongitude());
                                            mMap.addMarker(new MarkerOptions().position(driverLocationLatLng).title("Your Location"));


                                            LatLng riderLocation = new LatLng(userLocation.getLatitude(),userLocation.getLongitude());
                                            mMap.addMarker(new MarkerOptions().position(riderLocation).title("Driver Location").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                                            handler.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    callCabButton.setVisibility(View.VISIBLE);
                                                    callCabButton.setText("Call a Cab");
                                                    infoTextView.setText("");
                                                    mMap.clear();
                                                    requestActive = false;

                                                    ParseQuery<ParseObject> query2 = ParseQuery.getQuery("Request");
                                                    query2.whereEqualTo("username",ParseUser.getCurrentUser().getUsername());
                                                    query2.findInBackground(new FindCallback<ParseObject>() {
                                                        @Override
                                                        public void done(List<ParseObject> objects, ParseException e) {
                                                            if(e==null){
                                                                for(ParseObject object : objects){
                                                                    object.deleteInBackground();
                                                                }
                                                            }
                                                        }
                                                    });
                                                }
                                            }, 5000);
                                        }else{
                                            Double distanceOneDP = (double) Math.round(distance * 10) / 10;
                                            infoTextView.setText("Your driver is "+distanceOneDP+" k.m. away!");
                                            mMap.clear();

                                            LatLng driverLocationLatLng = new LatLng(driverLocation.getLatitude(),driverLocation.getLongitude());
                                            mMap.addMarker(new MarkerOptions().position(driverLocationLatLng).title("Your Location"));


                                            LatLng riderLocation = new LatLng(userLocation.getLatitude(),userLocation.getLongitude());
                                            mMap.addMarker(new MarkerOptions().position(riderLocation).title("Driver Location").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

                                            LatLngBounds.Builder builder = new LatLngBounds.Builder();
                                            builder.include(driverLocationLatLng);
                                            builder.include(riderLocation);

                                            LatLngBounds bounds = builder.build();
                                            int padding = 60;

                                            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds,padding);

                                            mMap.animateCamera(cameraUpdate);
                                            handler.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    checkForUpdates();
                                                }
                                            }, 2000);
                                        }
                                    }
                                }
                            }
                        }
                    });

                    infoTextView.setText("Your driver is on the way!");
                    callCabButton.setVisibility(View.INVISIBLE);
                }else{
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            checkForUpdates();
                        }
                    }, 2000);
                }
            }
        });
    }

    public void logout(View view){
        ParseUser.logOut();
        Intent intent = new Intent(this,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    public void callCab(View view){
        Log.i("Info","Searching for Cab");

        if(requestActive){
            final ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Request");
            query.whereEqualTo("username",ParseUser.getCurrentUser().getUsername());
            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    if(e==null){

                        for(ParseObject object : objects){
                            object.deleteInBackground();
                        }
                        requestActive = false;
                        callCabButton.setText("Call a cab");
                    }
                }
            });
        }else{
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                if(lastKnownLocation !=null){
                    ParseObject request = new ParseObject("Request");
                    request.put("username", ParseUser.getCurrentUser().getUsername());

                    ParseGeoPoint parseGeoPoint = new ParseGeoPoint(lastKnownLocation.getLatitude(),lastKnownLocation.getLongitude());

                    request.put("location",parseGeoPoint);

                    request.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if(e == null){
                                requestActive=true;
                                callCabButton.setText("Cancel Search");
                                checkForUpdates();
                            }
                        }
                    });
                }else {
                    Toast.makeText(this, "Could not find Location", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                    Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                    if(lastKnownLocation!=null){
                        LatLng userLocation = new LatLng(lastKnownLocation.getLatitude(),lastKnownLocation.getLongitude());

                        mMap.clear();
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation,15));
                        mMap.addMarker(new MarkerOptions().position(userLocation).title("Your Location"));
                    }
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        callCabButton = findViewById(R.id.callCabButton);
        infoTextView = findViewById(R.id.infoTextView);
        ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Request");
        query.whereEqualTo("username",ParseUser.getCurrentUser().toString());
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if(e==null&&objects.size()>0){
                    requestActive = true;
                    callCabButton.setText("Cancel Search");
                    checkForUpdates();
                }
            }
        });
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                LatLng userLocation = new LatLng(location.getLatitude(),location.getLongitude());

                if(!requestActive){
                    mMap.clear();
                    //mMap.moveCamera(CameraUpdateFactory.newLatLng(userLocation));
                    mMap.addMarker(new MarkerOptions().position(userLocation).title("Your Location"));

                }

            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }
        };


        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
        }else{
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,locationListener);

            Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            if(lastKnownLocation!=null){
                LatLng userLocation = new LatLng(lastKnownLocation.getLatitude(),lastKnownLocation.getLongitude());

                mMap.clear();
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation,15));
                mMap.addMarker(new MarkerOptions().position(userLocation).title("Your Location"));
            }

        }


    }
}