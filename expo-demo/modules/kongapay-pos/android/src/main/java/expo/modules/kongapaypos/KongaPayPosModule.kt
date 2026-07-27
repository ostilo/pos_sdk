package expo.modules.kongapaypos

import android.app.Activity
import android.content.Intent
import com.google.gson.Gson
import com.konga.pos_utils.kongapay.KongaTransactionResponse
import com.konga.pos_utils.sdk.KongaPos
import com.konga.pos_utils.sdk.KongaPosConfig
import com.konga.pos_utils.sdk.Location
import com.konga.pos_utils.sdk.MerchantProfile
import com.konga.pos_utils.sdk.PaymentRequest
import com.konga.pos_utils.sdk.PosType
import com.konga.pos_utils.utils.Constants
import expo.modules.kotlin.Promise
import expo.modules.kotlin.exception.Exceptions
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import expo.modules.kotlin.records.Field
import expo.modules.kotlin.records.Record

class KongaPosConfigRecord : Record {
  @Field var posType: String = "ANFU"
  @Field var isTestEnvironment: Boolean = false
  @Field var appReleaseVersion: String = "1.0.0"
  @Field var isMerchantApp: Boolean = false
}

class MerchantProfileRecord : Record {
  @Field var serialNumber: String = ""
  @Field var terminalId: String = ""
  @Field var merchantId: String = ""
  @Field var walletId: String = ""
  @Field var phoneNumber: String = ""
  @Field var parentMerchantId: String = ""
  @Field var parentMerchantName: String = ""
  @Field var parentAccountNumber: String = ""
  @Field var parentEmail: String = ""
  @Field var parentAddress: String = ""
  @Field var parentPhone: String = ""
  @Field var kycProfileId: String = ""
  @Field var merchantType: String = ""
  @Field var transactionType: String = "00"
}

class PaymentRequestRecord : Record {
  @Field var amount: String = ""
  @Field var orderReference: String = ""
  @Field var paymentReference: String = ""
  @Field var phoneNumber: String? = null
  @Field var longitude: Double? = null
  @Field var latitude: Double? = null
}

class KongaPayPosModule : Module() {
  private var pendingPromise: Promise? = null
  private val gson = Gson()

  override fun definition() = ModuleDefinition {
    Name("KongaPayPos")

    Function("init") { config: KongaPosConfigRecord ->
      val context = appContext.reactContext ?: throw Exceptions.ReactContextLost()
      KongaPos.init(
        context.applicationContext,
        KongaPosConfig(
          posType = parsePosType(config.posType),
          isTestEnvironment = config.isTestEnvironment,
          appReleaseVersion = config.appReleaseVersion,
          isMerchantApp = config.isMerchantApp
        )
      )
    }

    Function("setMerchant") { merchant: MerchantProfileRecord ->
      KongaPos.setMerchant(toMerchantProfile(merchant))
    }

    AsyncFunction("startCardPayment") { request: PaymentRequestRecord, promise: Promise ->
      val activity = appContext.currentActivity
        ?: run {
          promise.reject("E_NO_ACTIVITY", "No current Activity available to launch payment UI", null)
          return@AsyncFunction
        }

      if (pendingPromise != null) {
        promise.reject("E_PAYMENT_IN_PROGRESS", "A payment is already in progress", null)
        return@AsyncFunction
      }

      pendingPromise = promise

      KongaPos.startCardPayment(
        activity,
        toPaymentRequest(request)
      ) { intent ->
        activity.startActivityForResult(intent, PAYMENT_REQUEST_CODE)
      }
    }

    AsyncFunction("startCardPaymentOneShot") { params: Map<String, Any?>, promise: Promise ->
      val activity = appContext.currentActivity
        ?: run {
          promise.reject("E_NO_ACTIVITY", "No current Activity available to launch payment UI", null)
          return@AsyncFunction
        }

      if (pendingPromise != null) {
        promise.reject("E_PAYMENT_IN_PROGRESS", "A payment is already in progress", null)
        return@AsyncFunction
      }

      @Suppress("UNCHECKED_CAST")
      val configMap = params["config"] as? Map<String, Any?> ?: emptyMap()
      @Suppress("UNCHECKED_CAST")
      val merchantMap = params["merchant"] as? Map<String, Any?> ?: emptyMap()
      @Suppress("UNCHECKED_CAST")
      val requestMap = params["request"] as? Map<String, Any?> ?: emptyMap()

      val config = KongaPosConfig(
        posType = parsePosType(configMap["posType"] as? String ?: "ANFU"),
        isTestEnvironment = configMap["isTestEnvironment"] as? Boolean ?: false,
        appReleaseVersion = configMap["appReleaseVersion"] as? String ?: "1.0.0",
        isMerchantApp = configMap["isMerchantApp"] as? Boolean ?: false
      )
      val merchant = MerchantProfile(
        serialNumber = merchantMap["serialNumber"] as? String ?: "",
        terminalId = merchantMap["terminalId"] as? String ?: "",
        merchantId = merchantMap["merchantId"] as? String ?: "",
        walletId = merchantMap["walletId"] as? String ?: "",
        phoneNumber = merchantMap["phoneNumber"] as? String ?: "",
        parentMerchantId = merchantMap["parentMerchantId"] as? String ?: "",
        parentMerchantName = merchantMap["parentMerchantName"] as? String ?: "",
        parentAccountNumber = merchantMap["parentAccountNumber"] as? String ?: "",
        parentEmail = merchantMap["parentEmail"] as? String ?: "",
        parentAddress = merchantMap["parentAddress"] as? String ?: "",
        parentPhone = merchantMap["parentPhone"] as? String ?: "",
        kycProfileId = merchantMap["kycProfileId"] as? String ?: "",
        merchantType = merchantMap["merchantType"] as? String ?: "",
        transactionType = merchantMap["transactionType"] as? String ?: "00",
        encryptionKey = null,
        encryptionIv = null
      )
      val location = locationFrom(requestMap["longitude"] as? Double, requestMap["latitude"] as? Double)
      val request = PaymentRequest(
        amount = requestMap["amount"] as? String ?: "",
        orderReference = requestMap["orderReference"] as? String ?: "",
        paymentReference = requestMap["paymentReference"] as? String ?: "",
        phoneNumber = requestMap["phoneNumber"] as? String,
        location = location
      )

      pendingPromise = promise
      KongaPos.startCardPayment(activity, config, merchant, request) { intent ->
        activity.startActivityForResult(intent, PAYMENT_REQUEST_CODE)
      }
    }

    OnActivityResult { _, payload ->
      if (payload.requestCode != PAYMENT_REQUEST_CODE) return@OnActivityResult
      val promise = pendingPromise ?: return@OnActivityResult
      pendingPromise = null
      promise.resolve(buildPaymentResult(payload.resultCode, payload.data))
    }
  }

