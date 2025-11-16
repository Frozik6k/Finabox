
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.lombok") version "2.2.0"
    id("com.google.dagger.hilt.android")
}

val lifecycleVersion = libs.versions.lifecycle.get()


android {
    namespace = "ru.frozik6k.finabox"
    compileSdk = 36

    defaultConfig {
        applicationId = "ru.frozik6k.finabox"
        minSdk = 24
        targetSdk = 36
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
}

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "android.lifecycle") {
            val sanitizedName = requested.name.replace("viewmodel-viewmodel", "viewmodel")
            useTarget("androidx.lifecycle:$sanitizedName:$lifecycleVersion")
        }
    }
}


dependencies {
    implementation(libs.androidx.legacy.support.v4)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.drawerlayout)
    implementation(libs.androidx.coordinatorlayout)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    val room_version = "2.8.2"

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation("androidx.room:room-runtime:${room_version}")
    ksp("androidx.room:room-compiler:$room_version")
    annotationProcessor("androidx.room:room-compiler:$room_version")
    implementation("androidx.room:room-ktx:${room_version}")
    implementation("androidx.room:room-rxjava2:${room_version}")
    implementation("androidx.room:room-rxjava3:${room_version}")
    implementation("androidx.room:room-guava:${room_version}")
    testImplementation("androidx.room:room-testing:${room_version}")
    implementation("androidx.room:room-paging:${room_version}")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    implementation("com.google.dagger:hilt-android:2.57.1")
    ksp("com.google.dagger:hilt-compiler:2.57.1")

    compileOnly("org.projectlombok:lombok:1.18.34")
}