package ch.amiv.android_app.core;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import ch.amiv.android_app.R;
import ch.amiv.android_app.util.Util;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.core_settings);

        Util.SetupToolbar(this, true);

    }
}
