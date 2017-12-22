package com.myb.livevideo;

import java.util.ArrayList;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

/**
 * Created by myb on 12/22/17.
 */

public class PermissionChecker {
  //一个整形常量
  public static final int MY_PERMISSIONS_REQUEST = 3000;

  private static String[] RUNTIME_PERMISSIONS = new String[] {
      Manifest.permission.CAMERA,
      Manifest.permission.RECORD_AUDIO,
      Manifest.permission.WRITE_EXTERNAL_STORAGE};

  //调用封装好的申请权限的方法
  public static boolean checkPermissions(Activity context) {
    // api 23以前，写在 Manifest里就行了。
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      return true;
    }

    for (int i = 0 ; i < RUNTIME_PERMISSIONS.length; i ++) {
      String permission = RUNTIME_PERMISSIONS[i];
      //检查权限是否已经申请
      int hasPermission = context.checkSelfPermission(permission);
      if (hasPermission != PackageManager.PERMISSION_GRANTED) {
        return false;
      }
    }
    return true;
  }

  public static void requestPermission(Activity context) {
    // api 23以前，写在 Manifest里就行了。
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      return;
    }
    context.requestPermissions(RUNTIME_PERMISSIONS, 1);
  }
}
