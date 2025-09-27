plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.emilockerclient"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.emilockerclient"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}
val workVersionWorkRuntime = "2.10.4"

dependencies {
    // Retrofit already present; add OkHttp logging if not
    implementation("com.squareup.okhttp3:logging-interceptor:5.1.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.3.0"))
    implementation("com.google.firebase:firebase-messaging-ktx")

    // WorkManager
    //noinspection GradleDependency
    implementation("androidx.work:work-runtime-ktx:$workVersionWorkRuntime")

    // Retrofit + Gson
    implementation("com.squareup.retrofit2:retrofit:3.0.0") // <-- 3.0.0 doesn’t exist!
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
