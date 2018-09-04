package ch.amiv.android_app.core;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.design.widget.NavigationView;
import android.view.Menu;
import android.view.MenuItem;
import ch.amiv.android_app.R;

/**
 * This class handles all microapps accessibly via the drawer in the MainActivity.
 * To add your own:
 * 1. add to the apps array and a unique int to the MicroAppPresets
 * 2. Add a menu item to the core_ac_main_drawer.xml, there you can set the title (use a resource string) and its icon.
 * For reference there is a commented out demo app
 */
public class MicroApp {
    //This is the list of microapps, add yours here, then add its index to the MicroAppPresets for easy access to the data
    public static MicroApp[] apps = {new MicroApp(UserInfo.AccessLevel.EMAIL, R.id.nav_checkin, ch.amiv.android_app.checkin.MainActivity.class),
                                     new MicroApp(UserInfo.AccessLevel.EMAIL, R.id.nav_barcode_id, ch.amiv.android_app.checkin.BarcodeIdActivity.class)/*,
                                     new MicroApp(UserInfo.AccessLevel.LOGIN, R.id.nav_demo, ch.amiv.android_app.demo.MainActivity.class)*/};

    public static final class MicroAppPresets {
        public static final int CHECKIN = 0;
        public static final int BARCODE_ID = 1;
        //public static final int DEMO = 2
    }

    public int accessLevel;     //Use an UserInfo.AccessLevel
    public int drawerMenuId;    //The id of the menu in core_ac_main_drawer.xml
    public Class<?> activityClass;  //The class of your activity in the form MyActivity.class,

    public MicroApp (int accessLevel_, int drawerMenuId_, Class<?> activityClass_){
        accessLevel = accessLevel_;
        drawerMenuId = drawerMenuId_;
        activityClass = activityClass_;
    }

    /**
     * Launches the microapp if the user is authorised
     */
    public void LaunchApp (Context context){
        if(!UserInfo.IsAuthorised(accessLevel, context))
            return;

        Intent intent = new Intent(context, activityClass);
        context.startActivity(intent);
    }

    /**
     * Will update the list of apps in the drawer accessible to the current user. Will only work in the MainActivity
     */
    public static void RefreshDrawer(final Activity activity){
        if(activity == null || !(activity instanceof MainActivity))
            return;

        NavigationView drawer = activity.findViewById(R.id.nav_view);
        if(drawer == null)
            return;
        Menu menu = drawer.getMenu();


        for (int i = 0; i < apps.length; i++){
            if(apps[i] == null || apps[i].drawerMenuId == 0 || menu.findItem(apps[i].drawerMenuId) == null)
                continue;

            MenuItem menuItem = menu.findItem(apps[i].drawerMenuId);
            boolean isVisible = UserInfo.IsAuthorised(apps[i].accessLevel, activity.getApplicationContext());
            menuItem.setVisible(isVisible);

            //Set to launch activity when clicked
            final int index = i;
            menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    MicroApp.apps[index].LaunchApp(activity.getApplicationContext());
                    return true;
                }
            });
        }
    }
}
