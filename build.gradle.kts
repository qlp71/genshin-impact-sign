import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
plugins {
    kotlin("jvm") version "1.4.21"
    kotlin("plugin.serialization") version "1.4.21"
    id("com.github.johnrengelman.shadow") version "5.0.0"
    application
}

group = "com.htt"
version = "1.0-SNAPSHOT"

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        jcenter()
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.9.0")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.1")
}

application {
    @Suppress("DEPRECATION")
    mainClassName = "ApplicationKt"
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "11"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "11"
    }
    named<ShadowJar>("shadowJar") {
        archiveBaseName.set("genshin-impact-sign")
        mergeServiceFiles()
    }
}