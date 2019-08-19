package com.flutter_webview_plugin;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
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


//        new AlertDialog.Builder(context)
//                .setTitle("test")
//                .setMessage("cmcc")
//                .create().show();

//        progressDialog = new ProgressDialog(context);
//        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//        progressDialog.setTitle("下载文件");
//        progressDialog.setMessage("当前下载进度");
//        progressDialog.setCancelable(false);

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
    private void installApk(Uri u) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                // intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
                intent.setDataAndType(u, "application/vnd.android.package-archive");
            } else {
                //  Uri uri = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", file);
                intent.setDataAndType(u, "application/vnd.android.package-archive");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            activity.startActivity(intent);
        } catch (Exception e) {
            //fileName
            Log.i("fileName", fileName);
            File file = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS + "/" + fileName);
            if (file != null) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Uri uri = FileProvider.getUriForFile(activity, context.getPackageName() + ".provider", file);
                    intent.setDataAndType(uri, "application/vnd.android.package-archive");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } else {
                    intent.setDataAndType(Uri.parse("file://" + file.toString()),
                            "application/vnd.android.package-archive");
                }
                if (activity.getPackageManager().queryIntentActivities(intent, 0).size() > 0) {
                    activity.startActivity(intent);
                }
            }
        }
    }

    //private ProgressDialog progressDialog;

//    private void startInstallPermissionSettingActivity() {
//        Uri packageURI = Uri.parse("package:" + context.getPackageName());
//        //注意这个是8.0新API
//        Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, packageURI);
//        activity.startActivityForResult(intent, 10086);
//    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1001) {
//                progressDialog.setMax(100);
//                progressDialog.setProgress(
//                        new Double(((double) msg.arg1) / (double) msg.arg2 * 100).intValue()
//                );

//                pb.setMax(msg.arg2);
//                pb.setProgress(msg.arg1);
                if (msg.arg1 == msg.arg2) {
                    Log.i("a", "下载完成");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {

                    }
                    removeCallbacks(mQuery);
                    installApk(dm.getUriForDownloadedFile(mDownLoadId));
//                    if (dialog.isShowing()) {
//                        dialog.dismiss();
//                    }
                }
            }
        }
    };
    private DownloadManager dm;
    private long mDownLoadId;

    private void queytState() {
        Cursor c = dm.query(new DownloadManager.Query().setFilterById(mDownLoadId));
        if (c != null) {
            if (c.moveToFirst()) {
                int d_so_far = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                int d_so_all = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

                Message msg = Message.obtain();
                if (d_so_all > 0) {
                    msg.what = 1001;
                    msg.arg1 = d_so_far;
                    msg.arg2 = d_so_all;
                    mHandler.sendMessage(msg);
                }

                if (!c.isClosed()) {
                    c.close();
                }

            }
        }

    }


    String fileName = "";

    @SuppressLint("NewApi")
    public boolean handleDownload(final Context context, final String fromUrl,
                                  final String toFilename) {
        fileName = toFilename;

        if (Build.VERSION.SDK_INT < 9) {
            throw new RuntimeException("Method requires API level 9 or above");
        }

        final DownloadManager.Request request = new DownloadManager.Request(Uri.parse(fromUrl));
        if (Build.VERSION.SDK_INT >= 11) {
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        }
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, toFilename);


        dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        try {
            try {
                mDownLoadId = dm.enqueue(request);

            } catch (SecurityException e) {
                if (Build.VERSION.SDK_INT >= 11) {
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
                }
                dm.enqueue(request);
            }
            if (mDownLoadId != 0) {
                mHandler.post(mQuery);
                // dialog.show();
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

    private final QueryRunnable mQuery = new QueryRunnable();

    private class QueryRunnable implements Runnable {
        @Override
        public void run() {
            queytState();
            mHandler.postDelayed(mQuery, 100);
        }
    }

    @SuppressLint("NewApi")
    private boolean openAppSettings(final Context context, final String packageName) {
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
        if (mOnScrollChangedCallback != null)
            mOnScrollChangedCallback.onScroll(l, t, oldl, oldt);
    }

    public OnScrollChangedCallback getOnScrollChangedCallback() {
        return mOnScrollChangedCallback;
    }

    public void setOnScrollChangedCallback(
            final OnScrollChangedCallback onScrollChangedCallback) {
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
