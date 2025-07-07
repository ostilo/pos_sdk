
buildscript {


    repositories {
        google()  // Google's Maven repository
        mavenCentral()  // Maven Central repository
        maven { url = uri("https://maven.fullstory.com") }
        maven { url = uri("https://jitpack.io") }
    }

//    dependencies {
//        classpath 'com.android.tools.build:gradle:7.2.2'
//        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.10"
//        classpath 'com.google.gms:google-services:4.3.15'
//        //classpath 'com.google.dagger:hilt-android-gradle-plugin:2.40.1'
//        classpath 'io.github.meituan-dianping:plugin:1.2.1'
//    }

}// Top-level build file where you can add configuration options common to all sub-projects/modules.
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.jetbrainsKotlinAndroid) apply false
}