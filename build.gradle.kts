import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.21"
    application
}

group = "com.jaoafa"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.sksamuel.scrimage:scrimage-core:4.0.32")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

application {
    mainClass.set("MainKt")
}