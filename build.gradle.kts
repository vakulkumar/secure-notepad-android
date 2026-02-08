// Top-level build file for Secure Notepad
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.5"
}

// Detekt configuration
detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    baseline = file("$rootDir/config/detekt/baseline.xml")
    
    // Configure reporting
    reports {
        html.required.set(true)
        xml.required.set(true)
        txt.required.set(false)
    }
}
