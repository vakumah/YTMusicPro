package com.ytmusic.pro;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.webkit.SslErrorHandler;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class MainActivity extends Activity {

    private YTMusicWebview webView;
    private ProgressBar progressBar;
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "YTMusicProPrefs";
    private static final String MUSIC_URL = "https://music.youtube.com/";
    private static final int PERMISSION_REQUEST_CODE = 100;
    private String injectionScript = "";
    private android.content.BroadcastReceiver mediaReceiver;
    private android.content.BroadcastReceiver networkReceiver;
    private boolean hasShownOfflineMessage = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressBar = findViewById(R.id.progress_loader);
        setupStatusBar();

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        loadInjectionScript();
        requestNotificationPermission();
        setupNetworkReceiver();
        initWebView();
        setupMediaReceiver();
    }

    private void setupStatusBar() {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(android.graphics.Color.BLACK);
    }

    private void loadInjectionScript() {
        try {
            InputStream is = getAssets().open("inject.js");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            injectionScript = new String(buffer, "UTF-8");
        } catch (Exception e) {
            injectionScript = "";
        }
    }
    
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        PERMISSION_REQUEST_CODE);
            }
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission denied. Media controls may not work.", 
                        Toast.LENGTH_LONG).show();
            }
        }
    }
    
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = 
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }
    
    private void loadErrorPage() {
        try {
            InputStream is = getAssets().open("error.html");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String errorHtml = new String(buffer, "UTF-8");
            webView.loadDataWithBaseURL("file:///android_asset/", errorHtml, "text/html", "UTF-8", null);
        } catch (Exception e) {
            webView.loadData("<html><body style='background:#000;color:#fff;text-align:center;padding:50px;'>"
                    + "<h2>Connection Error</h2><p>Please check your internet connection.</p></body></html>",
                    "text/html", "UTF-8");
        }
    }
    
    private void setupNetworkReceiver() {
        networkReceiver = new android.content.BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (isNetworkAvailable()) {
                    if (hasShownOfflineMessage && webView.getUrl() != null 
                            && !webView.getUrl().contains("music.youtube.com")) {
                        webView.loadUrl(MUSIC_URL);
                        hasShownOfflineMessage = false;
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkReceiver, filter);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebView() {
        webView = findViewById(R.id.webview);
        
        // JavaScript and DOM Settings
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setDatabaseEnabled(true);
        webView.getSettings().setMediaPlaybackRequiresUserGesture(false);
        
        // Cache Settings for better performance
        webView.getSettings().setCacheMode(android.webkit.WebSettings.LOAD_DEFAULT);
        
        webView.getSettings().setUserAgentString(
                "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36");

        webView.addJavascriptInterface(new WebAppInterface(this), "YTMusicPro");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                if (url.contains("music.youtube.com")) {
                    injectCoreScripts();
                }
            }
            
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                progressBar.setVisibility(View.GONE);
                if (!isNetworkAvailable()) {
                    loadErrorPage();
                    hasShownOfflineMessage = true;
                }
            }
            
            @Override
            public void onReceivedError(WebView view, android.webkit.WebResourceRequest request,
                    android.webkit.WebResourceError error) {
                if (request.isForMainFrame()) {
                    progressBar.setVisibility(View.GONE);
                    if (!isNetworkAvailable()) {
                        loadErrorPage();
                        hasShownOfflineMessage = true;
                    }
                }
            }
            
            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, android.net.http.SslError error) {
                // For production: We proceed only for YouTube Music domain
                if (error.getUrl().contains("youtube.com") || error.getUrl().contains("google.com")) {
                    handler.proceed();
                } else {
                    handler.cancel();
                }
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();

                // Block known YouTube Ad Servers
                if (url.contains("doubleclick.net") || url.contains("googleads.g.doubleclick.net") ||
                        url.contains("pagead2.googlesyndication.com")) {
                    return new WebResourceResponse("text/plain", "utf-8",
                            new ByteArrayInputStream("".getBytes()));
                }

                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.contains("music.youtube.com") || url.contains("accounts.google.com")) {
                    return false;
                }
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
                return true;
            }
        });

        webView.setWebChromeClient(new WebChromeClient());

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(webView, true);
        }
        
        // Check network before loading
        if (isNetworkAvailable()) {
            webView.loadUrl(MUSIC_URL);
        } else {
            loadErrorPage();
            hasShownOfflineMessage = true;
        }
    }

    private void injectCoreScripts() {
        if (!injectionScript.isEmpty()) {
            webView.evaluateJavascript(injectionScript, null);
        } else {
            webView.evaluateJavascript("console.error('YTMusic Pro: Injection script not found');", null);
        }
    }

    public class WebAppInterface {
        Context mContext;

        WebAppInterface(Context c) {
            mContext = c;
        }

        @JavascriptInterface
        public void updateNotification(String title, String artist, String albumArtUrl, boolean isPlaying,
                long position, long duration) {
            Intent intent = new Intent(mContext, ForegroundService.class);
            intent.putExtra("title", title);
            intent.putExtra("artist", artist);
            intent.putExtra("albumArt", albumArtUrl);
            intent.putExtra("isPlaying", isPlaying);
            intent.putExtra("position", position);
            intent.putExtra("duration", duration);
            startService(intent);
        }

        @JavascriptInterface
        public void showToast(String message) {
            Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    private void setupMediaReceiver() {
        mediaReceiver = new android.content.BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getStringExtra("action");
                if (action == null)
                    return;

                switch (action) {
                    case ForegroundService.ACTION_PLAY:
                        webView.evaluateJavascript("document.querySelector('video').play()", null);
                        break;
                    case ForegroundService.ACTION_PAUSE:
                        webView.evaluateJavascript("document.querySelector('video').pause()", null);
                        break;
                    case ForegroundService.ACTION_NEXT:
                        webView.evaluateJavascript("document.querySelector('.next-button').click()", null);
                        break;
                    case ForegroundService.ACTION_PREV:
                        webView.evaluateJavascript("document.querySelector('.previous-button').click()", null);
                        break;
                    case ForegroundService.ACTION_SEEK:
                        long seconds = intent.getLongExtra("position", 0);
                        webView.evaluateJavascript(
                                "if(document.querySelector('video')) document.querySelector('video').currentTime = "
                                        + seconds,
                                null);
                        break;
                }
            }
        };
        registerReceiver(mediaReceiver, new android.content.IntentFilter("YTPRO_CONTROL"),
                android.content.Context.RECEIVER_EXPORTED);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaReceiver != null) {
            unregisterReceiver(mediaReceiver);
        }
        if (networkReceiver != null) {
            unregisterReceiver(networkReceiver);
        }
    }
}
