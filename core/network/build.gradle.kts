plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.example.passagenexpress.core.network"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 30
        consumerProguardFiles("consumer-rules.pro")

        buildConfigField(
            "String",
            "API_BASE_URL",
            "\"http://172.30.237.224/\""
        )
        buildConfigField(
            "String",
            "API_TOKEN",
            "\"${providers.gradleProperty("passagenexpress.apiToken").orElse("").get()}\""
        )
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "ENABLE_HTTP_LOGGING", "true")
        }
        release {
            buildConfigField("boolean", "ENABLE_HTTP_LOGGING", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    api(project(":core:common"))
    api(project(":core:datastore"))

    api(libs.retrofit.core)
    api(libs.retrofit.kotlinx.serialization)
    api(libs.okhttp.core)
    implementation(libs.okhttp.logging)
    api(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
