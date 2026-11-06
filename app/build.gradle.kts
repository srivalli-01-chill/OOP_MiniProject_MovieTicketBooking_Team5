android {
    namespace = "com.team17.movieticketteam5"

    defaultConfig {
        applicationId = "com.team17.movieticketteam5"
        // Keep your other existing settings
    }
}
plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.team17.movieticketteam5"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.team17.movieticketteam5"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:35.1.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
}