package com.example.wonseokshin.myapplication;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;


public class WebViewOAuth extends ActionBarActivity {

    public static String EXTRA_URL = "extra_url";

    private int mScreenWidth;
    private int mScreenHeight;

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view_oauth);

        final String url = this.getIntent().getStringExtra(EXTRA_URL);

        webView = (WebView) findViewById(R.id.webView);
        webView.setWebViewClient(new MyWebViewClient());
        webView.clearCache(true);
        webView.loadUrl(url);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_web_view_oauth, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
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

    private void initAppDisplayConfig(){
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        hideActionBar();
        hideStatusBar();
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

    //from: http://javatechig.com/android/how-to-integrate-twitter-in-android-application
    class MyWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {

            if (url.contains(Const.CALLBACK_URL)) {
                Uri uri = Uri.parse(url);

				//pass results back to calling activity through intent
                String verifier = uri.getQueryParameter(Const.IEXTRA_OAUTH_VERIFIER);
                Intent resultIntent = new Intent();
                resultIntent.putExtra(Const.IEXTRA_OAUTH_VERIFIER, verifier);
                setResult(RESULT_OK, resultIntent);

				//end activity, close webview
                finish();
                return true;
            }
            return false;
        }
    }
}
