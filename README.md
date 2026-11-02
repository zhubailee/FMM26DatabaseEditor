# FMM26 Database Editor — Android

Aplikasi Android native Kotlin + Jetpack Compose dengan branding ungu-emas.

## Build 0.2.0 — target database 2603
- Target pemeriksaan versi database diubah ke **2603**.
- UI dashboard diperbarui dengan tema gelap ungu dan aksen emas.
- Launcher icon adaptif ditambahkan.
- Pemilihan folder melalui Android Storage Access Framework.
- Pemindaian file `.dat` dan pembacaan `config.xml`.

## Batasan saat ini
Aplikasi ini **belum** mengurai, mengedit, atau menulis ulang isi biner file `.dat`. Pemindaian versi hanya membaca pola `db_archive_2603` dari nama folder/config. Jangan gunakan aplikasi untuk menyimpan perubahan database game sampai parser dan serializer 2603 selesai diuji.

## Build APK
Gunakan JDK 17, Android SDK Platform 35, lalu jalankan:
```bash
gradle --no-daemon :app:assembleDebug
```
APK: `app/build/outputs/apk/debug/app-debug.apk`.
