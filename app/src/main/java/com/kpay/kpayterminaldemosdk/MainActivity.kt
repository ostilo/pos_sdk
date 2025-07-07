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

class MainActivity : AppCompatActivity() {
    private var posImpl: POSDeviceImpl? = null

    private var applClass : ApplicationClass? = null

    // Declare the launcher as a class property
    private val someActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            // Handle the result here
            if (result.resultCode == Activity.RESULT_OK) {
                // Handle the result here
                if (result.data != null && result.data?.hasExtra("data") == true) {
                    val response: KongaTransactionResponse? = Gson().fromJson(result.data?.getStringExtra("data"),
                        KongaTransactionResponse::class.java)
                    AlertDialog.Builder(this)
                        .setTitle(java.lang.String.valueOf(response?.responseCode))
                        .setMessage(response?.responseDescription)
                        .setPositiveButton(
                            "Okay"
                        ) { dialog, which -> dialog.dismiss() }
                        .show()

                }

            }
        }else{
            AlertDialog.Builder(this)
                .setTitle(java.lang.String.valueOf("Card Transactions"))
                .setMessage("Internal Server Error")
                .setPositiveButton(
                    "Okay"
                ) { dialog, which -> dialog.dismiss() }
                .show()
        }
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
        POSDeviceImpl.init(PosType.ANFU)
        initView()
    }

    private fun initView() {
        val transactionInfo = TransactionInfo(
            "1492430209",  // 1492430209 //1492018990 // S6805370006263
            "196.6.103.18",
            "501",
            "60",
            "31446",
            "1234",
            "31446",
            "2101MN91",  // 2ISWR874
            "2348044433322",
            "VoomNow Digital",
            "84ed228de9f566ec-AMS",
            "44e7152d9b160e0ec747b71687ff2e65be76e60a",
            false,
            "2001684006",
            "KongaTrendit",
            "",
            Location(3.356170f, 6.650580f),
            TerminalKey(
                "F1DC7310753DF8623EBA3731CBBA804A",
                "49FB2668DC6251949DC7BA10B6E6DA29",
                "89D3E364F73D2561A87F64D51525F27F"
            ),
            "31446",
            "Barkey",
            "10000312123",
            "browndon200@yahoo.com",
            "Nil",
            "2348160764301",
            true,
            "1.2.3",
            "19",
            "company"
        ) // Initialize as needed
        posImpl = POSDeviceImpl(this@MainActivity, PosType.ANFU, transactionInfo) { intent: Intent? ->
                // Handle the intent
              someActivityResultLauncher.launch(intent)
            }

        findViewById<View>(R.id.btnTest).setOnClickListener { v: View? ->
            posImpl?.cardTransaction(50.0)
        }
    }
}