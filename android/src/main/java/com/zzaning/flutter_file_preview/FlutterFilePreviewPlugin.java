package com.zzaning.flutter_file_preview;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.tencent.smtt.export.external.TbsCoreSettings;
import com.tencent.smtt.sdk.QbSdk;

import java.util.HashMap;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import pub.devrel.easypermissions.EasyPermissions;

public class FlutterFilePreviewPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware {
  private MethodChannel channel;
  private String TAG = "log | flutter_file_preview | ";
  private Context context;
  private Activity activity;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    channel = new MethodChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "flutter_file_preview");
    channel.setMethodCallHandler(this);
    context = flutterPluginBinding.getApplicationContext();
    initQbSdk();
  }

  public static void registerWith(Registrar registrar) {
    final MethodChannel channel = new MethodChannel(registrar.messenger(), "flutter_file_preview");
    FlutterFilePreviewPlugin plugin = new FlutterFilePreviewPlugin();
    channel.setMethodCallHandler(plugin);
    plugin.context = registrar.context();
    plugin.activity = registrar.activity();
    plugin.initQbSdk();
  }

  /* 监听Activity*/
  @Override
  public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
    this.activity = binding.getActivity();
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {
    // the Activity your plugin was attached to was destroyed to change configuration.
    // This call will be followed by onReattachedToActivityForConfigChanges().
  }

  @Override
  public void onReattachedToActivityForConfigChanges(ActivityPluginBinding binding) {
    // your plugin is now attached to a new Activity after a configuration change.
    this.activity = binding.getActivity();
  }

  @Override
  public void onDetachedFromActivity() {
    // your plugin is no longer associated with an Activity. Clean up references.
  }

  private void initQbSdk() {
    // 首次初始化冷启动优化
    // 在调用TBS初始化、创建WebView之前进行如下配置
    HashMap map = new HashMap();
    map.put(TbsCoreSettings.TBS_SETTINGS_USE_SPEEDY_CLASSLOADER, true);
    map.put(TbsCoreSettings.TBS_SETTINGS_USE_DEXLOADER_SERVICE, true);
    QbSdk.initTbsSettings(map);

    QbSdk.initX5Environment(context, new QbSdk.PreInitCallback() {
      @Override
      public void onCoreInitFinished() {
        //x5内核初始化完成回调接口，此接口回调并表示已经加载起来了x5，有可能特殊情况下x5内核加载失败，切换到系统内核。
        Log.d(TAG, "初始化X5成功");
      }

      @Override
      public void onViewInitFinished(boolean b) {
        //x5內核初始化完成的回调，为true表示x5内核加载成功，否则表示x5内核加载失败，会自动切换到系统内核。
        Log.e(TAG, "加载内核是否成功:" + b);
      }
    });
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    if (call.method.equals("openFile")) {
      String[] perms = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
      String filePath = call.argument("path");
      if (!EasyPermissions.hasPermissions(context, perms)) {
        EasyPermissions.requestPermissions(activity, "需要访问手机存储权限", 10086, perms);
      } else {
        FileDisplayActivity.show(context, filePath);
      }
      result.success("done");
    } else if (call.method.equals("getPlatformVersion")) {
      result.success("Android " + android.os.Build.VERSION.RELEASE);
    } else {
      result.notImplemented();
    }
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
  }
}
