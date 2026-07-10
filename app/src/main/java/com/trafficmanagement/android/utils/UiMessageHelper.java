package com.trafficmanagement.android.utils;

import android.content.Context;
import android.widget.Toast;

public final class UiMessageHelper {
  private UiMessageHelper() {}

  public static void showShort(Context context, String message) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
  }
}

