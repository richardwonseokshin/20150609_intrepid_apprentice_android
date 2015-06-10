package com.example.wonseokshin.myapplication;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

public class MainActivity extends ActionBarActivity {

    private static final String TAG = "INTREPID_TWITTER_CLIENT";
    private static final int WEBVIEW_INTENT_REQUEST_CODE = 101;//can be any number, used for starting webview intent

    private LinearLayout mllBackground;
    private LinearLayout mllSplashScreen;
    private LinearLayout mllLoginScreen;
    private ImageView mivLoginLogo;
    private ImageView mivSplashScreen;
    private TextView mtvLoginIDLabel;
    private EditText metLoginID;
    private TextView mtvLoginPasswordLabel;
    private EditText metLoginPassword;
    private Button mbSignIn;

    private Twitter mTwitter;
    private RequestToken requestToken;
    private SharedPreferences mSharedPreferences;
    private Thread threadSignIn = null;

    private int mScreenWidth = -1;
    private int mScreenHeight = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //set orientation to portrait, hide actionbar, hide status bar
        initAppDisplayConfig();

        //detect device api build version, get screen width and height
        setScreenDimMemberVars();

        //set sharedprefs member variable
        setSharedPrefsToMember(Const.PREFERENCE_NAME);

        /**
         * set thread policy to StrictMode, needed because of twitter4j api
         * OR starting activity (WebViewOAuth) from a worker thread and then calling loadUrl
         * when using oauth to sign in from app, see
         * http://stackoverflow.com/questions/21021657/android-webview-loadurl-does-not-work-when-coming-from-a-worker-thread
         */
        setThreadModeToStrict();

        //create initial login screen
        createAndShowSplashAndLogin(2500);

