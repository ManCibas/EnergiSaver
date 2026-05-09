plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.energisaver"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.energisaver"
        minSdk = 24
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

    buildFeatures {
        viewBinding = true //Allows to connect XMLs to kt more easily
    }

}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.appcompat:appcompat:1.6.1") //Because of usage of AppCompatActivity
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation(libs.material)
    implementation(libs.androidx.activity) //Because of usage of constraint layout
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(platform("com.google.firebase:firebase-bom:34.9.0")) //Importing BoM, it gives the compatible versions for the project
    implementation("com.google.firebase:firebase-analytics") //For analytics extension in the firebase
    implementation("com.google.firebase:firebase-database") //Dependency for the realtime Database library
    implementation("com.google.firebase:firebase-auth")

    // Lines for Compose support
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)

    // Debugging/testing if needed
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    //Chart library
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    //To conect with Shelly
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}