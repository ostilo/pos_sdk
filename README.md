# KongaPay POS SDK

Android SDK for card purchase on KongaPay POS terminals.

**Anfu** is the default terminal type. **Trendit** is also supported.

This repository (`pos_sdk`) is a working demo app that shows how external partners integrate the SDK.

The SDK manages terminal keys, encryption, and host routing internally. Partners only supply:

1. SDK initialization
2. Merchant / terminal identity (after login)
3. A payment request (amount + order reference)

---

## Requirements

| Item | Value |
|------|--------|
| Min SDK | 24+ (demo), 29 recommended for production POS |
| Language | Kotlin or Java |
| Devices | Anfu (default), Trendit |
| Build tools | Android Studio / AGP compatible with compileSdk 34 |

---

## Package contents

Place these files in your app `libs/` folder (as shown in this demo):

| File | Purpose |
|------|---------|
| `pos_utils-*.aar` | KongaPay POS SDK |
| `AFSDKInterface_*.aar` | Anfu device interface (required for Anfu terminals) |

This demo already includes them under:

```text
app/libs/pos_utils-release.aar
app/libs/AFSDKInterface_202502211810_V0.0.236_236.aar
```

---

## Installation

### 1. Add AARs

Copy the partner AARs into `app/libs/`.

### 2. Gradle dependencies

```kotlin
dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar", "*.jar"))))

    implementation(files("libs/pos_utils-test.aar")) // or pos_utils-release.aar
    implementation(files("libs/AFSDKInterface_202502211810_V0.0.236_236.aar"))

    implementation("com.google.code.gson:gson:2.9.0")
    implementation("com.airbnb.android:lottie:6.0.0")
    implementation("com.github.getActivity:TitleBar:9.2")
    implementation("com.github.getActivity:ToastUtils:9.5")

    implementation("com.squareup.retrofit2:retrofit:2.4.0") {
        exclude(group = "com.squareup.okhttp3")
    }
    implementation("com.squareup.retrofit2:converter-gson:2.4.0") {
        exclude(group = "com.squareup.okhttp3")
    }
    implementation("com.squareup.okhttp3:okhttp:3.14.9")
    implementation("com.squareup.okhttp3:logging-interceptor:3.14.9")
    implementation("com.squareup.retrofit2:converter-scalars:2.0.0")
}
```

Also ensure JitPack is available (needed by TitleBar / ToastUtils):

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

### 3. AndroidManifest

Use the SDK `ApplicationClass` (required):

```xml
<application
    android:name="com.konga.pos_utils.sdk.ApplicationClass"
    ... >
    <activity android:name=".MainActivity" android:exported="true">
        <intent-filter>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent-filter>
    </activity>
</application>
```

> Terminal keys and encryption material are baked into the AAR by KongaPay.  
> You do **not** configure them in your app.

---

## Quick start (3 steps)

### 1. Initialize (once)

```kotlin
import com.konga.pos_utils.sdk.KongaPos
import com.konga.pos_utils.sdk.KongaPosConfig
import com.konga.pos_utils.sdk.PosType

KongaPos.init(
    context = applicationContext,
    config = KongaPosConfig(
        posType = PosType.ANFU,          // or PosType.Trendit
        isTestEnvironment = false,
        appReleaseVersion = "1.0.0",
        isMerchantApp = false
    )
)
```

### 2. Set merchant profile (after your login)

```kotlin
import com.konga.pos_utils.sdk.MerchantProfile

KongaPos.setMerchant(
    MerchantProfile(
        serialNumber = "1492430209",
        terminalId = "2101MN91",
        merchantId = "31446",
        walletId = "2001684006",
        phoneNumber = "2348044433322",
        parentMerchantId = "31446",
        parentMerchantName = "Barkey",
        parentAccountNumber = "10000312123",
        parentEmail = "merchant@example.com",
        parentAddress = "Lagos",
        parentPhone = "2348160764301",
        kycProfileId = "19",
        merchantType = "company",
        transactionType = "00"
    )
)
```

### 3. Start a card payment

```kotlin
import com.konga.pos_utils.sdk.PaymentRequest

val launcher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
) { result ->
    handlePaymentResult(result.resultCode, result.data)
}

KongaPos.startCardPayment(
    context = this,
    request = PaymentRequest(
        amount = "50",
        orderReference = "ORD-1234",
        paymentReference = "",
        phoneNumber = null
    )
) { intent ->
    launcher.launch(intent)
}
```

