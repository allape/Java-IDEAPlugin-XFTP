plugins {
    kotlin("jvm") version "1.5.30"
    id("org.jetbrains.intellij") version "1.1.6"
    java
}

group = "net.allape"
version = "0.10.7"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.6.0")
    implementation("com.google.code.gson:gson:2.8.9")

    compileOnly(files("/Applications/IntelliJ IDEA.app/Contents/plugins/remote-run/lib/remote-run.jar"))
    compileOnly(files("/Applications/IntelliJ IDEA.app/Contents/plugins/terminal/lib/terminal.jar"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
}

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    version.set("IU-2021.3.1")
    updateSinceUntilBuild.set(false)
}
tasks {
    patchPluginXml {
        sinceBuild.set("211.000")
    }
}
tasks.getByName<Test>("test") {
    useJUnitPlatform()
}