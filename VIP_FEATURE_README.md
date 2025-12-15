# Tính năng VIP Account cho NewsAI

## Tổng quan

Tính năng VIP Account cho phép người dùng nâng cấp tài khoản để truy cập các tính năng cao cấp của ứng dụng NewsAI.

## Các thành phần đã tạo

### 1. Activity Classes

#### VipAccountActivity
- **Đường dẫn**: `app/src/main/java/com/example/newsai/VipAccountActivity.java`
- **Chức năng**: Hiển thị các gói VIP với giá và tính năng
- **Các gói VIP**:
  - VIP 1 Tháng: $9.99
  - VIP 3 Tháng: $24.99 (PHỔ BIẾN NHẤT - Tiết kiệm 17%)
  - VIP 1 Năm: $79.99 (GIÁ TRỊ TỐT NHẤT - Tiết kiệm 33%)

#### VipPaymentActivity
- **Đường dẫn**: `app/src/main/java/com/example/newsai/VipPaymentActivity.java`
- **Chức năng**: 
  - Hiển thị QR code thanh toán ngân hàng
  - Đếm ngược thời gian thanh toán (15 giờ)
  - Hiển thị thông tin thanh toán (tên ngân hàng, số tài khoản, chủ tài khoản)
  - Cho phép sao chép thông tin thanh toán
  - Lưu mã QR vào thư viện ảnh
  - Xác nhận thanh toán và kích hoạt VIP

### 2. Layout Files

#### activity_vip_account.xml
- **Đường dẫn**: `app/src/main/res/layout/activity_vip_account.xml`
- **Thiết kế**: 
  - Header với nút back
  - VIP badge với icon crown
  - 3 card gói VIP với danh sách tính năng
  - Badge "PHỔ BIẾN NHẤT" và "GIÁ TRỊ TỐT NHẤT"

#### activity_vip_payment.xml
- **Đường dẫn**: `app/src/main/res/layout/activity_vip_payment.xml`
- **Thiết kế**:
  - Header với tiêu đề
  - Bộ đếm ngược thời gian (giờ:phút:giây)
  - Mã QR thanh toán trong card đẹp
  - Thông tin thanh toán với nút Copy
  - Hướng dẫn thanh toán 3 bước
  - Nút "I Have Paid"
  - Link hỗ trợ

### 3. Drawable Resources

Đã tạo các drawable sau:
- `vip_badge_bg.xml` - Gradient vàng cho VIP badge
- `ic_vip_crown.xml` - Icon vương miện VIP
- `timer_box_bg.xml` - Background cho timer boxes
- `payment_icon_bg.xml` - Background xanh cho icon payment
- `bank_icon_bg.xml` - Background tím cho icon bank
- `user_icon_bg.xml` - Background xanh lá cho icon user
- `number_icon_bg.xml` - Background cam cho icon số tài khoản
- `step_number_bg.xml` - Background tròn xanh cho số bước

### 4. Utility Classes

#### VipManager.java
- **Đường dẫn**: `app/src/main/java/com/example/newsai/util/VipManager.java`
- **Các phương thức**:
  - `isVipActive(Context)` - Kiểm tra VIP còn hiệu lực
  - `getVipExpiryDate(Context)` - Lấy ngày hết hạn VIP
  - `getVipPackage(Context)` - Lấy tên gói VIP
  - `getVipAmount(Context)` - Lấy số tiền đã thanh toán
  - `getDaysRemaining(Context)` - Lấy số ngày còn lại
  - `setVipStatus(Context, ...)` - Đặt trạng thái VIP
  - `clearVipStatus(Context)` - Xóa trạng thái VIP
  - `canAccessVipFeatures(Context)` - Kiểm tra quyền truy cập tính năng VIP

### 5. ProfileActivity Updates

Đã cập nhật ProfileActivity để:
- Thêm nút "Nâng cấp VIP" với màu vàng và icon crown
- Hiển thị card trạng thái VIP khi user có VIP active
- Kiểm tra và cập nhật trạng thái VIP khi vào màn hình

### 6. Dependencies

Đã thêm vào `build.gradle.kts`:
```kotlin
implementation("com.google.zxing:core:3.5.3")
```

