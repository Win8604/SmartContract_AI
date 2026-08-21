# SmartContract AI 🚀

**SmartContract AI** là ứng dụng Android hiện đại được phát triển bằng **Kotlin** và **Jetpack Compose**, trợ lực bởi Trí tuệ Nhân tạo (AI) giúp cá nhân và doanh nghiệp khởi tạo, rà soát rủi ro, chỉnh sửa và quản lý hợp đồng pháp lý một cách nhanh chóng, an toàn và chuyên nghiệp.

---

## 🌟 Tính Năng Nổi Bật (Key Features)

### 🤖 1. Tạo Hợp Đồng Tự Động Bằng AI (AI Contract Generation)
- Sinh hợp đồng tự động theo yêu cầu (Prompt) hoặc chọn sẵn loại hợp đồng (Thử việc, NDA, Dịch vụ, Mua bán...).
- Tùy chỉnh thông tin Bên A, Bên B, giá trị hợp đồng, thời hạn và các điều khoản phụ.
- Hiệu ứng trực quan hóa quá trình AI phân tích và tạo hợp đồng.

### 📄 2. Kho Hợp Đồng Mẫu Phong Phú (Contract Templates)
- Thư viện hợp đồng mẫu đa dạng, phân loại rõ ràng: *Lao động & Thử việc, Bảo mật thông tin (NDA), Dịch vụ & Tư vấn, Mua bán thương mại, Bất động sản*.
- Xem trước nội dung mẫu và chỉnh sửa trực tiếp thông tin cần thiết.

### ✍️ 3. Trình Biên Soạn & Chỉnh Sửa Hợp Đồng (Document Editor & Review)
- Công cụ chỉnh sửa văn bản hợp đồng mượt mà, hỗ trợ tạo định dạng và sửa đổi các điều khoản.
- Tính năng **Phân tích Rủi ro AI (AI Risk Review)**: Phát hiện điều khoản bất lợi, thiếu sót pháp lý hoặc rủi ro tiềm ẩn.
- Ký duyệt kỹ thuật số và lưu trữ hợp đồng.

### 📊 4. Quản Lý & Báo Cáo Thống Kê (Dashboard & Management)
- Giao diện Dashboard trực quan phân loại hợp đồng theo trạng thái: *Tất cả hợp đồng, Chờ phê duyệt, Chờ ký kết, Đã hoàn tất*.
- Tìm kiếm, lọc hợp đồng theo từ khóa và trạng thái.
- Hỗ trợ giao diện chuyên biệt cho **Tài khoản Doanh nghiệp (Corporate)**.

### 🔐 5. Hệ Thống Xác Thực & Bảo Mật Đa Dạng (Authentication & Security)
- **Đăng ký / Đăng nhập thường**: Hỗ trợ phân loại tài khoản Cá nhân (Personal) và Doanh nghiệp (Corporate - tích hợp Mã số thuế).
- **Đăng nhập Mạng xã hội**: Tích hợp **Google Sign-In** (Credential Manager & Google Play Services) và **Facebook SDK**.
- **Xác thực Sinh trắc học (Biometric Authentication)**: Đăng nhập an toàn qua Vân tay, Khuôn mặt (FaceID) hoặc Mã PIN thiết bị.
- Ghi nhớ phiên đăng nhập (Remember Me) tiện lợi.

### 🔔 6. Thông Báo Trực Tuyến (Real-time Notifications)
- Tích hợp **Firebase Cloud Messaging (FCM)** gửi thông báo tự động khi hợp đồng cập nhật trạng thái.
- Quản lý lịch sử thông báo offline và đánh giá thông báo đã đọc/chưa đọc.

---

## 🛠️ Công Nghệ Sử Dụng (Tech Stack)

| Thành phần | Công nghệ / Thư viện |
| :--- | :--- |
| **Ngôn ngữ lập trình** | Kotlin 1.9+ |
| **Giao diện (UI)** | Jetpack Compose (Material Design 3, Compose Foundation, Material Icons Extended) |
| **Kiến trúc** | Single-Activity Architecture, State Management với Compose |
| **Lưu trữ dữ liệu** | SQLite (`SQLiteOpenHelper`), JSON Local Storage (`UserFileManager`), SharedPreferences |
| **Backend & Cloud** | Firebase Auth, Firebase Cloud Messaging (FCM), Firebase Analytics |
| **SDKs Bên thứ 3** | Facebook Android SDK, Google Identity Credentials, Google Play Services Auth |
| **Hình ảnh & Media** | Coil Compose, Glide |
| **Bảo mật** | AndroidX BiometricPrompt API |

