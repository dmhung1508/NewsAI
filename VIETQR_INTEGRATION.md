# VietQR Integration - Tích hợp VietQR cho thanh toán VIP

## Tổng quan

Dự án đã tích hợp VietQR API để tạo mã QR thanh toán ngân hàng tự động cho tính năng VIP upgrade.

## Thông tin ngân hàng

### Cấu hình mặc định
```java
BANK_ID = "MB"                    // Military Bank (Ngân hàng Quân Đội)
ACCOUNT_NUMBER = "669699669"       // Số tài khoản
ACCOUNT_HOLDER = "DINH Manh Hung"  // Chủ tài khoản
```

## VietQR API

### Base URL
```
https://img.vietqr.io/image
```

### Format URL
```
https://img.vietqr.io/image/{BANK_ID}-{ACCOUNT_NO}-{TEMPLATE}.jpg?addInfo={DESCRIPTION}&accountName={ACCOUNT_NAME}&amount={AMOUNT}
```

### Tham số

| Tham số | Mô tả | Ví dụ |
|---------|-------|-------|
| `BANK_ID` | Mã BIN/tên ngắn ngân hàng | `MB`, `970415`, `Vietinbank` |
| `ACCOUNT_NO` | Số tài khoản (max 19 ký tự) | `669699669` |
| `TEMPLATE` | Template QR code | `qronly`, `compact`, `compact2`, `print` |
| `addInfo` | Nội dung chuyển khoản (max 50 ký tự) | `VIP 1 month` |
| `accountName` | Tên chủ tài khoản | `DINH Manh Hung` |
| `amount` | Số tiền (max 13 chữ số) | `790000` |

## VietQRHelper Class

### Location
`app/src/main/java/com/example/newsai/util/VietQRHelper.java`

### Các phương thức chính

#### 1. Generate QR URL
```java
String qrUrl = VietQRHelper.generateQROnlyUrl(
    "MB",                    // Bank ID
    "669699669",            // Account number
    "DINH Manh Hung",       // Account holder
    790000,                 // Amount in VND
    "VIP 1 month"           // Description
);
```

#### 2. Convert USD to VND
```java
double usd = 9.99;
long vnd = VietQRHelper.convertUSDtoVND(usd);
// Result: 239,760 VND (rate: 1 USD = 24,000 VND)
```

#### 3. Parse amount from string
```java
double amount = VietQRHelper.parseAmountFromString("$9.99");
// Result: 9.99
```

#### 4. Validate parameters
```java
boolean validAccount = VietQRHelper.isValidAccountNumber("669699669");
boolean validAmount = VietQRHelper.isValidAmount(790000);
boolean validDesc = VietQRHelper.isValidDescription("VIP 1 month");
```

## Cách hoạt động

### 1. Trong VipPaymentActivity

```java
private void generateQrCode() {
    // 1. Parse USD amount and convert to VND
    double usdAmount = VietQRHelper.parseAmountFromString(amount);
    long vndAmount = VietQRHelper.convertUSDtoVND(usdAmount);
    
    // 2. Create transfer description
    String description = "VIP " + packageId.replace("_", " ");
    
    // 3. Generate VietQR URL
    String qrUrl = VietQRHelper.generateQROnlyUrl(
        BANK_ID,
        ACCOUNT_NUMBER,
        ACCOUNT_HOLDER,
        vndAmount,
        description
    );
    
    // 4. Load QR image from URL in background thread
    new Thread(() -> {
        try {
            URL url = new URL(qrUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.connect();
            InputStream input = connection.getInputStream();
            qrBitmap = BitmapFactory.decodeStream(input);
            
            // Update UI
            runOnUiThread(() -> imgQrCode.setImageBitmap(qrBitmap));
        } catch (Exception e) {
            // Handle error
        }
    }).start();
}
```

## Ví dụ QR URLs

### VIP 1 Month ($9.99 = 239,760 VND)
```
https://img.vietqr.io/image/MB-669699669-qronly.jpg?addInfo=VIP+1+month&accountName=DINH+Manh+Hung&amount=239760
```

### VIP 3 Months ($24.99 = 599,760 VND)
```
https://img.vietqr.io/image/MB-669699669-qronly.jpg?addInfo=VIP+3+months&accountName=DINH+Manh+Hung&amount=599760
```

### VIP 1 Year ($79.99 = 1,919,760 VND)
```
https://img.vietqr.io/image/MB-669699669-qronly.jpg?addInfo=VIP+1+year&accountName=DINH+Manh+Hung&amount=1919760
```

## QR Templates

