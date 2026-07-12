package com.trafficmanagement.android.utils;

import android.content.Context;
import android.content.SharedPreferences;

public final class AuthManager {
  private static final String PREF_NAME = "traffic_management_auth";
  private static final String KEY_NAME = "name";
  private static final String KEY_PHONE = "phone";
  private static final String KEY_PASSWORD = "password";
  private static final String KEY_ROLE = "role";
  private static final String KEY_SITE = "site";
  private static final String KEY_LOGGED_IN = "logged_in";
  private static final String KEY_USER_ID = "user_id";
  private static final String KEY_CATEGORY = "personnel_category";

  private AuthManager() {}

  public static boolean hasRegisteredAccount(Context context) {
    return !getPrefs(context).getString(KEY_PHONE, "").trim().isEmpty();
  }

  public static void register(
      Context context,
      String name,
      String phone,
      String password,
      String role,
      String site) {
    getPrefs(context)
        .edit()
        .putString(KEY_NAME, name)
        .putString(KEY_PHONE, phone)
        .putString(KEY_PASSWORD, password)
        .putString(KEY_ROLE, role)
        .putString(KEY_SITE, site)
        .putBoolean(KEY_LOGGED_IN, true)
        .apply();
  }

  public static void saveRemoteAccount(Context context, int userId, String name, String phone, String password, String role, String category, String site) {
    register(context, name, phone, password, role, site);
    getPrefs(context).edit().putInt(KEY_USER_ID, userId).putString(KEY_CATEGORY, category).apply();
  }

  public static int getCurrentUserId(Context context) { return getPrefs(context).getInt(KEY_USER_ID, 0); }
  public static String getPersonnelCategory(Context context) { return getPrefs(context).getString(KEY_CATEGORY, "traffic_police"); }

  public static boolean login(Context context, String phone, String password) {
    SharedPreferences prefs = getPrefs(context);
    String savedPhone = prefs.getString(KEY_PHONE, "");
    String savedPassword = prefs.getString(KEY_PASSWORD, "");
    boolean matched = savedPhone.equals(phone) && savedPassword.equals(password);

    if (matched) {
      prefs.edit().putBoolean(KEY_LOGGED_IN, true).apply();
    }

    return matched;
  }

  public static void logout(Context context) {
    getPrefs(context).edit().putBoolean(KEY_LOGGED_IN, false).apply();
  }

  public static boolean isLoggedIn(Context context) {
    return getPrefs(context).getBoolean(KEY_LOGGED_IN, false)
        && hasRegisteredAccount(context)
        && getCurrentUserId(context) > 0;
  }

  public static OperatorProfile getCurrentProfile(Context context) {
    if (!hasRegisteredAccount(context)) {
      return null;
    }

    SharedPreferences prefs = getPrefs(context);
    return new OperatorProfile(
        prefs.getString(KEY_NAME, "交通处置人员"),
        prefs.getString(KEY_ROLE, "事故违章处置组"),
        prefs.getString(KEY_PHONE, ""),
        prefs.getString(KEY_SITE, "中心城区交通指挥平台"));
  }

  private static SharedPreferences getPrefs(Context context) {
    return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
  }
}
