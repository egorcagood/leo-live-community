plugins { id("com.android.application") }
android {
    namespace = "community.leo.obd"
    compileSdk = 36
    defaultConfig {
        applicationId = "community.leo.obd"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-preview.1"
    }
    buildTypes { release { isMinifyEnabled = false } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
