package com.trafficmanagement.android.utils;

public class OperatorProfile {
  private final String name;
  private final String role;
  private final String phone;
  private final String currentSite;

  public OperatorProfile(String name, String role, String phone, String currentSite) {
    this.name = name;
    this.role = role;
    this.phone = phone;
    this.currentSite = currentSite;
  }

  public String getName() {
    return name;
  }

  public String getRole() {
    return role;
  }

  public String getPhone() {
    return phone;
  }

  public String getCurrentSite() {
    return currentSite;
  }
}