See `app/src/main/java/com/kpay/kpayterminaldemosdk/MainActivity.kt` for a full working example.

---

## One-shot API

If you prefer not to store merchant state with `setMerchant()`:

```kotlin
KongaPos.startCardPayment(
    context = this,
    config = KongaPosConfig(posType = PosType.ANFU),
    merchant = merchantProfile,
    request = paymentRequest
) { intent ->
    launcher.launch(intent)
}
```

---

## Request reference

### KongaPosConfig

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `posType` | `PosType` | No | `ANFU` | `ANFU` or `Trendit` |
| `isTestEnvironment` | `Boolean` | No | `false` | Test vs live flag |
| `appReleaseVersion` | `String` | No | `""` | Your app version |
| `isMerchantApp` | `Boolean` | No | `false` | Keep `false` for partner apps |

### MerchantProfile

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `serialNumber` | `String` | Yes | POS device serial number |
| `terminalId` | `String` | Yes | TID |
| `merchantId` | `String` | Yes | Merchant / agent profile id |
| `walletId` | `String` | Yes | Settlement / wallet account id |
| `phoneNumber` | `String` | Yes | Merchant phone (e.g. `2348...`) |
| `parentMerchantId` | `String` | Yes | Parent merchant id |
| `parentMerchantName` | `String` | Yes | Parent / business name |
| `parentAccountNumber` | `String` | Yes | Parent settlement account |
| `parentEmail` | `String` | Yes | Parent email |
| `parentAddress` | `String` | Yes | Parent address |
| `parentPhone` | `String` | Yes | Parent phone |
| `kycProfileId` | `String` | No | KYC profile id |
| `merchantType` | `String` | No | e.g. `company` |
| `transactionType` | `String` | No | Default `"00"` (purchase). `"31"` = balance check |
| `encryptionKey` | `String?` | No | Leave `null` — SDK uses built-in key |
| `encryptionIv` | `String?` | No | Leave `null` — SDK uses built-in IV |

### PaymentRequest

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `amount` | `String` | Yes | Amount as string, e.g. `"50"` |
| `orderReference` | `String` | Yes | Your unique order / invoice reference |
| `paymentReference` | `String` | No | Payment-request ref if applicable |
| `phoneNumber` | `String?` | No | Optional phone override for this txn |
| `location` | `Location?` | No | `Location(long, lat)` if available |

#### Example request

```json
{
  "amount": "50",
  "orderReference": "ORD-1234",
  "paymentReference": "PAY-5678",
  "phoneNumber": "2348044433322"
}
```

Partners never send terminal keys, encryption key/IV, NIBSS IP, or port.

---

## Response reference

Payment UI is launched via an `Intent`. Handle the result in your Activity Result callback.

### Result codes

| `resultCode` | Meaning |
|--------------|---------|
| `Activity.RESULT_OK` (`-1`) | Transaction payload returned in extra `"data"` |
| `101` | Card / EMV / processing error |
| `102` | Incomplete or invalid parameters |
| `1011` | User / flow error (message in extras) |

### Handling the result

```kotlin
import com.google.gson.Gson
import com.konga.pos_utils.kongapay.KongaTransactionResponse
import com.konga.pos_utils.utils.Constants

fun handlePaymentResult(resultCode: Int, data: android.content.Intent?) {
    when (resultCode) {
        Activity.RESULT_OK -> {
            val json = data?.getStringExtra("data") ?: return
            val response = Gson().fromJson(json, KongaTransactionResponse::class.java)

            if (response.responseCode == "00") {
                // Approved
            } else {
                // Declined / failed
            }
        }
        101 -> {
            val code = data?.getIntExtra(Constants.PRINTER_DATA_TRANSACTION_STATUS_CODE, 101)
            val desc = data?.getStringExtra(Constants.PRINTER_DATA_TRXN_STATUS_KEY)
            // Card/EMV error
        }
        102 -> {
            // Incomplete parameters
        }
        1011 -> {
            val msg = data?.getStringExtra(Constants.PRINTER_DATA_TRXN_STATUS_MSG)
            // Cancelled / flow error
        }
    }
}
```

### KongaTransactionResponse fields

