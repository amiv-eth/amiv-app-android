package ch.amiv.android_app.util;

import android.app.Activity;
import android.view.inputmethod.InputMethodManager;
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
}
