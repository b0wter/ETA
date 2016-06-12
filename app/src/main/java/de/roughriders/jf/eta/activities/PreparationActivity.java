package de.roughriders.jf.eta.activities;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import de.roughriders.jf.eta.R;

public class PreparationActivity extends AppCompatActivity {

    private String phone;
    private String name;
    private String destination;

    public static String DESTINATION_EXTRA = "destinationExtra";
    public static String PHONE_NUMBER_EXTRA = "phoneExtra";
    public static String NAME_EXTRA = "nameExtra";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preparation);
    }

    private void setIntentData(){
        Bundle bundle = getIntent().getExtras();
        phone = bundle.getString(PreparationActivity.PHONE_NUMBER_EXTRA);
        destination = bundle.getString(PreparationActivity.DESTINATION_EXTRA);
        if(bundle.containsKey(PreparationActivity.NAME_EXTRA))
            name = bundle.getString(NAME_EXTRA);
        else
            name = phone;
    }
}
