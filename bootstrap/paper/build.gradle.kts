plugins {
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.paper)
}

dependencies {
    implementation(project(":api"))
    implementation(project(":core"))

    // Paper specific
    compileOnly(libs.paper.api)

    // Command Framework (Lamp) & XSeries
    implementation(libs.lamp.common)
    implementation(libs.lamp.bukkit)
    implementation(libs.xseries)

    // Adventure text components
    implementation(libs.adventure.minimessage)
    implementation(libs.adventure.api)
    implementation(libs.adventure.platform.bukkit)

    // NBT API for cross-version persistent data
    implementation(libs.nbtapi)

    // Economy (compile-only, provided at runtime)
    compileOnly(libs.vault.api)

    // Testing
    testImplementation(libs.junit.api)
    testRuntimeOnly(libs.junit.engine)
}

tasks {
    shadowJar {
        archiveFileName.set("gnhopper.jar")
        archiveClassifier.set("")

        // Relocate dependencies to avoid conflicts
        relocate("com.cryptomorin.xseries", "com.gn027c.hopper.libs.xseries")
        relocate("revxrsal.commands", "com.gn027c.hopper.libs.lamp")
        relocate("de.tr7zw.changeme.nbtapi", "com.gn027c.hopper.libs.nbtapi")
    }

    build {
        dependsOn(shadowJar)
    }
}
