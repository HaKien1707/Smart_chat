import org.gradle.kotlin.dsl.implementation
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id ("com.android.application")
    id ("com.google.gms.google-services")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.Smart_Chat"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.Smart_Chat"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            merges += "META-INF/INDEX.LIST"
            excludes += "META-INF/DEPENDENCIES"
        }
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
    buildFeatures {
        viewBinding = true
    }

    kotlin {compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }


    buildFeatures {
        viewBinding = true
    }
    buildToolsVersion = "36.1.0"
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.activity)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.messaging)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.hbb20:ccp:2.7.3")

    // FirebaseUI for Firebase Realtime Database
    implementation("com.firebaseui:firebase-ui-database:9.1.1")
    // FirebaseUI for Cloud Firestore
    implementation("com.firebaseui:firebase-ui-firestore:9.1.1")
    // FirebaseUI for Firebase Auth
    implementation("com.firebaseui:firebase-ui-auth:9.1.1")
    // FirebaseUI for Cloud Storage
    implementation("com.firebaseui:firebase-ui-storage:9.1.1")

    // Import the BoM for the Firebase platform
    implementation(platform("com.google.firebase:firebase-bom:34.6.0"))
    implementation("com.google.firebase:firebase-analytics")

    // Add the dependency for the Firebase Authentication library
    // When using the BoM, you don't specify versions in Firebase library dependencies
    implementation(libs.firebase.auth)

    implementation("com.github.dhaval2404:imagepicker:2.1")

    implementation("com.github.bumptech.glide:glide:5.0.5")

    implementation("com.google.firebase:firebase-messaging:25.0.1")

    implementation("com.squareup.okhttp3:okhttp:5.3.2")

    implementation("com.google.auth:google-auth-library-oauth2-http:1.19.0")

    // Cloudinary for image store
    implementation("com.cloudinary:cloudinary-android:3.1.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.navigation:navigation-fragment:2.7.7")
    implementation("androidx.navigation:navigation-ui:2.7.7")

    implementation("com.github.chrisbanes:PhotoView:2.3.0")
}
}
dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.preference)
}
