import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Chữ ký phát hành: đọc từ keystore.properties (không đưa lên GitHub) hoặc biến môi trường (GitHub Actions).
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun signingValue(key: String, env: String): String? =
    keystoreProps.getProperty(key) ?: System.getenv(env)

android {
    namespace = "vn.homekara.box"
    compileSdk = 34

    defaultConfig {
        applicationId = "vn.hometube.app"
        minSdk = 19            // Android 4.4.2 KitKat trên VNPT SmartBox 2
        targetSdk = 28         // giữ hành vi cũ: cài APK qua file://, HTTP trong LAN
        versionCode = 1
        versionName = "1.0.0"
        multiDexEnabled = true
        // S805 là ARMv7; các ABI còn lại để chạy được trên máy giả lập / điện thoại.
        ndk { abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64") }
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Địa chỉ file JSON kiểm tra cập nhật (để trống = tắt). Xem docs/INSTALL.md.
        buildConfigField("String", "UPDATE_MANIFEST_URL", "\"${System.getenv("HOMEKARA_UPDATE_URL") ?: ""}\"")
    }

    signingConfigs {
        // Mặc định dùng khoá cố định keystore/homekara.jks (để bản mới cài đè được bản cũ).
        // Muốn dùng khoá riêng: tạo keystore.properties hoặc đặt biến môi trường HOMEKARA_*.
        create("homekara") {
            storeFile = rootProject.file(signingValue("storeFile", "HOMETUBE_KEYSTORE") ?: "keystore/hometube.jks")
            storePassword = signingValue("storePassword", "HOMETUBE_KEYSTORE_PASSWORD")
            keyAlias = signingValue("keyAlias", "HOMETUBE_KEY_ALIAS") ?: "hometube"
            keyPassword = signingValue("keyPassword", "HOMETUBE_KEY_PASSWORD")
            enableV1Signing = true   // BẮT BUỘC: Android 4.4 chỉ hiểu chữ ký v1
            enableV2Signing = true
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("homekara")
        }
        getByName("release") {
            // Tắt rút gọn mã: Rhino/NewPipe dễ lỗi khi R8 cắt bớt; multidex đã lo phần dung lượng.
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("homekara")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions { jvmTarget = "11" }

    buildFeatures { buildConfig = true }
    testOptions { unitTests.isIncludeAndroidResources = true }

    packaging {
        resources.excludes += listOf("META-INF/*.kotlin_module", "META-INF/DEPENDENCIES", "META-INF/LICENSE*", "META-INF/NOTICE*")
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
        // APK sideload cho KitKat, không phát hành qua Google Play; target 28 là chủ đích.
        disable += "ExpiredTargetSdkVersion"
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs_nio:2.1.2")

    implementation("androidx.multidex:multidex:2.0.1")

    // Media3 1.4.1 là bản cuối cùng còn hỗ trợ API 19 — KHÔNG nâng lên 1.5+.
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-common:1.4.1")
    implementation("androidx.media3:media3-datasource:1.4.1")

    // Lấy dữ liệu YouTube. Khi YouTube đổi định dạng, nâng phiên bản này rồi build lại.
    implementation("com.github.TeamNewPipe:NewPipeExtractor:v0.26.5")

    // TLS 1.2/1.3 hiện đại cho Android 4.4.
    implementation("org.conscrypt:conscrypt-android:2.5.2")

    implementation("org.nanohttpd:nanohttpd:2.3.1")
    implementation("com.google.zxing:core:3.5.3")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.12.2")
    testImplementation("org.json:json:20231013")
}

// Robolectric chạy trên JVM Windows, dùng Conscrypt OpenJDK do Robolectric cung cấp.
// APK vẫn chỉ đóng gói bản Conscrypt Android có libconscrypt_jni.so cho box.
configurations.matching { it.name.endsWith("UnitTestRuntimeClasspath") }.configureEach {
    exclude(group = "org.conscrypt", module = "conscrypt-android")
}
