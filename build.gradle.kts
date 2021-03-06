import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.31"
    id("org.jetbrains.compose") version "1.0.0"
    kotlin("plugin.serialization") version "1.5.31"
}

group = "de.mr-pine"
version = "1.0"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2-native-mt")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.5.2-native-mt")
    implementation("org.jetbrains.compose.ui:ui-util-desktop:1.0.0")
    implementation("com.google.code.gson:gson:2.8.9")


}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Exe)
            packageName = "LeagueOfInfo"
            packageVersion = "1.0.1"
            description = "An application to see more info about your League of Legends game"
            vendor = "Mr. Pine"
            windows{
                iconFile.set(project.file("./src/main/resources/icon.ico"))
                menuGroup = "LeagueOfInfo"
            }
            modules("java.instrument", "java.net.http", "java.sql", "jdk.unsupported")
        }
    }
}

