plugins {
    kotlin("jvm") version "2.3.0"
    id("com.gradleup.shadow") version "8.3.0"
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "com.hacklab.minecraft"
version = "0.4.25"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
    maven("https://jitpack.io") {
        name = "jitpack"
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Database connection pool (pure Java, relocates cleanly).
    // slf4j is provided by Paper at runtime, so exclude it to avoid bundling a conflicting copy.
    implementation("com.zaxxer:HikariCP:5.1.0") {
        exclude(group = "org.slf4j")
    }
    // JDBC drivers bundled for MySQL / PostgreSQL.
    // SQLite driver (org.xerial:sqlite-jdbc) is already provided by Paper at runtime.
    implementation("com.mysql:mysql-connector-j:9.1.0")
    implementation("org.postgresql:postgresql:42.7.4")

    // Test
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    // SQLite driver for tests (not present on the test classpath since Paper provides it at runtime)
    testImplementation("org.xerial:sqlite-jdbc:3.47.1.0")
    // slf4j-api for tests: HikariCP needs it at class-init. At runtime Paper provides it,
    // so it is excluded from the shaded jar; tests have no Paper, hence add it here.
    testImplementation("org.slf4j:slf4j-api:2.0.16")
}

tasks.test {
    useJUnitPlatform()
}

tasks {
    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion("1.21.11")
    }
}

val targetJavaVersion = 21
kotlin {
    jvmToolchain(targetJavaVersion)
}

tasks.build {
    dependsOn("shadowJar")
}

tasks.shadowJar {
    archiveClassifier.set("all")

    // Relocate Kotlin to avoid conflicts with other plugins
    relocate("kotlin", "com.hacklab.minecraft.skills.kotlin")

    // Merge META-INF/services so all bundled JDBC drivers (java.sql.Driver) are registered
    mergeServiceFiles()
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}
