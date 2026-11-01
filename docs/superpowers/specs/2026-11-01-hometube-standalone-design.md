# HomeTube — APK độc lập cho VNPT SmartBox 2

## Yêu cầu đã xác nhận

Người dùng muốn xây HomeTube theo tài liệu `C:/Users/vantr/Downloads/2026-10-24-hometube.md`, giao diện đẹp, có bản quyền tác giả Vân Trường. Người dùng đã chọn **APK riêng**, mở được bằng phím tắt/ô ứng dụng trong **HomeBox Lite đã build trước đó**. Yêu cầu này thay thế quyết định chung APK với HomeKara trong tài liệu cũ.

## Mục tiêu

Ứng dụng xem video YouTube trên VNPT SmartBox 2, Android 4.4.2/API 19, ưu tiên remote D-pad, RAM 1 GB, H.264, chất lượng mặc định 720p. Cài độc lập, không thay thế APK HomeKara và không đụng dữ liệu karaoke. Có APK đã ký, mã nguồn, hướng dẫn cài/gán phím tắt và báo cáo kiểm thử trung thực.

## Các phương án và lựa chọn

- Chung APK với HomeKara: tái sử dụng hàng chờ/trình phát trong tiến trình; người dùng đã không chọn.
- APK HomeTube riêng: người dùng đã chọn. Dùng lại nền tảng phát/TLS từ HomeKara dưới dạng mã nguồn, dữ liệu và vòng đời độc lập. Tốn thêm dung lượng APK nhưng cài, nâng cấp và gán launcher độc lập.
- Nhúng YouTube trong WebView: không chọn vì WebView cũ trên KitKat và không đáp ứng thiết kế trình phát H.264 trong tài liệu.

## Giao diện đề xuất của người triển khai

Khung ngang 1280×720 dp theo `TvActivity` hiện có; nền than `#0D1015`, panel `#171B22`, điểm nhấn vàng hổ phách `#FFB547`, chữ trắng dịu. Thanh trái khoảng 210 dp, biểu tượng màn hình/play tự thiết kế, nhóm Khám phá và Thư viện rõ ràng. Nội dung có tiêu đề lớn, phụ đề ngắn, lưới video bốn cột, thumbnail 16:9, thời lượng ở góc ảnh và tiến độ xem ở đáy. Tìm kiếm dùng bàn phím TV và ba loại Video/Kênh/Danh sách phát.

Thẻ bo góc, khoảng trống thoáng; viền focus vàng ít nhất 3 dp, focus không phụ thuộc màu duy nhất, tất cả nút tới được bằng remote. Không dùng hiệu ứng nặng hoặc emoji màu. Tiêu đề thẻ tối thiểu 15sp; phụ đề tối thiểu 13sp. Trạng thái tải/rỗng/lỗi có hướng dẫn và nút thử lại. Lề an toàn mặc định 5%, điều chỉnh 0–10%; không để scale chữ hệ thống làm mất nút hoặc kẹt focus.

Dòng chân màn hình **© 2026 Vân Trường**, tên tác giả nhấn vàng, nét phân cách mảnh; màn hình Giới thiệu ghi **Thiết kế & phát triển bởi Vân Trường**. Giữ thông báo giấy phép và ghi công mã nguồn/thư viện được sử dụng trong phần giấy phép.

## Chức năng

Tìm kiếm có gợi ý từ khoá; trang video, kênh và danh sách phát; đăng ký kênh cục bộ; đề xuất từ lịch sử/kênh đăng ký; Âm nhạc, Trò chơi, Phim & trailer, Podcast; Xem tiếp, Xem sau, Đã xem. Không hiển thị livestream. Trang kênh có đăng ký; playlist có phát tất cả. Phân trang, loại trùng và bỏ kết quả tải cũ khi đổi mục.

Trình phát toàn màn hình: OK tạm dừng/tiếp tục; trái/phải tua 10 giây, giữ lâu 30 giây; lên mở điều khiển; xuống video liên quan; Next video kế; Back ẩn HUD rồi thoát. Chọn tối đa 360/480/720/1080p, chỉ H.264; lưu vị trí mỗi 10 giây và khi rời màn hình. Tự phát tiếp có đếm ngược và huỷ. SponsorBlock tùy chọn, lỗi dịch vụ không chặn phát và tôn trọng tua lại của người xem.

## Đóng gói và HomeBox Lite

Đề xuất package ổn định `vn.hometube.app`, tên hiển thị **HomeTube**, launcher activity export rõ ràng với `MAIN`, `LAUNCHER`, `LEANBACK_LAUNCHER`. Không khai báo HOME. Có thể mở bằng PackageManager hoặc component ổn định; tài liệu bàn giao ghi chính xác component sau khi build.

APK HomeBox tại `C:/Users/vantr/Downloads/HomeBox-Launcher.apk` xác nhận package `vn.homekara.launcher`, nhãn HomeBox Lite, minSdk 19, phiên bản 1.0.0 (aapt). Thiết kế HomeBox cũ ghi ô YouTube lưu `AppTarget(packageName, activityName)` do người dùng chọn và có thể gán lại. Đọc tĩnh chuỗi DEX trong APK bằng công cụ shell cũng tìm thấy “Chọn ứng dụng cho ô YouTube”, “Đổi ứng dụng cho ô” và “Đặt cho ô YouTube”; đây là bằng chứng UI tĩnh, chưa phải đã thao tác thành công trên thiết bị. Sau khi cài HomeTube, gán ô/phím tắt YouTube của HomeBox sang HomeTube một lần. Không tự sửa cấu hình riêng của launcher; kiểm tra cách gán trong bản APK thực tế trước khi viết hướng dẫn thao tác chính xác. Không khẳng định phím tắt vật lý cụ thể đã hoạt động khi chưa thử trên box.