        setContentView(mllBackground);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            String verifier = data.getExtras().getString(Const.IEXTRA_OAUTH_VERIFIER);
            try {
                /**
                 *requestToken = twitter.getOAuthRequestToken(Const.CALLBACK_URL);
                 *get the request token, the user accesses the callback url through a browser or webview
                 *after the login from teh webview, the twitter object (factory.getinstance()) can
                 *retrieve the accessToken
                 */
                AccessToken accessToken = mTwitter.getOAuthAccessToken(requestToken, verifier);

                final long userID = accessToken.getUserId();
                final User user = mTwitter.showUser(userID);
                final String username = user.getName();

                //save user login to be persistent (use sharedprefs), optional

                //Temporary toast to test for login information
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "userID: " + userID + "\nusername:" + username, Toast.LENGTH_LONG).show();
                    }
                });

            } catch (Exception e) {
                Log.e("Twitter Login Failed", e.getMessage());
            }
        }
    }

    private void setScreenDimMemberVars(){
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentapiVersion >= Build.VERSION_CODES.HONEYCOMB_MR2){
            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            mScreenWidth = size.x;
            mScreenHeight = size.y;
        } else{
            Display display = getWindowManager().getDefaultDisplay();
            mScreenWidth = display.getWidth();  // deprecated
            mScreenHeight = display.getHeight();  // deprecated
        }
    }

    /**Creates and shows splash screen and login, handles splash screen removal
     * @params int splashscreenShowDuration, time in milliseconds to show splashscreen
     * @return Linearlayout a mllBackground is assigned a linearlayout, caller must set as contentview
     */
    private LinearLayout createAndShowSplashAndLogin(int splashscreenShowDuration){
        //The content frame
        mllBackground = new LinearLayout(this);
        mllBackground.setBackgroundColor(Color.argb(255, 85, 172, 238));
        //mllBackground.setGravity(Gravity.CENTER);//hmm... this didn't push views offscreen in the past...

        //The linearlayout containing the splash screen
        mllSplashScreen = new LinearLayout(this);
        mllSplashScreen.setGravity(Gravity.CENTER);
        mivSplashScreen = new ImageView(this);
        mivSplashScreen.setBackgroundResource(R.drawable.splashscreen);
        mivSplashScreen.setScaleType(ImageView.ScaleType.FIT_CENTER);
        mllSplashScreen.addView(mivSplashScreen, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mllBackground.addView(mllSplashScreen, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        //The Login Screen
        mllLoginScreen = new LinearLayout(this);
            mllLoginScreen.setGravity(Gravity.CENTER_HORIZONTAL);
            mllLoginScreen.setOrientation(LinearLayout.VERTICAL);
        //mllLoginScreen.setBackgroundResource(R.drawable.splashscreen);//looks terrible
        mivLoginLogo = new ImageView(this);
            mivLoginLogo.setBackgroundResource(R.drawable.loginlogo);
        mtvLoginIDLabel = new TextView(this);
            mtvLoginIDLabel.setGravity(Gravity.CENTER);
            mtvLoginIDLabel.setText("Username");
            mtvLoginIDLabel.setTextSize(22);
            mtvLoginIDLabel.setPadding(0, 10, 0, 10);
            mtvLoginIDLabel.setTextColor(Color.WHITE);
        mtvLoginPasswordLabel = new TextView(this);
            mtvLoginPasswordLabel.setGravity(Gravity.CENTER);
            mtvLoginPasswordLabel.setText("Password");
            mtvLoginPasswordLabel.setTextSize(22);
            mtvLoginPasswordLabel.setPadding(0, 10, 0, 10);
            mtvLoginPasswordLabel.setTextColor(Color.WHITE);
        metLoginID = new EditText(this);
            metLoginID.setBackgroundResource(R.drawable.style_rounded_edittext);
        metLoginPassword = new EditText(this);
            metLoginPassword.setBackgroundResource(R.drawable.style_rounded_edittext);
        mbSignIn = new Button(this);
            mbSignIn.setText("Log in to Twitter");
            mbSignIn.setBackgroundResource(R.drawable.style_rounded_button);
            mbSignIn.setGravity(Gravity.CENTER);
            mbSignIn.setTextColor(Color.WHITE);
            mbSignIn.setPadding(0, 0, 0, 0);
            setSignInButtonOnClickListener(mbSignIn);

        View vSpacing = new View(this);
            vSpacing.setBackgroundColor(Color.argb(0, 0, 0, 0));
        mllLoginScreen.addView(vSpacing, (int) (mScreenWidth * .6), (int) ((int) (mScreenWidth * .05)));
        mllLoginScreen.addView(mivLoginLogo, (int) (mScreenWidth * .6), (int) (mScreenWidth * .6));
        vSpacing = new View(this);
            vSpacing.setBackgroundColor(Color.argb(0, 0, 0, 0));
        mllLoginScreen.addView(vSpacing, (int) (mScreenWidth*.6), (int) ((int) (mScreenWidth*.05)));
        mllLoginScreen.addView(mbSignIn, (int) (mScreenWidth*.6), (int) ((int) (mScreenWidth*.1)));

        mllBackground.addView(mllLoginScreen, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        //remove splash screen after specified amount of time
        android.os.Handler handler = new android.os.Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mllBackground.removeView(mllSplashScreen);
            }
        }, 2500);

        return mllBackground;
    }

    /**
     *  Attaches an onclicklistenter to login to a twitter account. Note that the listener
     *  will first attempt to log out of the currently logged in user if one exists when triggered.
     * @param mbSignIn    the sign in button to which an onclicklistener will be attached
     */
    private void setSignInButtonOnClickListener(Button mbSignIn) {
        mbSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                threadSignIn = new Thread() {
                    @Override
                    public void run() {
                        super.run();
                        logOutOfTwitter();
                        signInToTwitter();
                        try {
                            threadSignIn.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                };
                threadSignIn.start();
            }
        });
    }

    //modified: from http://javatechig.com/android/how-to-integrate-twitter-in-android-application
    private void signInToTwitter() {
        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        configurationBuilder.setOAuthConsumerKey(Const.CONSUMER_KEY);
        configurationBuilder.setOAuthConsumerSecret(Const.CONSUMER_SECRET);
        Configuration configuration = configurationBuilder.build();
        mTwitter = new TwitterFactory(configuration).getInstance();

        try {
            requestToken = mTwitter.getOAuthRequestToken(Const.CALLBACK_URL);

            final Intent intent = new Intent(this, WebViewOAuth.class);
            intent.putExtra(WebViewOAuth.EXTRA_URL, requestToken.getAuthenticationURL());
            startActivityForResult(intent, WEBVIEW_INTENT_REQUEST_CODE);
        } catch (TwitterException e) {
            e.printStackTrace();
        }
    }

    /**
     * Removes the required sharedPrefs to sign out of app,
     * deletes cookies in the cookiemanager,
     * and sets the Twitter class's oauthaccesstoken to null
     */
    private void logOutOfTwitter(){
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.remove(Const.PREF_KEY_TOKEN);
        editor.remove(Const.PREF_KEY_SECRET);
        editor.commit();

        CookieSyncManager.createInstance(this);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeSessionCookie();

        if(mTwitter != null)
            mTwitter.setOAuthAccessToken(null);
        //mTwitter.shutdown();
    }

    private void setThreadModeToStrict(){
        if (android.os.Build.VERSION.SDK_INT > 8) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
    }

    private void initAppDisplayConfig(){
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        hideActionBar();
        hideStatusBar();
    }

    private void hideActionBar(){
        try{
            getSupportActionBar().hide();
        }
        catch (Exception e){
            e.printStackTrace();
        }

        try{
            getActionBar().hide();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private void hideStatusBar(){
        View decorView = getWindow().getDecorView();

        // Hide the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
    }


    public void clearAppSharedPreferences(String prefName){
        mSharedPreferences = getSharedPreferences(prefName, MODE_PRIVATE);
        mSharedPreferences.edit().clear();
        mSharedPreferences.edit().commit();
    }

    public void setSharedPrefsToMember(String prefsName){
        mSharedPreferences = getSharedPreferences(prefsName, MODE_PRIVATE);
    }
    /*
    private void initTwitterComponents(){
        mSharedPreferences = getSharedPreferences(Const.PREFERENCE_NAME, MODE_PRIVATE);

        Uri uri = getIntent().getData();
        if (uri != null && uri.toString().startsWith(Const.CALLBACK_URL)) {
            String verifier = uri.getQueryParameter(Const.IEXTRA_OAUTH_VERIFIER);
            try {
                AccessToken accessToken = mTwitter.getOAuthAccessToken(requestToken, verifier);
                SharedPreferences.Editor e = mSharedPreferences.edit();
                e.putString(Const.PREF_KEY_TOKEN, accessToken.getToken());
                e.putString(Const.PREF_KEY_SECRET, accessToken.getTokenSecret());
                e.clear();
            } catch (final Exception e) {
                Log.e(TAG, e.getMessage());
            }
        }

    }
    */

    /*
    protected void onResume() {
        super.onResume();

        if (isConnected()) {
            String oauthAccessToken = mSharedPreferences.getString(Const.PREF_KEY_TOKEN, "");
            String oAuthAccessTokenSecret = mSharedPreferences.getString(Const.PREF_KEY_SECRET, "");

            ConfigurationBuilder confbuilder = new ConfigurationBuilder();
            Configuration conf = confbuilder
                    .setOAuthConsumerKey(Const.CONSUMER_KEY)
                    .setOAuthConsumerSecret(Const.CONSUMER_SECRET)
                    .setOAuthAccessToken(oauthAccessToken)
                    .setOAuthAccessTokenSecret(oAuthAccessTokenSecret)
                    .build();
            //twitterStream = new TwitterStreamFactory(conf).getInstance();

            //buttonLogin.setText(R.string.label_disconnect);
            //getTweetButton.setEnabled(true);
        } else {
            //buttonLogin.setText(R.string.label_connect);
        }
    }
    */

    /*
    private boolean isConnected() {
        return mSharedPreferences.getString(Const.PREF_KEY_TOKEN, null) != null;
    }
    */
}
