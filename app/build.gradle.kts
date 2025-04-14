plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.example.commeradeswallet"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.commeradeswallet"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
        viewBinding = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField(type = "String", name = "CONSUMER_KEY", value = "\"vcAnflGGkdHmzBZlYSAGTwW2zU5O\"")
            buildConfigField(type = "String", name = "CONSUMER_SECRET", value = "\"${project.findProperty("DARAJA_CONSUMER_SECRET") ?: ""}\"")
            buildConfigField(type = "String", name = "PASS_KEY", value = "\"bfb279f9aa9bdbcf158e97dd71a467cd2e0c89305\"")
            buildConfigField(type = "String", name = "BUSINESS_SHORT_CODE", value = "\"174379\"")
        }
        debug {
            buildConfigField(type = "String", name = "CONSUMER_KEY", value = "\"vcAnflGGkdHmzBZlYSAGTwW2zU5O\"")
            buildConfigField(type = "String", name = "CONSUMER_SECRET", value = "\"${project.findProperty("DARAJA_CONSUMER_SECRET") ?: ""}\"")
            buildConfigField(type = "String", name = "PASS_KEY", value = "\"bfb279f9aa9bdbcf158e97dd71a467cd2e0c89305\"")
            buildConfigField(type = "String", name = "BUSINESS_SHORT_CODE", value = "\"174379\"")
        }
    }

    // Re-enable product flavors with BuildConfig fields
    flavorDimensions += "version"
    productFlavors {
        create("student") {
            dimension = "version"
            applicationIdSuffix = ".student"
            versionNameSuffix = "-student"
            manifestPlaceholders["appName"] = "Commerades Wallet"
            
            // Add the same BuildConfig fields to the flavor
            buildConfigField(type = "String", name = "CONSUMER_KEY", value = "\"vcAnflGGkdHmzBZlYSAGTwW2zU5O\"")
            buildConfigField(type = "String", name = "CONSUMER_SECRET", value = "\"${project.findProperty("DARAJA_CONSUMER_SECRET") ?: ""}\"")
            buildConfigField(type = "String", name = "PASS_KEY", value = "\"bfb279f9aa9bdbcf158e97dd71a467cd2e0c89305\"")
            buildConfigField(type = "String", name = "BUSINESS_SHORT_CODE", value = "\"174379\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    
    // Material Components and AppCompat
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    
    // Material Compose for pull-to-refresh
    implementation("androidx.compose.material:material:1.6.0")

    // Navigation
    implementation(libs.navigation.compose)

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.7.4"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-functions")
    implementation("com.google.firebase:firebase-messaging-ktx")

    // Google Identity
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    
    // AndroidX Startup
    implementation("androidx.startup:startup-runtime:1.1.1")

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.firebase.functions.ktx)
    ksp(libs.room.compiler)

    // Retrofit & OkHttp
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    // Modules
    implementation(project(":cashier"))

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

configurations.all {
    resolutionStrategy {
        force("com.google.firebase:firebase-common:20.4.2")
        force("com.google.firebase:firebase-firestore:24.10.3")
    }
}