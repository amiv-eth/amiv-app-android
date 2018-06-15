package ch.amiv.android_app.jobs;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ColorFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import ch.amiv.android_app.R;
import ch.amiv.android_app.core.LoginActivity;
import ch.amiv.android_app.core.Requests;
import ch.amiv.android_app.core.Settings;
import ch.amiv.android_app.core.UserInfo;
import ch.amiv.android_app.util.CustomNetworkImageView;

/**
 * The activity/screen used for showing a selected job in detail.
 * This mainly displays stored info about the job, eg description and also fetches more such as images. Also handles registering for and job and the possible outcomes
 */
public class JobDetailActivity extends AppCompatActivity {

    private int jobGroup = 0;
    private int jobIndex = 0;
    private JobInfo job(){  //Used to easily access the activities job
        if(!hasJob())
            return null;
        return Jobs.sortedJobs.get(jobGroup).get(jobIndex);
    }

    private boolean hasJob(){
        if(jobGroup >= Jobs.sortedJobs.size() || jobIndex >= Jobs.sortedJobs.get(jobGroup).size()){
            Log.e("jobs", "JobDetailActivity given invalid job indexes, (group, index) = (" + jobGroup + ", " + jobIndex + "), with sortedJobs size of 1st dim: " + Jobs.sortedJobs.size());
            return false;
        }
        return true;
    }

    private CustomNetworkImageView logoImage;
    private ScrollView scrollView;
    private Button openPdfButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.jobs_activity_detail);
        GetIntentData();
        InitUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        GetIntentData();
    }

    /**
     * This will retrieve the eventIndexes to display, is only set when we originate from the MainActivity, where the int is added to the intent.
     */
    private void GetIntentData (){
        if(jobGroup == 0 && jobIndex == 0) {
            Intent intent = getIntent();
            if(intent.hasExtra("jobGroup") && intent.hasExtra("jobIndex")) {
                jobGroup = intent.getIntExtra("jobGroup", 0);
                jobIndex = intent.getIntExtra("jobIndex", 0);
            }
        }
    }

    /**
     * This initialises UI variables and sets up various UI elements
     */
    private void InitUI (){
        //Set up toolbar and back button
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Check that we have been given a job that exists else return to the calling activity
        if(!hasJob()) {
            Log.e("jobs", "invlaid job index selected during InitUI(), (groupIndex, jobIndex): (" + jobGroup + "," + jobIndex + "), total job size" + Jobs.jobInfos.size() + ". Ensure that you are not clearing/overwiting the jobs list while viewing a job.");
            onBackPressed();
            return;
        }

        //Link up variables with UI elements from the layout xml
        scrollView = findViewById(R.id.scrollView_event);
        logoImage = findViewById(R.id.companyLogo);
        openPdfButton = findViewById(R.id.openPdf);

        ((TextView) findViewById(R.id.companyTitle)).setText(job().company);
        ((TextView) findViewById(R.id.jobTitle)).setText(job().GetTitle(getResources()));
        ((TextView) findViewById(R.id.jobDescription)).setText(job().GetDescription(getResources()));

        DateFormat dateFormat = new SimpleDateFormat("dd - MMM - yyyy HH:mm", getResources().getConfiguration().locale);
        ((TextView) findViewById(R.id.dateCreated)).setText(getResources().getString(R.string.date_created) + ": " + dateFormat.format(job().time_created).toString());

        //LoadEventImage();
        logoImage.setImageUrl(job().GetLogoUrl(), Requests.GetImageLoader(getApplicationContext()));
        logoImage.onImageLoaded = new Requests.OnDataReceivedCallback() {
            @Override
            public void OnDataReceived() {
                logoImage.setColorFilter(null);
                logoImage.setImageAlpha(1);
            }
        };

        UpdateOpenPdfButton();
    }

    /**
     * Will set the open pdf button to enabled only if a pdf exists
     */
    private void UpdateOpenPdfButton() {
        if(job().pdf_url != null && !job().pdf_url.isEmpty())
        {
            openPdfButton.setEnabled(true);
            openPdfButton.setText(R.string.open_pdf);
        }
        else {
            openPdfButton.setEnabled(false);
            openPdfButton.setText(R.string.no_pdf_found);
        }
    }

    public void OpenJobPdf(View view){
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(job().GetPdfUrl()));
        startActivity(browserIntent);
    }

    public void ScrollToTop (View view) {
        scrollView.fullScroll(ScrollView.FOCUS_UP);
    }
}
