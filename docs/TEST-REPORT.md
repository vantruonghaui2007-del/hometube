# HomeTube 1.0.0 — báo cáo kiểm chứng

Ngày: 2026-11-01, múi giờ Asia/Saigon. Nền tảng xây dựng: Windows, JDK 21, Android SDK 34 / AGP 8.5.2 / Gradle 8.7. Package `vn.hometube.app`; API tối thiểu 19, target 28.

## Đã kiểm chứng trên máy

- Bộ test logic, ánh xạ, remote policy, SQLite và khởi động UI: xem số lượng/kết quả chính xác trong `qa-summary.json`, được tạo khi đóng gói. Không đóng gói nếu có test thất bại hoặc lỗi lint.
- Robolectric API 19: dữ liệu xem tiếp/Xem sau còn sau khi mở lại DB; video gần hết không nằm trong Xem tiếp; xoá lịch sử giữ Xem sau và kênh đăng ký. Home khởi động, chuyển Tìm kiếm, bấm bàn phím TV và tái sử dụng thẻ video không crash.
- Test mạng thật: tìm kiếm VTV24 trả 19 video; lấy 20 stream candidate từ một video công khai, chọn được H.264/AAC; yêu cầu tới luồng hình và tiếng đều trả **HTTP 200** và có byte dữ liệu. Không chỉ kiểm tra URL hoặc build thành công.
- Parser: URL chia sẻ, timestamp, m./music./Shorts/embed, playlist/Mix, host giả và ID sai. Resume: gần cuối/video ngắn/không rõ thời lượng. Feed: lỗi RSS có fallback, một kênh lỗi không làm mất kênh khác. SponsorBlock: JSON hỏng, khoảng không hợp lệ, tua lại vào đoạn đã bỏ qua. Remote: token bắt buộc, sai origin và kích thước body bị từ chối.
- Hồi quy review: thông báo READY sau tải lại 403; chỉ lưu vị trí khi đúng media đang sở hữu bộ phát; queue giới hạn payload vẫn giữ video đã chọn sau mục 100; giữ cursor đến khi tải trang playlist tiếp theo; cache feed 30 phút và đổi kênh làm cache mất hiệu lực.
- Build release hoàn tất. Lint bật `abortOnError=true`, đã sửa opt-in Media3; không còn Error/Fatal. Chỉ tắt `ExpiredTargetSdkVersion` vì đây là APK sideload theo thiết kế, không phát hành Google Play. Các cảnh báo còn lại gồm chuỗi tiếng Việt viết trong Views và tài nguyên/nền HomeKara chưa sử dụng; không có lỗi dùng API mới thiếu kiểm tra.
- `apksigner verify --verbose --min-sdk-version 19`: chữ ký **v1=true, v2=true**. Aapt xác nhận nhãn HomeTube, package và launcher `vn.hometube.app.ui.HomeActivity`.
- Cú pháp JavaScript của trang điện thoại đã được kiểm bằng `node --check`.

## Ảnh kiểm tra UI

`hometube-home.png` và `hometube-search.png` trong thư mục outputs được dựng từ **Android Views thực tế** bằng Robolectric native graphics ở SDK 34, 1280×720. Dữ liệu trang chủ dùng fixture lấy từ tìm kiếm YouTube thật. Đây **không phải screenshot từ box VNPT**, không chứng minh giải mã video hoặc hiệu năng trên S805. Đã xem ảnh để kiểm khoảng trống, lề, bố cục, chữ ký và nút; phát hiện/sửa nút HomeKara bị chật và lỗi tái sử dụng thẻ ảnh trong vòng QA.

Robolectric 4.12.2 chỉ phục vụ kiểm thử; nhánh 4.12 bổ sung native graphics trên Windows theo [thông báo chính thức](https://github.com/robolectric/robolectric/discussions/8954). Không thêm thư viện này vào APK.

## Rà soát mã độc lập

Một reviewer độc lập đã đọc mã mới, manifest và remote. Bốn lỗi Important đã được sửa: overlay khi retry 403, lưu vị trí sai media lúc đổi nhanh, chọn sai video sau phần tử 100 và thiếu tiếp nối playlist. Bài render thực tế sau đó phát hiện thêm việc `Thumbs.load` thay `ImageView.tag` làm tìm view khi recycle thất bại; đã dùng ViewHolder giữ tham chiếu ổn định và kiểm tra hồi quy API19.

Các điểm nhỏ cũng được xử lý: QR shortcut cuộn tới phần điện thoại; lề an toàn áp dụng lại khi quay về màn hình; lỗi của lượt tìm điện thoại cũ không ghi đè lượt mới. Không còn mục nhỏ được chủ động hoãn trong đợt bàn giao này.

## Chưa kiểm chứng trên box thật

`adb devices` không có thiết bị kết nối. Các mục dưới đây **chưa kiểm**, không suy từ Robolectric/build:

- Cài và mở từ ô/phím tắt HomeBox Lite trên box VNPT thực tế; gán phím remote vật lý riêng.
- TLS/Conscrypt và giải mã H.264/AAC bằng S805, chọn 480p/720p/1080p; xem liên tục ba video, không quảng cáo đầu/giữa từ player YouTube.
- Độ trễ điều khiển, focus trên remote VNPT, overscan/cỡ chữ thực tế.
- Điện thoại quét QR và điều khiển qua LAN/Wi-Fi thật, chặn/cô lập Wi-Fi và hết hạn ghép.
- Chuyển HomeKara/HomeTube, tương tác audio focus/bộ giải mã của APK HomeKara đang cài. Nên dừng karaoke trước khi phát video.
- Phát playlist đi qua nhiều trang trên box, mất mạng, video giới hạn tuổi/khu vực, và dữ liệu SponsorBlock thật.
- Đo PSS/RAM sau mở/đóng mười video; phiên xem dài, rút điện/khởi động và khôi phục vị trí.

## Quyết định triển khai và giới hạn

1. Tạo repo/APK riêng trong thư mục người dùng yêu cầu, giữ nguyên các dự án cũ. Nền mã bị lặp lại nhưng không ảnh hưởng HomeKara đã cài.
2. Giữ NewPipeExtractor v0.26.5 vì test mạng thật hoạt động; khi YouTube thay đổi vẫn cần bảo trì.
3. Kiosk không có dữ liệu dùng gợi ý tìm kiếm theo chủ đề, có nhãn nguồn rõ; kết quả không được coi là bảng xếp hạng khu vực.
4. Target 28 và chữ ký v1 phục vụ box cũ; nếu phát hành Google Play cần kế hoạch target/API riêng.
5. Test JVM dùng Conscrypt OpenJDK; APK dùng Conscrypt Android. Kiểm trên host không thay cho TLS trên box.
6. Hai APK không dùng chung DB/session. Nút karaoke chuyển sang HomeKara để tìm bài, không báo giả đã thêm hàng chờ. Điều phối decoder xuyên APK phụ thuộc ứng xử của HomeKara cũ và cần test thiết bị.

Mã nguồn, hướng dẫn và SHA-256 đi kèm APK. Khoá ký và mật khẩu ở workspace được giữ riêng, không nằm trong gói nguồn.
