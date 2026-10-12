# Absensi RDMKU Android (build APK otomatis)

Proyek Android awal untuk membuka `https://absensi.rdmku.cloud/preview2.html`, mengalihkan pemanggilan Web Speech API (`speechSynthesis.speak`) ke Android TextToSpeech, dan menjaga layanan TTS sebagai foreground service.

## Fitur awal
- WebView membuka halaman jadwal dari FastPanel.
- TTS memakai mesin Text-to-Speech yang dipasang di Android (disarankan Google Speech Services, Bahasa Indonesia).
- Layanan foreground dengan notifikasi agar mesin TTS tetap hidup saat layar tidak aktif.
- Build APK otomatis menggunakan GitHub Actions; tidak perlu Android Studio di komputer.

## Penting tentang audio latar belakang
Versi ini mengalihkan ucapan yang diminta oleh `speechSynthesis` halaman ke native Android TTS. Namun, jadwal otomatis saat layar benar-benar mati belum dapat dijamin hanya dengan WebView: Android dapat menunda JavaScript halaman di latar belakang. Untuk pemicu yang benar-benar andal saat layar mati, perlu integrasi native dengan API jadwal yang dipakai halaman (atau menambahkan endpoint jadwal terstruktur di server) sehingga Android dapat menjadwalkan alarm native. Halaman preview yang tersedia belum memberi kontrak API jadwal yang dapat diandalkan dalam proyek ini.

## Prasyarat suara
Di HP Android, buka Pengaturan > Bahasa & masukan / Text-to-speech output, pilih Google Speech Services atau Google Text-to-Speech, unduh data suara Bahasa Indonesia, lalu buka aplikasi dan izinkan notifikasi.

## Build tanpa Android Studio
1. Buat repositori baru di GitHub.
2. Ekstrak ZIP proyek ini di komputer.
3. Unggah semua file dan folder proyek ke repositori GitHub, termasuk folder `.github/workflows`.
4. Buka tab **Actions** di repositori dan pilih **Build APK** > **Run workflow** (atau tunggu workflow berjalan setelah commit ke `main`).
5. Setelah status selesai dengan tanda hijau, buka run tersebut, cari bagian **Artifacts**, lalu unduh `Jadwal-Madrasah-APK`.
6. Ekstrak ZIP artifact, salin `app-debug.apk` ke HP, buka file, dan izinkan instalasi dari sumber tersebut jika diminta.

## Build lokal
Memerlukan JDK 17 dan Gradle 8.9: `gradle assembleDebug`.

## Catatan keamanan
APK ini memuat situs HTTPS yang diberikan pengguna. Jangan menambahkan API Key ke APK atau ke JavaScript frontend.


## Dukungan notifikasi situs
Aplikasi meminta izin notifikasi Android dan menjalankan layanan foreground untuk TTS. Notifikasi yang tampil sebagai bagian dari halaman (in-app) mengikuti fungsi situs. Namun, Web Push/Notification API dari website tidak otomatis didukung oleh Android WebView; jika situs mengandalkan push notification saat aplikasi ditutup, diperlukan integrasi native Firebase Cloud Messaging (FCM) atau endpoint notifikasi server.


## Ikon aplikasi
Ikon launcher Absensi RDMKU telah ditambahkan ke resource mipmap Android dan dipasang melalui AndroidManifest.xml.
