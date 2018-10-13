package ch.amiv.android_app.jobs;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import ch.amiv.android_app.R;
import ch.amiv.android_app.core.Request;
import ch.amiv.android_app.util.ui.CustomNetworkImageView;
import ch.amiv.android_app.util.Util;

/**
 * The activity/screen used for showing a selected job in detail.
 * This mainly displays stored info about the job, eg description and also fetches more such as images. Also handles registering for and job and the possible outcomes
 */
public class JobDetailActivity extends AppCompatActivity {
    public static final class LauncherExtras {
        public static final String JOB_GROUP = "jobGroup";
        public static final String JOB_INDEX = "jobIndex";
        public static final String JOB_ID = "eventId";
        public static final String LOAD_JOBS = "loadJobs";
    }

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
    private Button downloadPdfButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.jobs_detail);
        GetIntentData();
        InitUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        GetIntentData();
    }

    /**
     * This will retrieve the jobIndexes to display, is only set when we originate from the MainActivity, where the int is added to the intent.
     */
    private void GetIntentData (){
        if(jobGroup == 0 && jobIndex == 0) {
            Intent intent = getIntent();
            if(intent.hasExtra(JobDetailActivity.LauncherExtras.JOB_GROUP) && intent.hasExtra(LauncherExtras.JOB_INDEX)) {
                jobGroup = intent.getIntExtra(JobDetailActivity.LauncherExtras.JOB_GROUP, 0);
                jobIndex = intent.getIntExtra(LauncherExtras.JOB_INDEX, 0);
            }
        }
    }

    /**
     * This initialises UI variables and sets up various UI elements
     */
    private void InitUI (){
        //Set up toolbar and back button
        Util.SetupToolbar(this, true);

        //Check that we have been given a job that exists else return to the calling activity
        if(!hasJob()) {
            Log.e("jobs", "invalid job index selected during InitUI(), (groupIndex, jobIndex): (" + jobGroup + "," + jobIndex + "), total job size" + Jobs.jobInfos.size() + ". Ensure that you are not clearing/overwriting the jobs list while viewing a job.");
            onBackPressed();
            return;
        }

        //Link up variables with UI elements from the layout xml
        scrollView = findViewById(R.id.scrollView);
        logoImage = findViewById(R.id.companyLogo);
        downloadPdfButton = findViewById(R.id.openPdf);

        ((TextView) findViewById(R.id.companyTitle)).setText(job().company);
        ((TextView) findViewById(R.id.jobTitle)).setText(job().GetTitle(getResources()));
        ((TextView) findViewById(R.id.jobDescription)).setText(job().GetDescription(getResources()));

        DateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy", getResources().getConfiguration().locale);
        ((TextView) findViewById(R.id.dateCreated)).setText(getResources().getString(R.string.date_created) + ": " + dateFormat.format(job().time_created).toString());
        ((TextView) findViewById(R.id.dateEnd)).setText(getResources().getString(R.string.date_available_until) + ": " + dateFormat.format(job().time_end).toString());

        //LoadEventImage();
        logoImage.setImageUrl(job().GetLogoUrl(), Request.GetImageLoader(getApplicationContext()));
        logoImage.onImageLoaded = new Request.OnDataReceivedCallback() {
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
            downloadPdfButton.setEnabled(true);
            downloadPdfButton.setText(R.string.open_pdf);
        }
        else {
            downloadPdfButton.setEnabled(false);
            downloadPdfButton.setText(R.string.no_pdf_found);
        }
    }

    /**
     * Will start the pdf download as a notification to the downloads folder
     */
    public void OpenJobPdf(View view) {
        OpenJobPdf(true);
    }

    //Retry downloading when the permissions have changed, but dont ask again
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        OpenJobPdf(false);
    }

    public void OpenJobPdf(boolean askForPermission) {
        if (job().pdf_url.isEmpty()) {
            UpdateOpenPdfButton();
            return;
        }

        //Check first if we have the permission to write a file
        if (askForPermission && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) { //Get permission to write to downloads
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                //Add popup
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
            }
            else
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            return;

        Uri uri = Uri.parse(job().GetPdfUrl());
        String savePath = job().GetTitle(getResources());
        savePath = savePath.replace(' ', '-');
        savePath = savePath.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");  //remove illegal characters
        savePath = "/amiv/" + savePath;
        if(!savePath.substring(savePath.length() -4, savePath.length()).equalsIgnoreCase( ".pdf"))
            savePath += ".pdf";


        DownloadManager.Request request = new DownloadManager.Request(uri)
            .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE)
            .setAllowedOverRoaming(false)
            .setTitle(job().GetTitle(getResources()))
            .setDescription(getResources().getString(R.string.job_pdf_description))
            .setVisibleInDownloadsUi(true)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, Uri.parse(savePath).toString());


        Log.e("download", "Download Job PDF url: " + uri.toString() + " with filepath: " + Uri.parse(savePath));
        ((DownloadManager)getSystemService(Context.DOWNLOAD_SERVICE)).enqueue(request);
        Snackbar.make(downloadPdfButton, R.string.see_notification, Snackbar.LENGTH_SHORT).show();
    }

    public void ScrollToTop (View view) {
        scrollView.fullScroll(ScrollView.FOCUS_UP);
    }
}
