plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.emilockerclient"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.emilockerclient"
        minSdk = 30
        targetSdk = 36
        versionCode = 4
        versionName = "4.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // Signing will be configured via Android Studio's "Generate Signed Bundle/APK" dialog
            // or you can configure it here manually:
            // storeFile = file("/path/to/your/keystore.jks")
            // storePassword = "your_store_password"
            // keyAlias = "your_key_alias"
            // keyPassword = "your_key_password"

//            optional but good practice soo...
            storeFile = file("/home/lazy/.keystores/imelockerclient.jks")
            storePassword = "imelocker"
            keyAlias = "key0"
            keyPassword = "imelocker"

            // Enable V1 signing for better compatibility with older Android versions
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Signing config will be applied during "Generate Signed Bundle/APK"
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

val workVersion = "2.10.0"
val retrofitVersion = "2.11.0"
val okHttpVersion = "4.11.0"

dependencies {
    // WorkManager
    implementation("androidx.work:work-runtime-ktx:$workVersion")

    // Retrofit + Gson converter
    implementation("com.squareup.retrofit2:retrofit:$retrofitVersion")
    implementation("com.squareup.retrofit2:converter-gson:$retrofitVersion")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    // OkHttp core + logging interceptor (⚠ you missed okhttp core previously)
    implementation("com.squareup.okhttp3:okhttp:$okHttpVersion")
    implementation("com.squareup.okhttp3:logging-interceptor:$okHttpVersion")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.3.0"))
    implementation("com.google.firebase:firebase-messaging")

    // AndroidX core libs
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}