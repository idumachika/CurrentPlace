package com.example.user.currentplace;






import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;



import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBufferResponse;
import com.google.android.gms.location.places.Places;
import android.location.Location;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.jar.*;

public class CurrentPlaceActivity extends AppCompatActivity
        implements OnMapReadyCallback
{

    private static final String TAG = CurrentPlaceActivity.class.getSimpleName();
    private GoogleMap mMap;
    private CameraPosition mCameraPosition;





    //the entry point to the place Api.
    private GeoDataClient mGeoDataClient;
    private PlaceDetectionClient mPlaceDetectionClient;

    //the entry point to the Fused Location Provider.

    private FusedLocationProviderClient mFusedLocationProviderClient;

    private final LatLng mDefaultLocation = new LatLng(-33.8523341,151.2106085);
    private static final int DEFAULT_ZOOM =15;
    private static  final int PERMISSION_REQUEST_ACCESS_FINE_LOCATION =1;

    private  boolean mLocationPermissionGranted;

    //the geographical location where the device is currently located. that is , the last-know
    //location retrieved by the fused location provider.

    private Location mLastKnownLocation;

    //Key for storing activity state.
    private static  final String KEY_CAMERA_POSITION = "camera_position";
    private  static  final String KEY_LOCATION ="location";


    // used for selecting the current place.
    private static  final int M_MAX_ENTRIES = 5;
    private String[]mLikelyPlaceNames;
    private String[]mLikelyPlaceAddresses;
    private String[]mLikelyPlaceAttributions;
    private LatLng[]mLikelyPlaceLatLngs;










    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_current_place);







        //Retrieve location and camera position from saved instance state.
        if(savedInstanceState !=null){
            mLastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            mCameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }

        //Construct a GeoDataClient.
        mGeoDataClient = Places.getGeoDataClient(this, null);

        //construct a PlaceDetectionClient.
        mPlaceDetectionClient = Places.getPlaceDetectionClient(this, null);

        //Construct a FusedLocationProviderClient.

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        //Build the map.
        SupportMapFragment mapFragment =(SupportMapFragment)getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);



    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.current_place_menu, menu);
        return true;

    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.option_get_place) {
            showCurrentPlace();
        }
        return true;
    }


    // save the state of the map when the activity is paused.
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mMap != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, mMap.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, mLastKnownLocation);
            super.onSaveInstanceState(outState);
        }
    }



        // manipulate the map when it's available.
    //this callback is triggered when the map is ready to be used.

    @Override
    public void onMapReady(GoogleMap map) {

        mMap = map;

        // use a custom info window adapter to handle multiple lines of text in  the
        // info window contents,
        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter(){

            @Override
            // return null here, so that getInfoContent() is called next.

            public View getInfoWindow(Marker argO){

                return null;
            }

            @Override
            public  View getInfoContents(Marker marker){
                // Inflate the layout for the info window, title and snippet.
                View infoWindow = getLayoutInflater().inflate(R.layout.custom_row,(FrameLayout)findViewById(R.id.map),false);

                TextView title =((TextView) infoWindow.findViewById(R.id.title));
                title.setText(marker.getTitle());

                TextView snippet = ((TextView) infoWindow.findViewById(R.id.snippet));
                snippet.setText(marker.getSnippet());


                        return infoWindow;


            }


        });

        // prompt the user for permission.

        getLocationPermission();

        // Turn on the my Location Layer and the related control on the Map

        updateLocationUI();

        // Get the current location of the device, and the positions the map's camera,

        getDeviceLocation();


    }
    /*Gets the current location of the device, and position the Map's camera.*/

    private  void getDeviceLocation(){

        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */

        try{
            if (mLocationPermissionGranted){

                Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        try{
                        if (task.isSuccessful()){
                            //Set the map Camera position to the current location of the device.
                            mLastKnownLocation = task.getResult();
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLastKnownLocation.getLatitude(),
                                    mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                        }else {
                            Log.d(TAG,"Current location is null.using defaults.");
                            Log.e(TAG,"Exception: %s",task.getException());

                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mDefaultLocation,DEFAULT_ZOOM));
                            mMap.getUiSettings().setMyLocationButtonEnabled(false);

                        }
                        }catch (Exception e){
                            e.getMessage();
                        }


                    }
                });


            }



        }catch(SecurityException e){
            Log.e("Exception: %s",e.getMessage());
        }
    }

    /**
     * Prompts the user for permission to use the device location.
     */

    private void getLocationPermission(){

         /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */


            if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                 android.Manifest.permission.ACCESS_FINE_LOCATION)
                 == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionGranted = true;

         }else{
                ActivityCompat.requestPermissions(this,
                     new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                     PERMISSION_REQUEST_ACCESS_FINE_LOCATION);

         }

    }
    public void onRequestPermissionsResult(int requestCode,
                                          @NonNull String permissions[],
                                          @NonNull int[] grantResults){
        mLocationPermissionGranted = false;
        switch (requestCode){
            case PERMISSION_REQUEST_ACCESS_FINE_LOCATION:{
                //if request is cancelled, the result array are empty.
                if (grantResults.length > 0
                        && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    mLocationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();

    }

    /**
     *Prompts the user to select the current place from a list of likely places, and shows the
     * current place on the map - provided the user has granted location permission.
     */
    private void showCurrentPlace(){

        if (mMap == null){
            return;
        }

        if (mLocationPermissionGranted){

            @SuppressWarnings("MissingPermission")
            final Task<PlaceLikelihoodBufferResponse>placeResult =
                    mPlaceDetectionClient.getCurrentPlace(null);
            placeResult.addOnCompleteListener(new OnCompleteListener<PlaceLikelihoodBufferResponse>() {
                @Override
                public void onComplete(@NonNull Task<PlaceLikelihoodBufferResponse> task) {
                    if (task.isSuccessful()&& task.getResult() !=null){
                        PlaceLikelihoodBufferResponse likelyPlaces =task.getResult();

                        // Set the count, handling cases where less than 5 entries are returned.

                        int count;
                        if (likelyPlaces.getCount() < M_MAX_ENTRIES){
                            count = likelyPlaces.getCount();
                        }else{
                            count = M_MAX_ENTRIES;
                        }

                        int i = 0;
                        mLikelyPlaceNames =new String[count];
                        mLikelyPlaceAddresses = new String[count];
                        mLikelyPlaceAttributions = new String[count];
                        mLikelyPlaceLatLngs = new LatLng[count];

                        for(PlaceLikelihood placeLikelihood : likelyPlaces){
                            //Build a list of likely place to show the user.
                            mLikelyPlaceNames[i] = (String)placeLikelihood.getPlace().getName();
                            mLikelyPlaceAddresses[i] =(String)placeLikelihood.getPlace().getAddress();
                            mLikelyPlaceAttributions[i]=(String)placeLikelihood.getPlace().getAttributions();
                            mLikelyPlaceLatLngs [i]=placeLikelihood.getPlace().getLatLng();

                            i++;
                            if (i > (count - 1)){
                                break;
                            }
                        }
                        likelyPlaces.release();

                        openPlacesDialog();
                    }else {
                        Log.e(TAG,"Exception: %s",task.getException());
                    }

                }
            });


        }else {
            Log.i(TAG,"the user did not grant location permission.");
            // Add a default marker, because the user hasn't selected a place.

            mMap.addMarker(new MarkerOptions()
            .title(getString(R.string.default_info_title))
            .position(mDefaultLocation)
            .snippet(getString(R.string.default_info_snippet)));

            // Prompt the user for permission.

            getLocationPermission();
        }
    }

    private void openPlacesDialog(){
        // Ask the user to choose the place where they are now.

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // The "which" argument contains the position of the selected item.

                LatLng marKerLatLng = mLikelyPlaceLatLngs[which];
                String markerSnippet = mLikelyPlaceAddresses[which];
                if (mLikelyPlaceAttributions[which] !=null){
                    markerSnippet = markerSnippet + "\n" + mLikelyPlaceAttributions[which];
                }

                // Add a marker for the selected place, with an info window
                // showing information about that place.
                mMap.addMarker(new MarkerOptions().title(mLikelyPlaceNames[which])
                .position(marKerLatLng)
                .snippet(markerSnippet));

                // Position the map's camera at the location of the marker.
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(marKerLatLng,DEFAULT_ZOOM));





            }
        };

        // Display the dialog.
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.pick_place)
                .setItems(mLikelyPlaceNames, listener)
                .show();



    }


    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
            if (mLocationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                mLastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }












}
