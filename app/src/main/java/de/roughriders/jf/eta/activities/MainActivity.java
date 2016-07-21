package de.roughriders.jf.eta.activities;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.AutocompletePredictionBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.maps.android.SphericalUtil;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import de.roughriders.jf.eta.R;
import de.roughriders.jf.eta.adapters.PredictionsAdapter;
import de.roughriders.jf.eta.adapters.RecentDestinationsAdapter;
import de.roughriders.jf.eta.adapters.RecentTripsAdapter;
import de.roughriders.jf.eta.helpers.IRecyclerViewItemClicked;
import de.roughriders.jf.eta.models.Contact;
import de.roughriders.jf.eta.models.RecentDestination;
import de.roughriders.jf.eta.models.RecentTrip;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {

    private SlidingUpPanelLayout slidingPanel;
    private static String TAG = "MainActivity";

    private EditText destinationSearchBox;
    private EditText targetPhoneBox;
    private Button startButton;
    private boolean ignoreNextAddressBoxChange = false;

    private final int PICK_CONTACT = 0;
    private final int PICK_ADDRESS = 1;
    private final int REQUEST_LOCATION_PERMISSION_KEY = 1;
    private final int SEARCH_RADIUS = 250;

    private String targetPhoneNumber;
    private String targetDestination;
    private Contact currentContact;
    private RecentDestination currentDestination;

    private RecyclerView predictionsView;
    private CardView predictionsEmptyCardView;
    private CardView predictionsCardView;
    private PredictionsAdapter predictionsAdapter;

    private RecyclerView recentDestinationsView;
    private CardView recentDestinationsCardView;
    private CardView recentDestinationsEmptyCardView;
    private RecentDestinationsAdapter recentDestinationsAdapter;

    private RecyclerView recentTripsView;
    private CardView recentTripsCardView;
    private CardView noRecentTripsCardView;
    private RecentTripsAdapter recentTripsAdapter;

    private GoogleApiClient googleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        targetPhoneBox = (EditText)findViewById(R.id.editTextTargetPhone);
        startButton = (Button)findViewById(R.id.startButton);
        initDestinationEditText();
        initSlidingPanel();
        connectToApiClient();
        initPredictionsView();
        initRecentDestinationsView();
        initRecentTripsView();
    }

    @Override
    public void onStart(){
        super.onStart();
        askOrCheckForLocationPermission();
    }

    @Override
    public void onResume(){
        super.onResume();
    }

    private void askOrCheckForLocationPermission(){
        if(!wasLocationPermissionGranted())
            showLocationPermissionExplanationAndAsk();
    }

    private boolean wasLocationPermissionGranted(){
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
    private void showLocationPermissionExplanationAndAsk(){
        new AlertDialog.Builder(this)
                .setTitle("ETA")
                .setMessage("The app needs to access your location in order to compute the time it takes to reach your destination. The app is useless if you do not grant this permission.")
                .setCancelable(false)
                .setPositiveButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        askForLocationPermission();
                    }
                }).show();
    }

    private void askForLocationPermission(){
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION_KEY);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode){
            case REQUEST_LOCATION_PERMISSION_KEY:{
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    // permission granted, yeah!
                }
                else {
                    finish();
                }
            }
        }
    }

    private void initPredictionsView(){
        predictionsCardView = (CardView)findViewById(R.id.sliding_layout_search_result_card);
        predictionsEmptyCardView = (CardView)findViewById(R.id.sliding_layout_no_search_results_container);
        predictionsView = (RecyclerView)findViewById(R.id.predictionsList);
        RecyclerView.LayoutManager predictionsLayoutManager = new LinearLayoutManager(this);
        predictionsView.setLayoutManager(predictionsLayoutManager);
        predictionsAdapter = new PredictionsAdapter();
        predictionsView.setAdapter(predictionsAdapter);
        predictionsAdapter.addOnItemclickedListener(new IRecyclerViewItemClicked<RecentDestination>() {
            @Override
            public void onItemclicked(RecentDestination item) {
                Log.i(TAG, "A RecyclerView item has been clicked: " + item.toString());
                onDestinationSelected(item);
            }
        });
    }

    private void initRecentDestinationsView(){
        recentDestinationsCardView = (CardView)findViewById(R.id.sliding_layout_recent_destinations_card);
        recentDestinationsEmptyCardView = (CardView)findViewById(R.id.sliding_layout_no_recent_destinations_card);
        recentDestinationsView = (RecyclerView)findViewById(R.id.recentDestinations);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recentDestinationsView.setLayoutManager(layoutManager);
        recentDestinationsAdapter = new RecentDestinationsAdapter(this);
        recentDestinationsView.setAdapter(recentDestinationsAdapter);
        if(recentDestinationsAdapter.size() > 0){
            recentDestinationsCardView.setVisibility(View.VISIBLE);
            recentDestinationsEmptyCardView.setVisibility(View.GONE);
        }
        else{
            recentDestinationsCardView.setVisibility(View.GONE);
            recentDestinationsEmptyCardView.setVisibility(View.VISIBLE);
        }
        recentDestinationsAdapter.addOnItemclickedListener(new IRecyclerViewItemClicked<RecentDestination>() {
            @Override
            public void onItemclicked(RecentDestination item) {
                Log.i(TAG, "A RecyclerView item has been selected: " + item.toString());
                onDestinationSelected(item);
            }
        });
    }

    private void initRecentTripsView(){
        recentTripsView = (RecyclerView)findViewById(R.id.main_activity_recent_trips_list);
        recentTripsAdapter = new RecentTripsAdapter(this);
        noRecentTripsCardView = (CardView) findViewById(R.id.main_activity_no_recent_trips_card_view);
        recentTripsCardView = (CardView)findViewById(R.id.main_activity_recent_trips_card_view);
        recentTripsView.setLayoutManager(new LinearLayoutManager(this));
        recentTripsView.setAdapter(recentTripsAdapter);
        if(recentTripsAdapter.size() > 0){
            recentTripsCardView.setVisibility(View.VISIBLE);
            noRecentTripsCardView.setVisibility(View.GONE);
        }
        else{
            recentTripsCardView.setVisibility(View.GONE);
            noRecentTripsCardView.setVisibility(View.VISIBLE);
        }
        recentTripsAdapter.addOnItemclickedListener(new IRecyclerViewItemClicked<RecentTrip>() {
            @Override
            public void onItemclicked(RecentTrip item) {
                Log.i(TAG, "A RecyclerView item has been selected: " + item.toString());
                currentContact = item.contact;
                targetPhoneNumber = currentContact.phone;
                targetPhoneBox.setText(targetPhoneNumber);
                onDestinationSelected(item.destination);
                /*
                currentDestination = item.destination;
                targetDestination = currentDestination.primaryText + " " + currentDestination.secondaryText;
                destinationSearchBox.setText(targetDestination);
                */
            }
        });
    }

    private void initDestinationEditText(){
        destinationSearchBox = ((EditText)findViewById(R.id.editTextDestination));
        setDestinationEditTextFocusListener();
        setDestinationEditTextChangeListener();
    }

    private void setDestinationEditTextChangeListener(){
        destinationSearchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // used to ignore setting the text from code
                if(ignoreNextAddressBoxChange){
                    ignoreNextAddressBoxChange = false;
                    return;
                }
                String text = charSequence.toString();
                Log.i(TAG, "Text in destionationSearchBox has changed, new content: " + text);
                sendAutocompleteRequest(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) { }
        });
    }
    private void setDestinationEditTextFocusListener(){
        destinationSearchBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                slidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
            }
        });
        destinationSearchBox.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
               if(b == true)
                   slidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
               else slidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
            }
        });
    }

    private void initSlidingPanel(){
        slidingPanel = (SlidingUpPanelLayout)findViewById(R.id.sliding_layout);
        slidingPanel.setTouchEnabled(false);
        int[] coords = new int[2];
        destinationSearchBox.getLocationInWindow(coords);
        Log.i(TAG, String.valueOf(coords[0]));
        Log.i(TAG, String.valueOf(coords[1]));
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
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (slidingPanel != null &&
                (slidingPanel.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED || slidingPanel.getPanelState() == SlidingUpPanelLayout.PanelState.ANCHORED)) {
            slidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        } else {
            super.onBackPressed();
        }
    }

    public void selectContactForAddressButton(View view){
        showSelectContactForAddressPicker();
    }

    private void showSelectContactForAddressPicker(){
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        intent.setType(ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_TYPE);
        startActivityForResult(intent, PICK_ADDRESS);
    }

    public void selectContactButton_Clicked(View view){
       showSelectContactPicker();
    }

    private void showSelectContactPicker(){
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
        startActivityForResult(intent, PICK_CONTACT);
    }

    public void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);
        switch (reqCode) {
            case PICK_CONTACT:
                processContactIntent(resultCode, data);
                break;
            case PICK_ADDRESS:
                processAddresIntent(resultCode, data);
                break;
        }
    }

    private void processContactIntent(int resultCode, Intent intent){
        if(resultCode == Activity.RESULT_OK)
        {
            Uri uri = intent.getData();
            String[] projection = { ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME };

            Cursor cursor = getContentResolver().query(uri, projection,
                    null, null, null);
            cursor.moveToFirst();

            int numberColumnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            String number = cursor.getString(numberColumnIndex);

            int nameColumnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
            String name = cursor.getString(nameColumnIndex);

            currentContact = new Contact(name, number);

            Log.i(TAG, "Contact selected: name: " + name + " - phone: " + number);

            targetPhoneNumber = number;

            updateUi();
        }
    }

    private void processAddresIntent(int resultCode, Intent intent){
        if(resultCode == Activity.RESULT_OK)
        {
            Uri uri = intent.getData();
            String[] projection = { ContactsContract.CommonDataKinds.StructuredPostal.CITY, ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE, ContactsContract.CommonDataKinds.StructuredPostal.STREET};

            Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
            cursor.moveToFirst();

            int streetColumnIndex = cursor.getColumnIndex(projection[2]);

            String street = cursor.getString(streetColumnIndex);

            currentDestination = new RecentDestination(street, "", "-1");
            targetDestination = street;
            updateUi();
        }
    }

    /**
     * Initiates a connection to the Google API Client (needed for the places autocomplete).
     */
    private void connectToApiClient(){
        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .build();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Toast.makeText(this, "Connection to the Google API Client could not be established. Are you online?", Toast.LENGTH_SHORT).show();
    }

    private void sendAutocompleteRequest(String query){
        try {
            AutocompleteFilter filter = new AutocompleteFilter.Builder().setTypeFilter(AutocompleteFilter.TYPE_FILTER_NONE).build();
            LatLngBounds bounds = createBoundsFromLastKnownLocation();
            PendingResult<AutocompletePredictionBuffer> result = Places.GeoDataApi.getAutocompletePredictions(googleApiClient, query, bounds, filter);
            result.setResultCallback(new ResultCallback<AutocompletePredictionBuffer>() {
                @Override
                public void onResult(@NonNull AutocompletePredictionBuffer autocompletePredictions) {
                    String statusMessage = autocompletePredictions.getStatus().getStatusMessage();
                    Log.i(TAG, "Status of the predictions:\r\n" + statusMessage);
                    Log.i(TAG, "Received " + autocompletePredictions.getCount() + " predictions.");
                    Log.i(TAG, statusMessage);

                    findViewById(R.id.sliding_layout_search_result_card).setVisibility(View.VISIBLE);
                    predictionsAdapter.clear();

                    if(autocompletePredictions.getCount() == 0) {
                        predictionsCardView.setVisibility(View.GONE);
                        predictionsEmptyCardView.setVisibility(View.VISIBLE);
                    }
                    else {
                        for (int i = 0; i < autocompletePredictions.getCount(); i++) {
                            AutocompletePrediction prediction = autocompletePredictions.get(i);
                            Log.i(TAG, prediction.getPrimaryText(null).toString() + " / " + prediction.getSecondaryText(null).toString());
                            predictionsAdapter.addItem(prediction);
                        }
                        predictionsCardView.setVisibility(View.VISIBLE);
                        predictionsEmptyCardView.setVisibility(View.GONE);
                    }

                    autocompletePredictions.release();
                }
            });
        }
        catch(NullPointerException ex){
            Log.e(TAG, ex.getMessage());
        }
    }

    private LatLngBounds createBoundsFromLastKnownLocation(){
        Location location = getLastKnownLocation();
        if(location != null) {
            Log.i(TAG, "last known location: " + location.getLatitude() + " - " + location.getLongitude());
            LatLng center = new LatLng(location.getLatitude(), location.getLongitude());
            LatLng southWest = SphericalUtil.computeOffset(center, SEARCH_RADIUS * 1000 * Math.sqrt(2), 225);
            LatLng northEast = SphericalUtil.computeOffset(center, SEARCH_RADIUS * 1000 * Math.sqrt(2), 45);
            return new LatLngBounds(southWest, northEast);
        } else {
            throw new NullPointerException("getLastKnownLocation did not give any result");
        }
    }

    private Location getLastKnownLocation(){
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if(permissionCheck == PackageManager.PERMISSION_GRANTED) {
            Location location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
            return location;
        }
        else {
            Toast.makeText(this, "The app is not allowed to access your location.", Toast.LENGTH_SHORT).show();
            throw new IllegalStateException("App cannot run without location permission.");
        }
    }

    private void onDestinationSelected(RecentDestination destination){
        slidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
        destinationSearchBox.setText(destination.primaryText + " " + destination.secondaryText);
        currentDestination = destination;
        recentDestinationsAdapter.addItem(destination);
        recentDestinationsCardView.setVisibility(View.VISIBLE);
        recentDestinationsEmptyCardView.setVisibility(View.GONE);
        RecentDestination.saveToSharedPreferences(recentDestinationsAdapter.getItems(), this);
    }

    public void clearSearchEditText_onClick(View view){
        destinationSearchBox.setText("");
    }

    public void startButton_Clicked(View view) {
        updateFromUi();
        if(!targetDestination.isEmpty() && !targetPhoneNumber.isEmpty()) {
            saveCurrentTrip();
            startTrip();
        }
        else
            Toast.makeText(this, "You have to enter a phone number and a destination to start a trip.", Toast.LENGTH_SHORT).show();
    }

    private void saveCurrentTrip() {
        recentTripsAdapter.addItem(new RecentTrip(System.currentTimeMillis(), currentDestination, currentContact));
        RecentTrip.saveToSharedPreferences(recentTripsAdapter.getItems(), this);
    }

    public void startTrip(){
        Intent intent = new Intent(this, TripActivity.class);
        intent.putExtra(TripActivity.DESTINATION_EXTRA, targetDestination);
        intent.putExtra(TripActivity.PHONE_NUMBER_EXTRA, targetPhoneNumber);
        if(currentContact != null)
            intent.putExtra(TripActivity.NAME_EXTRA, currentContact.name);
        startActivity(intent);
    }

    /**
     * Updates the ui with the values stored in the local variables.
     */
    private void updateUi(){
        targetPhoneBox.setText(targetPhoneNumber);
        ignoreNextAddressBoxChange = true;
        destinationSearchBox.setText(targetDestination);

        setControlEnable();
    }

    /**
     * Pulls values from the ui and stores them in the corresponding variables.
     */
    private void updateFromUi(){
        targetPhoneNumber = targetPhoneBox.getText().toString();
        targetDestination = destinationSearchBox.getText().toString();
    }

    /**
     * Checks the conditions for all controls to be enabled or not.
     */
    private void setControlEnable(){
        setStartButtonEnabled();
    }

    /**
     * Enable or disable the start-button.
     */
    private void setStartButtonEnabled(){
        startButton.setEnabled(isValidStart());
    }

    /**
     * Check the conditions for the start button to be enabled.
     * @return
     */
    private boolean isValidStart(){
        if(targetDestination != null && targetDestination.isEmpty())
            return false;
        if(targetPhoneNumber != null && targetPhoneNumber.isEmpty())
            return false;
        return true;
    }
}
