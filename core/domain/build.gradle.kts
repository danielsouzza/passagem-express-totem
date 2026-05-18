plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    api(project(":core:common"))
    api(libs.kotlinx.coroutines.core)
    implementation(libs.javax.inject)
}
