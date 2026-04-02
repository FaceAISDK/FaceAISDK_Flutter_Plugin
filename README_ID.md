# FaceAI SDK Flutter Plugin

Plugin Flutter untuk [FaceAISDK](https://github.com/FaceAISDK/FaceAISDK_Android) — verifikasi wajah, deteksi liveness, dan pendaftaran wajah di perangkat Android.

**[English](README.md)**

## Fitur

- **Pendaftaran Wajah (Enroll)** — Daftarkan wajah untuk verifikasi 1:1
- **Verifikasi Wajah (1:1)** — Verifikasi wajah langsung dengan wajah tersimpan
- **Deteksi Liveness** — Anti-spoofing (gerakan, flash warna, silent)
- **Tambah Wajah** — Tambahkan wajah ke database pencarian

Semua proses dilakukan di perangkat. Tidak memerlukan internet.

## Persyaratan

- Android `minSdk >= 21`
- Perangkat `arm64-v8a` (ARM 64-bit)
- Izin kamera

## Instalasi

Tambahkan ke `pubspec.yaml`:

```yaml
dependencies:
  face_ai_sdk:
    git:
      url: https://github.com/your-repo/FaceAISDK_Flutter_Plugin.git
```

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

    // Cegah kompresi file model SDK
    androidResources {
        noCompress += listOf("model", "bin", "param", "tfl")
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

## Penggunaan

### Inisialisasi SDK

Wajib dipanggil sebelum method lainnya:

```dart
final faceAiSdk = FaceAiSdk();
await faceAiSdk.initializeSDK({});
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
  faceId: "user_123",
  threshold: 0.85,          // 0.75 - 0.95
  livenessType: 1,          // 0=NONE, 1=MOTION, 2=MOTION+COLOR, 3=COLOR, 4=SILENT
  motionStepSize: 1,        // 1-2 langkah
  motionTimeout: 10,        // 3-22 detik
  motionTypes: "1,2,3",     // 1=buka mulut, 2=senyum, 3=kedip, 4=geleng, 5=angguk
  format: "base64",
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
4. `noCompress` dikonfigurasi untuk file model di build.gradle

### Kamera tidak berfungsi

Pastikan izin kamera diberikan saat runtime. Plugin mendeklarasikan `<uses-permission android:name="android.permission.CAMERA" />` secara otomatis.

## Lisensi

Lihat [LICENSE](LICENSE) untuk detail.

## Kredit

- [FaceAISDK](https://github.com/FaceAISDK/FaceAISDK_Android) — Core face AI engine
