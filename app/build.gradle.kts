// build.gradle.kts (Module: app)
plugins {
    id("com.android.application") // Plugin ứng dụng Android
}

android {
    compileSdk = 34 // Phiên bản SDK biên dịch, sử dụng phiên bản mới nhất (có thể điều chỉnh)

    defaultConfig {
        applicationId = "com.example.newsai" // Khớp với package Java
        minSdk = 21 // Phiên bản SDK tối thiểu
        targetSdk = 34 // Phiên bản SDK mục tiêu
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    // Kích hoạt View Binding
    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1") // Hỗ trợ AppCompat
    implementation("com.google.android.material:material:1.9.0") // Hỗ trợ Material Design
    implementation("androidx.constraintlayout:constraintlayout:2.1.4") // Hỗ trợ ConstraintLayout

    testImplementation("junit:junit:4.13.2") // Thư viện test JUnit
    androidTestImplementation("androidx.test.ext:junit:1.1.5") // Thư viện test Android
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1") // Thư viện Espresso
}