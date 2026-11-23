plugins { id("com.android.application") }
android {
    namespace = "com.doctorag.arrhythmialab"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.doctorag.arrhythmialab"
        minSdk = 24
        targetSdk = 35
        versionCode = 3
        versionName = "1.2"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
