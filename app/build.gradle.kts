plugins {
 id("com.android.application")
 id("org.jetbrains.kotlin.android")
 id("org.jetbrains.kotlin.plugin.compose")
}
android {
 namespace = "com.HG.Forge.Ts"
 compileSdk = 36
 defaultConfig {
  applicationId = "com.HG.Forge.Ts"
  minSdk = 29
  targetSdk = 36
  versionCode = 1
  versionName = "0.1.0"
 }
 buildFeatures { compose = true; buildConfig = true }
 buildTypes {
  getByName("debug") { buildConfigField("boolean", "DIAGNOSTICS_UPLOAD_ENABLED", "true") }
  getByName("release") { buildConfigField("boolean", "DIAGNOSTICS_UPLOAD_ENABLED", "false") }
 }
 compileOptions {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
 }
 kotlinOptions { jvmTarget = "17" }
}
dependencies {
 implementation(platform("androidx.compose:compose-bom:2025.10.00"))
 implementation("androidx.activity:activity-compose:1.11.0")
 implementation("androidx.compose.ui:ui")
 implementation("androidx.compose.ui:ui-tooling-preview")
 implementation("androidx.compose.foundation:foundation")
 implementation("androidx.compose.material3:material3")
 implementation("androidx.compose.material:material-icons-extended")
 debugImplementation("androidx.compose.ui:ui-tooling")
 implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
 implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
 implementation("androidx.navigation:navigation-compose:2.9.5")
 implementation("androidx.room:room-runtime:2.8.0")
 implementation("androidx.room:room-ktx:2.8.0")
 implementation("androidx.datastore:datastore-preferences:1.1.7")
 implementation("androidx.work:work-runtime-ktx:2.10.5")
 implementation("io.ktor:ktor-client-core:3.3.1")
 implementation("io.ktor:ktor-client-android:3.3.1")
 implementation("io.ktor:ktor-client-content-negotiation:3.3.1")
 implementation("io.ktor:ktor-serialization-kotlinx-json:3.3.1")
}
