# FMM26 Database Editor (Android)

Fondasi awal aplikasi Android native menggunakan Kotlin + Jetpack Compose.

## Status versi 0.1.0
- Memilih folder database melalui Android Storage Access Framework.
- Memindai file `.dat` dan membaca `config.xml`.
- Menampilkan perkiraan versi database dari nama folder/config.
- Belum mendekode atau menulis isi file `.dat`.

## Persyaratan
- Android Studio versi yang mendukung Android Gradle Plugin 8.10.1.
- JDK 17.
- Android SDK Platform 35.
- Koneksi internet saat Gradle pertama kali mengunduh dependensi.

## Cara membuka dan membuat APK
1. Ekstrak ZIP proyek.
2. Buka folder `FMM26DatabaseEditor` melalui Android Studio.
3. Tunggu Gradle Sync selesai.
4. Pilih **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. APK debug akan muncul di `app/build/outputs/apk/debug/app-debug.apk`.

## Penting
Jangan gunakan aplikasi ini untuk menyimpan perubahan database game dulu. Parser/serializer biner belum diimplementasikan dan harus diuji dengan satu set database FMM26 yang lengkap serta versinya terkonfirmasi.
