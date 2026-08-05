import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// This module builds the SharpEmu Kotlin/Compose UI (game library, settings,
// controller config, etc.) as an Android *library* (.aar), not a standalone
// app. The actual installed app is the SharpEmu.Android net10.0-android
// project (src/SharpEmu.Android in the main SharpEmu repo), which consumes
// this .aar via the AndroidLibrary MSBuild item and hosts the real Activity
// lifecycle, the Mono runtime, and SharpEmu's managed emulator core
// directly — SDL3 is driven from C# (ppy.SDL3-CS), the same way it already
// is on desktop, not through this module's Kotlin/native code. This is a
// deliberate inversion of shadPS4's own Android/ layout (whose Android/app
// module IS a standalone application with a native C++ core baked in): that
// shape assumes a native core, so it can't host a managed .NET core the same
// way — see the port's design notes for why.
//
// No externalNativeBuild / CMake target lives here any more for the same
// reason: there is no native shim to build. GameActivity's SDL-driven
// emulation loop is implemented in C# in the app project instead.
// versionCode/versionName aren't valid defaultConfig fields for a library module (AGP's
// com.android.library plugin doesn't expose them, unlike com.android.application) -- the real
// APK version lives in SharpEmu.Android.csproj's ApplicationVersion/ApplicationDisplayVersion.
// This project-level Gradle version is kept in sync with that value for anyone building/publishing
// this .aar independently.
version = "0.0.2_poc"

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "org.sharpemu.android"
    compileSdk = 36

    defaultConfig {
        minSdk = 29
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // shadPS4's Android/ folder sourced this from its wider checkout's
    // "externals/sdl3" git submodule (SDL 3.5.0), which isn't part of this
    // copy — vendored directly instead, since there is no equivalent wider
    // repo to hang a submodule off of here.
    //
    // TODO: most of org.libsdl.app.* (SDLActivity and its surface/input glue)
    // is no longer needed now that SDL3 is driven from C#, not from a Kotlin
    // SDLActivity subclass — GamePadBridge's references to SDL constants are
    // the one piece still worth keeping. Trim this source set once
    // GameActivity.kt is fully replaced by the C# GameActivity.
    sourceSets["main"].java.srcDir(file("../../externals/sdl3-android-java"))

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.01.00"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.documentfile:documentfile:1.0.0")
    implementation("androidx.startup:startup-runtime:1.2.0")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material-icons-core:1.7.6")
    implementation("androidx.compose.material:material-icons-extended:1.7.2")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
