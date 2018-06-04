package ch.amiv.android_app;

import android.content.Intent;
import android.net.Uri;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.security.SecureRandom;

public class LoginActivity extends AppCompatActivity {

    WebView webView;
    private static final String API_OAUTH_URL = Settings.API_URL + "oauth?response_type=token&client_id=Android+App&redirect_uri=http://localhost:5000&state=";
    private String CSRFState = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        webView = findViewById(R.id.WebView);

        Intent intent = getIntent();
        String loginCause = intent.getStringExtra("cause");
        if(loginCause != null && loginCause.equals("register_event"))
            Snackbar.make(webView, "Need to be logged in to register", Snackbar.LENGTH_LONG).show();

        webView.loadUrl(GenerateOAuthUrl());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                Uri uri = Uri.parse(url);
                String token = uri.getQueryParameter("access_token");
                String state = uri.getQueryParameter("state");
                Log.e("token", "token received: " + token + ", from url: " + url);

                //Check state

                if(token != null && !token.isEmpty())
                {
                    if(state.equals(CSRFState))
                    {
                        Settings.SetToken(token, getApplicationContext());
                        //Notify and update UI
                        StartMainActivity(true);
                    }
                    else {
                        view.loadUrl(GenerateOAuthUrl()); //states do not match so retry login, possible CSRF attack occured
                        Snackbar.make(view, "Error Occured, Please Retry", Snackbar.LENGTH_LONG).show();
                        Log.e("login", "OAuth amiv api login: CSRF States do not match, token invalid! Potential CSRF attack occured, will redirect to login retry.");
                    }
                }


                super.onPageFinished(view, url);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {

                //super.onReceivedError(view, request, error);
            }
        });

        //Uncomment if js is required by website
        //webView.getSettings().setJavaScriptEnabled(true);
    }

    private void StartMainActivity(boolean sucess) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("login_sucess", sucess);
        startActivity(intent);

    }

    /**
     * @return Will set the csrfstate and return the url for accessing the api login page
     */
    private String GenerateOAuthUrl()
    {
        CSRFState = GenerateRandomString(16);
        return API_OAUTH_URL + CSRFState;
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            StartMainActivity(false);
        }
    }

    static final String charSet = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    /**
     * @return Generates a random string of the specified length
     */
    public static String GenerateRandomString(int length) {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for(int i = 0; i < length; i++)
            sb.append(charSet.charAt(random.nextInt(charSet.length())));

        return sb.toString();
    }


}
