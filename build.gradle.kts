import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.21"
    kotlin("plugin.serialization") version "1.8.0"
    distribution
    application
}

group = "com.jaoafa"
version = "1.2.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.sksamuel.scrimage:scrimage-core:4.0.32")
    implementation("com.github.ajalt.mordant:mordant:2.0.0-beta10")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    implementation("com.github.ajalt.clikt:clikt:3.5.1")
    implementation("com.mysql:mysql-connector-j:8.0.32")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

application {
    mainClass.set("MainKt")
}

val `package` = task("package", type = Jar::class) {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveBaseName.set("DynmapProcessor")
    manifest {
        attributes["Main-Class"] = "MainKt"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks.jar.get() as CopySpec)
}

tasks {
    `package`

}