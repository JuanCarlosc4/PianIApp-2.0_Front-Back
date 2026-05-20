plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.piania.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.piania.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"http://34.203.9.247:8090/\"")
            isMinifyEnabled = false
        }
        release {
            buildConfigField("String", "BASE_URL", "\"http://34.203.9.247:8090/\"")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
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
        compose = true
    }
}

configurations.all {
    resolutionStrategy.dependencySubstitution {
        substitute(module("com.intellij:annotations")).using(module("org.jetbrains:annotations:23.0.0"))
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    implementation("io.coil-kt:coil-compose:2.6.0")

    implementation("com.google.android.ump:user-messaging-platform:2.2.0")
    implementation("com.google.android.gms:play-services-ads:23.0.0")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Corutinas para llamadas asíncronas
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Lifecycle para ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // HILT (Inyección de Dependencias)
    implementation("com.google.dagger:hilt-android:2.51.1")
    implementation(libs.androidx.compose.foundation)
    implementation(libs.vision.internal.vkp)
    kapt("com.google.dagger:hilt-compiler:2.51.1")

    // Para la navegación con Hilt (si usas Compose Navigation)
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // --- ESTAS SON LAS QUE TE FALTABAN O ESTABAN INCOMPLETAS ---
    // Librería de componentes Material (necesaria para Theme.MaterialComponents)
    implementation("com.google.android.material:material:1.11.0")

    // AppCompat (necesaria para AppCompatActivity)
    // 1.7+ es la que soporta per-app language (AppCompatDelegate.setApplicationLocales) de forma consistente
    implementation("androidx.appcompat:appcompat:1.7.0")
    // -----------------------------------------------------------

    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    implementation(libs.androidx.material.icons.extended)

    // DataStore (para almacenamiento asíncrono y seguro)
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    // OkHttp (ya deberías tenerlo, pero lo menciono por si acaso)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    // Retrofit & Networking
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)
    implementation(libs.material)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.runtime.livedata)

    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(libs.androidx.ui)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.ui.graphics)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Para tests unitarios con corrutinas
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("junit:junit:4.13.2")
}
