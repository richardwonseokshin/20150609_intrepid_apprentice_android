package com.example.wonseokshin.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Point;
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
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import twitter4j.Status;
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
    private TextView mtvNewsFeed;

    private Twitter mTwitter;
    private RequestToken requestToken;
    private SharedPreferences mSharedPreferences;
    private Thread threadSignIn = null;

    private int mScreenWidth = -1;
    private int mScreenHeight = -1;
    private String mStringToken;
    private String mStringTokenSecret;
    private Button mbSignOut;
    private ScrollView msvNewsFeed;
    private LinearLayout mllNewsFeed;
    private LinearLayout mllPostTweet;
    private EditText metPostTweet;
    private Button mbPostTweet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //set orientation to portrait, hide actionbar, hide status bar
        initAppDisplayConfig();

        //detect device api build version, get screen width and height
        setScreenDimMemberVars();

        //set sharedprefs member variable
        setSharedPrefsToMemberVar(Const.PREFERENCE_NAME);

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


        InputMethodManager imm = (InputMethodManager)getSystemService(MainActivity.this.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mllBackground.getWindowToken(), 0);
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

        if (resultCode == Activity.RESULT_OK && requestCode == MainActivity.WEBVIEW_INTENT_REQUEST_CODE) {
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

                mStringTokenSecret = accessToken.getTokenSecret();
                mStringToken = accessToken.getToken();

                //Temporary toast to test for login information
                Toast.makeText(MainActivity.this, "Returned From Webview Activity\nuserID: " + userID +
                                "\nusername :" + username +
                                "\naccess token: " + mStringToken +
                                "\naccess token secret: " + mStringTokenSecret
                        ,
                        Toast.LENGTH_LONG).show();

                saveAccessTokenToSharedPrefs(accessToken);
                getUserNewsFeed();

                //update the main screen ui
                mbSignIn.setVisibility(View.GONE);
                mbSignOut.setVisibility(View.VISIBLE);
                mbSignOut.setText("Sign out of " + accessToken.getScreenName());
                mbSignIn.postInvalidate();
                mbSignOut.postInvalidate();
                msvNewsFeed.setVisibility(View.VISIBLE);
                mivLoginLogo.setVisibility(View.GONE);
                mllPostTweet.setVisibility(View.VISIBLE);

            } catch (Exception e) {
                Log.e("Twitter Login Failed", e.getMessage());
            }
        }
    }

    public void saveAccessTokenToSharedPrefs(AccessToken accessToken) {
        SharedPreferences.Editor e = mSharedPreferences.edit();
        e.putString(Const.PREF_KEY_TOKEN, accessToken.getToken());
        e.putString(Const.PREF_KEY_SECRET, accessToken.getTokenSecret());
        e.putString(Const.SCREENNAME, accessToken.getScreenName());
        e.putString(Const.USER_ID, accessToken.getUserId() + "");
        e.commit();
    }

    //twitter4j examples page: http://twitter4j.org/en/code-examples.html
    //modified: https://github.com/yusuke/twitter4j/blob/master/twitter4j-examples/src/main/java/twitter4j/examples/timeline/GetHomeTimeline.java
    public void getUserNewsFeed() {
        try {
            if (mTwitter == null) {
                ConfigurationBuilder cb = new ConfigurationBuilder();
                cb.setDebugEnabled(true)
                        .setOAuthConsumerKey(Const.CONSUMER_KEY)
                        .setOAuthConsumerSecret(Const.CONSUMER_SECRET)
                        .setOAuthAccessToken(mSharedPreferences.getString(Const.PREF_KEY_TOKEN, null))
                        .setOAuthAccessTokenSecret(mSharedPreferences.getString(Const.PREF_KEY_SECRET, null));

                TwitterFactory tf = new TwitterFactory(cb.build());
                mTwitter = tf.getInstance();
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mtvNewsFeed.setText("Twitter Feed\n");
                }
            });

            final User user = mTwitter.verifyCredentials();
            List<Status> statuses = mTwitter.getHomeTimeline();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mtvNewsFeed.append("Showing @" + user.getScreenName() + "'s home timeline.\n");
                    mtvNewsFeed.postInvalidate();
                }
            });

            for (final Status status : statuses) {
                final Status statusForUI = status;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mtvNewsFeed.append("\n\n@" + statusForUI.getUser().getScreenName() + "\n- " + statusForUI.getText());
                        mtvNewsFeed.postInvalidate();
                    }
                });
            }
        } catch (TwitterException te) {
            te.printStackTrace();
            //System.out.println("Failed to get timeline: " + te.getMessage());
            System.exit(-1);
        }
    }

    private void setScreenDimMemberVars() {
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentapiVersion >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            mScreenWidth = size.x;
            mScreenHeight = size.y;
        } else {
            Display display = getWindowManager().getDefaultDisplay();
            mScreenWidth = display.getWidth();  // deprecated
            mScreenHeight = display.getHeight();  // deprecated
        }
    }

    /**
     * Creates and shows splash screen and login, handles splash screen removal
     *
     * @return Linearlayout a mllBackground is assigned a linearlayout, caller must set as contentview
     * @params int splashscreenShowDuration, time in milliseconds to show splashscreen
     */
    private LinearLayout createAndShowSplashAndLogin(int splashscreenShowDuration) {
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

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
        mivLoginLogo = new ImageView(this);
        mivLoginLogo.setBackgroundResource(R.drawable.loginlogo);

        String userScreenName = mSharedPreferences.getString(Const.SCREENNAME, null);
        //String userID = mSharedPreferences.getString(Const.USER_ID, null);

        mbSignIn = new Button(this);
        mbSignIn.setText("Sign in to Twitter");
        mbSignIn.setBackgroundResource(R.drawable.style_rounded_button);
        mbSignIn.setGravity(Gravity.CENTER);
        mbSignIn.setTextColor(Color.WHITE);
        mbSignIn.setPadding(0, 0, 0, 0);
        setSignInButtonOnClickListener(mbSignIn);
        mbSignOut = new Button(this);
        mbSignOut.setText("Sign out of " + userScreenName);
        mbSignOut.setBackgroundResource(R.drawable.style_rounded_button);
        mbSignOut.setGravity(Gravity.CENTER);
        mbSignOut.setTextColor(Color.WHITE);
        mbSignOut.setPadding(0, 0, 0, 0);
        setSignOutButtonOnClickListener(mbSignOut);
        mtvNewsFeed = new TextView(MainActivity.this);
        mtvNewsFeed.setTextColor(getResources().getColor(R.color.twitter_blue));
        mtvNewsFeed.setText("Twitter Feed\n");
        mtvNewsFeed.setBackgroundColor(Color.DKGRAY);
        mtvNewsFeed.setPadding(5, 5, 5, 5);
        mllNewsFeed = new LinearLayout(MainActivity.this);
        mllNewsFeed.addView(mtvNewsFeed);
        msvNewsFeed = new ScrollView(MainActivity.this);
        msvNewsFeed.addView(mllNewsFeed);

        mllPostTweet = new LinearLayout(MainActivity.this);
        mllPostTweet.setOrientation(LinearLayout.HORIZONTAL);
        mllPostTweet.setGravity(Gravity.CENTER);
        metPostTweet = new EditText(MainActivity.this);
        metPostTweet.setHint("Tweet Message");
        metPostTweet.setBackgroundResource(R.drawable.style_rounded_edittext);
        metPostTweet.setGravity(Gravity.CENTER);
        mbPostTweet = new Button(MainActivity.this);
        mbPostTweet.setText("Post");
        mbPostTweet.setTextColor(Color.WHITE);
        mbPostTweet.setBackgroundResource(R.drawable.style_rounded_button);
        setOnClickListenerPostButton(mbPostTweet);
        mllPostTweet.addView(metPostTweet, (int) (mScreenWidth * .6), ViewGroup.LayoutParams.WRAP_CONTENT);
        View vSpacing = new View(this);
        vSpacing.setBackgroundColor(Color.argb(0, 0, 0, 0));
        mllPostTweet.addView(vSpacing, (int) (mScreenWidth * .02), (int) (mScreenWidth * .02));
        mllPostTweet.addView(mbPostTweet, (int) (mScreenWidth * .2), ViewGroup.LayoutParams.WRAP_CONTENT);


        vSpacing = new View(this);
        vSpacing.setBackgroundColor(Color.argb(0, 0, 0, 0));
        mllLoginScreen.addView(vSpacing, (int) (mScreenWidth * .6), (int) (mScreenWidth * .02));
        mllLoginScreen.addView(mbSignIn, (int) (mScreenWidth * .6), (int) (mScreenWidth * .1));
        mllLoginScreen.addView(mbSignOut, (int) (mScreenWidth * .6), (int) (mScreenWidth * .1));
        vSpacing = new View(this);
        vSpacing.setBackgroundColor(Color.argb(0, 0, 0, 0));
        mllLoginScreen.addView(vSpacing, (int) (mScreenWidth * .6), (int) (mScreenWidth * .02));
        mllLoginScreen.addView(mllPostTweet, (int) (mScreenWidth), ViewGroup.LayoutParams.WRAP_CONTENT);
        vSpacing = new View(this);
        vSpacing.setBackgroundColor(Color.argb(0, 0, 0, 0));
        mllLoginScreen.addView(vSpacing, (int) (mScreenWidth * .6), (int) (mScreenWidth * .02));
        mllLoginScreen.addView(mivLoginLogo, (int) (mScreenWidth * .6), (int) (mScreenWidth * .6));
        mllLoginScreen.addView(msvNewsFeed, (int) (mScreenWidth * .9), (int) (mScreenWidth * .9));

        mllBackground.addView(mllLoginScreen, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        //display the appropriate sign in/sign out button based on if tokens and user info is set in sharedprefs
        //retrieve the user newsfeed (if signed into app with auth token)
        if (prefTokensIsSet()) {
            mbSignIn.setVisibility(View.GONE);
            mbSignOut.setVisibility(View.VISIBLE);

            msvNewsFeed.setVisibility(View.VISIBLE);
            mivLoginLogo.setVisibility(View.GONE);

            mllPostTweet.setVisibility(View.VISIBLE);
            getUserNewsFeed();
        } else {
            mbSignIn.setVisibility(View.VISIBLE);
            mbSignOut.setVisibility(View.GONE);

            msvNewsFeed.setVisibility(View.GONE);
            mivLoginLogo.setVisibility(View.VISIBLE);

            mllPostTweet.setVisibility(View.GONE);
        }

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

    private void setOnClickListenerPostButton(final Button postButton){
        postButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO: post tweet
                String stringTweet = metPostTweet.getText().toString();
                if(stringTweet.length() > 140){
                    stringTweet = stringTweet.substring(0, 140);
                }
                try {
                    Status status = mTwitter.updateStatus(stringTweet);
                } catch (TwitterException e) {
                    e.printStackTrace();
                }

                getUserNewsFeed();
                //System.out.println("Successfully updated the status to [" + status.getText() + "].");
            }
        });
    }

    /**
     * Attaches an onclicklistenter to login to a twitter account. Note that the listener
     * will first attempt to log out of the currently logged in user if one exists when triggered.
     *
     * @param bSignIn the sign in button to which an onclicklistener will be attached
     */
    private void setSignInButtonOnClickListener(Button bSignIn) {
        mbSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                threadSignIn = new Thread() {
                    @Override
                    public void run() {
                        super.run();
                        //called to ensure that app's webview cache and shared prefs are cleared
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

    /**
     * Attaches an onclicklistenter to login to a twitter account. Note that the listener
     * will first attempt to log out of the currently logged in user if one exists when triggered.
     *
     * @param bSignOut the sign in button to which an onclicklistener will be attached
     */
    private void setSignOutButtonOnClickListener(Button bSignOut) {
        bSignOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logOutOfTwitter();
                Intent intentRestartApp = getBaseContext().getPackageManager()
                        .getLaunchIntentForPackage(getBaseContext().getPackageName());
                intentRestartApp.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intentRestartApp);
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

    private boolean prefTokensIsSet() {
        return mSharedPreferences.getString(Const.PREF_KEY_TOKEN, null) != null
                && mSharedPreferences.getString(Const.PREF_KEY_SECRET, null) != null
                && mSharedPreferences.getString(Const.SCREENNAME, null) != null
                && mSharedPreferences.getString(Const.USER_ID, null) != null;
    }

    /**
     * Removes the required sharedPrefs to sign out of app,
     * deletes cookies in the cookiemanager,
     * and sets the Twitter class's oauthaccesstoken to null
     */
    private void logOutOfTwitter() {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.remove(Const.PREF_KEY_TOKEN);
        editor.remove(Const.PREF_KEY_SECRET);
        editor.remove(Const.SCREENNAME);
        editor.remove(Const.USER_ID);
        editor.commit();

        CookieSyncManager.createInstance(this);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeSessionCookie();

        if (mTwitter != null)
            mTwitter.setOAuthAccessToken(null);
    }

    private void setThreadModeToStrict() {
        if (android.os.Build.VERSION.SDK_INT > 8) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
    }

    private void initAppDisplayConfig() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        hideActionBar();
        hideStatusBar();
    }

    private void hideActionBar() {
        try {
            getSupportActionBar().hide();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            getActionBar().hide();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void hideStatusBar() {
        View decorView = getWindow().getDecorView();

        // Hide the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
    }

    public void clearAppSharedPreferences(String prefName) {
        mSharedPreferences = getSharedPreferences(prefName, MODE_PRIVATE);
        mSharedPreferences.edit().clear();
        mSharedPreferences.edit().commit();
    }

    public void setSharedPrefsToMemberVar(String prefsName) {
        mSharedPreferences = getSharedPreferences(prefsName, MODE_PRIVATE);
    }
}
