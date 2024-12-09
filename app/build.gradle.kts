plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.kotlin)
    alias(libs.plugins.ksp)
    alias(libs.plugins.lsplugin.apksign)
    alias(libs.plugins.lsplugin.resopt)
    alias(libs.plugins.kotlinSerialization)
}

apksign {
    storeFileProperty = "KEYSTORE_FILE"
    storePasswordProperty = "KEYSTORE_PASSWORD"
    keyAliasProperty = "KEY_ALIAS"
    keyPasswordProperty = "KEY_PASSWORD"
}

android {
    namespace = "moe.chenxy.hyperpods"
    compileSdk = 35

    defaultConfig {
        applicationId = "moe.chenxy.hyperpods"
        minSdk = 35
        targetSdk = 35
        versionCode = 1
        versionName = "1.1-V-HyperOS"
    }

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            multiDexEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    dependenciesInfo.includeInApk = false

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(JavaVersion.VERSION_22.majorVersion)
        }
    }

    kotlin {
        jvmToolchain(JavaVersion.VERSION_22.majorVersion.toInt())
    }

    buildFeatures {
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/**.version"
            excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
            excludes += "okhttp3/**"
            excludes += "kotlin/**"
            excludes += "org/**"
            excludes += "**.properties"
            excludes += "**.bin"
            excludes += "kotlin-tooling-metadata.json"
        }
    }
}

configurations.configureEach {
//    exclude(group = "androidx.appcompat", module = "appcompat")
    exclude(group = "androidx.lifecycle", module = "lifecycle-viewmodel-ktx")
}

dependencies {
    implementation(libs.coreKtx)
    compileOnly(libs.xposedApi)
    implementation(libs.yukihookApi)
    ksp(libs.yukihookKsp)
    implementation(libs.kotlinx.serialization.json)
}