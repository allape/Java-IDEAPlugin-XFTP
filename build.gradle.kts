plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.23"
    id("org.jetbrains.intellij") version "1.17.3"
}

group = "net.allape"
version = "0.10.15"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.0.0")
    implementation("com.google.code.gson:gson:2.10")

    compileOnly(files("/Applications/IntelliJ IDEA.app/Contents/plugins/remoteRun/lib/remoteRun.jar"))
    compileOnly(files("/Applications/IntelliJ IDEA.app/Contents/plugins/terminal/lib/terminal.jar"))
}

intellij {
    version.set("IU-2024.1")
}
tasks {
    patchPluginXml {
        sinceBuild.set("241")
        untilBuild.set("251.*")
    }

    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }
}