## Kiến trúc và tương thích

Native Kotlin/Android Views, compileSdk 34, minSdk 19, targetSdk 28; nền phát Media3 1.4.1, Conscrypt 2.5.2, NewPipeExtractor v0.26.5 theo tài liệu gốc, NanoHTTPD và ZXing đã có trong nền HomeKara. Không dùng Compose hoặc API mới thiếu kiểm tra. Kiểm tra khả năng trích xuất YouTube hiện tại trước nghiệm thu; nếu bản extractor cũ không hoạt động, ghi rõ kết quả và xử lý tương thích bằng thay đổi riêng được mô tả, không tuyên bố xem được dựa trên build thành công.

Lõi thuần JVM cho parser link, resume, chọn video kế, cache, feed, SponsorBlock và bàn phím; Android adapter cho UI, mạng, SQLite. Dữ liệu `hometube.db` riêng trong sandbox của HomeTube. IO ngoài main thread; token hủy kết quả cũ; cache thumbnail giới hạn kích thước; release trình phát khi rời màn hình.

Trang điện thoại của HomeTube dùng cổng riêng **8687**, QR kèm mã ghép cục bộ, tránh cổng 8686 của HomeKara. Tìm video, dán link video/playlist/kênh, phát trên TV; báo lỗi đầu vào rõ ràng. Không gọi đây là Chromecast. Máy chủ chỉ khả dụng khi HomeTube có phiên đang mở; không dùng dịch vụ thường trực để giữ tiến trình sống. Nếu hệ thống đã đóng HomeTube, người dùng mở HomeTube từ HomeBox trước rồi quét QR/gửi video. Thông báo trạng thái rõ khi không mở được cổng; không hiển thị QR như thể máy chủ đang hoạt động.

## Liên kết HomeKara khi tách APK

Giữ nút mở HomeKara. Không dùng trực tiếp `App.session`, SQLite hoặc hàng chờ của APK kia. Nút “Hát karaoke bài này” chỉ thêm vào hàng chờ nếu API/broadcast đã export và cơ chế ghép được xác minh; nếu bản HomeKara đang cài không có giao diện tích hợp phù hợp, chuyển sang HomeKara và hiển thị cách tìm bài, không báo giả “đã thêm”. Không tự bỏ kiểm tra token API của HomeKara.

HomeTube xin audio focus và dừng/nhả bộ phát khi mất focus hoặc rời playback. Nếu HomeKara cũ không nhả decoder qua audio focus, không thể bảo đảm điều phối xuyên APK chỉ bằng HomeTube; cần kiểm tra trên box và ghi nhận hạn chế, yêu cầu dừng karaoke trước khi phát nếu cần. Không tự giết tiến trình ứng dụng khác. Tích hợp hàng chờ tự động/chống phát chồng hoàn toàn có thể cần bản cập nhật HomeKara riêng; không coi đó là chức năng đã xác nhận của APK độc lập.

## Ngoài phạm vi

Đăng nhập Google, đồng bộ tài khoản, bình luận, tải offline, livestream, giao diện Shorts riêng, Chromecast chuẩn, chỉnh ROM hoặc launcher mặc định. Không tự đẩy mã/tag/phát hành GitHub.

## Kiểm chứng và bàn giao

Trước khi triển khai toàn bộ UI, build nền tối thiểu và thử tìm kiếm/lấy chi tiết/trích xuất luồng H.264 từ video công khai thật với extractor đã khóa. Kiểm tra minSdk của dependency và build cho API 19; khả năng chạy thật trên KitKat phải kiểm bằng emulator/box nếu có. Nếu không có thiết bị, ghi rõ phần chưa xác minh và không công bố đạt tương thích runtime. Nếu extractor v0.26.5 hỏng, ghi nguyên nhân và chốt bản nâng tương thích trong thay đổi riêng trước nghiệm thu phát YouTube.

Chạy test JVM cho parser link (share text, Shorts, timestamp, Mix, URL không hợp lệ), resume gần cuối, feed lỗi từng kênh, cache hết hạn và SponsorBlock/tua lại. Build APK release và xác minh chữ ký v1 cho Android 4.4; kiểm manifest package/launcher/minSdk bằng aapt. Kiểm tra UI bằng screenshot/emulator nếu khả dụng.

Tiêu chí không quảng cáo: phát luồng nội dung bằng trình phát riêng, không nhúng YouTube player và không thêm SDK quảng cáo; thử ba video công khai để xác nhận không có quảng cáo đầu/giữa video do player YouTube chèn. Đoạn tài trợ do tác giả chèn là chức năng SponsorBlock riêng, không bảo đảm bỏ hết khi dữ liệu cộng đồng thiếu. Kết quả phát mạng hiện tại và kết quả trên box phải ghi riêng.

Checklist trên box: cài song song HomeKara; gán và mở HomeTube từ HomeBox; D-pad không kẹt focus; tìm/phát video thật; chất lượng; QR điện thoại; xem tiếp sau khởi động; chuyển karaoke/video; mở/đóng mười video và đo RAM. Mục chưa kiểm trên thiết bị thật phải ghi **chưa kiểm**, không suy từ test JVM/build. Bàn giao `HomeTube.apk`, SHA-256, source và hướng dẫn cài bằng USB/ADB.

## Trạng thái

Thiết kế được người dùng duyệt bằng “triển khai đi” ngày 2026-11-01. Package, cổng và bố cục chi tiết do người triển khai quyết định trong phạm vi đã duyệt. Các thay đổi nhỏ và kết quả kiểm chứng được ghi ở docs/PROGRESS.md và docs/TEST-REPORT.md.
