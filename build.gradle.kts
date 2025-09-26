import com.github.spotbugs.snom.Effort
import com.github.spotbugs.snom.Confidence
import org.cthing.projectversion.BuildType
import org.cthing.projectversion.ProjectVersion
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

repositories {
    mavenCentral()
}

plugins {
    `java-gradle-plugin`
    checkstyle
    jacoco
    signing
    alias(libs.plugins.dependencyAnalysis)
    alias(libs.plugins.pluginPublish)
    alias(libs.plugins.spotbugs)
    alias(libs.plugins.versions)
}

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath(libs.cthingProjectVersion)
    }
}

version = ProjectVersion("3.0.1", BuildType.snapshot)
group = "org.cthing"
description = "A Gradle plugin that establishes the versioning scheme for C Thing Software projects."

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get())
    }
}

gradlePlugin {
    website = "https://github.com/cthing/gradle-cthing-versioning"
    vcsUrl = "https://github.com/cthing/gradle-cthing-versioning"

    plugins {
        create("cthingVersioningPlugin", Action {
            id = "org.cthing.cthing-versioning"
            displayName = "C Thing Software versioning plugin"
            description = "A Gradle plugin that establishes the versioning scheme for C Thing Software projects."
            tags = listOf("versioning")
            implementationClass = "org.cthing.gradle.plugins.versioning.VersioningPlugin"
        })
    }
}

// Dependency Restriction
//
// This project is a dependency of all C Thing Software projects. Therefore, to avoid circular
// dependencies, the only C Thing Software project it should depend on is cthing-projectversion.
configurations.all {
    resolutionStrategy {
        eachDependency {
            if (requested.group == "com.cthing"
                    || (requested.group == "org.cthing" && requested.name != "cthing-projectversion")) {
                throw GradleException("A dependency on '${requested.group}:${requested.name}' is prohibited.")
            }
        }
    }
}

dependencies {
    api(libs.cthingProjectVersion)

    implementation(libs.jspecify)

    testImplementation(libs.assertJ)
    testImplementation(libs.junitApi)
    testImplementation(libs.junitParams)

    testRuntimeOnly(libs.junitEngine)
    testRuntimeOnly(libs.junitLauncher)

    spotbugsPlugins(libs.spotbugsContrib)
}

checkstyle {
    toolVersion = libs.versions.checkstyle.get()
    isIgnoreFailures = false
    configFile = file("dev/checkstyle/checkstyle.xml")
    configDirectory = file("dev/checkstyle")
    isShowViolations = true
}

spotbugs {
    toolVersion = libs.versions.spotbugs
    ignoreFailures = false
    effort = Effort.MAX
    reportLevel = Confidence.MEDIUM
    excludeFilter = file("dev/spotbugs/suppressions.xml")
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

dependencyAnalysis {
    issues {
        all {
            onAny {
                severity("fail")
                exclude("com.google.code.findbugs:jsr305", "org.cthing:cthing-projectversion")
            }
        }
    }
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

tasks {
    withType<JavaCompile> {
        options.release = libs.versions.java.get().toInt()
        options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-options", "-Werror"))
    }

    withType<Jar> {
        manifest.attributes(mapOf("Implementation-Title" to project.name,
                                  "Implementation-Vendor" to "C Thing Software",
                                  "Implementation-Version" to project.version))
    }

    withType<Javadoc> {
        val year = SimpleDateFormat("yyyy", Locale.ENGLISH).format(Date())
        with(options as StandardJavadocDocletOptions) {
            breakIterator(false)
            encoding("UTF-8")
            bottom("Copyright &copy; $year C Thing Software")
            addStringOption("Xdoclint:all,-missing", "-quiet")
            addStringOption("Werror", "-quiet")
            memberLevel = JavadocMemberLevel.PUBLIC
            outputLevel = JavadocOutputLevel.QUIET
        }
    }

    check {
        dependsOn(buildHealth)
    }

    spotbugsMain {
        reports.create("html").required = true
    }

    spotbugsTest {
        isEnabled = false
    }

    publishPlugins {
        doFirst {
            if ((version as ProjectVersion).isSnapshotBuild) {
                throw GradleException("Cannot publish a developer build to the Gradle Plugin Portal")
            }
            if (!project.hasProperty("gradle.publish.key") || !project.hasProperty("gradle.publish.secret")) {
                throw GradleException("Gradle Plugin Portal credentials not defined")
            }
        }
    }

    withType<JacocoReport> {
        dependsOn("test")
        with(reports) {
            xml.required = false
            csv.required = false
            html.required = true
            html.outputLocation = layout.buildDirectory.dir("reports/jacoco")
        }
    }

    withType<Test> {
        useJUnitPlatform()

        systemProperty("projectDir", projectDir)
        systemProperty("buildDir", layout.buildDirectory.get().asFile)
    }

    withType<GenerateModuleMetadata> {
        enabled = false
    }

    withType<Sign>().configureEach {
        onlyIf("Signing credentials are present") {
            hasProperty("signing.keyId") && hasProperty("signing.password") && hasProperty("signing.secretKeyRingFile")
        }
    }

    dependencyUpdates {
        revision = "release"
        gradleReleaseChannel = "current"
        outputFormatter = "plain,xml,html"
        outputDir = layout.buildDirectory.dir("reports/dependencyUpdates").get().asFile.absolutePath

        rejectVersionIf {
            isNonStable(candidate.version)
        }
    }
}

publishing {
    publications {
        maybeCreate("pluginMaven", MavenPublication::class.java).pom {
            name = project.name
            description = project.description
            url = "https://github.com/cthing/${project.name}"
            organization {
                name = "C Thing Software"
                url = "https://www.cthing.com"
            }
            licenses {
                license {
                    name = "Apache-2.0"
                    url = "https://www.apache.org/licenses/LICENSE-2.0"
                }
            }
            developers {
                developer {
                    id = "baron"
                    name = "Baron Roberts"
                    email = "baron@cthing.com"
                    organization = "C Thing Software"
                    organizationUrl = "https://www.cthing.com"
                }
            }
            scm {
                connection = "scm:git:https://github.com/cthing/${project.name}.git"
                developerConnection = "scm:git:git@github.com:cthing/${project.name}.git"
                url = "https://github.com/cthing/${project.name}"
            }
            issueManagement {
                system = "GitHub Issues"
                url = "https://github.com/cthing/${project.name}/issues"
            }
            ciManagement {
                url = "https://github.com/cthing/${project.name}/actions"
                system = "GitHub Actions"
            }
            properties.putAll(mapOf("cthing.build.date" to (project.version as ProjectVersion).buildDate,
                                    "cthing.build.number" to (project.version as ProjectVersion).buildNumber,
                                    "cthing.dependencies" to libs.cthingProjectVersion.get().toString(),
                                    "cthing.gradle.plugins" to gradlePlugin.plugins["cthingVersioningPlugin"].id))
        }
    }

    val repoUrl = if ((version as ProjectVersion).isSnapshotBuild)
        findProperty("cthing.nexus.snapshotsUrl") else findProperty("cthing.nexus.candidatesUrl")
    if (repoUrl != null) {
        repositories {
            maven {
                name = "CThingMaven"
                setUrl(repoUrl)
                credentials {
                    username = property("cthing.nexus.user") as String
                    password = property("cthing.nexus.password") as String
                }
            }
        }
    }
}
