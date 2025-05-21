// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.androidx.navigation.safeargs) apply false
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1"
    id("io.gitlab.arturbosch.detekt") version "1.23.1"
    id("com.autonomousapps.dependency-analysis") version "1.25.0"
}

// Apply ktlint and detekt to all projects
allprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "io.gitlab.arturbosch.detekt")
    
    // Configure ktlint
    ktlint {
        debug.set(true)
        verbose.set(true)
        android.set(true)
        outputToConsole.set(true)
        ignoreFailures.set(false)
        enableExperimentalRules.set(true)
        filter {
            exclude { element -> 
                element.file.path.contains("generated/")
            }
        }
    }
    
    // Configure detekt
    detekt {
        toolVersion = "1.23.1"
        config = files("${project.rootDir}/config/detekt/detekt.yml")
        buildUponDefaultConfig = true
        parallel = true
        autoCorrect = true
    }
}

// Task to run all code quality checks
tasks.register("codeQualityCheck") {
    group = "verification"
    description = "Run all code quality checks"
    
    dependsOn("ktlintCheck")
    dependsOn("detekt")
    dependsOn("test")
    dependsOn("lint")
}

// Configure detekt for all projects
tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    reports {
        html.required.set(true)
        xml.required.set(true)
        txt.required.set(true)
        sarif.required.set(true)
    }
}

// Configure detekt baseline
tasks.register<io.gitlab.arturbosch.detekt.DetektCreateBaselineTask>("detektBaseline") {
    description = "Creates a detekt baseline for all modules"
    ignoreFailures.set(true)
    parallel.set(true)
    setSource(files(projectDir))
    baseline.set(file("$projectDir/config/detekt/baseline.xml"))
    config.setFrom(files("$projectDir/config/detekt/detekt.yml"))
    include("**/*.kt")
    include("**/*.kts")
    exclude("**/resources/**")
    exclude("**/build/**")
}

// Configure dependency analysis
dependencyAnalysis {
    issues {
        all {
            onAny {
                severity("fail")
                exclude("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
                exclude("org.jetbrains.kotlin:kotlin-stdlib-jdk7")
                exclude("org.jetbrains.kotlin:kotlin-stdlib")
                exclude("org.jetbrains.kotlin:kotlin-stdlib-common")
            }
        }
    }
}