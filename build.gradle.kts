plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.8.20"
    id("org.jetbrains.intellij") version "1.13.3"
}

group = "net.allape"
version = "0.10.10"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.6.0")
    implementation("com.google.code.gson:gson:2.9.0")

    compileOnly(files("/Applications/IntelliJ IDEA.app/Contents/plugins/remoteRun/lib/remoteRun.jar"))
    compileOnly(files("/Applications/IntelliJ IDEA.app/Contents/plugins/terminal/lib/terminal.jar"))
}

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    version.set("IU-2023.1.1")
//    updateSinceUntilBuild.set(false)
}
tasks {
    patchPluginXml {
        sinceBuild.set("230.*")
//        untilBuild.set("232.*")
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

// TODO for future downgrade compatibility
//sourceSets {
//    main {
//        @Suppress("DEPRECATION")
//        withConvention(org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet::class) {
//            kotlin.srcDir("src/main/kotlin")
//            kotlin.exclude("net/allape/common/XFTPManager.kt")
//        }
//    }
//}
