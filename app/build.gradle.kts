import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
}

val signingProps = Properties().apply {
    val f = rootProject.file("signing.properties")
    if (f.exists()) load(f.inputStream())
}

val customProps = Properties().apply {
    val f = rootProject.file("custom.properties")
    if (f.exists()) load(f.inputStream())
}

android {
    namespace = "id.stargan.intikasirfnb"
    compileSdk = 36

    signingConfigs {
        create("release") {
            storeFile = rootProject.file(signingProps.getProperty("RELEASE_STORE_FILE", "dummy.jks"))
            storePassword = signingProps.getProperty("RELEASE_STORE_PASSWORD", "")
            keyAlias = signingProps.getProperty("RELEASE_KEY_ALIAS", "")
            keyPassword = signingProps.getProperty("RELEASE_KEY_PASSWORD", "")
        }
    }

    defaultConfig {
        applicationId = "id.stargan.intikasirfnb"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Licensing config from custom.properties
        buildConfigField("String", "PUBLIC_KEY_HEX", "\"${customProps.getProperty("PUBLIC_KEY_HEX", "")}\"")
        buildConfigField("long", "CLOUD_PROJECT_NUMBER", "${customProps.getProperty("CLOUD_PROJECT_NUMBER", "0")}L")
        buildConfigField("String", "CERT_PIN_HOSTNAME", "\"${customProps.getProperty("CERT_PIN_HOSTNAME", "")}\"")
        buildConfigField("String", "CERTIFICATE_PINS", "\"${customProps.getProperty("CERTIFICATE_PINS", "")}\"")
    }

    flavorDimensions += "env"

    productFlavors {
        create("dev") {
            dimension = "env"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            buildConfigField("String", "APPREG_BASE_URL", "\"${customProps.getProperty("DEV_APPREG_BASE_URL", "http://10.0.2.2:8000")}\"")
        }
        create("prod") {
            dimension = "env"
            buildConfigField("String", "APPREG_BASE_URL", "\"${customProps.getProperty("PROD_APPREG_BASE_URL", "https://appreg.stargan.id")}\"")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":feature:identity"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Image loading
    implementation(libs.coil.compose)

    // Activity & Lifecycle
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Network (Retrofit + OkHttp)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Play Integrity
    implementation(libs.play.integrity)
    implementation(libs.kotlinx.coroutines.play.services)

    // Encrypted storage
    implementation(libs.androidx.security.crypto)

    // Ed25519 signature verification
    implementation(libs.bouncycastle)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.bouncycastle)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
