package com.example.wonseokshin.myapplication;

import android.graphics.Color;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Display;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;


//git change test 2
public class MainActivity extends ActionBarActivity {

    int mScreenWidth = -1;
    int mScreenHeight = -1;
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


    public void hideActionBar(){
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

    public void hideStatusBar(){
        View decorView = getWindow().getDecorView();

        // Hide the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        hideActionBar();
        hideStatusBar();




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
            mbSignIn.setText("Sign In");
            mbSignIn.setBackgroundResource(R.drawable.style_rounded_button);
            mbSignIn.setGravity(Gravity.CENTER);
            mbSignIn.setTextColor(Color.WHITE);
            mbSignIn.setPadding(0, 0, 0, 0);


            View vSpacing = new View(this);
            vSpacing.setBackgroundColor(Color.argb(0, 0, 0, 0));
        mllLoginScreen.addView(vSpacing, (int) (mScreenWidth * .6), (int) ((int) (mScreenWidth * .05)));
        mllLoginScreen.addView(mivLoginLogo, (int) (mScreenWidth * .6), (int) (mScreenWidth * .6));
        mllLoginScreen.addView(mtvLoginIDLabel);
        mllLoginScreen.addView(metLoginID, (int) (mScreenWidth*.6), (int) ((int) (mScreenWidth*.1)));
        mllLoginScreen.addView(mtvLoginPasswordLabel);
        mllLoginScreen.addView(metLoginPassword, (int) (mScreenWidth*.6), (int) ((int) (mScreenWidth*.1)));
            vSpacing = new View(this);
            vSpacing.setBackgroundColor(Color.argb(0, 0, 0, 0));
        mllLoginScreen.addView(vSpacing, (int) (mScreenWidth*.6), (int) ((int) (mScreenWidth*.05)));
        mllLoginScreen.addView(mbSignIn, (int) (mScreenWidth*.6), (int) ((int) (mScreenWidth*.1)));




        mllBackground.addView(mllLoginScreen, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);


        android.os.Handler handler = new android.os.Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mllBackground.removeView(mllSplashScreen);
            }
        }, 2500);



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
}
