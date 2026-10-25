plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.calculaparajugar"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.calculaparajugar"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.1"

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
    }

    buildFeatures {
        viewBinding = true
    }
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.service)

    // Real-time blur for liquid glass effect
    implementation("com.github.Dimezis:BlurView:version-2.0.6")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
