plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
}

android {
    namespace = "com.kpay.kpayterminaldemosdk"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.kpay.kpayterminaldemosdk"
        minSdk = 24
        targetSdk = 34
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

    buildFeatures {
        viewBinding = true
        buildConfig = true
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
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar", "*.jar"))))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

//    implementation(files("libs/pos_utils-debug.aar"))
    implementation(files("libs/pos_utils-test.aar"))
    implementation(files("libs/AFSDKInterface_202502211810_V0.0.236_236.aar"))


    implementation("com.google.code.gson:gson:2.9.0")
    implementation("com.airbnb.android:lottie:6.0.0")

//    implementation("com.squareup.retrofit2:retrofit:2.4.0") { exclude group: "com.squareup.okhttp3" }
//    implementation("com.squareup.retrofit2:converter-gson:2.4.0") { exclude group: "com.squareup.okhttp3" }
    // Retrofit without OkHttp3 directly
    implementation("com.squareup.retrofit2:retrofit:2.4.0") {
        exclude(group = "com.squareup.okhttp3")
    }

    // Gson Converter without OkHttp3 directly
    implementation("com.squareup.retrofit2:converter-gson:2.4.0") {
        exclude(group = "com.squareup.okhttp3")
    }


    // You would typically then explicitly add the OkHttp3 version you want
    // For example, if you want a specific OkHttp version (e.g., for logging-interceptor compatibility)
    implementation("com.squareup.okhttp3:okhttp:3.14.9") // Or a later compatible version
    implementation("com.squareup.okhttp3:logging-interceptor:3.14.9") // Make sure version matches OkHttp
//    implementation("com.squareup.okhttp3:okhttp:3.7.0")
//    implementation("com.squareup.okhttp3:logging-interceptor:3.7.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.0.0")


    implementation("com.github.getActivity:TitleBar:9.2")
    implementation("com.github.getActivity:ToastUtils:9.5")
    implementation("com.airbnb.android:lottie:6.0.0")




}