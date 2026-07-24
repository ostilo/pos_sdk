package com.kpay.kpayterminaldemosdk

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.gson.Gson
import com.konga.pos_utils.kongapay.KongaTransactionResponse
import com.konga.pos_utils.sdk.ApplicationClass
import com.konga.pos_utils.sdk.Location
import com.konga.pos_utils.sdk.POSDeviceImpl
import com.konga.pos_utils.sdk.PosType
import com.konga.pos_utils.sdk.TerminalKey
import com.konga.pos_utils.sdk.TransactionInfo
import android.widget.Toast
import com.konga.pos_utils.sdk.KongaPos
import com.konga.pos_utils.sdk.KongaPosConfig
import com.konga.pos_utils.sdk.MerchantProfile
import com.konga.pos_utils.sdk.PaymentRequest
import com.konga.pos_utils.utils.Constants

class MainActivity : AppCompatActivity() {
    private val paymentResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        handlePaymentResult(result.resultCode, result.data)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // 1) Init once — Anfu is default
        KongaPos.init(
            context = applicationContext,
            config = KongaPosConfig(
                posType = PosType.ANFU,          // or PosType.Trendit
                isTestEnvironment = false,
                appReleaseVersion = "1.0.0",
                isMerchantApp = false
            )
        )
        // 2) Merchant/terminal identity after your login
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
                parentEmail = "browndon200@yahoo.com",
                parentAddress = "Nil",
                parentPhone = "2348160764301",
                kycProfileId = "19",
                merchantType = "company",
                transactionType = "00"
            )
        )
        findViewById<View>(R.id.btnTest).setOnClickListener {
            startCardPayment()
        }
    }
    private fun startCardPayment() {
        // 3) Start payment — keys / IP / port are NOT passed by the partner
        KongaPos.startCardPayment(
            context = this,
            request = PaymentRequest(
                amount = "50",
                orderReference = "ORD-${System.currentTimeMillis()}",
                paymentReference = "",
                phoneNumber = null
            )
        ) { intent ->
            paymentResultLauncher.launch(intent)
        }
    }
    private fun handlePaymentResult(resultCode: Int, data: android.content.Intent?) {
        when (resultCode) {
            Activity.RESULT_OK -> {
                if (data?.hasExtra("data") == true) {
                    val response = Gson().fromJson(
                        data.getStringExtra("data"),
                        KongaTransactionResponse::class.java
                    )
                    val title = response?.responseCode ?: "Unknown"
                    val message = response?.responseDescription
                        ?: response?.responseMessage
                        ?: "No description"
                    AlertDialog.Builder(this)
                        .setTitle(title)
                        .setMessage(message)
                        .setPositiveButton("Okay") { dialog, _ -> dialog.dismiss() }
                        .show()
                } else {
                    showError("Declined", "No response payload")
                }
            }
            101 -> {
                val code = data?.getIntExtra(Constants.PRINTER_DATA_TRANSACTION_STATUS_CODE, 101)
                val desc = data?.getStringExtra(Constants.PRINTER_DATA_TRXN_STATUS_KEY)
                showError("Card Error", desc ?: "Card/EMV error ($code)")
            }
            102 -> {
                showError("Invalid Parameters", "Incomplete transaction parameters")
            }
            1011 -> {
                val msg = data?.getStringExtra(Constants.PRINTER_DATA_TRXN_STATUS_MSG)
                showError("Transaction Error", msg ?: "Transaction cancelled")
            }
            else -> {
                showError("Cancelled", "Transaction cancelled or unknown result ($resultCode)")
            }
        }
    }
    private fun showError(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Okay") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}