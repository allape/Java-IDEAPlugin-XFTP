plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.23"
    id("org.jetbrains.intellij") version "1.17.3"
}

group = "net.allape"
version = "0.10.13"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.22")
    implementation("com.google.code.gson:gson:2.10.1")

    compileOnly(files("/Applications/IntelliJ IDEA.app/Contents/plugins/remoteRun/lib/remoteRun.jar"))
    compileOnly(files("/Applications/IntelliJ IDEA.app/Contents/plugins/terminal/lib/terminal.jar"))
}

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    version.set("IU-2024.1")
    updateSinceUntilBuild.set(true)
}
tasks {
    patchPluginXml {
//        sinceBuild.set("232")
//        untilBuild.set("233")
    }

    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }
}
