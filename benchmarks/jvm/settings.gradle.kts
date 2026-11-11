pluginManagement { repositories { gradlePluginPortal(); mavenCentral() } }
dependencyResolutionManagement { repositories { mavenCentral() } }
rootProject.name = "forge-ts-jvm-benchmarks"

// CodSpeed JMH fork (git submodule) replaces upstream JMH so results are reported to CodSpeed.
includeBuild("../../third_party/codspeed-jvm/jmh-fork") {
    dependencySubstitution {
        substitute(module("org.openjdk.jmh:jmh-core")).using(project(":jmh-core"))
        substitute(module("org.openjdk.jmh:jmh-generator-annprocess")).using(project(":jmh-generator-annprocess"))
        substitute(module("org.openjdk.jmh:jmh-generator-bytecode")).using(project(":jmh-generator-bytecode"))
        substitute(module("org.openjdk.jmh:jmh-generator-reflection")).using(project(":jmh-generator-reflection"))
        substitute(module("org.openjdk.jmh:jmh-generator-asm")).using(project(":jmh-generator-asm"))
    }
}
