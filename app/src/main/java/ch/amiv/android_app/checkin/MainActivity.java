package ch.amiv.android_app.checkin;

/**
 * Author: Roger Barton, rbarton@ethz.ch
 * Date Created: 2/12/17
 */

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.TextView;

import ch.amiv.android_app.R;

public class MainActivity extends AppCompatActivity {
    public static String CurrentPin;
    private boolean mWaitingOnServer = false;

    private EditText mPinField;
    private TextView mInvalidPinLabel;

    public static Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.checkin_activity_main);

        InitialiseUI();
        CheckPermissions();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if(vibrator != null)
            vibrator.cancel();
    }

    private void InitialiseUI()
    {
        mPinField = findViewById(R.id.PinField);
        mInvalidPinLabel = findViewById(R.id.InvalidPinLabel);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        mPinField.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View view, int keyCode, KeyEvent keyevent) {
                if ((keyevent.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    SubmitPin(view);
                    return true;
                }
                return false;
            }
        });

        View logo = findViewById(R.id.LogoImage);
        if(logo != null) {
            Animation animation = AnimationUtils.loadAnimation(this, R.anim.item_anim_pop);
            animation.setDuration(150);
            logo.startAnimation(animation);
        }
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
     * Submit a pin for an event to the server and act on response accondingly, ie open scanActivity if valid, or request pin entry again
     * @param view
     */
    public void SubmitPin(View view)
    {
        if(vibrator != null)
            vibrator.vibrate(50);
        View button = findViewById(R.id.SubmitPin);
        if(button != null)
            button.startAnimation(AnimationUtils.loadAnimation(this, R.anim.item_anim_pop));

        if(mWaitingOnServer || mPinField.getText().toString().isEmpty())  //prevents submitting a second pin while still waiting on the response for the first pin
            return;
        mWaitingOnServer = true;

        if(!ServerRequests.CheckConnection(getApplicationContext())) {
            ApplyServerResponse(true, 0, getResources().getString(R.string.no_internet));
            return;
        }

        CurrentPin = mPinField.getText().toString();

        //Create a callback, this is what happens when we get the response
        ServerRequests.OnCheckPinReceivedCallback callback = new ServerRequests.OnCheckPinReceivedCallback() {
            @Override
            public void OnStringReceived(final boolean validResponse, final int statusCode, final String data) {
                mPinField.post(new Runnable() {    //delay to other thread by using a ui element, as this is in a callback on another thread
                    public void run() {
                        ApplyServerResponse(validResponse, statusCode, data);
                }});
            }
        };

        ServerRequests.CheckPin(this, callback);

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
            StartScanActivity();
        }
        else if(statusCode == 401)//invalid pin
        {
            mInvalidPinLabel.setVisibility(View.VISIBLE);
            mInvalidPinLabel.setText(responseText);
            mPinField.setText("");
        }
        else if (statusCode == 0) //no internet connection
        {
            mInvalidPinLabel.setVisibility(View.VISIBLE);
            mInvalidPinLabel.setText(R.string.no_internet);
        }
        else                    //Other error
        {
            InvalidUrlResponse();       //Should interpret other errors as well instead of just displaying invalid url, which may not be the case
        }
    }

    private void InvalidUrlResponse()
    {
        mInvalidPinLabel.setVisibility(View.VISIBLE);
        mInvalidPinLabel.setText(R.string.invalid_url);
    }

    //=====Changing Activity====
    private void StartScanActivity()
    {
        mWaitingOnServer = false;
        mPinField.setText("");  //clear pin field
        mInvalidPinLabel.setVisibility(View.INVISIBLE);
        EventDatabase.instance = null;

        Intent intent = new Intent(this, ScanActivity.class);
        startActivity(intent);
    }

    public void StartSettingsActivity(View view)
    {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    /**
     * Will open the checkin website in a browser
     */
    public void GoToWebsite(View view)
    {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(SettingsActivity.GetServerURL(this)));
        startActivity(browserIntent);
    }
}
