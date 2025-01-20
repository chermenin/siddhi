import org.gradle.api.JavaVersion

buildscript {
    repositories {
        mavenCentral()
    }
}

plugins {
    java
}

subprojects {
    group = "io.siddhi"
    version = "6.0-SNAPSHOT"

    apply {
        plugin("java")
    }

    java.sourceCompatibility = JavaVersion.VERSION_11
    java.targetCompatibility = JavaVersion.VERSION_11
}