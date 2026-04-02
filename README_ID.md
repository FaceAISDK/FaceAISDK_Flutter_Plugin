# FaceAI SDK Flutter Plugin

Plugin Flutter untuk FaceAISDK — verifikasi wajah, deteksi liveness, dan pendaftaran wajah di perangkat Android dan iOS.

**[English](README.md)**

## Fitur

- **Pendaftaran Wajah (Enroll)** — Daftarkan wajah untuk verifikasi 1:1
- **Verifikasi Wajah (1:1)** — Verifikasi wajah langsung dengan wajah tersimpan
- **Deteksi Liveness** — Anti-spoofing (gerakan, flash warna, silent)
- **Tambah Wajah** — Tambahkan wajah ke database pencarian

Semua proses dilakukan di perangkat. Tidak memerlukan internet.

## Persyaratan

| Platform | Persyaratan |
|----------|-------------|
| Android | `minSdk >= 24`, perangkat `armeabi-v7a` atau `arm64-v8a` |
| iOS | iOS 15.5+, perangkat fisik saja (tanpa simulator) |
| Keduanya | Izin kamera |

## Konfigurasi Android (Wajib)

### 1. AndroidManifest.xml

Tambahkan `extractNativeLibs="true"` di tag `<application>`:

```xml
<application
    android:extractNativeLibs="true"
    ...>
```

### 2. build.gradle.kts

Tambahkan konfigurasi berikut di `android/app/build.gradle.kts`:

```kotlin
android {
    // Wajib Java 17
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    // Sign dengan keystore yang terdaftar di FaceAISDK
    signingConfigs {
        create("faceai") {
            storeFile = file("file-keystore-anda")
            storePassword = "password-anda"
            keyAlias = "alias-anda"
            keyPassword = "password-anda"
        }
    }
    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("faceai")
        }
        release {
            signingConfig = signingConfigs.getByName("faceai")
        }
    }
}
```

### 3. Registrasi Signing Key

FaceAISDK memvalidasi **nama paket + sertifikat signing** aplikasi. Anda harus mendaftarkan signing key ke penyedia SDK:

- Kontak: FaceAISDK.Service@gmail.com
- GitHub: https://github.com/FaceAISDK/FaceAISDK_Android

Untuk pengembangan/testing, gunakan demo keystore (`FaceAIPublic`) dengan `applicationId = "com.ai.face.Demo"`.

## Konfigurasi iOS (Wajib)

### 1. Podfile

Set platform ke iOS 15.5+ dan tambahkan pod `FaceAISDK_Core` di `ios/Podfile`:

```ruby
platform :ios, '15.5'

target 'Runner' do
  use_frameworks!

  flutter_install_all_ios_pods File.dirname(File.realpath(__FILE__))

  pod 'FaceAISDK_Core', :git => 'https://github.com/FaceAISDK/FaceAISDK_Core.git', :tag => '2026.03.27'
end
```

Tambahkan blok `post_install` untuk memperbaiki ABI mismatch `BUILD_LIBRARY_FOR_DISTRIBUTION`:

```ruby
post_install do |installer|
  installer.pods_project.targets.each do |target|
    flutter_additional_ios_build_settings(target)

    target.build_configurations.each do |config|
      config.build_settings['BUILD_LIBRARY_FOR_DISTRIBUTION'] = 'YES'
      config.build_settings['OTHER_SWIFT_FLAGS'] ||= '$(inherited)'
      config.build_settings['OTHER_SWIFT_FLAGS'] += ' -Xfrontend -enable-library-evolution'
    end
  end
end
```

Lalu jalankan:

```bash
cd ios && pod install
```

### 2. Info.plist

Tambahkan izin kamera di `ios/Runner/Info.plist`:

```xml
<key>NSCameraUsageDescription</key>
<string>Akses kamera diperlukan untuk verifikasi wajah dan deteksi liveness.</string>
```

### 3. Build Settings

Plugin memerlukan **perangkat fisik** — build simulator tidak didukung (`EXCLUDED_ARCHS[sdk=iphonesimulator*]` mengecualikan `i386` dan `arm64`).

## Penggunaan

### Inisialisasi SDK

Wajib dipanggil sebelum method lainnya:

```dart
final faceAiSdk = FaceAiSdk();
await faceAiSdk.initializeSDK({
  'locale': 'id',  // Khusus iOS: bahasa UI — "en" (default), "id", "zh-Hans"
});
```

### Daftarkan Wajah (Enroll)

Ambil dan daftarkan wajah untuk verifikasi 1:1:

```dart
final result = await faceAiSdk.startEnroll(
  faceId: "user_123",
  format: "base64",  // atau "filePath"
);

if (result['code'] == 1) {
  print('Terdaftar: ${result['faceID']}');
}
```

### Verifikasi Wajah (1:1)

