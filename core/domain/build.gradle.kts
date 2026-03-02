plugins {
    alias(libs.plugins.kotlin.jvm)
}

group = "id.stargan.intikasirfnb"

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(libs.kotlinx.coroutines.core)
}
