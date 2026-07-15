plugins {
  id("com.android.application")
}

val trafficApiBaseUrl = providers.gradleProperty("TRAFFIC_API_BASE_URL").orElse("http://10.0.2.2:8000").get()
val workOrderUseMock = providers.gradleProperty("WORK_ORDER_USE_MOCK").orElse("false").get().toBoolean()
val releaseStoreFile = System.getenv("ANDROID_KEYSTORE_PATH")
val releaseStorePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
val releaseKeyAlias = System.getenv("ANDROID_KEY_ALIAS")
val releaseKeyPassword = System.getenv("ANDROID_KEY_PASSWORD")
val hasReleaseSigning = listOf(
  releaseStoreFile,
  releaseStorePassword,
  releaseKeyAlias,
  releaseKeyPassword,
).all { !it.isNullOrBlank() }

android {
  namespace = "com.trafficmanagement.android"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.trafficmanagement.android"
    minSdk = 26
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    buildConfigField("String", "API_BASE_URL", "\"$trafficApiBaseUrl\"")
    buildConfigField("boolean", "WORK_ORDER_USE_MOCK", workOrderUseMock.toString())

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    if (hasReleaseSigning) {
      create("ciRelease") {
        storeFile = file(requireNotNull(releaseStoreFile))
        storePassword = requireNotNull(releaseStorePassword)
        keyAlias = requireNotNull(releaseKeyAlias)
        keyPassword = requireNotNull(releaseKeyPassword)
      }
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      if (hasReleaseSigning) {
        signingConfig = signingConfigs.getByName("ciRelease")
      }
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro",
      )
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  buildFeatures {
    buildConfig = true
  }
}

dependencies {
  implementation("androidx.core:core-ktx:1.16.0")
  implementation("androidx.appcompat:appcompat:1.7.1")
  implementation("com.google.android.material:material:1.12.0")
  implementation("androidx.constraintlayout:constraintlayout:2.2.1")
  implementation("androidx.fragment:fragment-ktx:1.8.8")
  implementation("androidx.activity:activity-ktx:1.10.1")
  implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.1")
  implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.9.1")
  implementation("androidx.recyclerview:recyclerview:1.4.0")
  implementation("androidx.cardview:cardview:1.0.0")
  implementation("io.noties.markwon:core:4.6.2")

  testImplementation("junit:junit:4.13.2")
  androidTestImplementation("androidx.test.ext:junit:1.2.1")
  androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
