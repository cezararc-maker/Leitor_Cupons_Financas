import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val releaseKeystorePath = System.getenv("LCF_RELEASE_KEYSTORE_PATH")
val releaseKeystorePassword = System.getenv("LCF_RELEASE_KEYSTORE_PASSWORD")
val releaseKeyAlias = System.getenv("LCF_RELEASE_KEY_ALIAS")
val releaseKeyPassword = System.getenv("LCF_RELEASE_KEY_PASSWORD")

val releaseSigningReady = listOf(
    releaseKeystorePath,
    releaseKeystorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { !it.isNullOrBlank() }

val configuredVersionCode = providers.gradleProperty("LCF_VERSION_CODE")
    .orNull
    ?.toIntOrNull()
val configuredVersionName = providers.gradleProperty("LCF_VERSION_NAME")
    .orNull
    ?.trim()
    ?.takeIf { it.isNotBlank() }
val remoteAccessRequiredInDebug = providers.gradleProperty("LCF_REMOTE_ACCESS_REQUIRED")
    .orNull
    ?.equals("true", ignoreCase = true)
    ?: false
val appCheckEnabled = providers.gradleProperty("LCF_APP_CHECK_ENABLED")
    .orNull
    ?.equals("true", ignoreCase = true)
    ?: false

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("androidx.room")
}

if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "br.com.leitorcuponsfinancas"
    compileSdk = 36

    defaultConfig {
        applicationId = "br.com.leitorcuponsfinancas"
        minSdk = 23
        targetSdk = 36
        versionCode = configuredVersionCode ?: 1
        versionName = configuredVersionName ?: "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    val secureReleaseSigning = if (releaseSigningReady) {
        signingConfigs.create("secureRelease") {
            storeFile = file(releaseKeystorePath!!)
            storePassword = releaseKeystorePassword
            keyAlias = releaseKeyAlias
            keyPassword = releaseKeyPassword
        }
    } else {
        null
    }

    buildTypes {
        getByName("debug") {
            buildConfigField(
                "boolean",
                "REMOTE_ACCESS_REQUIRED",
                remoteAccessRequiredInDebug.toString(),
            )
            buildConfigField(
                "boolean",
                "APP_CHECK_ENABLED",
                appCheckEnabled.toString(),
            )
            buildConfigField("boolean", "IN_APP_UPDATES_ENABLED", "false")
        }

        getByName("release") {
            signingConfig = secureReleaseSigning
            isMinifyEnabled = false
            buildConfigField("boolean", "REMOTE_ACCESS_REQUIRED", "true")
            buildConfigField(
                "boolean",
                "APP_CHECK_ENABLED",
                appCheckEnabled.toString(),
            )
            buildConfigField("boolean", "IN_APP_UPDATES_ENABLED", "false")
        }

        create("beta") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
            signingConfig = secureReleaseSigning
            buildConfigField("boolean", "REMOTE_ACCESS_REQUIRED", "true")
            buildConfigField(
                "boolean",
                "APP_CHECK_ENABLED",
                appCheckEnabled.toString(),
            )
            buildConfigField("boolean", "IN_APP_UPDATES_ENABLED", "true")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    room {
        schemaDirectory("$buildDir/generated/room-schemas")
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget("17")
    }
}

val requireReleaseSigning = providers.gradleProperty("LCF_REQUIRE_RELEASE_SIGNING")
    .orNull
    ?.equals("true", ignoreCase = true)
    ?: false

tasks.matching { task ->
    task.name == "assembleRelease" ||
        task.name == "bundleRelease" ||
        task.name == "assembleBeta" ||
        task.name == "bundleBeta"
}.configureEach {
    doFirst {
        if (requireReleaseSigning && !releaseSigningReady) {
            throw org.gradle.api.GradleException(
                "Assinatura de release obrigatoria, mas as variaveis LCF_RELEASE_* nao foram configuradas.",
            )
        }
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    val composeBom = platform("androidx.compose:compose-bom:2025.11.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.12.4")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")

    val firebaseBom = platform("com.google.firebase:firebase-bom:34.19.0")
    implementation(firebaseBom)
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-appdistribution-api:16.0.0-beta20")
    debugImplementation("com.google.firebase:firebase-appcheck-debug")
    releaseImplementation("com.google.firebase:firebase-appcheck-playintegrity")
    add("betaImplementation", "com.google.firebase:firebase-appcheck-playintegrity")
    add("betaImplementation", "com.google.firebase:firebase-appdistribution:16.0.0-beta20")

    implementation("org.jsoup:jsoup:1.23.2")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    implementation("com.google.mlkit:text-recognition:16.0.1")

    val cameraXVersion = "1.6.2"
    implementation("androidx.camera:camera-core:$cameraXVersion")
    implementation("androidx.camera:camera-camera2:$cameraXVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraXVersion")
    implementation("androidx.camera:camera-view:$cameraXVersion")

    val roomVersion = "2.8.5"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
