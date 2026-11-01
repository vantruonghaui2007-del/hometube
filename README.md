# HomeTube · Vân Trường

Ứng dụng xem video YouTube riêng cho VNPT SmartBox 2 / Android 4.4+, dùng remote TV. Package `vn.hometube.app`, có thể gán cho ô YouTube trong HomeBox Lite. Giao diện tối/vàng hổ phách, bản quyền © 2026 Vân Trường.

## Cài và sử dụng

Xem [hướng dẫn cài, gán HomeBox Lite và QR điện thoại](docs/INSTALL.md), [báo cáo kiểm thử](docs/TEST-REPORT.md) và [ghi công/giấy phép](NOTICE.md).

- Tìm video/kênh/playlist, gợi ý từ khoá và bàn phím TV.
- Khám phá Âm nhạc/Game/Phim/Podcast; nếu kiosk không có dữ liệu, dùng tìm kiếm cùng chủ đề và ghi rõ.
- Kênh đăng ký cục bộ, Xem tiếp, Xem sau, Đã xem, đề xuất từ lịch sử/kênh.
- H.264 + AAC, tối đa 360/480/720/1080p; xem tiếp, playlist phân trang, liên quan, tự phát có huỷ.
- SponsorBlock tuỳ chọn; không có SDK quảng cáo, không nhúng player YouTube.
- QR và web remote trên cổng 8687, ghép bằng token; độc lập HomeKara.

Nút HomeKara chuyển sang ứng dụng karaoke; bản này chưa thêm trực tiếp vào hàng chờ riêng của HomeKara. Livestream, tài khoản Google, tải offline và Chromecast không thuộc phiên bản này.

## Build

JDK 17/21, Android SDK platform 34 và build-tools 34, Gradle 8.7; thư viện runtime theo app/build.gradle.kts. Min SDK 19, target 28 là chủ đích cho box, không dùng để phát hành trên Google Play.

Tạo khoá ký riêng một lần bằng keytool, alias hometube; sao chép keystore.properties.example thành keystore.properties rồi điền đường dẫn/mật khẩu. Không ghi đè khoá cũ nếu đã phân phối APK và muốn cập nhật tại chỗ.

```powershell
./gradlew.bat testDebugUnitTest assembleRelease lintDebug
# Trên máy đang triển khai có thể dùng bộ SDK/cache cũ tự tìm:
./tools/build.ps1
# Chạy thêm test mạng thật (không dùng cho CI offline):
$env:HOMETUBE_LIVE_TEST = '1'
./gradlew.bat testDebugUnitTest --rerun-tasks
```

APK: app/build/outputs/apk/release/app-release.apk. Bản bàn giao: outputs/HomeTube.apk. Không đóng gói khoá ký vào ZIP mã nguồn. Test Robolectric dùng Conscrypt OpenJDK riêng; APK giữ Conscrypt Android.

## Cấu trúc

- app/src/main/java/vn/hometube/app/core: link, resume, feed/cache, SponsorBlock, queue/ownership.
- data: NewPipe adapter, SQLite và SponsorBlock client.
- ui: giao diện TV, trang kênh/playlist, trình phát, cài đặt.
- remote và assets/phone: ghép điện thoại và điều khiển LAN.
- vn/homekara/box: nền HomeKara được giữ làm phần triển khai nội bộ, có ghi công.
- docs/superpowers: thiết kế đã duyệt và kế hoạch; docs/PROGRESS.md ghi các quyết định triển khai.

Mã nguồn GPL-3.0-or-later; xem LICENSE và NOTICE.md. Tính tương thích YouTube có thể thay đổi theo dịch vụ; kết quả trên máy tính và trên box thật được phân biệt trong báo cáo kiểm thử.
