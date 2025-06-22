plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.xjyzs.shortcuts"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.xjyzs.shortcuts"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        androidResources. localeFilters+= listOf("zh")
    }

    signingConfigs {
        create("release") {
            storeFile = file("${project.rootDir}/keystore.jks")
            storePassword = System.getenv("KEY_STORE_PASSWORD") ?: ""
            keyAlias = System.getenv("KEY_ALIAS") ?: ""
            keyPassword = System.getenv("KEY_PASSWORD") ?: ""
            enableV1Signing=false
        }
    }
    
    flavorDimensions += "abi"
    productFlavors {
        create("x86") {
            dimension = "abi"
            ndk { abiFilters.add("x86") }
        }
        create("x86_64") {
            dimension = "abi"
            ndk { abiFilters.add("x86_64") }
        }
        create("arm") {
            dimension = "abi"
            ndk { abiFilters.add("armeabi-v7a") }
        }
        create("arm64") {
            dimension = "abi"
            ndk { abiFilters.add("arm64-v8a") }
        }
        create("universal") {
            dimension = "abi"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            packaging {
                resources {
                    excludes += setOf(
                        "DebugProbesKt.bin",
                        "kotlin-tooling-metadata.json",
                        "okhttp3/**",
                        "META-INF/*version*"
                    )
                }
            }
            androidResources {
                noCompress += setOf("so", "arsc")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    //implementation(libs.androidx.core.ktx)
    //implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    //implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
