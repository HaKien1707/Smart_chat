import org.gradle.kotlin.dsl.implementation
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id ("com.android.application")
    id ("com.google.gms.google-services")
    id ("org.jetbrains.kotlin.android")
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

        buildConfigField(
            "String",
            "GEMINI_API_KEY",
            "\"${project.findProperty("GEMINI_API_KEY")}\""
        )
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            merges += "META-INF/INDEX.LIST"
            excludes += "META-INF/DEPENDENCIES"

            pickFirsts += "META-INF/INDEX.LIST"
            pickFirsts += "META-INF/io.netty.versions.properties"
            excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
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
        buildConfig = true
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
            freeCompilerArgs.add("-Xnested-type-aliases")
        }
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
    implementation(libs.preference)
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
    implementation(platform("com.google.firebase:firebase-bom:34.7.0"))
    implementation("com.google.firebase:firebase-analytics")

    // Add the dependency for the Firebase Authentication library
    // When using the BoM, you don't specify versions in Firebase library dependencies
    implementation(libs.firebase.auth)

    implementation("com.github.dhaval2404:imagepicker:2.1")

    implementation("com.github.bumptech.glide:glide:5.0.5")

    implementation(libs.firebase.messaging)

    implementation("com.squareup.okhttp3:okhttp:5.3.2")

    implementation("com.google.auth:google-auth-library-oauth2-http:1.41.0")

    // Cloudinary for image store
    implementation("com.cloudinary:cloudinary-android:3.1.2")
    implementation("com.github.chrisbanes:PhotoView:2.3.0")

    // Video Call
    implementation("io.getstream:stream-webrtc-android:1.3.10")

    // Gemini AI
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    implementation("io.grpc:grpc-okhttp:1.77.0")
    implementation("io.grpc:grpc-android:1.77.0")
    implementation("com.google.guava:guava:33.5.0-android")
}