### 1. qronly (QR Only)
- Chỉ hiển thị mã QR
- Không có thông tin tài khoản
- Phù hợp cho mobile app

### 2. compact (Compact)
- QR + thông tin tài khoản cơ bản
- Thiết kế nhỏ gọn

### 3. compact2 (Compact 2)
- Biến thể của compact
- Thiết kế khác

### 4. print (Print)
- Tối ưu cho in ấn
- Hiển thị đầy đủ thông tin

## Danh sách ngân hàng Việt Nam

### Các ngân hàng phổ biến

| Ngân hàng | Mã BIN | Code | Short Name |
|-----------|--------|------|------------|
| Vietcombank | 970436 | VCB | Vietcombank |
| Techcombank | 970407 | TCB | Techcombank |
| MB Bank | 970422 | MB | MBBank |
| Vietinbank | 970415 | ICB | Vietinbank |
| BIDV | 970418 | BIDV | BIDV |
| ACB | 970416 | ACB | ACB |
| Agribank | 970405 | VBAA | Agribank |
| VPBank | 970432 | VPB | VPBank |
| TPBank | 970423 | TPB | TPBank |
| Sacombank | 970403 | STB | Sacombank |

### API tra cứu danh sách ngân hàng
```
GET https://api.vietqr.io/v2/banks
```

## Giao diện Profile mới

### Thiết kế
- Clean, modern design theo Material Design 3
- Avatar tròn lớn ở đầu
- Thông tin user hiển thị rõ ràng
- VIP status card với gradient vàng
- Profile items với icon màu sắc
- Nút "Upgrade to VIP" nổi bật màu xanh

### Các section
1. **Header**: Back button + "My Profile"
2. **Avatar section**: Ảnh đại diện + Tên + Email
3. **VIP Status Card**: Hiển thị khi có VIP (với ngày hết hạn)
4. **Profile Items**:
   - Full Name (icon xanh lá)
   - Email (icon xanh dương)
   - Phone Number (icon tím)
5. **Action Buttons**:
   - Upgrade to VIP (xanh, nổi bật)
   - Cập nhật thông tin (outline)
   - Log Out (text button)

## Lưu ý quan trọng

### 1. Tỷ giá USD/VND
- Hiện tại sử dụng tỷ giá cố định: **1 USD = 24,000 VND**
- Production nên sử dụng API tỷ giá thời gian thực:
  - https://api.exchangerate-api.com/v4/latest/USD
  - https://openexchangerates.org/api/latest.json

### 2. Validation
- Số tài khoản: max 19 ký tự, chỉ chữ và số
- Số tiền: max 13 chữ số, phải dương
- Nội dung: max 50 ký tự, không ký tự đặc biệt

### 3. Error Handling
- Timeout kết nối: 10 giây
- Xử lý lỗi HTTP
- Hiển thị thông báo lỗi rõ ràng

### 4. Security
- Không lưu thông tin ngân hàng nhạy cảm
- Validate tất cả input
- Use HTTPS cho API calls

## Testing

### Test Cases

1. **Generate QR for VIP 1 Month**
   - Input: $9.99
   - Expected VND: 239,760
   - Expected QR: Contains correct amount

2. **Generate QR for VIP 3 Months**
   - Input: $24.99
   - Expected VND: 599,760
   - Expected QR: Contains "VIP 3 months"

3. **Generate QR for VIP 1 Year**
   - Input: $79.99
   - Expected VND: 1,919,760
   - Expected QR: Contains correct account info

4. **Save QR Code**
   - Generate QR
   - Click Save button
   - Check file in Pictures/NewsAI folder

5. **Copy Bank Info**
   - Click Copy buttons
   - Verify clipboard content

## Troubleshooting

### QR không hiển thị
- Kiểm tra kết nối internet
- Xem logcat cho error messages
- Verify VietQR URL format

### Số tiền không đúng
- Kiểm tra conversion rate
- Verify amount parsing logic

### QR timeout
- Tăng connection timeout
- Retry logic

## Future Improvements

1. **Real-time Exchange Rate**
   - Integrate với API tỷ giá
   - Cache exchange rate

2. **Multiple Payment Methods**
   - Momo
   - ZaloPay
   - VNPay

3. **Payment Verification**
   - Backend verification
   - Webhook from payment gateway

4. **Dynamic Bank Account**
   - Admin panel để thay đổi thông tin
   - Multiple bank accounts

5. **Transaction History**
   - Lưu lịch sử giao dịch
   - Receipt generation

## Resources

- [VietQR Documentation](https://www.vietqr.io/)
- [NAPAS Bank Codes](https://www.napas.com.vn/)
- [Material Design 3](https://m3.material.io/)