| Field | Type | Description |
|-------|------|-------------|
| `responseCode` | `String` | `"00"` = approved; any other = declined/failed |
| `responseDescription` | `String` | Human-readable status |
| `responseMessage` | `String` | Alternate message |
| `resDescription` | `String` | Alternate description |
| `amount` | `String` | Transaction amount |
| `terminalId` | `String` | TID used |
| `transRef` | `String` | Transaction reference |
| `stan` | `String` | STAN |
| `authcode` | `String` | Auth code (if approved) |
| `pan` | `String` | Masked PAN |
| `cardType` | `String` | Card scheme / type |
| `cardHolderName` | `String` | Cardholder name |
| `expireDate` | `String` | Card expiry |
| `aid` | `String` | Application ID |
| `accountType` | `String` | Selected account type |
| `transactionType` | `String` | Transaction type |
| `transDate` | `String` | Date |
| `transTime` | `String` | Time |
| `customerName` | `String` | Customer name |
| `customerPhoneNo` | `String` | Customer phone |
| `orderReferenceNumber` | `String` | Your order reference |
| `paymentRequestRef` | `String` | Payment request reference |
| `additionalAmounts` | `List` | Extra amount breakdown (if any) |

#### Example approved response (`"data"` JSON)

```json
{
  "responseCode": "00",
  "responseDescription": "Approved",
  "amount": "50",
  "terminalId": "2101MN91",
  "transRef": "000012345678",
  "stan": "123456",
  "authcode": "A1B2C3",
  "pan": "506099******1234",
  "cardType": "Verve",
  "cardHolderName": "JOHN DOE",
  "orderReferenceNumber": "ORD-1234",
  "paymentRequestRef": "PAY-5678",
  "transDate": "240724",
  "transTime": "133015"
}
```

#### Example declined response (`"data"` JSON)

```json
{
  "responseCode": "51",
  "responseDescription": "Insufficient funds",
  "amount": "50",
  "terminalId": "2101MN91",
  "pan": "506099******1234",
  "orderReferenceNumber": "ORD-1234"
}
```

### Error Intent extras (non-`RESULT_OK`)

| Extra key | Constant | Used when |
|-----------|----------|-----------|
| `printer_status_code` | `Constants.PRINTER_DATA_TRANSACTION_STATUS_CODE` | Result `101` / `102` |
| `trans_status_code` | `Constants.PRINTER_DATA_TRXN_STATUS_KEY` | Status description |
| `transaction_res` | `Constants.PRINTER_DATA_TRXN_STATUS_MSG` | Result `1011` message |

---

## Printing (optional)

```kotlin
val device = KongaPos.createDevice(context) { /* unused for print-only */ }
device.printTransaction(kongaTransactionResponse, logoBitmapOrNull)
```

---

## Supported terminals

| PosType | Notes |
|---------|--------|
| `PosType.ANFU` | Default — use unless told otherwise |
| `PosType.Trendit` | Supported — set explicitly in `KongaPosConfig` |

---

## Integration flow

```text
Partner App                    KongaPay POS SDK                 Terminal / Host
───────────                    ────────────────                 ───────────────
init(config)
setMerchant(profile)
startCardPayment(request)
        ────── Intent ──────►  Card UI (PIN / EMV)
                                    │
                                    ├─ uses built-in keys
                                    ├─ talks to host
                                    └─ returns Intent
handle RESULT_OK / 101 / 102
parse "data" → KongaTransactionResponse
```

---

## Run this demo

1. Open the project in Android Studio.
2. Connect an Anfu POS device (or matching emulator/device setup).
3. Update `MerchantProfile` values in `MainActivity` with credentials from KongaPay.
4. Run the app and tap **Test KPay Transaction**.

```bash
./gradlew :app:assembleDebug
```

---

## Go-live checklist

- [ ] Correct AAR for your environment (keys are baked in by KongaPay)
- [ ] Anfu interface AAR included for Anfu devices
- [ ] `ApplicationClass` set in `AndroidManifest.xml`
- [ ] `KongaPos.init` called before payments
- [ ] `MerchantProfile` filled from your authenticated session
- [ ] `orderReference` unique per attempt
- [ ] Activity Result handler covers `RESULT_OK`, `101`, `102`, `1011`
- [ ] Treat only `responseCode == "00"` as approved
- [ ] Device type matches `PosType` (`ANFU` / `Trendit`)

---

## Do not

- Do not hardcode or request terminal clear keys (`TerminalKey`)
- Do not pass NIBSS IP / port
- Do not reconstruct internal `TransactionInfo` unless KongaPay support asks you to
- Do not ship debug/test AARs to production devices

---

## Support

Contact KongaPay POS support with:

- Partner name
- Device serial / TID
- App version
- `orderReference` and approximate time of the failed transaction
