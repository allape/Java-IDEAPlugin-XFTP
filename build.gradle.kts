plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.23"
    id("org.jetbrains.intellij") version "1.17.3"
}

group = "net.allape"
version = "0.11.0-beta.1"

val userHome = System.getProperty("user.home") ?: "/Users/Shared"
val ideVersion = "IU-2024.1"
val ideHome =
    "$userHome/.gradle/caches/modules-2/files-2.1/com.jetbrains.intellij.idea/ideaIU/2024.1/ff15cc0aec80b20c4893865e77af9ebd34cf540c/ideaIU-2024.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.0.0")
    implementation("com.google.code.gson:gson:2.10")

    compileOnly(files("$ideHome/plugins/remoteRun/lib/remoteRun.jar"))
    compileOnly(files("$ideHome/plugins/terminal/lib/terminal.jar"))
}

intellij {
    version.set(ideVersion)
}
tasks {
    patchPluginXml {
        sinceBuild.set("241")
        untilBuild.set("264.*")
    }

    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }
}
