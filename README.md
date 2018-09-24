# amiv App - Android
*The idea is to have all social features and helper/admin tools in one app.*

If you have an idea for an app feature, this can easily be added as a 'micro-app', an app accessed through the drawer in the main screen, see details below.
  
*Current Maintainer for Questions: rbarton@ethz.ch*

## To Start Developing
1. Install Android Studio, if android studio asks you to install extra stuff once opened, install it.
    Gradle, the build system, will have to build first before you can edit properly.
2. Stay Organised:
   1. Create a new branch on git, if the feature's branch does not yet exist. Do *not* commit directly on the master branch.
   2. Write code in the according java package `app/src/main/ch.amiv.android_app/my_package`. Create a new package if needed.
   3. Follow the naming convention for layouts: `package_activity_name`
   4. Try to save strings, colors, dimensions in the `res/values` xml files instead of hard-coding
4. Post issues on gitlab, if there are bugs or mid/long-term problems.

## App Overview
The app consists of the following activities (screens):
* **Main Activity** - has multiple pages and a side drawer. Quick access features belong here.
    * *Events list* - See list of events, tap to view details
    * *Jobs list* - See list of posted jobs
    * *Drawer* - Access to login, settings and microapps
* **Event Detail** - View and register for an event
* **Job Detail** - View a job with its full description
* **Login** - Login to the amiv API, can be started from anywhere
* **Intro** - App intro for setup and info, uses page viewer for multiple pages
    * **Splash Screen** - This is the *launcher activity*, it decides whether to enter the MainActivity or show the intro.
    Keep functionality, esp. layout, very simple. Layout is done with a theme for performance.
* **Settings**
* **Any MicroApps** - Accessible through the drawer in the *Main Activity*
    * **Check-In** - See Check-In Readme. Used at events to scan legi's, submit to a checkin server and display the response.
    * **Barcode ID** - Encodes user info to a barcode to be scanned by the Check-In app
    * ***Demo App*** - Example for how to add a micro-app

## Useful Classes
The `core` and `util` java packages may provide useful tools:
* **Settings** - Store variables in shared preferences easily, so they can be saved between sessions (ie. restarting the app). Get the amiv API token for requests.
    * *SetPref*, *GetPref*, *GetToken*
* **Request** - Used for making most server requests with the 'volley' libary. Use SendRequest to send a volley request, *do not create another RequestQueue.*
    * *SendRequest*, Fetch Events, User
* **UserInfo** - static storage of the current user's data
* **PersistentStorage** - Use this to save large amounts of data in a json
* **CustomNetworkImageView** - Use this in your UI to load images from the web

Also:
* **Events** - static access to all events, see EventInfo for all available variables 
* **Jobs** - static access to all jobs, see JobInfo for all available variables
* **LoginActivity** - can start this with startActivityForResult, if the user needs to be logged in. See EventDetailActivity for example

## To Add Your Own Micro-App
Micro-Apps are intended for isolated app features and can be accessed through the drawer in the main activity.
All the functionality that the app already has can be re-used to speed up development, e.g. login, user data, settings.

1. Create seperate branch on git
2. Create own java package
3. See `core/MicroApp.java` for how to add your microapp to the drawer
    1. Also worth seeing how the 'Demo Activity' is implemented
4. Write your app in the MainActivity.java of your package

### Advisory Merge Checklist
Some things that *should* be completed before a micro-app is merged into the master branch. In rough order of importance

1. *Testing*, should be ***almost* bug-free**, with no serious bugs like crashing. Should also not interfere with other parts of the app.
    1. Try to test on other devices, screen sizes
    2. Appropriate error handling for requests to a server, informative feedback to user.
2. *Navigation* to the app and back to the Main Activity
3. *English language support*, preferably German as well, use `strings[-de].xml`
4. Consistent theme and core UI features such as the toolbar (if used)
5. Fix orientation to portrait if landscape has not been tested
6. Try to make the code easily expansible, esp. for server requests where the response is likely to change.
7. *Overview code comments* (!) At least describe what each file does.

## Deploying an Update
*Note to **increment the version *code*** in the app level `build.gradle` before committing.*

We have set up Gitlab CI/CD to automatically test, build and deploy the app.
Therefore to deploy an update to the Google Play Store, in the Gitlab website go to CI/CD, then manually trigger the deploy stage.
*Requires sufficient access rights*

### Changing Store Page Content 
When updating store page content use the guidelines in: https://github.com/Triple-T/gradle-play-publisher
Therefore, fill the folders in `app/src/main/play` with the appropriate content.
To reset the content to what is live on the store page run `app/Tasks/play store/bootstrapReleasePlayResources` in the gradle tab in Android Studio (only works when the google-play-key.json is in the project, see CI variables on GitLab) 

*Please update both languages equally.*

### Errors with CI/CD
If a CI stage fails, when it shouldn't, you can view the terminal output on the Gitlab CI site under CI/CD. The `.gitlab-ci.yml` determines what is executed.
The error could be caused due to out of date versions in the gitlab-ci.yml, please update these and be *consistent* everywhere. Also see [Issue #1](https://gitlab.ethz.ch/amiv/amiv-app-android/issues/1)

### *Authors*
Roger Barton: rbarton@ethz.ch