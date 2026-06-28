import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    // Pure Kotlin so both pure-Kotlin (core:domain) and Android modules can consume it.
    implementation(project(":core:domain"))
    implementation(libs.kotlinx.coroutines)
    implementation(libs.coroutines.test)
    implementation(libs.junit5.api)
}
