package ch.amiv.android_app.util;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import ch.amiv.android_app.R;
import ch.amiv.android_app.core.Settings;
import static android.content.Context.INPUT_METHOD_SERVICE;

public final class Util {

    public static String BuildFileUrl (String urlExtension){
        StringBuilder s = new StringBuilder();
        s.append(urlExtension);
        if(s.charAt(0) == '/')
            s.deleteCharAt(0);
        s.insert(0, Settings.API_URL);
        return s.toString();
    }

    /**
     * Hides the soft keyboard
     */
    public static void HideKeyboard(Activity activity){
        InputMethodManager inputMethodManager =(InputMethodManager) activity.getSystemService(INPUT_METHOD_SERVICE);
        if(inputMethodManager != null && activity.getCurrentFocus() != null)
            inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
    }

    /**
     * Will convert raw text to formatted text, e.g. converting \n to a new line
     * @param source
     * @return
     */
    public static String ApplyStringFormatting(String source){
        return source.replaceAll("\\\\n", "\n");
    }

    /**
     * Adds the toolbar and back navigation if required. Note: needs to exist in the layout with id:toolbar
     * @param useBackNavigation whether to show the back arrow to navigate backwards to the previous activity
     * @return the toolbar
     */
    public static Toolbar SetupToolbar (AppCompatActivity activity, boolean useBackNavigation){
        Toolbar toolbar = activity.findViewById(R.id.toolbar);
        activity.setSupportActionBar(toolbar);

         if (activity.getSupportActionBar() == null)
             Log.e("toolbar", "getSupportActionBar returns null, toobar has not been set, back navigation with toolbar will not work");
         else {
             if(useBackNavigation)
                 activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
             activity.getSupportActionBar().setDisplayShowTitleEnabled(false);
         }

        return toolbar;
    }

    /**
     * Set for the keyboard to resize the window so the snackbars appear just above the keyboard
     * Be sure to set this back once activity finishes
     */
    public static void SetWindowResizing (Activity activity, boolean adjustToKeyboard){
        Window window = activity.getWindow();
        window.setSoftInputMode(adjustToKeyboard ? WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE : WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
    }
}
