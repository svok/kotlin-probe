plugins {
//    kotlin("multiplatform")
    id("crowdproj-platforms")
    `maven-publish`
    id("org.jetbrains.dokka")
}

kotlin {

    sourceSets {
        val coroutinesVersion: String by project
        val atomicfuVersion: String by project

        all { languageSettings.optIn("kotlin.RequiresOptIn") }

        @Suppress("UNUSED_VARIABLE")
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
                implementation("org.jetbrains.kotlinx:atomicfu:$atomicfuVersion")
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val jsMain by getting {
            dependencies {
                implementation(kotlin("stdlib-js"))
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
//                implementation("io.kotlintest:kotlintest-runner-junit5:3.3.2")
//                runtimeOnly("org.junit.jupiter:junit-jupiter-engine:5.5.2")
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val linuxX64Main by getting {
            dependencies {
                implementation(kotlin("stdlib"))
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val linuxX64Test by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

val dokkaHtml by tasks.getting(org.jetbrains.dokka.gradle.DokkaTask::class)

val javadocJar: TaskProvider<Jar> by tasks.registering(Jar::class) {
    dependsOn(dokkaHtml)
    group = "publishing"
    archiveClassifier.set("javadoc")
    from(dokkaHtml.outputDirectory)
}

publishing {
    repositories {
        val repoHost: String = System.getenv("NEXUS_HOST") ?: "https://maven.pkg.github.com/crowdproj/kotlin-cor"
        val repoUser: String? = System.getenv("NEXUS_USER") ?: System.getenv("GITHUB_ACTOR")
        val repoPass: String? = System.getenv("NEXUS_PASS") ?: System.getenv("GITHUB_TOKEN")
        if (repoUser != null && repoPass != null) {
            maven {
                name = "GitHubPackages"
                url = uri(repoHost)
                credentials {
                    username = repoUser
                    password = repoPass
                }
            }
        }

    }
    publications {
        withType(MavenPublication::class).configureEach {
            artifact(javadocJar)
            pom {
                name.set("Kotlin CoR")
                description.set("Chain of Responsibility Design Template Library for human readable business logic: $name platform")
                url.set("https://github.com/crowdproj/kotlin-cor")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        name.set("Sergey Okatov")
                        email.set("sokatov@gmail.com")
                        id.set("svok")
                        organization.set("CrowdProj")
                        organizationUrl.set("https://crowdproj.com")
                        timezone.set("GMT+5")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/crowdproj/kotlin-cor.git")
                    developerConnection.set("scm:git:ssh://github.com/crowdproj/kotlin-cor.git")
                    url.set("https://github.com/crowdproj/kotlin-cor")
                }
            }
        }
    }
}

tasks {
    withType<Test> {
        useJUnitPlatform()
        reports {
            junitXml.required.set(true)
        }
        setupTestLogging()
    }

    publish {
        dependsOn(build)
    }

    create("deploy") {
        group = "build"
        dependsOn(publish)
//        dependsOn(closeAndReleaseRepository)
    }

}

fun Test.setupTestLogging() {
    testLogging {
        events(
            org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED,
            org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED,
            org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED,
            org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_OUT,
        )
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true

        addTestListener(object : TestListener {
            override fun beforeSuite(suite: TestDescriptor) {}
            override fun beforeTest(testDescriptor: TestDescriptor) {}
            override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {}
            override fun afterSuite(suite: TestDescriptor, result: TestResult) {
                if (suite.parent != null) { // will match the outermost suite
                    val output = "Results: ${result.resultType} (${result.testCount} tests, ${result.successfulTestCount} passed, ${result.failedTestCount} failed, ${result.skippedTestCount} skipped)"
                    val startItem = "|  "
                    val endItem = "  |"
                    val repeatLength = startItem.length + output.length + endItem.length
                    val messages = """
                        ${(1..repeatLength).joinToString("") { "-" }}
                        $startItem$output$endItem
                        ${(1..repeatLength).joinToString("") { "-" }}
                    """.trimIndent()
                    println(messages)
                }
            }
        })
    }
}
