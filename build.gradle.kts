plugins {
    kotlin("multiplatform")
    `maven-publish`
}

group = "org.cikit"
version = "0.2.0"

repositories {
    mavenCentral()
}

kotlin {
    jvm {
        jvmToolchain(8)
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
    js(IR) {
        nodejs {
            binaries.executable()
        }
    }
    sourceSets {
        commonMain {
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
                api("com.squareup.okio:okio:3.7.0")
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        jvmMain {}
        jvmTest {}
        jsMain {}
        jsTest {}
    }
}

publishing {
    publications {
        withType(MavenPublication::class.java).configureEach {
            pom {
                name = "forte"
                description = "Twig like template engine"
                url = "https://github.com/b8b/forte"
                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }
                developers {
                    developer {
                        id = "b8b@cikit.org"
                        name = "b8b@cikit.org"
                        email = "b8b@cikit.org"
                    }
                }
                scm {
                    connection = "scm:git:https://github.com/b8b/forte.git"
                    developerConnection = "scm:git:ssh://github.com/b8b/forte.git"
                    url = "https://github.com/b8b/forte.git"
                }
            }
        }
    }
}
