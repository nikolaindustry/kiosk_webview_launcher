package com.kiosk.webviewlauncher;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "KioskPrefs";
    private static final String PREF_URL = "web_url";
    private static final String DEFAULT_URL = "https://www.nikolaindustry.com";
    private static final int REQUEST_PERMISSIONS = 100;
    private static final int FILE_CHOOSER_REQUEST = 101;
    private static final int TAP_COUNT_THRESHOLD = 11;
    private static final long TAP_TIMEOUT_MS = 3000; // 3 seconds to complete all taps
    private static final int REFRESH_POINTERS = 3; // Three fingers for refresh
    private static final int REFRESH_MIN_DISTANCE_DP = 120; // Minimum swipe distance
    private static final long REFRESH_MAX_DURATION_MS = 1000; // Max time for swipe
    
    private WebView webView;
    private ValueCallback<Uri[]> fileUploadCallback;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private FrameLayout fullscreenContainer;
    
    // Tap detection variables
    private int tapCount = 0;
    private long firstTapTime = 0;
    private String currentLoadedUrl = "";
    
    // Refresh gesture variables
    private float gestureStartY = 0;
    private long gestureStartMs = 0;
    private boolean refreshTrackingEnabled = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        // Hide navigation and status bars for kiosk mode
        enableKioskMode();

        webView = findViewById(R.id.webView);
        fullscreenContainer = findViewById(R.id.fullscreen_container);
        
        // Set up tap gesture detector
        setupTapGestureDetector();
        
        // Request necessary permissions
        requestNecessaryPermissions();
        
        // Configure WebView settings for full web functionality
        configureWebView();
        
        // Load the configured URL
        loadConfiguredUrl();
    }

    private void enableKioskMode() {
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);
    }

    private void setupTapGestureDetector() {
        // Set touch listener on WebView since it consumes touch events
        webView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Handle multi-finger refresh gesture
                handleRefreshGesture(event);
                
                // Handle single-finger tap for settings access
                if (event.getAction() == MotionEvent.ACTION_DOWN && event.getPointerCount() == 1) {
                    handleTap();
                }
                
                // Return false to allow WebView to handle touch normally
                return false;
            }
        });
    }

    private void handleRefreshGesture(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                if (event.getPointerCount() >= REFRESH_POINTERS) {
                    gestureStartY = averageY(event, REFRESH_POINTERS);
                    gestureStartMs = System.currentTimeMillis();
                    refreshTrackingEnabled = true;
                }
                break;
                
            case MotionEvent.ACTION_MOVE:
                if (refreshTrackingEnabled && event.getPointerCount() >= REFRESH_POINTERS) {
                    float currentAvgY = averageY(event, REFRESH_POINTERS);
                    long dt = System.currentTimeMillis() - gestureStartMs;
                    float dy = currentAvgY - gestureStartY; // positive = swipe down
                    int minDistancePx = dpToPx(REFRESH_MIN_DISTANCE_DP);
                    
                    if (dy >= minDistancePx && dt <= REFRESH_MAX_DURATION_MS) {
                        Toast.makeText(MainActivity.this, "Refreshing page...", 
                                Toast.LENGTH_SHORT).show();
                        webView.reload();
                        refreshTrackingEnabled = false;
                    }
                }
                break;
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
                refreshTrackingEnabled = false;
                break;
        }
    }

    private float averageY(MotionEvent event, int maxPointers) {
        int count = Math.min(event.getPointerCount(), maxPointers);
        float sum = 0f;
        for (int i = 0; i < count; i++) {
            sum += event.getY(i);
        }
        return sum / count;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void handleTap() {
        long currentTime = System.currentTimeMillis();
        
        // Reset if timeout exceeded
        if (tapCount > 0 && (currentTime - firstTapTime) > TAP_TIMEOUT_MS) {
            tapCount = 0;
        }
        
        // First tap or reset
        if (tapCount == 0) {
            firstTapTime = currentTime;
        }
        
        tapCount++;
        
        // Check if threshold reached
        if (tapCount >= TAP_COUNT_THRESHOLD) {
            tapCount = 0; // Reset counter
            Toast.makeText(this, "Opening Settings...", Toast.LENGTH_SHORT).show();
            openSettings();
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView() {
        WebSettings webSettings = webView.getSettings();
        
        // Enable JavaScript (required for modern web apps and payment gateways)
        webSettings.setJavaScriptEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        
        // Enable DOM storage (required for many web apps)
        webSettings.setDomStorageEnabled(true);
        
        // Enable database storage
        webSettings.setDatabaseEnabled(true);
        
        // Enable file access
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        
        // Enable mixed content (HTTP and HTTPS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        
        // Enable zoom controls
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        
        // Enable multiple windows (required for popups and payment gateways)
        webSettings.setSupportMultipleWindows(true);
        
        // Set user agent to desktop mode for better compatibility
        webSettings.setUserAgentString(webSettings.getUserAgentString());
        
        // Enable geolocation
        webSettings.setGeolocationEnabled(true);
        
        // Enable media playback
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        
        // Load images automatically
        webSettings.setLoadsImagesAutomatically(true);
        
        // Enable safe browsing
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            webSettings.setSafeBrowsingEnabled(true);
        }
        
        // Set cache mode
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        
        // Enable cookies (required for sessions and payments)
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(webView, true);
        }
        
        // Set WebViewClient to handle navigation
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // Allow all URLs to load in WebView
                view.loadUrl(url);
                return true;
            }
        });
        
        // Set WebChromeClient for advanced features
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, 
                    GeolocationPermissions.Callback callback) {
                // Grant geolocation permission
                callback.invoke(origin, true, false);
            }

            @Override
            public void onPermissionRequest(PermissionRequest request) {
                // Grant all requested permissions (camera, microphone, etc.)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    request.grant(request.getResources());
                }
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
                    FileChooserParams fileChooserParams) {
                // Handle file upload
                if (fileUploadCallback != null) {
                    fileUploadCallback.onReceiveValue(null);
                }
                fileUploadCallback = filePathCallback;

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    String[] acceptTypes = fileChooserParams.getAcceptTypes();
                    if (acceptTypes != null && acceptTypes.length > 0) {
                        intent.setType(acceptTypes[0]);
                    }
                }

                try {
                    startActivityForResult(Intent.createChooser(intent, "File Upload"), 
                            FILE_CHOOSER_REQUEST);
                } catch (Exception e) {
                    fileUploadCallback = null;
                    Toast.makeText(MainActivity.this, "Cannot open file chooser", 
                            Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
            }

            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, 
                    android.os.Message resultMsg) {
                // Handle popup windows and new window requests (for payment gateways)
                WebView newWebView = new WebView(MainActivity.this);
                newWebView.setLayoutParams(new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));
                
                // Configure the new WebView with same settings
                WebSettings newWebSettings = newWebView.getSettings();
                newWebSettings.setJavaScriptEnabled(true);
                newWebSettings.setJavaScriptCanOpenWindowsAutomatically(true);
                newWebSettings.setDomStorageEnabled(true);
                newWebSettings.setSupportMultipleWindows(true);
                
                newWebView.setWebChromeClient(new WebChromeClient() {
                    @Override
                    public void onCloseWindow(WebView window) {
                        // Remove the popup window
                        if (fullscreenContainer.getChildCount() > 0) {
                            fullscreenContainer.removeAllViews();
                            fullscreenContainer.setVisibility(View.GONE);
                        }
                    }
                });
                
                newWebView.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        view.loadUrl(url);
                        return true;
                    }
                });
                
                // Add the new WebView to fullscreen container
                fullscreenContainer.removeAllViews();
                fullscreenContainer.addView(newWebView);
                fullscreenContainer.setVisibility(View.VISIBLE);
                fullscreenContainer.bringToFront();
                
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(newWebView);
                resultMsg.sendToTarget();
                
                return true;
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                // Handle fullscreen video
                if (customView != null) {
                    onHideCustomView();
                    return;
                }
                
                customView = view;
                customViewCallback = callback;
                
                fullscreenContainer.addView(customView);
                fullscreenContainer.setVisibility(View.VISIBLE);
                fullscreenContainer.bringToFront();
                
                webView.setVisibility(View.GONE);
            }

            @Override
            public void onHideCustomView() {
                if (customView == null) {
                    return;
                }
                
                customView.setVisibility(View.GONE);
                fullscreenContainer.removeView(customView);
                fullscreenContainer.setVisibility(View.GONE);
                customView = null;
                
                webView.setVisibility(View.VISIBLE);
                
                if (customViewCallback != null) {
                    customViewCallback.onCustomViewHidden();
                }
            }
        });
        
        // Enable hardware acceleration
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
    }

    private void loadConfiguredUrl() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String url = prefs.getString(PREF_URL, DEFAULT_URL);
        
        // Ensure URL has protocol
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        
        // Store the loaded URL
        currentLoadedUrl = url;
        webView.loadUrl(url);
    }

    private void checkAndReloadUrl() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String url = prefs.getString(PREF_URL, DEFAULT_URL);
        
        // Ensure URL has protocol
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        
        // Check if URL has changed
        if (!url.equals(currentLoadedUrl)) {
            Toast.makeText(this, "Loading new URL: " + url, Toast.LENGTH_SHORT).show();
            currentLoadedUrl = url;
            webView.loadUrl(url);
        }
    }

    private void requestNecessaryPermissions() {
        String[] permissions = {
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        boolean needsPermission = false;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) 
                    != PackageManager.PERMISSION_GRANTED) {
                needsPermission = true;
                break;
            }
        }

        if (needsPermission) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSIONS);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == FILE_CHOOSER_REQUEST) {
            if (fileUploadCallback == null) return;
            
            Uri[] results = null;
            if (resultCode == RESULT_OK && data != null) {
                String dataString = data.getDataString();
                if (dataString != null) {
                    results = new Uri[]{Uri.parse(dataString)};
                }
            }
            
            fileUploadCallback.onReceiveValue(results);
            fileUploadCallback = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (!allGranted) {
                Toast.makeText(this, "Some permissions were denied. App may not function properly.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (customView != null) {
            if (webView.getWebChromeClient() != null) {
                webView.getWebChromeClient().onHideCustomView();
            }
        } else if (webView.canGoBack()) {
            webView.goBack();
        } else {
            // In kiosk mode, prevent exiting the app
            Toast.makeText(this, "Press and hold settings button to configure", 
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        // Long press on menu or volume down to open settings
        if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            Toast.makeText(this, "Long press detected! Opening settings...", 
                    Toast.LENGTH_SHORT).show();
            openSettings();
            return true;
        }
        return super.onKeyLongPress(keyCode, event);
    }

    private void openSettings() {
        try {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Error opening settings: " + e.getMessage(), 
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
        webView.resumeTimers();
        enableKioskMode();
        
        // Check if URL has changed and reload if necessary
        checkAndReloadUrl();
        
        // Restart app if it's not the top task (kiosk mode)
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            am.moveTaskToFront(getTaskId(), 0);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
        webView.pauseTimers();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            enableKioskMode();
        }
    }
}
