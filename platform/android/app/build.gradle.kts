import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val includeX64AndroidAbi = providers.gradleProperty("includeX64")
    .map(String::toBoolean)
    .getOrElse(false)

android {
    namespace = "org.sharpemu.android"
    compileSdk = 36
    ndkVersion = "29.0.13113456"

    defaultConfig {
        applicationId = "org.sharpemu.android"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-poc"

        ndk {
            abiFilters += listOf("arm64-v8a")
            if (includeX64AndroidAbi) {
                abiFilters += listOf("x86_64")
            }
        }

        externalNativeBuild {
            cmake {
                // sharpemu_shim is a small C shim (no C++ core to build here —
                // SharpEmu's emulator core is managed C#/.NET, loaded via the
                // SharpEmu.Android bridge, not this .so). See
                // app/src/main/cpp/CMakeLists.txt.
                targets += listOf("sharpemu_shim")
            }
        }
    }

    buildTypes {
        // The interpreter is extremely sensitive to compiler optimization: a Debug (-O0) native
        // build leaves every hot helper (register/flag/operand access) as a real, non-inlined
        // call. The release variant compiles the native code with CMAKE_BUILD_TYPE=Release (-O3,
        // NDEBUG), which is by far the biggest single speed-up for the guest interpreter. It is
        // signed with the debug key so it stays directly installable for testing.
        getByName("release") {
            isMinifyEnabled = false
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
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

    externalNativeBuild {
        cmake {
            // Points directly at this module's own CMakeLists.txt — the
            // shadPS4 source this was copied from instead pointed two levels
            // up at the wider repo's root CMakeLists.txt (its Android/
            // folder was built as part of a much larger CMake project);
            // there is no equivalent wider C++ project here to hang off of.
            path = file("src/main/cpp/CMakeLists.txt")
            version = "4.3.2"
        }
    }

    // shadPS4's Android/ folder sourced this from its wider checkout's
    // "externals/sdl3" git submodule (SDL 3.5.0), which isn't part of this
    // copy — vendored directly instead, since there is no equivalent wider
    // repo to hang a submodule off of here. ppy.SDL3-CS's Android runtime
    // packages ship the matching native .so but not this Java glue, which
    // upstream SDL3 only distributes as source.
    sourceSets["main"].java.srcDir(file("../../externals/sdl3-android-java"))

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
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
