package ch.amiv.android_app.checkin;

/**
 * Author: Roger Barton, rbarton@ethz.ch
 * Date Created: 2/12/17
 */

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.animation.AnimationUtils;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import ch.amiv.android_app.R;

import ch.amiv.android_app.core.Settings;

public class ScanActivity extends AppCompatActivity {
    private static int NEXT_LEGI_DELAY = 1000;   //delay between the response from the server and scanning the next legi (in ms)

    private boolean mIsCheckingIn = true;   //sets whether we a checking people in or out, will be sent to the server
    private boolean mAllowNextBarcode = true;   //whether a new barcode can be detected
    private boolean mCanClearResponse = true;   //Whether the user can tap to dismiss the response (in turn allow for a new barcode to be scanned)

    //----Server Communication-----
    private boolean mWaitingOnServer_LegiSubmit = false;    //whether we are waiting for a response from the server, regarding a legi/nethz/email submission to /mutate
    private Handler handler = new Handler();    //Used for delaying function calls, in conjunction with runnables
    private Runnable refreshMemberDB = new Runnable() {    //Refresh stats every x seconds
        @Override
        public void run() {
            RefreshMemberDB();
            if(SettingsActivity.GetAutoRefresh(getApplicationContext()))
                handler.postDelayed(this, SettingsActivity.GetRefreshFrequency(getApplicationContext()));  //ensure to call this same runnable again so it repeats, if this is allowed
        }
    };

    //-----UI Elements----
    private Switch mCheckInSwitch;
    private TextView mCheckInSwitchLabel_In;
    private TextView mCheckInSwitchLabel_Out;

    private EditText mLegiInputField;
    private TextView mWaitLabel;
    private TextView mResponseLabel;
    private ImageView mTickImage;
    private ImageView mCrossImage;
    private TextView mCheckinCountLabel;
    private ImageView mBGTint;

    //Stats UI
    private TextView mLeftStatValue;
    private TextView mRightStatValue;
    private TextView mLeftStatDesc;
    private TextView mRightStatDesc;

    //-----Barcode Scanning Related----
    private BarcodeDetector mBarcodeDetector;
    private CameraSource mCameraSource;
    private SurfaceView mCameraView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        setContentView(R.layout.checkin_activity_scan);

