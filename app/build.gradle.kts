// --- START: Read local.properties BEFORE android block ---
import java.util.Properties
import java.io.FileInputStream
import java.io.IOException

fun getApiKey(propertyKey: String, rootDir: java.io.File): String {
    val properties = Properties()
    val localPropertiesFile = rootDir.resolve("local.properties")
    var apiKey = ""
    if (localPropertiesFile.exists()) {
        try {
            FileInputStream(localPropertiesFile).use { fis ->
                properties.load(fis)
                apiKey = properties.getProperty(propertyKey, "")
            }
        } catch (e: IOException) {
            println("Warning: Could not read local.properties file: ${e.message}")
        }
    } else {
        println("Warning: local.properties file not found in ${rootDir.absolutePath}.")
    }
    return apiKey
}

val stripePublishableKeyValue = getApiKey("stripe.publishableKey", rootProject.rootDir)
// --- END: Read local.properties BEFORE android block ---

plugins {
    id("com.google.gms.google-services") version "4.4.2" apply true
    id("com.android.application")
}

android {
    namespace = "com.example.peer2peer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.peer2peer"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // Uses default build config fields
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        buildConfig = true
    }

    packagingOptions {
        // These are still good to keep for Firebase and other libraries
        pickFirst("google/protobuf/any.proto")
        pickFirst("google/protobuf/api.proto")
        pickFirst("google/protobuf/descriptor.proto")
        pickFirst("google/protobuf/duration.proto")
        pickFirst("google/protobuf/empty.proto")
        pickFirst("google/protobuf/field_mask.proto")
        pickFirst("google/protobuf/source_context.proto")
        pickFirst("google/protobuf/struct.proto")
        pickFirst("google/protobuf/timestamp.proto")
        pickFirst("google/protobuf/type.proto")
        pickFirst("google/protobuf/wrappers.proto")

        pickFirst("google/api/annotations.proto")
        pickFirst("google/api/auth.proto")
        pickFirst("google/api/backend.proto")
        pickFirst("google/api/billing.proto")
        // ... (keep other specific .proto pickFirsts if they were relevant to Firebase)

        exclude("META-INF/INDEX.LIST")
        exclude("META-INF/DEPENDENCIES")
        exclude("META-INF/LICENSE")
        exclude("META-INF/LICENSE.txt")
        exclude("META-INF/license.txt")
        exclude("META-INF/NOTICE")
        exclude("META-INF/NOTICE.txt")
        exclude("META-INF/notice.txt")
        exclude("META-INF/LGPL2.1")
        exclude("META-INF/AL2.0")
        exclude("META-INF/io.netty.versions.properties")
        exclude("META-INF/maven/**")
        exclude("**/attach_hotspot_windows.dll")
        exclude("META-INF/native-image/**")
        exclude("META-INF/versions/9/**")
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.activity:activity:1.8.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-functions-ktx")
    // --- START: Added Firebase Cloud Messaging (FCM) Dependency ---
    implementation("com.google.firebase:firebase-messaging")
    // --- END: Added Firebase Cloud Messaging (FCM) Dependency ---

    implementation("androidx.lifecycle:lifecycle-viewmodel:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata:2.7.0")

    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    implementation("com.stripe:stripe-android:20.35.0")

    // Dependencies for Dialogflow REST API Approach
    implementation("com.android.volley:volley:1.2.1")      // HTTP client
    implementation("com.google.code.gson:gson:2.10.1")     // JSON parsing

    // Google Auth Library for getting OAuth2 access token from service account JSON
    implementation("com.google.auth:google-auth-library-credentials:1.23.0")
    implementation("com.google.auth:google-auth-library-oauth2-http:1.23.0") {
        // google-auth-library-oauth2-http pulls in some http client dependencies,
        // exclude them if they conflict with Volley or cause other issues.
        // Example of excluding Apache HTTP client if it causes issues:
        // exclude group: 'org.apache.httpcomponents'
    }

    implementation("androidx.multidex:multidex:2.0.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}