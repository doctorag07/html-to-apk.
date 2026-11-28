package com.doctorag.medicinemcq;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {
    private static final int PICK_FILE = 41;
    private WebView web;
    private ValueCallback<Uri[]> fileCallback;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(0xFF0B2545);
        getWindow().setNavigationBarColor(0xFF0B2545);
        web = new WebView(this);
        web.setBackgroundColor(0xFF0B2545);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);          // localStorage keeps progress
        s.setAllowFileAccess(true);
        s.setAllowFileAccessFromFileURLs(true);
        s.setTextZoom(100);                    // keep layout stable with large system font
        web.addJavascriptInterface(new Bridge(), "AGNative");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                if ("file".equals(u.getScheme())) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception ignored) {}
                return true;                   // Instagram & other links open outside the app
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = cb;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                try { startActivityForResult(Intent.createChooser(i, "Choose backup file"), PICK_FILE); }
                catch (Exception e) { fileCallback = null; return false; }
                return true;
            }
        });
        setContentView(web);
        if (state != null) web.restoreState(state);
        else web.loadUrl("file:///android_asset/www/index.html");
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == PICK_FILE && fileCallback != null) {
            Uri[] r = (res == RESULT_OK && data != null && data.getData() != null) ? new Uri[]{data.getData()} : null;
            fileCallback.onReceiveValue(r);
            fileCallback = null;
        }
    }

    /** Saves a file into Downloads/DoctorAG (or Pictures/DoctorAG for images). Returns the content Uri or null. */
    private Uri store(String name, byte[] bytes, String mime) throws Exception {
        if (Build.VERSION.SDK_INT >= 29) {
            boolean img = mime.startsWith("image/");
            ContentValues cv = new ContentValues();
            cv.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            cv.put(MediaStore.MediaColumns.MIME_TYPE, mime);
            cv.put(MediaStore.MediaColumns.RELATIVE_PATH, (img ? Environment.DIRECTORY_PICTURES : Environment.DIRECTORY_DOWNLOADS) + "/DoctorAG");
            ContentResolver cr = getContentResolver();
            Uri uri = cr.insert(img ? MediaStore.Images.Media.EXTERNAL_CONTENT_URI : MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
            if (uri == null) throw new Exception("could not create file");
            try (OutputStream o = cr.openOutputStream(uri)) { o.write(bytes); }
            return uri;
        }
        File dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (dir != null && !dir.exists()) dir.mkdirs();
        try (FileOutputStream o = new FileOutputStream(new File(dir, name))) { o.write(bytes); }
        return null;
    }

    private class Bridge {
        @JavascriptInterface
        public String saveFile(String name, String b64, String mime) {
            try {
                store(name, Base64.decode(b64, Base64.DEFAULT), mime);
                if (Build.VERSION.SDK_INT >= 29)
                    return "Saved to " + (mime.startsWith("image/") ? "Pictures" : "Downloads") + "/DoctorAG";
                return "Saved in the app's Downloads folder";
            } catch (Exception e) { return "Could not save: " + e.getMessage(); }
        }

        @JavascriptInterface
        public String shareImage(String name, String b64, String text) {
            try {
                Uri uri = store(name, Base64.decode(b64, Base64.DEFAULT), "image/png");
                final Intent send = new Intent(Intent.ACTION_SEND);
                if (uri != null) {
                    send.setType("image/png");
                    send.putExtra(Intent.EXTRA_STREAM, uri);
                    send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } else send.setType("text/plain");
                send.putExtra(Intent.EXTRA_TEXT, text);
                runOnUiThread(() -> startActivity(Intent.createChooser(send, "Share score card")));
                return "";
            } catch (Exception e) { return "Could not share: " + e.getMessage(); }
        }
    }

    @Override protected void onSaveInstanceState(Bundle out) { super.onSaveInstanceState(out); web.saveState(out); }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack(); else super.onBackPressed();
    }
}
