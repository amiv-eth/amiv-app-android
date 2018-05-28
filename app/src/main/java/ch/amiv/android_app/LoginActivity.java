package ch.amiv.android_app;

import android.content.Intent;
import android.net.Uri;
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
    private static final String API_OAUTH_URL = "http://192.168.1.105:5000/oauth?response_type=token&client_id=AndroidApp&redirect_uri=http://localhost&state=";
    private String CSRFState = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        webView = findViewById(R.id.WebView);

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
                        StartMainActivity();
                    }
                    else
                        view.loadUrl(GenerateOAuthUrl()); //states do not match so retry login, possible CSRF attack occured
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

    private void StartMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
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
            super.onBackPressed();
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
