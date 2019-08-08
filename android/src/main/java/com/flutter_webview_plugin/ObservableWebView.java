package com.flutter_webview_plugin;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebView;
import android.widget.Toast;

import java.io.File;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import io.flutter.plugin.common.PluginRegistry;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class ObservableWebView extends WebView {
    private OnScrollChangedCallback mOnScrollChangedCallback;
    private Context context;
    private Activity activity;
    private PluginRegistry.Registrar registrar;

    public ObservableWebView(final Context context, PluginRegistry.Registrar registrar) {
        super(context);
        init(context);
        this.context = context;
        this.activity = (Activity) context;
        this.registrar = registrar;
    }

    public ObservableWebView(final Context context, final AttributeSet attrs, PluginRegistry.Registrar registrar) {
        super(context, attrs);
        init(context);
        this.context = context;
        this.activity = (Activity) context;
        this.registrar = registrar;
    }

    public ObservableWebView(final Context context, final AttributeSet attrs, final int defStyle, PluginRegistry.Registrar registrar) {
        super(context, attrs, defStyle);
        init(context);
        this.context = context;
        this.activity = (Activity) context;
        this.registrar = registrar;
    }

    protected void init(final Context context) {


        Log.d("MESSAGE", "webView-init成功");
        setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(final String url, String userAgent, final String contentDisposition, final String mimetype, long contentLength) {
                registrar.addRequestPermissionsResultListener(new PluginRegistry.RequestPermissionsResultListener() {
                    @Override
                    public boolean onRequestPermissionsResult(int i, String[] strings, int[] ints) {
                        Log.d("DOWNLOAD", "下载" + i);
                        if (i == 255) {
                            Log.d("DOWNLOAD", "下载" + ints[0]);
                            Log.d("DOWNLOAD", "下载" + strings[0]);
                            if (ints[0] == PackageManager.PERMISSION_GRANTED) {
                                download(url, contentDisposition, mimetype);
                            } else {
                                Toast.makeText(context, "请同意权限申请，用于下载App", Toast.LENGTH_LONG).show();
                            }
                        }
                        return false;
                    }
                });
                if (!checkPermission()) {
                    requestPermission();
                } else {
                    download(url, contentDisposition, mimetype);
                }
            }
        });

    }

    private void download(String url, String contentDisposition, String mimetype) {
        final String suggestedFilename = URLUtil.guessFileName(url, contentDisposition, mimetype);
        Log.d("DOWNLOAD", "下载" + suggestedFilename);
        if (handleDownload(context, url, suggestedFilename)) {
            Toast.makeText(context, "正在下载,请在状态栏查看", Toast.LENGTH_LONG).show();
        }

    }

    private void requestPermission() {
        String[] perm = {WRITE_EXTERNAL_STORAGE};
        ActivityCompat.requestPermissions(activity, perm, 255);
    }

    private boolean checkPermission() {
        return PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(activity, WRITE_EXTERNAL_STORAGE);
    }

//    private void installProcess(String apk) {
//        // 有权限，开始安装应用程序
//        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + apk;
//        Log.i("wuli", "path" + path);
//        File file = new File(path);
//        installApk(file);
//    }

    //安装应用
    private void installApk(File file) {

        Intent intent = new Intent(Intent.ACTION_VIEW);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
        } else {
            Uri uri = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", file);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
    }

//    private void startInstallPermissionSettingActivity() {
//        Uri packageURI = Uri.parse("package:" + context.getPackageName());
//        //注意这个是8.0新API
//        Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, packageURI);
//        activity.startActivityForResult(intent, 10086);
//    }


    @SuppressLint("NewApi")
    public static boolean handleDownload(final Context context, final String fromUrl, final String toFilename) {
        if (Build.VERSION.SDK_INT < 9) {
            throw new RuntimeException("Method requires API level 9 or above");
        }

        final DownloadManager.Request request = new DownloadManager.Request(Uri.parse(fromUrl));
        if (Build.VERSION.SDK_INT >= 11) {
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        }
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, toFilename);

        final DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        try {
            try {
                dm.enqueue(request);
            } catch (SecurityException e) {
                if (Build.VERSION.SDK_INT >= 11) {
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
                }
                dm.enqueue(request);
            }

            return true;
        }
        // if the download manager app has been disabled on the device
        catch (IllegalArgumentException e) {
            // show the settings screen where the user can enable the download manager app again
            openAppSettings(context, "com.android.providers.downloads");

            return false;
        }
    }

    @SuppressLint("NewApi")
    private static boolean openAppSettings(final Context context, final String packageName) {
        if (Build.VERSION.SDK_INT < 9) {
            throw new RuntimeException("Method requires API level 9 or above");
        }

        try {
            final Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + packageName));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);

            return true;
        } catch (Exception e) {
            return false;
        }
    }


    @Override
    protected void onScrollChanged(final int l, final int t, final int oldl, final int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (mOnScrollChangedCallback != null) mOnScrollChangedCallback.onScroll(l, t, oldl, oldt);
    }

    public OnScrollChangedCallback getOnScrollChangedCallback() {
        return mOnScrollChangedCallback;
    }

    public void setOnScrollChangedCallback(final OnScrollChangedCallback onScrollChangedCallback) {
        mOnScrollChangedCallback = onScrollChangedCallback;
    }

    /**
     * Impliment in the activity/fragment/view that you want to listen to the webview
     */
    public static interface OnScrollChangedCallback {
        public void onScroll(int l, int t, int oldl, int oldt);
    }

//    @Override
//    public void setDownloadListener(DownloadListener listener) {
//        super.setDownloadListener(listener);
//
//    }
}