        InitialiseUI();
        InitialiseBarcodeDetection();
        ResetResponseUI();
        //Note the refreshMemberDB handler function is called in OnResume, as this is called after onCreate
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(refreshMemberDB);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.postDelayed(refreshMemberDB, 0);
    }

    /**
     * Initalises UI so variables are linked to layout, setting up onClick callbacks
     */
    private void InitialiseUI()
    {
        //Assigning from layout
        mLegiInputField = findViewById(R.id.LegiInputField);
        mCheckInSwitch = findViewById(R.id.CheckInSwitch);
        mCheckInSwitchLabel_In = findViewById(R.id.CheckInLabel);
        mCheckInSwitchLabel_Out = findViewById(R.id.CheckOutLabel);
        mWaitLabel = findViewById(R.id.PleaseWaitLabel);
        mResponseLabel = findViewById(R.id.ResponseLabel);
        mTickImage = findViewById(R.id.TickImage);
        mCrossImage = findViewById(R.id.CrossImage);
        mCheckinCountLabel = findViewById(R.id.CheckInCountLabel);
        mBGTint = findViewById(R.id.BackgroundTint);
        mBGTint.setAlpha(0.4f);

        mLeftStatValue = findViewById(R.id.LeftStatLabel);
        mRightStatValue = findViewById(R.id.RightStatLabel);
        mLeftStatDesc = findViewById(R.id.LeftStatDescription);
        mRightStatDesc = findViewById(R.id.RightStatDescription);

        mCameraView = findViewById(R.id.CameraView);

        //creating onClick callbacks
        mCheckInSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mIsCheckingIn = isChecked;
            }
        });

        RelativeLayout mCameraLayout = findViewById(R.id.CameraLayout);
        mCameraLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mCanClearResponse) {
                    mAllowNextBarcode = true;
                    ResetResponseUI();
                }
            }
        });
    }

    /**
     * Creates a barcode scanner and sets up repeating call to scan for barcodes, also creates cameraPreview for layout. Need to initUI before.
     */
    private void InitialiseBarcodeDetection()
    {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width = displayMetrics.widthPixels;
        if(width <= 64)
            width = 720;
        int height = displayMetrics.heightPixels;
        if(height <= 64)
            height = 1280;
        mBarcodeDetector = new BarcodeDetector.Builder(this).setBarcodeFormats(Barcode.CODE_39).build();                 //IMPORTANT: Set the correct format for the barcode reader, can choose all formats but will be slower
        CameraSource.Builder camBuilder = new CameraSource.Builder(this, mBarcodeDetector).setRequestedPreviewSize(width, height - 64);
        camBuilder.setAutoFocusEnabled(true);
        mCameraSource = camBuilder.build();

        //initialising the camera view, so we can see the camera and analyse the frames for barcodes
        mCameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    if ( ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA ) != PackageManager.PERMISSION_GRANTED ) {
                        //IMPLEMENT: ask for camera permission
                    }
                    else
                        mCameraSource.start(mCameraView.getHolder());   //Note: may need to pause camera during onPause
                } catch (IOException e) {
                    Log.e("mCameraSource", e.getMessage());
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                mCameraSource.stop();
            }
        });

        //Detecting Barcodes
        mBarcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {}

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {    //This is called every frame or so and will give the detected barcodes
                final SparseArray<Barcode> barcodes = detections.getDetectedItems();

                if (barcodes.size() == 0)
                    return;

                //Only allow another barcode if: time since the last scan has passed, the barcode is different or the checkmode has changed
                if(mAllowNextBarcode) {
                    String s = barcodes.valueAt(0).displayValue.toLowerCase();
                    //Decode the barcode, for emails replace the chosen char with @, as @ cannot be encoded with CODE_39 so we use a replacement, needs to match the generated barcode code
                    if(s.contains(".")) //All emails have at least one '.'
                        s = s.replace(' ', '@');    //replace ' ' with '@' as encoded

                    final String value = s;
                    Log.e("barcodeDetect", "detected barcode: " + value);

                    mAllowNextBarcode = false;  //prevent the same barcode being submitted in the next frame until this is set to true again in the postDelayed call
                    mCanClearResponse = false;

                    mLegiInputField.post(new Runnable() {    //delay to other thread by using a ui element, as this is in a callback on another thread
                        public void run() {
                            SubmitLegiNrToServer(value); //submit the legi value to the server on the main thread

                            handler.postDelayed(new Runnable() {    //Creates delay call to only allow scanning again after x seconds
                                @Override
                                public void run() {
                                    mCanClearResponse = true;
                                }
                            }, NEXT_LEGI_DELAY);
                        }
                    });
                }
            }
        });
    }

    //=====END OF INITIALISATION=====

    /**
     * Call this to submit a legi nr from a UI Element
     */
    public void SubmitLegiNrFromTextField(View view)
    {
        if(mWaitingOnServer_LegiSubmit)
            return;

        String s = mLegiInputField.getText().toString();
        if(s.isEmpty())
            return;
        View submitButton = findViewById(R.id.SubmitLegiNrButton);
        if(submitButton != null)
            submitButton.startAnimation(AnimationUtils.loadAnimation(this, R.anim.item_anim_pop));

        mLegiInputField.setText("");
        ResetResponseUI();

        SubmitLegiNrToServer(s);
    }

    /**
     * Will submit a legi nr/nethz/email to the server and will set the UI accondingly, POST Request is done with Volley.
     */
    public void SubmitLegiNrToServer(String leginr)
    {
        if(mWaitingOnServer_LegiSubmit)
            return;

        Settings.Vibrate(Settings.VibrateTime.SHORT, getApplicationContext());

        if(!ServerRequests.CheckConnection(getApplicationContext())) {
            SetUIFromResponse_Invalid(0, getResources().getString(R.string.no_internet));
            return;
        }

        SetWaitingOnServer(true);       //Clear UI
        mResponseLabel.setVisibility(View.INVISIBLE);

        ServerRequests.OnJsonReceivedCallback callback = new ServerRequests.OnJsonReceivedCallback() {
            @Override
            public void OnJsonReceived(final int statusCode, final JSONObject data) {
                mResponseLabel.post(new Runnable() {    //delay to other thread by using a ui element, as this is in a callback on another thread
                    public void run() {
                        String msg = "No Message Found";
                        Log.e("json", "mutate json received: " + data.toString());
                        try {
                            msg = data.getString("message");
                            Member m = new Member(data.getJSONObject("signup"));
                            SetUIFromResponse_Valid(statusCode, msg, m);
                        }
                        catch (JSONException e){
                            Log.e("json", "Couldnt parse mutate json");
                        }
                }});
            }

            @Override
            public void OnStringReceived(final int statusCode, final String data) {
                mResponseLabel.post(new Runnable() {    //delay to other thread by using a ui element, as this is in a callback on another thread
                    public void run() {
                        SetUIFromResponse_Invalid(statusCode, data);
                    }});
            }
        };

        Log.e("postrequest", "Params sent: pin=" + MainActivity.CurrentPin + ", info=" + leginr + ", checkmode=" + (mIsCheckingIn ? "in" : "out") + ", URL used: " + SettingsActivity.GetServerURL(getApplicationContext()));
        ServerRequests.CheckLegi(this, callback, leginr, mIsCheckingIn);
    }

    /**
     * Will set UI elements correctly based on the response from the server on a legi submission.
     * @param statusCode http status code from the response, eg 200 or 400
     * @param member the text received from the server about our post request
     */
    private void SetUIFromResponse_Valid(int statusCode, String message, Member member)
    {
        SetWaitingOnServer(false);
        if(message.isEmpty())
            return;

        if(statusCode == 200) { //success
            mResponseLabel.setVisibility(View.VISIBLE);
            mResponseLabel.setText(message);
            mTickImage.setVisibility(View.VISIBLE);
            mBGTint.setVisibility(View.VISIBLE);
            mTickImage.startAnimation(AnimationUtils.loadAnimation(this, R.anim.item_anim_grow));
            mTickImage.setColorFilter(getResources().getColor(R.color.valid));

            if(EventDatabase.instance.eventData.eventType == EventData.EventType.Counter) {
                mCheckinCountLabel.setVisibility(View.VISIBLE);
                mCheckinCountLabel.setText(member.checkinCount);
            }
            else {
                mCheckinCountLabel.setVisibility(View.INVISIBLE);
                mCheckinCountLabel.setText("");
            }

            if(member.membership.equalsIgnoreCase("regular"))
            {
                mTickImage.setColorFilter(getResources().getColor(R.color.valid));
                mBGTint.setColorFilter(getResources().getColor(R.color.valid));
            }
            else
            {
                mTickImage.setColorFilter(getResources().getColor(R.color.orange));
                mBGTint.setColorFilter(getResources().getColor(R.color.orange));
            }

            Settings.Vibrate(Settings.VibrateTime.NORMAL, getApplicationContext());
        }
        else
        {
            SetUIFromResponse_Invalid(statusCode, message);
        }

        RefreshMemberDB();
    }

    public void SetUIFromResponse_Invalid(int statusCode, String responseText)
    {
        SetWaitingOnServer(false);

        if(statusCode == 200) { //success
            mResponseLabel.setVisibility(View.VISIBLE);
            mResponseLabel.setText(responseText);
            mTickImage.setVisibility(View.VISIBLE);
            mTickImage.setColorFilter(getResources().getColor(R.color.valid));
            mTickImage.startAnimation(AnimationUtils.loadAnimation(this, R.anim.item_anim_grow));
            mBGTint.setVisibility(View.VISIBLE);


            if(responseText.substring(0, 12).equalsIgnoreCase("regular"))
            {
                mTickImage.setColorFilter(getResources().getColor(R.color.valid));
                mBGTint.setColorFilter(getResources().getColor(R.color.valid));
            }
            else
            {
                mTickImage.setColorFilter(getResources().getColor(R.color.orange));
                mBGTint.setColorFilter(getResources().getColor(R.color.orange));
            }

            Settings.Vibrate(Settings.VibrateTime.NORMAL, getApplicationContext());
        }
        else if (statusCode == 0)   //no internet
        {
            mResponseLabel.setVisibility(View.VISIBLE);
            mResponseLabel.setText(R.string.no_internet);
            mCrossImage.setVisibility(View.VISIBLE);
            mCrossImage.startAnimation(AnimationUtils.loadAnimation(this, R.anim.item_anim_grow));
            mBGTint.setVisibility(View.VISIBLE);
            mBGTint.setColorFilter(getResources().getColor(R.color.invalid));
        }
        else
        {                  //invalid legi/already checked in etc
            mResponseLabel.setVisibility(View.VISIBLE);
            mResponseLabel.setText(responseText);
            mCrossImage.setVisibility(View.VISIBLE);
            mCrossImage.startAnimation(AnimationUtils.loadAnimation(this, R.anim.item_anim_grow));
            mBGTint.setVisibility(View.VISIBLE);
            mBGTint.setColorFilter(getResources().getColor(R.color.invalid));

            Settings.Vibrate(Settings.VibrateTime.LONG, getApplicationContext());
        }

        RefreshMemberDB();
    }

    /**
     * Will clear the response UI and hide it
     */
    private void ResetResponseUI ()
    {
        mResponseLabel.setText("");
        mResponseLabel.setVisibility(View.INVISIBLE);
        mTickImage.setVisibility(View.INVISIBLE);
        mCrossImage.setVisibility(View.INVISIBLE);
        mCheckinCountLabel.setVisibility(View.INVISIBLE);
        mBGTint.setVisibility(View.INVISIBLE);
    }

    /**
     * Use this to set UI accordingly and prevent a second request being sent before the first one returns
     * @param isWaiting true we are still waiting for the response from the server
     */
    private void SetWaitingOnServer (boolean isWaiting)
    {
        mWaitingOnServer_LegiSubmit = isWaiting;
        mWaitLabel.setVisibility((isWaiting ? View.VISIBLE : View.INVISIBLE));
    }


    //-----Updating Stats-----
    /**
     * Will Get the list of people for the event from the server, with stats.
     */
    private void RefreshMemberDB()
    {
        if(EventDatabase.instance == null)
            new EventDatabase();

        ServerRequests.UpdateMemberDB(getApplicationContext(),
            new ServerRequests.OnDataReceivedCallback(){
                @Override
                public void OnDataReceived()
                {
                    UpdateStatsUI();
                    ActionBar actionBar = getSupportActionBar();
                    if(!EventDatabase.instance.eventData.name.isEmpty() && actionBar != null)
                        actionBar.setTitle(EventDatabase.instance.eventData.name);
                }
            });
    }

    /**
     * This function is called when the memberDB has been updated, the callback is handled in RefreshMemberDB()
     */
    public void UpdateStatsUI()
    {
        if(EventDatabase.instance == null)
            return;

        //Showing the two stats accordingly
        if(EventDatabase.instance.stats != null){
            boolean showLStat = (EventDatabase.instance.stats.size() >= 2);
            boolean showRStat = (EventDatabase.instance.stats.size() >= 1);

            if(showLStat) {
                KeyValuePair lStat = EventDatabase.instance.stats.get(0);
                mLeftStatValue.setText(lStat.value);
                //mLeftStatValue.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorValid));
                mLeftStatDesc.setText(lStat.name);
            }
            mLeftStatValue.setVisibility(showLStat ? View.VISIBLE : View.INVISIBLE);
            mLeftStatDesc.setVisibility(showLStat ? View.VISIBLE : View.INVISIBLE);

            if(showRStat) {
                KeyValuePair rStat = EventDatabase.instance.stats.get(1);
                mRightStatValue.setText(rStat.value);
                //mRightStatValue.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorInvalid));
                mRightStatDesc.setText(rStat.name);
            }
            mRightStatValue.setVisibility(showRStat ? View.VISIBLE : View.INVISIBLE);
            mRightStatDesc.setVisibility(showRStat ? View.VISIBLE : View.INVISIBLE);
        }

        //Show hide the checkin toggle depending on the event type
        if(EventDatabase.instance.eventData != null) {
            if (EventDatabase.instance.eventData.checkinType == EventData.CheckinType.Counter)
                SetCheckInToggle(true); //NB: set to false to hide toggle UI, if it is not wanted
            else
                SetCheckInToggle(true);
        }
    }

    private void SetCheckInToggle(boolean isVisible)
    {
        mCheckInSwitch.setVisibility(isVisible ? View.VISIBLE : View.GONE);
        mCheckInSwitchLabel_In.setVisibility(isVisible ? View.VISIBLE : View.GONE);
        mCheckInSwitchLabel_Out.setVisibility(isVisible ? View.VISIBLE : View.GONE);
        if(!isVisible && !mCheckInSwitch.isChecked())
            mCheckInSwitch.setChecked(true);
    }

    //===Transition to  Member List Activity===
    public void StartMemberListActivity(View view)
    {
        Intent intent = new Intent(this, MemberListActivity.class);
        startActivity(intent);
    }

    private static final int BACKBUTTON_REPEAT_TIME = 2000;
    private static long timeBackPressed;

    @Override
    public void onBackPressed() {   //Used to press back twice to exit scanning screen, prevent accidental logouts
        if (timeBackPressed + BACKBUTTON_REPEAT_TIME > System.currentTimeMillis())
            super.onBackPressed();
        else
            Toast.makeText(getBaseContext(), "Press Again To Logout", Toast.LENGTH_SHORT).show();

        timeBackPressed = System.currentTimeMillis();
    }
}
