# KongaPay POS SDK

Android SDK for card purchase on KongaPay POS terminals (**Anfu** default, **Trendit** supported).

The SDK handles terminal keys, encryption, and host routing internally.  
Partners only supply **merchant/terminal identity** (after login) and a **payment request**.

---

## Requirements

| Item | Value |
|------|--------|
| Min SDK | 29 |
| Language | Kotlin or Java |
| Devices | Anfu (default), Trendit |
| Dependency | KongaPay POS AAR (`pos_utils` release build) |

---

## Installation

1. Add the AAR (and any required vendor AARs KongaPay supplies) to your app.
2. In `app/build.gradle`:

```gradle
dependencies {
    implementation files('libs/pos_utils-release.aar')
    // plus vendor AARs / modules as provided in the partner package
}
```


Quick start (3 steps)
1. Initialize (once, e.g. in Application or after device boot)

import com.konga.pos_utils.sdk.KongaPos
import com.konga.pos_utils.sdk.KongaPosConfig
import com.konga.pos_utils.sdk.PosType
// Anfu is the default — omit posType unless you need Trendit
KongaPos.init(
    context = applicationContext,
    config = KongaPosConfig(
        posType = PosType.ANFU,          // or PosType.Trendit
        isTestEnvironment = false,
        appReleaseVersion = BuildConfig.VERSION_NAME,
        isMerchantApp = false
    )
)




2. Set merchant profile (after your login / account resolve)

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
        kycProfileId = "19",             // optional
        merchantType = "company",        // optional
        transactionType = "00"           // "00" = purchase (default)
    )
)




3. Start a card payment

import com.konga.pos_utils.sdk.PaymentRequest
val launcher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
) { result ->
    handlePaymentResult(result)
}
KongaPos.startCardPayment(
    context = this,
    request = PaymentRequest(
        amount = "60",                    // major units as string (e.g. Naira)
        orderReference = "ORD-1234",
        paymentReference = "",            // optional
        phoneNumber = null                // optional override
    )
) { intent ->
    launcher.launch(intent)
}


One-shot API (no stored merchant state)
Use this if you prefer not to call setMerchant() globally:


KongaPos.startCardPayment(
    context = this,
    config = KongaPosConfig(posType = PosType.ANFU),
    merchant = merchantProfile,
    request = paymentRequest
) { intent ->
    launcher.launch(intent)
}



Success / decline payload (RESULT_OK)
Read JSON from Intent extra "data" and parse to KongaTransactionResponse:


import com.google.gson.Gson
import com.konga.pos_utils.kongapay.KongaTransactionResponse
fun handlePaymentResult(result: ActivityResult) {
    when (result.resultCode) {
        Activity.RESULT_OK -> {
            val json = result.data?.getStringExtra("data") ?: return
            val response = Gson().fromJson(json, KongaTransactionResponse::class.java)
            if (response.responseCode == "00") {
                // APPROVED
                showApproved(response)
            } else {
                // DECLINED / FAILED (still may include card details)
                showDeclined(response.responseDescription ?: "Transaction declined")
            }
        }
        101 -> {
            val code = result.data?.getIntExtra("printer_status_code", 101)
            val desc = result.data?.getStringExtra("trans_status_code")
            showError(desc ?: "Card error ($code)")
        }
        102 -> showError("Incomplete transaction parameters")
        1011 -> {
            val msg = result.data?.getStringExtra("transaction_res")
            showError(msg ?: "Transaction cancelled")
        }
        else -> showError("Transaction cancelled or unknown result")
    }
}
