import org.gradle.api.JavaVersion

buildscript {
    repositories {
        mavenCentral()
    }
}

plugins {
    `kotlin-dsl`
    `java-library`
}

allprojects {
    repositories {
        mavenCentral()
        maven {
            url = uri("https://oss.sonatype.org/content/repositories/releases/")
        }
        maven {
            url = uri("https://maven.wso2.org/nexus/content/repositories/public/")
        }
        maven {
            url = uri("https://maven.wso2.org/nexus/content/repositories/releases/")
        }
        maven {
            url = uri("https://maven.wso2.org/nexus/content/repositories/snapshots/")
        }
        maven {
            url = uri("https://repo.maven.apache.org/maven2/")
        }
    }
}

subprojects {
    group = "io.siddhi"
    version = "6.0-SNAPSHOT"

    apply {
        plugin("java-library")
    }

    java.sourceCompatibility = JavaVersion.VERSION_11
    java.targetCompatibility = JavaVersion.VERSION_11

    java {
        withSourcesJar()
    }
}