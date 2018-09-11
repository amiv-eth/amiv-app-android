package ch.amiv.android_app.core;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import ch.amiv.android_app.R;
import ch.amiv.android_app.util.Util;

/**
 * This handles logging into the api, getting a token with the provided username and password
 */
public class LoginActivity extends AppCompatActivity {

    boolean isIntroLogin;

    EditText userField;
    EditText passwordField;
    Button submitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        isIntroLogin = !Settings.GetBoolPref(Settings.introDoneKey, getApplicationContext());

        //Set for the keyboard to resize the window so the snackbars appear just above the keyboard
        Util.SetWindowResizing(this, true);
        setContentView(R.layout.core_activity_login);

        //Show skip button if this is in the intro
        Button btnSkip = findViewById(R.id.buttonSkip);
        if (isIntroLogin) {
            btnSkip.setVisibility(View.VISIBLE);
            btnSkip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ReturnToCallingActivity(false, false);
                }
            });
        }
        else {
            btnSkip.setVisibility(View.GONE);
        }

        Util.SetupToolbar(this, true);

        //Link UI elements to xml
        userField = findViewById(R.id.usernameField);
        passwordField = findViewById(R.id.passwordField);
        submitButton = findViewById(R.id.submitLoginButton);
        SetSubmitButtonState(true, false);

        //This sets the action when pressing enter whilst editing the passwordfield so we immediately submit but dont close the keyboard
        passwordField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_ACTION_GO)    //Note:this needs to match the imeOptions in the layout file for the password field
                    SubmitLoginDetails(null);   //If we pressed the enter button then submit the details
                return true;    //return true to keep the keyboard open, in case the user has to re-enter details
            }
        });
    }

    /**
     * Checks and submits the details in the username and password fields to the server, handles the response as well
     * @param view Used for making this directly accessible from the layout file, can set to null otherwise
     */
    public void SubmitLoginDetails (View view) {
        final String username = userField.getText().toString().toLowerCase();
        final String password = passwordField.getText().toString();

        if(username.isEmpty()) {
            Snackbar.make(submitButton, R.string.snack_fill_fields, Snackbar.LENGTH_SHORT).show();
            return;
        }

        if(password.isEmpty()){
            UserInfo.SetEmailOnlyLogin(getApplicationContext(), username, false);
            SetSubmitButtonState(false, true);
            Settings.Vibrate(Settings.VibrateTime.NORMAL, getApplicationContext());
            ReturnToCallingActivity(true, false);
            return;
        }

        //Does a POST request to sessions to create a session and get a token. Does *not* use the OAuth process, see api docs
        StringRequest request = new StringRequest(com.android.volley.Request.Method.POST, Settings.API_URL + "sessions", null, null)
        {
            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                if(response != null) {
                    Log.e("request", "status Code: " + response.statusCode);

                    try {
                        final JSONObject json = new JSONObject(new String(response.data));

                        if(json.has("token")) {
                            Settings.SetToken(json.getString("token"), getApplicationContext());
                            SetSubmitButtonState(false, true);
                            Settings.Vibrate(Settings.VibrateTime.NORMAL, getApplicationContext());
                            ReturnToCallingActivity(true, false);
                        }
                        else{
                            Snackbar.make(userField, R.string.snack_error_retry, Snackbar.LENGTH_SHORT).show();
                            SetSubmitButtonState(true, false);
                        }

                        //Store the details in the current user, do this on the main thread to prevent multi thread errors, as several requests could be editing the userinfo at the same time otherwise
                        if(json.has("user")) {
                            submitButton.post(new Runnable() {
                                @Override
                                public void run() {
                                    UserInfo.UpdateCurrent(getApplicationContext(), json, true, false);
                                }
                            });
                        }

                        //Log.e("request", json.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                else
                    Log.e("request", "Request returned null response.");
                return super.parseNetworkResponse(response);
            }

            @Override
            protected VolleyError parseNetworkError(final VolleyError volleyError) {  //see comments at parseNetworkResponse()
                if(volleyError != null && volleyError.networkResponse != null)
                {
                    Snackbar.make(userField, R.string.invalid_login, Snackbar.LENGTH_SHORT).show();
                    Log.e("request", "status code: " + volleyError.networkResponse.statusCode + "\n" + new String(volleyError.networkResponse.data));
                }
                else
                {
                    Snackbar.make(userField, R.string.no_internet, Snackbar.LENGTH_SHORT).show();
                    Log.e("request", "Request returned null response.");
                }

                SetSubmitButtonState(true, false);
                return super.parseNetworkError(volleyError);
            }

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("username", username);
                params.put("password", password);
                return params;
            }
        };

        boolean wasSent = Request.SendRequest(request, getApplicationContext());
        if(wasSent)
            SetSubmitButtonState(false, false);
        else
            Snackbar.make(userField, R.string.no_internet, Snackbar.LENGTH_SHORT).show();
    }

    /**
     * Used to update the submit button to give relevant feedback
     * @param enable true if presses of the button are allowed
     * @param loginSuccess was the login successful or not
     */
    private void SetSubmitButtonState (final boolean enable, final boolean loginSuccess) {
        submitButton.post(new Runnable() {
            @Override
            public void run() {
                submitButton.setEnabled(enable);
                submitButton.setText(enable ? R.string.login_title : (loginSuccess ? R.string.success : R.string.wait));
            }
        });
    }

//region -   ==Navigating Back to calling activity===

    /**
     * This is used for backwards navigation using the back arrow in the toolbar
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            ReturnToCallingActivity(false, true);
            return true;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        ReturnToCallingActivity(false, true);
    }

    /**
     * Will return to the calling activity and pass the login success parameter
     * @param success is the user now logged in
     */
    private void ReturnToCallingActivity(final boolean success, final boolean canceled) {
        final Activity activity = this;
        submitButton.post(new Runnable() {
            @Override
            public void run() {
                Util.SetWindowResizing(activity, false); //reset the keyboard and window layout to how it was before
                Intent intent = new Intent();
                intent.putExtra("login_success", success);
                setResult(canceled ? RESULT_CANCELED : RESULT_OK, intent);
                finish();   //return to the calling activity
            }
        });
    }
//endregion
}