Bandingkan wajah langsung dengan wajah tersimpan:

```dart
final result = await faceAiSdk.startVerification(
  faceId: "user_123",        // ID wajah tersimpan (faceId atau faceFeature wajib diisi)
  faceFeature: null,          // atau langsung kirim string fitur wajah
  threshold: 0.85,            // 0.75 - 0.95
  livenessType: 1,            // 0=NONE, 1=MOTION, 2=MOTION+COLOR, 3=COLOR, 4=SILENT
  motionStepSize: 1,          // 1-2 langkah
  motionTimeout: 10,          // 3-22 detik
  motionTypes: "1,2,3",       // 1=buka mulut, 2=senyum, 3=kedip, 4=geleng, 5=angguk
  allowRetry: true,           // izinkan coba ulang saat timeout/gagal
  format: "base64",           // "base64" atau "filePath"
);

if (result['code'] == 1) {
  print('Cocok! Kemiripan: ${result['similarity']}');
  print('Liveness: ${result['livenessValue']}');
}
```

### Deteksi Liveness

Deteksi apakah wajah adalah orang nyata (tanpa pencocokan wajah):

```dart
final result = await faceAiSdk.startLiveness(
  livenessType: 1,          // 1=MOTION, 2=MOTION+COLOR, 3=COLOR, 4=SILENT
  motionStepSize: 1,
  motionTimeout: 10,
  motionTypes: "1,2,3",
  format: "base64",
);

if (result['code'] == 10) {
  print('Skor liveness: ${result['livenessValue']}');
}
```

### Tambah Wajah ke Database Pencarian

Tambahkan wajah untuk pencarian 1:N:

```dart
final result = await faceAiSdk.addFace(
  faceId: "user_456",
  format: "base64",
);

if (result['code'] == 1) {
  print('Wajah ditambahkan: ${result['faceID']}');
}
```

## Kode Hasil

| Kode | Arti |
|------|------|
| 0 | Dibatalkan oleh pengguna |
| 1 | Berhasil |
| 2 | Verifikasi gagal (bukan orang yang sama) |
| 3 | Timeout |
| 4 | Timeout (melebihi batas coba ulang) |
| 5 | Wajah tidak terdeteksi berulang kali |
| 10 | Deteksi liveness selesai |
| 11 | Silent liveness gagal |

## Tipe Liveness

| Nilai | Tipe | Deskripsi |
|-------|------|-----------|
| 0 | NONE | Tanpa deteksi liveness |
| 1 | MOTION | Berbasis gerakan (buka mulut, senyum, kedip, dll.) |
| 2 | MOTION + COLOR | Gerakan + flash warna kombinasi |
| 3 | COLOR_FLASH | Flash warna saja (tidak untuk lingkungan terang) |
| 4 | SILENT | Liveness pasif tanpa gerakan |

## Tipe Gerakan

String dipisahkan koma berisi ID gerakan:

| ID | Gerakan |
|----|---------|
| 1 | Buka mulut |
| 2 | Senyum |
| 3 | Kedipkan mata |
| 4 | Gelengkan kepala |
| 5 | Anggukkan kepala |

## Troubleshooting

### SIGSEGV crash di `checkModel`

SDK memvalidasi **nama paket + sertifikat signing** aplikasi Anda. Jika tidak cocok dengan nilai yang terdaftar, native code akan crash. Pastikan:

1. `applicationId` Anda terdaftar di FaceAISDK
2. Keystore signing Anda terdaftar di FaceAISDK
3. `android:extractNativeLibs="true"` diset di AndroidManifest.xml
### Kamera tidak berfungsi

Pastikan izin kamera diberikan saat runtime. Plugin mendeklarasikan `<uses-permission android:name="android.permission.CAMERA" />` secara otomatis di Android. Di iOS, tambahkan `NSCameraUsageDescription` di `Info.plist`.

### iOS: Error `BUILD_LIBRARY_FOR_DISTRIBUTION` / ABI mismatch

`FaceAISDK_Core` adalah binary pre-compiled. Semua pod harus menggunakan pengaturan `BUILD_LIBRARY_FOR_DISTRIBUTION` yang konsisten. Tambahkan blok `post_install` dari bagian Konfigurasi iOS ke Podfile Anda.

### iOS: `No such module 'FaceAISDK_Core'`

Pastikan Anda sudah menambahkan pod `FaceAISDK_Core` di Podfile dan menjalankan `pod install`. Modul ini tidak tersedia di CocoaPods trunk — harus direferensikan via URL Git.

## Lisensi

Lihat [LICENSE](LICENSE) untuk detail.

## Kredit

- [FaceAISDK Android](https://github.com/FaceAISDK/FaceAISDK_Android) — Core face AI engine (Android)
- [FaceAISDK_Core](https://github.com/FaceAISDK/FaceAISDK_Core) — Core face AI engine (iOS)
