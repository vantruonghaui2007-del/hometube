# Cài HomeTube và gán vào HomeBox Lite

HomeTube là **APK riêng**, mã gói `vn.hometube.app`, không cài đè hoặc xoá dữ liệu HomeKara. Yêu cầu Android 4.4/API 19 trở lên, màn hình ngang và kết nối Internet. Phiên bản này dành cho sử dụng cá nhân.

## Cài bằng USB

1. Chép `outputs/HomeTube.apk` vào USB FAT32.
2. Trên box, bật **Nguồn không xác định** trong Cài đặt → Bảo mật nếu chưa bật.
3. Mở trình quản lý tệp, chọn APK và bấm Cài đặt.
4. Mở HomeTube từ danh sách ứng dụng. Nên chọn 480p khi Wi-Fi yếu, 720p khi dùng LAN.

Không cần gỡ HomeKara hoặc HomeBox Lite.

## Ô YouTube / phím tắt trong HomeBox Lite

APK HomeBox Lite 1.0.0 đang có trên máy chứa các mục **Ô YouTube**, **Đổi ứng dụng cho ô**, **Chọn ứng dụng cho ô YouTube** và **Đặt cho ô YouTube**. Đây là cơ chế dùng để mở APK riêng:

1. Cài HomeTube trước.
2. Mở **HomeBox Lite → Cài đặt → Ô YouTube / Ứng dụng YouTube**.
3. Chọn **Đổi ứng dụng / Chọn ứng dụng khác**, chọn **HomeTube** rồi bấm OK.
4. Trở về HomeBox Lite, chọn ô/phím tắt YouTube để mở HomeTube.

Nếu mở danh sách tất cả ứng dụng và có mục **Đặt cho ô YouTube**, có thể chọn HomeTube tại đó. Tên mục có thể khác nhẹ tuỳ bản launcher đã cài. Bản cũ tự tìm SmartTube/YouTube/NewPipe khi chưa gán; HomeTube cần được chọn thủ công một lần. Chưa có box kết nối để xác minh thao tác và cách gán phím remote vật lý riêng; hướng dẫn này dùng cơ chế gán ô ứng dụng đã xác nhận bằng phân tích tĩnh APK.

Component ổn định cho launcher có chức năng nhập đích mở:

```text
Package: vn.hometube.app
Activity: vn.hometube.app.ui.HomeActivity
Component: vn.hometube.app/vn.hometube.app.ui.HomeActivity
```

## Cài bằng ADB

```powershell
adb connect <IP-box>:5555
adb install -r HomeTube.apk
adb shell am start -n vn.hometube.app/vn.hometube.app.ui.HomeActivity
```

Địa chỉ IP và việc bật gỡ lỗi phụ thuộc box. Không cần root hoặc sửa ROM.

## Remote TV

| Phím | Thao tác |
|---|---|
| D-pad / OK | Di chuyển và chọn nội dung/nút |
| Menu hoặc giữ OK trên thẻ | Phát, Xem sau, mở kênh, chuyển HomeKara |
| OK khi ẩn điều khiển | Tạm dừng/tiếp tục |
| Trái/phải khi ẩn điều khiển | Tua 10 giây, giữ lâu 30 giây |
| Lên khi đang xem | Mở điều khiển, chọn chất lượng/kênh/xem sau |
| Xuống khi đang xem | Video liên quan |
| Next / CH+ | Video kế tiếp |
| Back | Ẩn điều khiển rồi quay lại; huỷ đếm ngược tự phát |

Khi thanh điều khiển đang hiện, D-pad di chuyển giữa các nút. Vị trí xem được lưu mỗi 10 giây và khi rời màn hình; video gần hết sẽ phát lại từ đầu. Livestream không được hiển thị/phát. Chất lượng là giới hạn tối đa: video không có luồng tương ứng sẽ chọn mức phù hợp thấp hơn.

## Chọn bằng điện thoại

1. Mở HomeTube trên TV, vào **Điện thoại → TV** hoặc Cài đặt.
2. Kết nối điện thoại cùng Wi-Fi/LAN với box, quét mã QR.
3. Tìm video hoặc dán nội dung chia sẻ/link YouTube, bấm **Phát trên TV**.

Cổng HomeTube là **8687**, độc lập cổng 8686 của HomeKara. QR có mã ghép riêng; chỉ nhập IP sẽ không điều khiển được. Đóng HomeTube/rời sang ứng dụng khác có thể dừng máy chủ; mở lại HomeTube và quét lại QR nếu mất kết nối. Phiên ghép hết hạn sau 12 giờ hoặc khi máy chủ khởi động lại. Không phải Chromecast/DIAL.

## HomeKara

Nút HomeKara mở ứng dụng karaoke hiện có. “Hát karaoke bài này” hiển thị tên bài để chuyển sang tìm trong HomeKara; **chưa thêm trực tiếp vào hàng chờ** vì hai APK có dữ liệu riêng và API của HomeKara cần mã ghép riêng. Dừng karaoke trước khi xem HomeTube, đặc biệt trên box S805 chỉ có ít bộ giải mã. HomeTube xin audio focus và nhả bộ phát khi rời màn hình; chưa xác minh ứng xử audio focus của HomeKara cũ trên box thật.

## Khi có vấn đề

| Vấn đề | Cách xử lý |
|---|---|
| Ô YouTube vẫn mở app cũ | Gán lại ô sang HomeTube theo các bước trên |
| Tìm được nhưng không phát | Thử 360p/480p, dừng karaoke; chạy Kiểm tra hệ thống |
| Mục khám phá ghi “Gợi ý qua tìm kiếm” | Kiosk khu vực không có dữ liệu; app dùng kết quả tìm kiếm cùng chủ đề và ghi rõ nguồn gợi ý |
| QR không hiện | Kiểm tra LAN/Wi-Fi và mở lại HomeTube; cổng 8687 phải đang hoạt động |
| Không điều khiển được từ điện thoại | Quét lại QR, kiểm tra hai thiết bị cùng mạng, tắt chế độ cô lập thiết bị Wi-Fi nếu có |
| Nội dung bị cắt ở mép TV | Cài đặt HomeTube → Lề an toàn, chỉnh 0–10% |
| Cả tìm kiếm và phát cùng hỏng sau một thời gian | YouTube có thể đã thay đổi; cần cập nhật phần trích xuất và build bản mới |
| Vẫn có đoạn tài trợ trong nội dung | SponsorBlock dựa vào dữ liệu cộng đồng, không phải video nào cũng có dữ liệu |

Không có SDK quảng cáo hoặc player nhúng YouTube. Phần kiểm chứng mạng đã thực hiện không thay thế bài thử xem liên tục trên TV; xem `TEST-REPORT.md`.

## Cập nhật, dữ liệu và khoá ký

Cài bản mới cùng package và chữ ký bằng `adb install -r`; không gỡ trước nếu muốn giữ lịch sử, Xem sau và kênh đăng ký. Khoá ký của dự án nằm tại `keystore/hometube.jks`, thông tin tại `keystore.properties`, đều không đưa vào mã nguồn/ZIP nguồn. Giữ hai tệp này riêng để ký các bản cập nhật của bạn. Gỡ HomeTube sẽ xoá dữ liệu riêng của HomeTube, không xoá dữ liệu HomeKara.
