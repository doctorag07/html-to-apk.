plugins { id("com.android.application") }
val ksFile = rootProject.file("release.jks")
android {
    namespace = "com.doctorag.medicinemcq"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.doctorag.medicinemcq"
        minSdk = 24
        targetSdk = 35
        versionCode = 4
        versionName = "3.0"
    }
    signingConfigs {
        if (ksFile.exists()) {
            create("release") {
                storeFile = ksFile
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = "doctorag"
                keyPassword = System.getenv("KEYSTORE_PASSWORD")
            }
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            if (ksFile.exists()) signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