### 7. Permissions

Đã thêm vào `AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="28" />
```

## Cách sử dụng

### 1. Nâng cấp VIP
1. Mở ProfileActivity
2. Nhấn nút "Nâng cấp VIP"
3. Chọn một trong 3 gói VIP
4. Màn hình thanh toán sẽ hiển thị

### 2. Thanh toán
1. Quét mã QR bằng app ngân hàng
2. Hoặc sao chép thông tin thanh toán thủ công:
   - Ngân hàng: Global Trust Bank
   - Chủ tài khoản: VIP Services Inc.
   - Số tài khoản: 1234567890123
3. Thực hiện thanh toán
4. Nhấn "I Have Paid" để xác nhận
5. Tài khoản VIP sẽ được kích hoạt

### 3. Kiểm tra trạng thái VIP trong code

```java
import com.example.newsai.util.VipManager;

// Kiểm tra VIP active
if (VipManager.isVipActive(context)) {
    // Cho phép truy cập tính năng VIP
}

// Lấy thông tin VIP
String expiryDate = VipManager.getVipExpiryDate(context);
String packageName = VipManager.getVipPackage(context);
long daysRemaining = VipManager.getDaysRemaining(context);
```

## Tính năng VIP có thể implement

Các tính năng có thể giới hạn cho user VIP:
1. **Không giới hạn xác minh tin tức** - Trong VerifyNewsActivity
2. **AI Chatbot thông minh** - Trong ChatbotActivity
3. **Không có quảng cáo** - Ẩn ads trong toàn bộ app
4. **Ưu tiên hỗ trợ khách hàng**
5. **Badge VIP độc quyền** - Hiển thị badge trong profile
6. **Truy cập tính năng Beta**
7. **Cập nhật tính năng mới sớm nhất**

## Ví dụ giới hạn tính năng

### Trong VerifyNewsActivity:
```java
if (!VipManager.isVipActive(this)) {
    // Giới hạn số lần verify cho free user
    int verifyCount = prefs.getInt("verify_count", 0);
    if (verifyCount >= 5) {
        Toast.makeText(this, "Vui lòng nâng cấp VIP để sử dụng không giới hạn", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(this, VipAccountActivity.class);
        startActivity(intent);
        return;
    }
}
```

### Trong ChatbotActivity:
```java
if (!VipManager.isVipActive(this)) {
    Toast.makeText(this, "Tính năng này chỉ dành cho VIP", Toast.LENGTH_SHORT).show();
    Intent intent = new Intent(this, VipAccountActivity.class);
    startActivity(intent);
    return;
}
```

## Lưu ý

1. **Thanh toán thực tế**: Hiện tại app sử dụng QR code tĩnh. Để production, cần tích hợp với payment gateway thực (VNPay, Momo, ZaloPay, etc.)

2. **Xác thực thanh toán**: Cần có backend để xác thực thanh toán thực sự thay vì để user tự xác nhận

3. **Bảo mật**: Trạng thái VIP hiện lưu trong SharedPreferences local. Production nên lưu trên server và xác thực mỗi lần mở app

4. **QR Code động**: Nên tạo QR code riêng cho mỗi giao dịch với mã transaction ID

5. **Notifications**: Có thể thêm thông báo khi VIP sắp hết hạn

## Màu sắc sử dụng

- **VIP Gold**: #FFD700
- **Primary Blue**: #4F6EF7
- **Background**: #F6F4EE
- **Text Dark**: #1D1D1F
- **Text Gray**: #5A5A5A
- **Success Green**: #16A34A

## Testing

Để test tính năng:
1. Chạy app và đăng nhập
2. Vào Profile → Nâng cấp VIP
3. Chọn gói VIP bất kỳ
4. Xem QR code và thông tin thanh toán
5. Nhấn "I Have Paid"
6. Quay lại Profile để xem VIP status card

## Support

Nếu có vấn đề, kiểm tra:
- Logcat cho error messages
- SharedPreferences có key `is_vip`, `vip_expiry`, etc.
- Layout rendering đúng trên các màn hình khác nhau

