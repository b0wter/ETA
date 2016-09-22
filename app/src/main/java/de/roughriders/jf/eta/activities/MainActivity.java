package de.roughriders.jf.eta.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.databinding.tool.util.L;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.AutocompletePredictionBuffer;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.maps.android.SphericalUtil;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.roughriders.jf.eta.R;
import de.roughriders.jf.eta.adapters.PredictionsAdapter;
import de.roughriders.jf.eta.adapters.RecentDestinationsAdapter;
import de.roughriders.jf.eta.adapters.RecentTripsAdapter;
import de.roughriders.jf.eta.helpers.DividerItemDecoration;
import de.roughriders.jf.eta.helpers.IRecyclerViewItemClicked;
import de.roughriders.jf.eta.helpers.ISettingsChangeRequiresReload;
import de.roughriders.jf.eta.helpers.Logger;
import de.roughriders.jf.eta.helpers.NotImplementedException;
import de.roughriders.jf.eta.models.Contact;
import de.roughriders.jf.eta.models.RecentDestination;
import de.roughriders.jf.eta.models.RecentTrip;
import de.roughriders.jf.eta.services.DistanceNotificationService;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener, ISettingsChangeRequiresReload {

    private SlidingUpPanelLayout slidingPanel;
    private static String TAG = "MainActivity";

    private EditText destinationSearchBox;
    private EditText targetPhoneBox;
    private Button startButton;
    private CheckBox sendStartSmsCheckbox;
    private CheckBox sendContinouosSmsCheckbox;
    private CheckBox sendAlmostThereSmsCheckbox;
    private CheckBox sendArrivalSmsCheckbox;
    private CardView slidingPanelButtonCardview;
    private ImageButton clearDestinationSearchBoxButton;
    private ImageButton clearPhoneNumberButton;
    private boolean ignoreNextAddressBoxChange = false;
    private boolean hasAskedForContactsPermission = false;

    private final int PICK_CONTACT = 0;
    private final int PICK_ADDRESS = 1;
    private final int PICK_BY_PLACE_PICKER = 2;
    private final int REQUEST_LOCATION_PERMISSION_KEY = 1;
    private final int REQUEST_CONTACTS_PERMISSION_KEY = 2;
    private final int SEARCH_RADIUS = 250;
    private static final int REQUEST_SMS_PERMISSION_KEY = 0;
    private final String ASKED_FOR_CONTACTS_PERMISSION_PREFERENCE_KEY = "askedForContactsPermission";
    private static final int VERTICAL_ITEM_SPACE = 48;

    private Contact currentContact;
    private RecentDestination currentDestination;
    private Uri contactPhotoUri = null;

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

    private Pattern coordinatePattern = Pattern.compile("^(\\-?\\d+(\\.\\d+)?),\\s*(\\-?\\d+(\\.\\d+)?)$");

    private boolean locationServiceAutocompleteHintShown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initControls();
        initDestinationEditText();
        initSlidingPanel();
        connectToApiClient();
        initPredictionsView();
        initRecentDestinationsView();
        initRecentTripsView();
        setSettingsData();
    }

    private void initControls(){
        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        targetPhoneBox = (EditText)findViewById(R.id.editTextTargetPhone);
        startButton = (Button)findViewById(R.id.startButton);
        slidingPanelButtonCardview = (CardView)findViewById(R.id.sliding_layout_select_destination_panel);
        clearDestinationSearchBoxButton = (ImageButton)findViewById(R.id.main_activity_clear_destination_searchbox);
        clearPhoneNumberButton = (ImageButton)findViewById(R.id.main_activity_clear_phone_button);

        targetPhoneBox.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus && !hasAskedForContactsPermission){
                    showContactsPermissionExplanationAndAsk();
                }
            }
        });
        targetPhoneBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
            @Override
            public void afterTextChanged(Editable editable) { if(editable.length() == 0) { clearPhoneNumberButton.setVisibility(View.GONE); } else { clearPhoneNumberButton.setVisibility(View.VISIBLE); } }
        });

        sendStartSmsCheckbox = ((CheckBox) findViewById(R.id.main_activity_initial_message));
        sendContinouosSmsCheckbox = ((CheckBox) findViewById(R.id.main_activity_continuous_updates));
        sendAlmostThereSmsCheckbox = ((CheckBox) findViewById(R.id.main_activity_almost_there_message));
        sendArrivalSmsCheckbox = ((CheckBox) findViewById(R.id.main_activity_arrival_message));
    }

    @Override
    public void onStart(){
        super.onStart();
        askOrCheckForLocationPermission();
        startTripActivityIfServiceRunning();
        reloadAdapterData();
    }

    private void reloadAdapterData(){
        recentTripsAdapter.updateFromSharedPreferences(this);
        if(recentTripsAdapter.size() > 0){
            recentTripsCardView.setVisibility(View.VISIBLE);
            noRecentTripsCardView.setVisibility(View.GONE);
        }
        else{
            recentTripsCardView.setVisibility(View.GONE);
            noRecentTripsCardView.setVisibility(View.VISIBLE);
        }

        recentDestinationsAdapter.updateFromSharedPreferences(this);
        if(recentDestinationsAdapter.size() > 0){
            recentDestinationsCardView.setVisibility(View.VISIBLE);
            recentDestinationsEmptyCardView.setVisibility(View.GONE);
        }
        else{
            recentDestinationsCardView.setVisibility(View.GONE);
            recentDestinationsEmptyCardView.setVisibility(View.VISIBLE);
        }
    }

    public void onRequiresReload(String entityName, boolean clearedAll){
        if(!clearedAll){
            Logger.getInstance().e(TAG, "onRequiresReload was called with 'clearedAll' = false. This option is not yet supported.");
            return;
        }

        if(entityName.equals("RecentDestinations")){
            recentDestinationsAdapter.clearAll();
        }
        else if(entityName.equals("RecentTrips")){
            recentTripsAdapter.clearAll();
        }
        else{
            Logger.getInstance().w(TAG, "An update has been requestes for an unknown entity: " + entityName);
        }
    }

    private void showLocationServiceAutocompleteHint() {
        if(locationServiceAutocompleteHintShown)
            return;

        locationServiceAutocompleteHintShown = true;
        Toast.makeText(this, getString(R.string.gps_needed_for_autocomplete), Toast.LENGTH_LONG).show();
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
                .setMessage(getString(R.string.locationPermissionExplanation))
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
            case REQUEST_SMS_PERMISSION_KEY:{
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    // permission granted, press the button again
                    startButton_Clicked(null);
                }
                else {
                    Toast.makeText(this, getString(R.string.smsPermissionNotGranted), Toast.LENGTH_LONG).show();
                }
            }
            case REQUEST_CONTACTS_PERMISSION_KEY:{
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(ASKED_FOR_CONTACTS_PERMISSION_PREFERENCE_KEY, true);
                editor.commit();
            }
        }
    }

    private void showContactsPermissionExplanationAndAsk(){
        if(PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getBoolean(ASKED_FOR_CONTACTS_PERMISSION_PREFERENCE_KEY, false) == false) {
            new AlertDialog.Builder(this)
                    .setTitle("ETA")
                    .setMessage(getString(R.string.contactPermissionExplanation))
                    .setCancelable(false)
                    .setPositiveButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            askForContactsPermission();
                        }
                    }).show();
        }
        hasAskedForContactsPermission = true;
    }

    private void askForContactsPermission(){
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, REQUEST_CONTACTS_PERMISSION_KEY);
    }

    private void startTripActivityIfServiceRunning(){
        if(DistanceNotificationService.IsServiceRunning) {
            Log.i(TAG, "Background service is running, starting TripActivity");
            Intent intent = new Intent(this, TripActivity.class);
            startActivity(intent);
        }
    }

    private void initPredictionsView(){
        predictionsCardView = (CardView)findViewById(R.id.sliding_layout_search_result_card);
        predictionsEmptyCardView = (CardView)findViewById(R.id.sliding_layout_no_search_results_container);
        predictionsView = (RecyclerView)findViewById(R.id.predictionsList);
        predictionsView.addItemDecoration(new DividerItemDecoration(this));
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
        recentDestinationsView.addItemDecoration(new DividerItemDecoration(this));
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
        recentTripsView.addItemDecoration(new DividerItemDecoration(this));
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
                targetPhoneBox.setText(currentContact.phone);
                onDestinationSelected(item.destination);
            }
        });
    }

    private void initDestinationEditText(){
        destinationSearchBox = ((EditText)findViewById(R.id.editTextDestination));
        setDestinationEditTextFocusListener();
        setDestinationEditTextChangeListener();
    }

    @SuppressLint("CommitPrefEdits")
    private void setSettingsData(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        Logger.writeToLogFile = prefs.getBoolean("enable_logging", false);

        if(!prefs.contains("default_send_almost_there_sms")){
            prefs.edit().putBoolean("default_send_almost_there_sms", false).commit();
        }

        try {
            sendStartSmsCheckbox.setChecked(prefs.getBoolean("default_start_sms", true));
            sendContinouosSmsCheckbox.setChecked(prefs.getBoolean("default_send_continuous_sms", true));
            sendAlmostThereSmsCheckbox.setChecked(prefs.getBoolean("default_send_almost_there_sms", true));
            sendArrivalSmsCheckbox.setChecked(prefs.getBoolean("default_send_trip_finished_sms", true));
        }
        catch(NullPointerException ex){
            Log.d(TAG, "At least one of the checkboxes could not be found.");
        }
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

                if(charSequence.length() == 0)
                    return;

                String text = charSequence.toString();
                Log.i(TAG, "Text in destionationSearchBox has changed, new content: " + text);
                sendAutocompleteRequest(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // the number of characters in the textbox define the layout:
                // no text -> show buttons (map, contacts,...) and recent destinations
                // text -> show search results only
                Log.i(TAG, "New length of the content of the destinationTextBox is: " + editable.length());
                if(editable.length() == 0){
                    slidingPanelButtonCardview.setVisibility(View.VISIBLE);
                    recentDestinationsCardView.setVisibility(View.VISIBLE);
                    predictionsCardView.setVisibility(View.GONE);
                    //predictionsEmptyCardView.setVisibility(View.GONE);
                    clearDestinationSearchBoxButton.setVisibility(View.GONE);
                } else {
                    slidingPanelButtonCardview.setVisibility(View.GONE);
                    recentDestinationsCardView.setVisibility(View.GONE);
                    //predictionsEmptyCardView.setVisibility(View.VISIBLE);
                    predictionsCardView.setVisibility(View.VISIBLE);
                    clearDestinationSearchBoxButton.setVisibility(View.VISIBLE);
                }
            }
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
               if(b == true) {
                   slidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
                   if(!isGPSEnabled())
                       showLocationServiceAutocompleteHint();
               }
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
            intent.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
            intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, SettingsActivity.GeneralPreferenceFragment.class.getName());
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
        if(currentContact != null) {
            if (!currentContact.phone.equals(targetPhoneBox.getText().toString()))
                currentContact = new Contact("", targetPhoneBox.getText().toString());
        }
        else{
            if(!targetPhoneBox.getText().toString().isEmpty()){
                currentContact = new Contact("", targetPhoneBox.getText().toString());
            }
        }
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        intent.setType(ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_TYPE);
        startActivityForResult(intent, PICK_ADDRESS);
    }

    public void selectContactButton_Clicked(View view){
        showSelectContactPicker();
    }

    private void showSelectContactPicker(){
        if(currentDestination != null){
            if(!(destinationSearchBox.getText().toString().contains(currentDestination.primaryText) && destinationSearchBox.getText().toString().contains(currentDestination.secondaryText))){
                currentDestination = new RecentDestination(destinationSearchBox.getText().toString());
            }
        }
        else{
            if(!destinationSearchBox.getText().toString().isEmpty())
                currentDestination = new RecentDestination(destinationSearchBox.getText().toString());
        }

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
                processAddressIntent(resultCode, data);
                break;
            case PICK_BY_PLACE_PICKER:
                processPlacesPickerIntent(resultCode, data);
                break;
        }
    }

    private void processContactIntent(int resultCode, Intent intent){
        if(resultCode == Activity.RESULT_OK)
        {
            Uri uri = intent.getData();
            String[] projection = { ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,ContactsContract.Contacts.PHOTO_URI };

            Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
            if(cursor == null)
                return;
            cursor.moveToFirst();

            int numberColumnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            String number = cursor.getString(numberColumnIndex);

            int nameColumnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
            String name = cursor.getString(nameColumnIndex);

            try {
                contactPhotoUri = Uri.parse(cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)));
                //contactPhotoUri = Uri.withAppendedPath(intent.getData(), ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
                //contactPhotoUri = getFacebookPhoto(number);
            }catch (NullPointerException ex){
                // doesn't matter, no picture available
            }

            cursor.close();
            currentContact = new Contact(name, number);

            Log.i(TAG, "Contact selected: name: " + name + " - phone: " + number);

            updateUi();
        }
    }

    public Uri getFacebookPhoto(String phoneNumber) {
        Uri phoneUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        Uri photoUri = null;
        ContentResolver cr = this.getContentResolver();
        Cursor contact = cr.query(phoneUri,
                new String[] { ContactsContract.Contacts._ID }, null, null, null);

        if (contact.moveToFirst()) {
            long userId = contact.getLong(contact.getColumnIndex(ContactsContract.Contacts._ID));
            photoUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, userId);
        }
        return photoUri;
    }

    private void processPlacesPickerIntent(int resultCode, Intent intent){
        if(resultCode == Activity.RESULT_OK){
            slidingPanel.postDelayed(new Runnable() {
                @Override
                public void run() {
                    slidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                }
            }, 150);

            Place place = PlacePicker.getPlace(this, intent);
            // When a point is selected that is not a point of interest the coordinates will be used as name.
            if(guessIfIsCoordinates(place.getName().toString()))
            {
                List<String> parts = Arrays.asList(place.getAddress().toString().split(","));

                boolean isEventList = parts.size() % 2 == 0;
                int halfSize = parts.size()/2;

                List<String> primaryParts = parts.subList(0, halfSize + (isEventList ? 0 : 1));
                List<String> secondaryParts = parts.subList(parts.size()/2 + (isEventList ? 0 : 1), parts.size());
                String primary = TextUtils.join(", ", primaryParts).replace("  ", " ");
                String secondary = TextUtils.join(", ", secondaryParts).replace("  ", " ");

                currentDestination = new RecentDestination(primary, secondary);
            } else {
                currentDestination = new RecentDestination(place.getName().toString(), place.getAddress().toString(), place.getLatLng().latitude, place.getLatLng().longitude);
            }
            recentDestinationsAdapter.addItem(currentDestination);
            updateUi();
        }
    }

    private boolean guessIfIsCoordinates(String s){
        return s.contains("\"") && s.contains("°") && (s.contains("N") || s.contains("S")) && (s.contains("E") || s.contains("W")) && s.contains("'");
    }

    private void processAddressIntent(int resultCode, Intent intent){
        if(resultCode == Activity.RESULT_OK)
        {
            //slidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
            slidingPanel.postDelayed(new Runnable() {
                @Override
                public void run() {
                    slidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                }
            }, 150);

            Uri uri = intent.getData();
            String[] projection = { ContactsContract.CommonDataKinds.StructuredPostal.CITY, ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE, ContactsContract.CommonDataKinds.StructuredPostal.STREET};

            Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
            if(cursor == null)
                return;
            cursor.moveToFirst();

            int cityColumnIndex = cursor.getColumnIndex(projection[0]);
            int zipColumnIndex = cursor.getColumnIndex(projection[1]);
            int streetColumnIndex = cursor.getColumnIndex(projection[2]);

            // the retrieved data is not necessarily stored in a logical fashion
            // e.g. the whole address my be within the street
            String city = cursor.getString(cityColumnIndex);
            String zip = cursor.getString(zipColumnIndex);
            String street = cursor.getString(streetColumnIndex);
            cursor.close();

            String primary = "", secondary = "";
            if(city != null && zip != null && street != null){
                primary = street;
                secondary = zip + " " + city;
            }
            else if(city != null && street != null){
                primary = street;
                secondary = city;
            }
            else if(street != null){ // in this case city has to be null
                // try to split
                String[] parts = street.split(",");
                if(parts.length == 3){
                    primary = parts[0];
                    // sometimes the addresses are messed up and the city is saved in the street as well
                    primary = primary.replace(parts[1], "").replace(parts[2], "");
                    secondary = parts[1] + " " + parts[2];
                }
                else if(parts.length == 2) {
                    primary = parts[0];
                    // sometimes the addresses are messed up and the city is saved in the street as well
                    primary = primary.replace(parts[1], "");
                    secondary = parts[1];
                }
                else if(parts.length == 1){
                    primary = parts[0];
                    secondary = "";
                }
                else {
                    primary = street;
                    secondary = "";
                }
            }
            currentDestination = new RecentDestination(primary, secondary);
            recentDestinationsAdapter.addItem(currentDestination);
            Logger.getInstance().i(TAG, "created new currentDestination: " + currentDestination.primaryText + " <> " + currentDestination.secondaryText);
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
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        Toast.makeText(this, R.string.mainActivity_apiClientConnectionFailure, Toast.LENGTH_SHORT).show();
        Logger.getInstance().w(TAG, "Google Api Client could not connect, reason: " + result.getErrorMessage());
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

                    // sometimes the requests arrive although the destination textbox has already been cleared
                    // to make the correct interface appear we need to make sure there are no new "ghost" completions
                    if(destinationSearchBox.getText().length() == 0)
                        return;

                    View view = findViewById(R.id.sliding_layout_search_result_card);
                    if(view != null)
                        view.setVisibility(View.VISIBLE);
                    else
                        Logger.getInstance().w(TAG, "A referenced view could not be found: sliding_layout_search_result_card");
                    predictionsAdapter.clear();

                    if(autocompletePredictions.getCount() == 0) {
                        predictionsCardView.setVisibility(View.GONE);
                        if(destinationSearchBox.length() != 0)
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
        slidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        destinationSearchBox.setText(destination.primaryText + ", " + destination.secondaryText);
        currentDestination = destination;
        recentDestinationsAdapter.addItem(destination);
        recentDestinationsCardView.setVisibility(View.VISIBLE);
        recentDestinationsEmptyCardView.setVisibility(View.GONE);
        RecentDestination.saveToSharedPreferences(recentDestinationsAdapter.getItems(), this);
    }

    public void onPasteClipboardIntoDestination(View view){
        ClipboardManager clipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        String content = clipboard.getText().toString();
        if(!content.isEmpty())
            destinationSearchBox.setText(content);
    }

    public void clearSearchEditText_onClick(View view){
        destinationSearchBox.setText("");
    }

    public void clearContactEditText_onClick(View view){
        targetPhoneBox.setText("");
    }

    public void startButton_Clicked(View view) {
        updateFromUi();
        if(destinationSearchBox.getText().toString().isEmpty() || targetPhoneBox.getText().toString().isEmpty()) {
            Toast.makeText(this, getString(R.string.not_all_information_entered_on_trip_start) , Toast.LENGTH_SHORT).show();
            return;
        }

        if(!isGPSEnabled()) {
            showGPSHint();
            return;
        }

        if(!isInternetAvailable()){
            Toast.makeText(this, R.string.internet_not_available_toast, Toast.LENGTH_SHORT).show();
            return;
        }

        if(wasSmsPermissionGranted())
            saveCurrentTrip();
        else {
            showSmsPermissionExplanationAndAsk();
            return;
        }

        startTrip();
    }

    private boolean isInternetAvailable(){
        ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }

    private void showGPSHint(){
        new AlertDialog.Builder(this)
            .setTitle("ETA")
            .setMessage(getString(R.string.activate_gps_hint))
            .setCancelable(false)
            .setPositiveButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            })
            .show();
    }

    private boolean isGPSEnabled(){
        try {
            int locationMode = Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCATION_MODE);
            switch(locationMode){
                case Settings.Secure.LOCATION_MODE_HIGH_ACCURACY:
                case Settings.Secure.LOCATION_MODE_SENSORS_ONLY:
                case Settings.Secure.LOCATION_MODE_BATTERY_SAVING:
                    return true;
                case Settings.Secure.LOCATION_MODE_OFF:
                    return false;
                default:
                    return false;
            }
        }
        catch(Settings.SettingNotFoundException ex){
            // we have no clue about the gps status so just give it a try,...
            return true;
        }
    }

    private void saveCurrentTrip() {

        RecentDestination destination;
        Contact contact;

        if(currentDestination == null)
            destination = new RecentDestination(destinationSearchBox.getText().toString());
        else
            destination = currentDestination;

        if(currentContact == null)
            contact = new Contact("", targetPhoneBox.getText().toString());
        else
            contact = currentContact;

        recentTripsAdapter.addItem(new RecentTrip(System.currentTimeMillis(), destination, contact));
        RecentTrip.saveToSharedPreferences(recentTripsAdapter.getItems(), this);
    }

    public void startTrip(){
        startBackgroundService();

        Intent intent = new Intent(this, TripActivity.class);
        if(currentDestination.longitude != null && currentDestination.latitude != null && !currentDestination.longitude.isEmpty() && !currentDestination.latitude.isEmpty())
            intent.putExtra(TripActivity.DESTINATION_EXTRA, currentDestination.latitude + "," + currentDestination.longitude);
        else
            intent.putExtra(TripActivity.DESTINATION_EXTRA, destinationSearchBox.getText().toString());
        intent.putExtra(TripActivity.PHONE_NUMBER_EXTRA, targetPhoneBox.getText().toString());
        if(contactPhotoUri != null)
            intent.putExtra(TripActivity.PHOTO_EXTRA, contactPhotoUri.toString());
        if(currentContact != null)
            intent.putExtra(TripActivity.NAME_EXTRA, currentContact.name);
        startActivity(intent);
    }

    /**
     * Starts the background service with a check if already running.
     * @return true if the service was started, false if it was already running
     */
    private boolean startBackgroundService(){
        if(DistanceNotificationService.IsServiceRunning) {
            Log.i(TAG, "The DistanceNotificationService is already running, no need to start it again.");
            return false;
        }

        Log.d(TAG, "starting background service");
        Intent intent = new Intent(this, DistanceNotificationService.class);
        intent.putExtra(DistanceNotificationService.COMMAND_EXTRA, DistanceNotificationService.COMMAND_START);
        intent.putExtra(DistanceNotificationService.DESTINATION_EXTRA, currentDestination.toNiceString());
        intent.putExtra(DistanceNotificationService.PHONE_EXTRA, currentContact.phone);
        intent.putExtra(DistanceNotificationService.SEND_INITIAL_MESSAGE_EXTRA, ( findViewById(R.id.main_activity_initial_message)) != null ? ((CheckBox) findViewById(R.id.main_activity_initial_message)).isChecked() : false);
        intent.putExtra(DistanceNotificationService.SEND_CONTINUOUS_UPDATES_EXTRA, ( findViewById(R.id.main_activity_continuous_updates)) != null ? ((CheckBox) findViewById(R.id.main_activity_continuous_updates)).isChecked() : false);
        intent.putExtra(DistanceNotificationService.SEND_ALMOST_THERE_MESSAGE_EXTRA, ( findViewById(R.id.main_activity_almost_there_message)) != null ? ((CheckBox) findViewById(R.id.main_activity_almost_there_message)).isChecked() : false);
        intent.putExtra(DistanceNotificationService.SEND_ARRIVAL_MESSAGE_EXTRA, ( findViewById(R.id.main_activity_arrival_message)) != null ? ((CheckBox) findViewById(R.id.main_activity_arrival_message)).isChecked() : false);
        startService(intent);
        return true;
    }

    private boolean wasSmsPermissionGranted(){
        return ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    private void showSmsPermissionExplanationAndAsk(){
        new AlertDialog.Builder(this)
                .setTitle("ETA")
                .setMessage(getString(R.string.smsPermissionNotGranted))
                .setCancelable(false)
                .setPositiveButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        askForSmsPermission();
                    }
                }).show();
    }

    private void askForSmsPermission(){
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, REQUEST_SMS_PERMISSION_KEY);
    }
    //
    // -----------

    /**
     * Updates the ui with the values stored in the local variables.
     */
    private void updateUi(){
        if(currentContact != null)
            targetPhoneBox.setText(currentContact.phone);

        ignoreNextAddressBoxChange = true;

        if(currentDestination != null) {
            String destination;
            if(!currentDestination.primaryText.isEmpty() && !currentDestination.secondaryText.isEmpty())
                destination = currentDestination.primaryText + ", " + currentDestination.secondaryText;
            else if(!currentDestination.primaryText.isEmpty() && currentDestination.secondaryText.isEmpty())
                destination = currentDestination.primaryText;
            else if(currentDestination.primaryText.isEmpty() && !currentDestination.secondaryText.isEmpty())
                destination = currentDestination.secondaryText;
            else
                destination = "";
            destination = destination.replace(", ,", ", ").replace(",,", ",");
            destinationSearchBox.setText(destination);
        }
    }

    /**
     * Pulls values from the ui and stores them in the corresponding variables.
     */
    private void updateFromUi(){
        if(currentContact != null)
            currentContact.phone = targetPhoneBox.getText().toString();
        else
            currentContact = new Contact("", targetPhoneBox.getText().toString());

        if(currentDestination != null) {
            String enteredText = destinationSearchBox.getText().toString().replace(",", "").replace(" ", "");
            String condensedText = (currentDestination.primaryText + currentDestination.secondaryText).replace(",", "").replace(" ","");
            if(!condensedText.contains(enteredText)) {
                if(enteredText.contains(",")){
                    String firstPart = enteredText.substring(0, enteredText.indexOf(","));
                    String secondPart = enteredText.substring(enteredText.indexOf(",")+1);
                    currentDestination.primaryText = firstPart;
                    currentDestination.secondaryText = secondPart;
                }
                else
                    currentDestination.primaryText = destinationSearchBox.getText().toString();
            }
        }
        else
            currentDestination = new RecentDestination(destinationSearchBox.getText().toString());
    }

    public void onUseMapToSelectDestinationClicked(View view) {
        showMapDestinationPicker();
    }

    private void showMapDestinationPicker(){
        PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
        try {
            startActivityForResult(builder.build(this), PICK_BY_PLACE_PICKER);
        }
        catch (GooglePlayServicesRepairableException e) {
            Logger.getInstance().w(TAG, "Places picker could not be startet: " + e.getMessage());
            e.printStackTrace();
        } catch (GooglePlayServicesNotAvailableException e) {
            Logger.getInstance().w(TAG, "Google Play Services seem to not be available: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
