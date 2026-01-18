plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // CORRECCIÓN AQUÍ: Sintaxis Kotlin DSL
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "host.senk.dosenk" // Ajustado a tu nuevo package
    compileSdk = 34 // Ojo: API 36 aún es experimental en muchos casos, sugiero 34 (Android 14) por estabilidad, pero 36 está bien si quieres.

    defaultConfig {
        applicationId = "host.senk.dosenk"
        minSdk = 26
        targetSdk = 34 // Coincidir con compileSdk
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
        sourceCompatibility = JavaVersion.VERSION_1_8 // Hilt suele preferir Java 8 o 17
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    // IMPORTANTE PARA HILT: Permitir referencias cruzadas
    kapt {
        correctErrorTypes = true
    }
}

dependencies {
  
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)



    // UI
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")


    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")

    // Room la Base de datos Offline
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Ciclo de vida y ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")

    //Para navegar
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")
}