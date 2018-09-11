package ch.amiv.android_app.checkin;

/**
 * Author: Roger Barton, rbarton@ethz.ch
 * Date Created: 2/12/17
 */

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import ch.amiv.android_app.R;

import ch.amiv.android_app.core.Settings;
import ch.amiv.android_app.util.Util;

public class MainActivity extends AppCompatActivity {
    public static String CurrentPin;
    private boolean mWaitingOnServer = false;

    private EditText mPinField;
    private TextView mInvalidPinLabel;
    private Button mOpenRecent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Util.SetWindowResizing(this, true);
        setContentView(R.layout.checkin_main);

        InitialiseUI();
        CheckPermissions();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        Settings.CancelVibrate();
    }

    //region ---Toolbar

    /**
     * Inflate the menu in the toolbar; this adds items to the action bar if it is present.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.checkin_main_toolbar_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Detecting when the toolbar button is pressed
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_settings) {
            StartSettingsActivity();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    //endregion


    private void InitialiseUI()
    {
        Util.SetupToolbar(this, true);

        mPinField = findViewById(R.id.PinField);
        mInvalidPinLabel = findViewById(R.id.pinStatusLabel);
        mInvalidPinLabel.setText("");
        mOpenRecent = findViewById(R.id.openRecent);

        mPinField.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View view, int keyCode, KeyEvent keyevent) {
                if ((keyevent.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    SubmitPin(view);
                    return true;
                }
                return false;
            }
        });

        String lastPin = Settings.GetPref(Settings.recentEventPin, getApplicationContext());
        mOpenRecent.setEnabled(!lastPin.isEmpty());

        /*Think this animation causes flashing screen bug, when the activity starts
        View logo = findViewById(R.id.logoImage);
        if(logo != null) {
            Animation animation = AnimationUtils.loadAnimation(this, R.anim.item_anim_pop);
            animation.setDuration(150);
            logo.startAnimation(animation);
        }*/
    }

    private void CheckPermissions()
    {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) { //Get permission for camera
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                //Add popup
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 0);
            }
            else
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 0);
        }
    }

    /**
     * Function called by button, set in xml
     */
    public void SubmitRecentPin(View view){
        SubmitPin(true);
    }

    public void SubmitPin(View view){
        SubmitPin(false);
    }

    /**
     * Submit a pin for an event to the server and act on response accordingly, ie open scanActivity if valid, or request pin entry again
     */
    public void SubmitPin(boolean openRecent)
    {
        Settings.Vibrate(Settings.VibrateTime.SHORT, getApplicationContext());
        View button = findViewById(openRecent ? R.id.openRecent : R.id.SubmitPin);
        if(button != null)
            button.startAnimation(AnimationUtils.loadAnimation(this, R.anim.item_anim_pop));

        if(mWaitingOnServer || (!openRecent && mPinField.getText().toString().isEmpty()))  //prevents submitting a second pin while still waiting on the response for the first pin
            return;
        mWaitingOnServer = true;
        mInvalidPinLabel.setText(R.string.wait);

        if(!Requests.CheckConnection(getApplicationContext())) {
            ApplyServerResponse(true, 0, getResources().getString(R.string.no_internet));
            return;
        }

        if(openRecent)
            CurrentPin = Settings.GetPref(Settings.recentEventPin, getApplicationContext());
        else
            CurrentPin = mPinField.getText().toString();



        //Create a callback, this is what happens when we get the response
        Requests.OnCheckPinReceivedCallback callback = new Requests.OnCheckPinReceivedCallback() {
            @Override
            public void OnStringReceived(final boolean validResponse, final int statusCode, final String data) {
                mPinField.post(new Runnable() {    //delay to other thread by using a ui element, as this is in a callback on another thread
                    public void run() {
                        ApplyServerResponse(validResponse, statusCode, data);
                }});
            }
        };

        Requests.CheckPin(this, callback);

        //StartScanActivity();    //NOTE: Uncomment for debugging without valid pin
    }

    /**
     * Submit a server response to the function, will apply UI feedback or start the scan activity
     * @param statusCode http status code from the response, eg 200 or 400
     * @param responseText the text received from the server about our post request
     */
    private void ApplyServerResponse(boolean validResponse, int statusCode, String responseText)
    {
        if(!mWaitingOnServer)   //Dont display response if we are not expecting one
            return;
        mWaitingOnServer = false;

        if(!validResponse) {
            InvalidUrlResponse();
            return;
        }

        Log.e("postrequest", "Response from server for pin submission: " + statusCode + " with text: " + responseText + " on event pin: " + MainActivity.CurrentPin);

        if(statusCode == 200) { //success
            mInvalidPinLabel.setText(R.string.success);
            Settings.SetPref(Settings.recentEventPin, CurrentPin, getApplicationContext()); //store as last used pin
            mOpenRecent.setEnabled(true);
            StartScanActivity();
        }
        else if(statusCode == 401)//invalid pin
        {
            mInvalidPinLabel.setText(responseText);
            mPinField.setText("");
        }
        else if (statusCode == 0) //no internet connection
        {
            mInvalidPinLabel.setText(R.string.no_internet);
        }
        else                    //Other error
        {
            InvalidUrlResponse();       //Should interpret other errors as well instead of just displaying invalid url, which may not be the case
        }
    }

    private void InvalidUrlResponse()
    {
        mInvalidPinLabel.setText(R.string.invalid_url);
    }

    //=====Changing Activity====
    private void StartScanActivity()
    {
        mWaitingOnServer = false;
        mPinField.setText("");  //clear pin field
        EventDatabase.instance = null;

        Intent intent = new Intent(this, ScanActivity.class);
        startActivity(intent);
    }

    public void StartSettingsActivity()
    {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    /**
     * Will open the checkin website in a browser
     */
    public void GoToWebsite(View view)
    {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(Settings.GetPref(Settings.checkin_url, getApplicationContext())));
        startActivity(browserIntent);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Util.SetWindowResizing(this, false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Util.SetWindowResizing(this, true);
        mInvalidPinLabel.setText("");
    }
}