---

## 📁 Cấu Trúc Thư Mục Dự Án (Project Structure)

```text
SmartContractAI/
├── app/
│   ├── src/main/
│   │   ├── java/com/smartcontractai/
│   │   │   ├── data/
│   │   │   │   ├── NotificationRepository.kt  # Quản lý & lưu trữ thông báo
│   │   │   │   ├── UserDatabaseHelper.kt      # SQLite Database lưu User, Stats, Contracts
│   │   │   │   └── UserFileManager.kt         # Quản lý file JSON lưu User & Session
│   │   │   ├── service/
│   │   │   │   └── MyFirebaseMessagingService.kt # Xử lý sự kiện nhận thông báo FCM
│   │   │   ├── ui/theme/
│   │   │   │   ├── Color.kt                   # Bảng màu giao diện
│   │   │   │   ├── Theme.kt                   # SmartContractAI Compose Theme
│   │   │   │   └── Type.kt                    # Kiểu chữ & Typography
│   │   │   ├── utils/
│   │   │   │   └── FcmUtils.kt                # Utility xin quyền & lấy Token FCM
│   │   │   ├── ContractDocumentEditorScreen.kt # Màn hình chỉnh sửa văn bản hợp đồng
│   │   │   ├── ContractReviewScreen.kt         # Màn hình rà soát hợp đồng & AI Risk Review
│   │   │   ├── ContractTemplatesScreen.kt      # Màn hình danh sách hợp đồng mẫu
│   │   │   ├── CreateContractOverviewScreen.kt # Màn hình tổng quan chọn cách tạo hợp đồng
│   │   │   ├── CreateContractWithAIScreen.kt   # Màn hình tạo hợp đồng bằng AI
│   │   │   ├── DashboardScreen.kt              # Màn hình chính Dashboard & Thống kê
│   │   │   ├── LoginActivity.kt                # Giao diện Đăng nhập / Đăng ký / Quên mật khẩu
│   │   │   ├── MainActivity.kt                 # Activity chính, cài đặt OAuth & Biometrics
│   │   │   └── SmartContractApplication.kt     # Khởi tạo Application & SDKs
│   │   └── AndroidManifest.xml                 # Khai báo Quyền, Service & Activity
│   └── build.gradle.kts                        # Khai báo dependency của ứng dụng
├── gradle/                                     # Gradle Wrapper & Version Catalogs
├── build.gradle.kts                            # Cấu hình Gradle chung
└── settings.gradle.kts                         # Cấu hình dự án Gradle
```

---

## ⚙️ Yêu Cầu Hệ Thống & Cài Đặt (Prerequisites & Setup)

### Yêu cầu môi trường
- **Android Studio**: Jellyfish (2023.3.1) hoặc phiên bản mới hơn.
- **JDK**: Version 17.
- **Android SDK**: `compileSdk = 35`, `minSdk = 24` (Android 7.0 Nougat trở lên).
- **Gradle**: 8.x.

### Các bước khởi chạy dự án
1. **Clone repository về máy local:**
   ```bash
   git clone https://github.com/Win8604/SmartContract_AI.git
   cd SmartContract_AI
   ```

2. **Mở dự án trong Android Studio:**
   - Chọn `Open an Existing Project` và chỉ định thư mục `SmartContractAI`.
   - Chờ Gradle Sync hoàn tất tải các thư viện.

3. **Cấu hình Firebase & Facebook (Nếu cần thiết):**
   - Đảm bảo file `app/google-services.json` đã có mặt trong thư mục `app/`.
   - Cấu hình Facebook App ID và Client Token trong `app/src/main/res/values/strings.xml` nếu thử nghiệm Facebook Auth.

4. **Biên dịch và Chạy ứng dụng:**
   - Chọn thiết bị ảo (Emulator API 24+) hoặc thiết bị Android thật đã bật USB Debugging.
   - Nhấn **Run (Shift + F10)**.

---

## 📄 Giấy Phép (License)

Dự án thuộc sở hữu của **SmartContract AI Team**. Mọi quyền được bảo lưu.
