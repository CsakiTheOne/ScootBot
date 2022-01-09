import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.10"
    application
}

group = "com.csakitheone"
version = "1.0-SNAPSHOT"

repositories {
    jcenter()
    mavenCentral()
}

dependencies {
    implementation("net.dv8tion:JDA:4.2.0_241")
    implementation("ch.qos.logback:logback-classic:1.2.10")
    implementation("com.google.code.gson:gson:2.8.9")
    implementation("com.sedmelluq:lavaplayer:1.3.73")
    implementation("net.dean.jraw:JRAW:1.1.0")
    testImplementation(kotlin("test-junit"))
    implementation(kotlin("stdlib-jdk8"))
}

tasks.test {
    useJUnit()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClassName = "MainKt"
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}