  private fun buildPaymentResult(resultCode: Int, data: Intent?): Map<String, Any?> {
    return when (resultCode) {
      Activity.RESULT_OK -> {
        val json = data?.getStringExtra("data")
        if (json.isNullOrBlank()) {
          mapOf(
            "ok" to false,
            "resultCode" to resultCode,
            "errorCode" to "NO_PAYLOAD",
            "message" to "No response payload"
          )
        } else {
          val response = gson.fromJson(json, KongaTransactionResponse::class.java)
          mapOf(
            "ok" to true,
            "resultCode" to resultCode,
            "approved" to (response.responseCode == "00"),
            "rawJson" to json,
            "response" to mapOf(
              "responseCode" to response.responseCode,
              "responseDescription" to response.responseDescription,
              "responseMessage" to response.responseMessage,
              "resDescription" to response.resDescription,
              "amount" to response.amount,
              "terminalId" to response.terminalId,
              "transRef" to response.transRef,
              "stan" to response.stan,
              "authcode" to response.authcode,
              "pan" to response.pan,
              "cardType" to response.cardType,
              "cardHolderName" to response.cardHolderName,
              "expireDate" to response.expireDate,
              "aid" to response.aid,
              "accountType" to response.accountType,
              "transactionType" to response.transactionType,
              "transDate" to response.transDate,
              "transTime" to response.transTime,
              "customerName" to response.customerName,
              "customerPhoneNo" to response.customerPhoneNo,
              "orderReferenceNumber" to response.orderReferenceNumber,
              "paymentRequestRef" to response.paymentRequestRef
            )
          )
        }
      }
      101 -> mapOf(
        "ok" to false,
        "resultCode" to resultCode,
        "errorCode" to "CARD_EMV_ERROR",
        "statusCode" to (data?.getIntExtra(Constants.PRINTER_DATA_TRANSACTION_STATUS_CODE, 101) ?: 101),
        "message" to (data?.getStringExtra(Constants.PRINTER_DATA_TRXN_STATUS_KEY) ?: "Card/EMV error")
      )
      102 -> mapOf(
        "ok" to false,
        "resultCode" to resultCode,
        "errorCode" to "INVALID_PARAMETERS",
        "message" to "Incomplete transaction parameters"
      )
      1011 -> mapOf(
        "ok" to false,
        "resultCode" to resultCode,
        "errorCode" to "FLOW_ERROR",
        "message" to (data?.getStringExtra(Constants.PRINTER_DATA_TRXN_STATUS_MSG) ?: "Transaction cancelled")
      )
      else -> mapOf(
        "ok" to false,
        "resultCode" to resultCode,
        "errorCode" to "CANCELLED_OR_UNKNOWN",
        "message" to "Transaction cancelled or unknown result ($resultCode)"
      )
    }
  }

  private fun parsePosType(value: String): PosType {
    return when (value.uppercase()) {
      "TRENDIT" -> PosType.Trendit
      else -> PosType.ANFU
    }
  }

  private fun toMerchantProfile(merchant: MerchantProfileRecord): MerchantProfile {
    return MerchantProfile(
      serialNumber = merchant.serialNumber,
      terminalId = merchant.terminalId,
      merchantId = merchant.merchantId,
      walletId = merchant.walletId,
      phoneNumber = merchant.phoneNumber,
      parentMerchantId = merchant.parentMerchantId,
      parentMerchantName = merchant.parentMerchantName,
      parentAccountNumber = merchant.parentAccountNumber,
      parentEmail = merchant.parentEmail,
      parentAddress = merchant.parentAddress,
      parentPhone = merchant.parentPhone,
      kycProfileId = merchant.kycProfileId,
      merchantType = merchant.merchantType,
      transactionType = merchant.transactionType,
      encryptionKey = null,
      encryptionIv = null
    )
  }

  private fun toPaymentRequest(request: PaymentRequestRecord): PaymentRequest {
    return PaymentRequest(
      amount = request.amount,
      orderReference = request.orderReference,
      paymentReference = request.paymentReference,
      phoneNumber = request.phoneNumber,
      location = locationFrom(request.longitude, request.latitude)
    )
  }

  private fun locationFrom(longitude: Double?, latitude: Double?): Location? {
    if (longitude == null || latitude == null) return null
    return Location(longitude.toFloat(), latitude.toFloat())
  }

  companion object {
    private const val PAYMENT_REQUEST_CODE = 0x4B50 // 'KP'
  }
}
