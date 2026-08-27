plugins {
    alias(libs.plugins.android.application)
    id("org.openapi.generator") version "7.10.0"
}

openApiGenerate {
    generatorName.set("java")
    inputSpec.set("${project.rootDir}/swagger.yaml")

    // Map the Directory provider to an absolute path String provider
    outputDir.set(layout.buildDirectory.dir("generated").map { it.asFile.absolutePath })

    apiPackage.set("com.sfedu.campus.generated.api")
    invokerPackage.set("com.sfedu.campus.generated.invoker")
    modelPackage.set("com.sfedu.campus.generated.model")

    library.set("okhttp-gson")
}

android {
    namespace = "com.sfedu.campus"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sfedu.campus"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    sourceSets {
        getByName("main") {
            // Use a string path to avoid Provider compatibility issues in SourceSets
            java.srcDirs("${project.layout.buildDirectory.get().asFile.absolutePath}/generated/src/main/java")
        }
    }
}

configurations.all {
    resolutionStrategy {
        force("androidx.graphics:graphics-shapes:1.0.1")
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.10.0")
    implementation("io.gsonfire:gson-fire:1.8.5")

    implementation("io.swagger:swagger-core:1.6.9")
    implementation("javax.ws.rs:javax.ws.rs-api:2.1.1")
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    implementation("com.google.code.gson:gson:2.10.1")

    // CircleImageView for circular avatar
    implementation("de.hdodenhof:circleimageview:3.1.0")
}

tasks.named("preBuild") {
    dependsOn("openApiGenerate")
}
