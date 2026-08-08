plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
}

// App version — the catalog holds the base (major + prerelease); CI makes the MINOR auto-increment
// on every push to main by passing -PappMinor=<github.run_number>. Patch stays 0 in this scheme.
//   versionName = "<major>.<minor>.0-<appPrerelease>"
//     · CI run 37   -> 1.37.0-SNAPSHOT
//     · local build -> 1.0.0-SNAPSHOT   (no -PappMinor -> falls back to the catalog's minor)
//   versionCode = major*1_000_000 + minor   (monotonic; safe even for large minors)
val appSemVer = libs.versions.appVersion.get()            // appVersion, e.g. "1.0.0"
val appPrerelease = libs.versions.appPrerelease.get()     // appPrerelease, e.g. "SNAPSHOT"
val appMajor = appSemVer.substringBefore(".").toInt()
val appMinor = (project.findProperty("appMinor") as String?)?.toIntOrNull()
    ?: appSemVer.split(".")[1].toInt()
val appVersionName = "$appMajor.$appMinor.0-$appPrerelease"
val appVersionCode = appMajor * 1_000_000 + appMinor

android {
    namespace = "com.qhathia.mobile.staff"
    compileSdk = 36
    compileSdkMinor = 1

    defaultConfig {
        applicationId = "com.qhathia.mobile.staff"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // API base URL — production VPS (plain HTTP, no domain yet). Isolated here so a future
        // HTTPS domain is a one-line change. See network-security-config for the cleartext allow.
        buildConfigField("String", "BASE_URL", "\"http://87.99.156.164\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        // QA / Android Studio profiling build:
        //  · initWith(release)                 -> inherits release config as the baseline
        //  · isProfileable = true              -> allows the Android Studio profiler to attach
        //  · non-debuggable (inherited)        -> clean profiler metrics, close to release behavior
        //  · signingConfig = debug keystore    -> installable by QA WITHOUT release signing secrets
        //  · R8/minify left OFF (from release) -> matches release output without keep-rule risk
        //  · matchingFallbacks += "release"    -> lets library modules resolve when they lack a profile variant
        create("profile") {
            initWith(getByName("release"))
            isProfileable = true
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += "release"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// Deterministic APK filename per variant; each build type writes to its own outputs/apk/<buildType>/
// folder, so identical names never collide.
androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            (output as? com.android.build.api.variant.impl.VariantOutputImpl)
                ?.outputFileName
                ?.set("qhathia-staff-v$appVersionName.apk")
        }
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:network"))
    implementation(project(":core:notifications"))
    implementation(project(":core:ui"))
    implementation(project(":features:auth"))
    implementation(project(":features:product-inventory"))
    implementation(project(":features:product-register"))
    implementation(project(":features:device-linking"))
    implementation(project(":features:order-requests"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
    ksp(libs.kotlin.metadata.jvm)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
