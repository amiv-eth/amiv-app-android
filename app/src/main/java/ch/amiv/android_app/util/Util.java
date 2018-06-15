package ch.amiv.android_app.util;

import ch.amiv.android_app.core.Settings;

public final class Util {

    public static String BuildFileUrl (String urlExtension){
        StringBuilder s = new StringBuilder();
        s.append(urlExtension);
        if(s.charAt(0) == '/')
            s.deleteCharAt(0);
        s.insert(0, Settings.API_URL);
        return s.toString();
    }
}
