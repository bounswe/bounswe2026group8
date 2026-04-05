plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.bounswe2026group8.emergencyhub"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.bounswe2026group8.emergencyhub"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    // AndroidX
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Retrofit + OkHttp for API calls
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // Coroutines for async API calls
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // Map
    implementation("org.mapsforge:mapsforge-map-android:0.18.0")
    implementation("org.mapsforge:mapsforge-map:0.18.0")
    implementation("org.mapsforge:mapsforge-themes:0.18.0")
    implementation("org.mapsforge:mapsforge-poi-android:0.18.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    // Google Play Services — Location
    implementation("com.google.android.gms:play-services-location:21.1.0")

    // OpenStreetMap for map display
    implementation("org.osmdroid:osmdroid-android:6.1.18")

    // Glide for image loading
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Firebase Cloud Messaging
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-messaging")
}
