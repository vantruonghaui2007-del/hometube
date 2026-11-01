# Ghi công và giấy phép

HomeTube — giao diện và phần mở rộng © 2026 Vân Trường.

Nền TLS, HTTP, thumbnail, H.264/Media3 và một số tiện ích được kế thừa từ dự án HomeKara của tài khoản `vantruonghaui2007-del`, nền tham chiếu commit `c2d77ee0bdbb6a1ba1d7448ba5a9e511a67fd238`: https://github.com/vantruonghaui2007-del/HomeKara . Các namespace `vn.homekara.box.*` còn lại trong nguồn là phần triển khai nội bộ; APK phân phối mang package riêng `vn.hometube.app`.

HomeTube kết hợp NewPipeExtractor và được phân phối dưới GPL-3.0-or-later; xem LICENSE và mã nguồn đi kèm. Dòng bản quyền Vân Trường không xoá ghi công của các thư viện hoặc dự án gốc.

Các thành phần chính:

- NewPipeExtractor — Team NewPipe, GPL-3.0-or-later: https://github.com/TeamNewPipe/NewPipeExtractor
- AndroidX Media3, multidex — Android Open Source Project, Apache-2.0.
- Conscrypt — Conscrypt contributors, Apache-2.0.
- NanoHTTPD — NanoHTTPD contributors, BSD-3-Clause.
- ZXing — ZXing authors, Apache-2.0.
- Kotlin — JetBrains và contributors, Apache-2.0.
- Các dependency bắc cầu giữ giấy phép/NOTICE trong artifact gốc; danh sách được Gradle giải quyết từ app/build.gradle.kts.

SponsorBlock là dịch vụ cộng đồng bên ngoài. HomeTube không liên kết với Google, YouTube hoặc SponsorBlock. Hướng dẫn gốc của HomeKara ghi việc dùng trình phát YouTube riêng có thể không phù hợp điều khoản dịch vụ của YouTube; dự án dành cho sử dụng cá nhân.
