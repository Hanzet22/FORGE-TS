plugins {
    kotlin("jvm") version "2.2.21"
    id("me.champeau.jmh") version "0.7.3"
}

kotlin { jvmToolchain(21) }

// Compile only the platform-independent planning logic from the Android app, so the
// benchmarks measure the exact production code without pulling in Android dependencies.
sourceSets {
    main {
        kotlin {
            setSrcDirs(listOf("../../app/src/main/java"))
            include("com/HG/Forge/Ts/data/network/CidrCalculator.kt")
        }
    }
}

jmh {
    jmhVersion.set("0.2.1")
    warmupIterations.set(2)
    warmup.set("1s")
    iterations.set(3)
    timeOnIteration.set("1s")
    fork.set(1)
    forceGC.set(true)
}
