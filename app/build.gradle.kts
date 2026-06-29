import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// R2 credentials are read from local.properties (git-ignored) so secrets stay out of source
// control. Missing keys default to empty so CI builds still compile. See android-development-guide.md.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun localProp(key: String): String =
    (localProperties.getProperty(key) ?: "").replace("\\", "\\\\").replace("\"", "\\\"")

android {
    namespace = "com.qhathia.mobile.staff"
    compileSdk = 36
    compileSdkMinor = 1

    defaultConfig {
        applicationId = "com.qhathia.mobile.staff"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // API base URL — production VPS (plain HTTP, no domain yet). Isolated here so a future
        // HTTPS domain is a one-line change. See network-security-config for the cleartext allow.
        buildConfigField("String", "BASE_URL", "\"http://87.99.156.164\"")

        // Cloudflare R2 object storage — values from local.properties (see localProp above).
        buildConfigField("String", "R2_ENDPOINT", "\"${localProp("R2_ENDPOINT")}\"")
        buildConfigField("String", "R2_BUCKET", "\"${localProp("R2_BUCKET")}\"")
        buildConfigField("String", "R2_REGION", "\"${localProp("R2_REGION").ifEmpty { "auto" }}\"")
        buildConfigField("String", "R2_ACCESS_KEY_ID", "\"${localProp("R2_ACCESS_KEY_ID")}\"")
        buildConfigField("String", "R2_SECRET_ACCESS_KEY", "\"${localProp("R2_SECRET_ACCESS_KEY")}\"")
        buildConfigField("String", "R2_PUBLIC_BASE_URL", "\"${localProp("R2_PUBLIC_BASE_URL")}\"")
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
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:network"))
    implementation(project(":core:ui"))
    implementation(project(":features:auth"))
    implementation(project(":features:product-inventory"))
    implementation(project(":features:product-register"))

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
