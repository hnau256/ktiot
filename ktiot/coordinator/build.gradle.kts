plugins {
    alias(libs.plugins.kotlin.serialization)
    kotlin("jvm")
    id("java-library")
    id("maven-publish")
}

java {
    val javaVersion = JavaVersion.valueOf(libs.versions.java.get())
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}

dependencies {
    implementation(libs.arrow.core)
    implementation(libs.logging)
    implementation(libs.coroutines)
    implementation(libs.kotlin.datetime)
    implementation(libs.kotlin.serialization.json)
    implementation(libs.hnau.kotlin)
    implementation(libs.hnau.model)
    implementation(project(":common:logging"))
    implementation(project(":common:mqtt"))
    implementation(project(":ktiot:scheme"))
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(project(":common:logging").tasks.named("compileKotlinDesktop").get().outputs.files)
    from(project(":common:mqtt").tasks.named("compileKotlinDesktop").get().outputs.files)
    from(project(":ktiot:scheme").tasks.named("compileKotlinDesktop").get().outputs.files)
}

group = "com.github.hnau256"
version = "1.0.0"

publishing {
    repositories {
        mavenLocal()
    }
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = project.group as String
            version = project.version as String
        }
    }
}