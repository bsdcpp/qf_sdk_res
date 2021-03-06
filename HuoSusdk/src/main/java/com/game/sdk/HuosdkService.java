package com.game.sdk;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.game.sdk.view.UpdateDailog;
import com.kymjs.rxvolley.RxVolley;
import com.kymjs.rxvolley.client.HttpCallback;
import com.kymjs.rxvolley.client.ProgressListener;

import java.io.File;
import java.util.Map;

/**
 * author janecer 2014年7月22日上午9:46:00 sdk系统核心类
 */
public class HuosdkService extends Service {

    public static final String DOWNLOAD_APK_URL = "downLoadApkUrl";//下载apk的url常量
    private String downLoadApkUrl;//apk下载地址
    private ProgressDialog pd;
    private static Context mContext;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static void startService(Context ctx) {
        Intent intent_service = new Intent(ctx, HuosdkService.class);
        intent_service.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startService(intent_service);
    }

    public static void startServiceByUpdate(Context ctx, String downLoadApkUrl) {
        mContext = ctx;
        Intent intent_service = new Intent(ctx, HuosdkService.class);
        intent_service.putExtra(DOWNLOAD_APK_URL, downLoadApkUrl);
        ctx.startService(intent_service);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            downLoadApkUrl = intent.getStringExtra(DOWNLOAD_APK_URL);
//            downLoadApkUrl = "http://down.520cai.com/sdkgame/testand_6031/testand_6031_974.apk";
            if (!TextUtils.isEmpty(downLoadApkUrl)) {
                // 调用下载
//                initDownManager();
                update(downLoadApkUrl);
                return START_STICKY;
            }
        }
        return START_STICKY;
    }

    private void update(String downLoadApkUrl) {
        String fileName = "defaultgame.apk";
        String saveFilePath = new StringBuffer(getSDPath() + "/qfgame").append(File.separator).append(fileName).toString();
        if (downLoadApkUrl.lastIndexOf("/") >= 0) {
            fileName = downLoadApkUrl.substring(downLoadApkUrl.lastIndexOf("/"));
            saveFilePath = new StringBuffer(getSDPath() + "/qfgame").append(fileName).toString();
        }
        if (checkNetworkConnection(mContext) && !isWifi(mContext)) {
            File file = new File(saveFilePath);
            if (file.exists()) {
                install(mContext, file);
                AlertDialog.Builder normalDialog =
                        new AlertDialog.Builder(mContext);
                normalDialog.setCancelable(false);
                normalDialog.setTitle("更新");
                normalDialog.setMessage("安装包已下载好,请及时更新版本！");
                normalDialog.show();
                return;
            }
            UpdateDailog updateDailog = new UpdateDailog((Activity) mContext, R.style.update_dialog_theme, downLoadApkUrl);
            updateDailog.setCanceledOnTouchOutside(false);
            updateDailog.setCancelable(true);
            updateDailog.show();
            return;
        }

        final String finalSaveFilePath = saveFilePath;
        File file = new File(saveFilePath);
        if (file.exists()) {
            install(mContext, file);
            AlertDialog.Builder normalDialog =
                    new AlertDialog.Builder(mContext);
            normalDialog.setCancelable(false);
            normalDialog.setTitle("更新");
            normalDialog.setMessage("安装包已下载好,请及时更新版本！");
            normalDialog.show();
            return;
        }
        RxVolley.download(saveFilePath,
                downLoadApkUrl,
                new ProgressListener() {
                    @Override
                    public void onProgress(long transferredBytes, long totalSize) {
                        pd.setProgress((int) (transferredBytes * 100 / totalSize));
                        pd.show();
                    }
                }, new HttpCallback() {
                    @Override
                    public void onPreStart() {
                        super.onPreStart();
                        Log.e("", "onPreStart()");
                    }

                    @Override
                    public void onPreHttp() {
                        super.onPreHttp();
                        Toast.makeText(mContext, "游戏版本有更新，正在下载最新版本", Toast.LENGTH_SHORT).show();
                        pd = new ProgressDialog(mContext);
                        pd.setCanceledOnTouchOutside(false);
                        pd.setCancelable(false);
                        pd.setTitle("正在下载新版本安装包:");
                        pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                        pd.setMax(100);
                    }

                    @Override
                    public void onSuccessInAsync(byte[] t) {
                        super.onSuccessInAsync(t);
                        Log.e("", "onSuccessInAsync()");
                    }

                    @Override
                    public void onSuccess(String t) {
                        super.onSuccess(t);
                        Log.e("", "onPreStart()");
                    }

                    @Override
                    public void onFailure(int errorNo, String strMsg, String completionInfo) {
                        super.onFailure(errorNo, strMsg, completionInfo);
                        Log.e("", "onFailure()");
                    }

                    @Override
                    public void onFinish() {
                        super.onFinish();
                        Log.e("", "onFinish()");
                        install(mContext, new File(finalSaveFilePath));
                        pd.cancel();
                        AlertDialog.Builder normalDialog =
                                new AlertDialog.Builder(mContext);
                        normalDialog.setCancelable(false);
                        normalDialog.setTitle("更新");
                        normalDialog.setMessage("安装包已下载好,请及时更新版本！");
                        normalDialog.show();
                    }

                    @Override
                    public void onSuccess(Map<String, String> headers, Bitmap bitmap) {
                        super.onSuccess(headers, bitmap);
                        Log.e("", "onSuccess()");
                    }
                });
    }

    private static void install(Context context, File file) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        // 由于没有在Activity环境下启动Activity,设置下面的标签
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= 24) { //判读版本是否在7.0以上
            //参数1 上下文, 参数2 Provider主机地址 和配置文件中保持一致   参数3  共享的文件
            Uri apkUri =
                    FileProvider.getUriForFile(context, "com.game.sdk.installapk", file);
            //添加这一句表示对目标应用临时授权该Uri所代表的文件
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        } else {
            intent.setDataAndType(Uri.fromFile(file),
                    "application/vnd.android.package-archive");
        }
        context.startActivity(intent);
    }

    public static boolean checkNetworkConnection(Context context) {
        final ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        final android.net.NetworkInfo wifi = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        final android.net.NetworkInfo mobile = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        if (wifi.isAvailable() || mobile.isAvailable())  //getState()方法是查询是否连接了数据网络
            return true;
        else
            return false;
    }

    private static boolean isWifi(Context mContext) {
        ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetInfo != null && activeNetInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            return true;
        }
        return false;
    }

    public String getSDPath() {
        File sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState()
                .equals(android.os.Environment.MEDIA_MOUNTED);//判断sd卡是否存在
        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory();//获取跟目录
        }
        return sdDir.toString();
    }
}
