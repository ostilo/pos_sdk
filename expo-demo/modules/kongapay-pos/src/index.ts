import { requireNativeModule, Platform } from 'expo-modules-core';

export type PosType = 'ANFU' | 'Trendit';

export type KongaPosConfig = {
  posType?: PosType;
  isTestEnvironment?: boolean;
  appReleaseVersion?: string;
  isMerchantApp?: boolean;
};

export type MerchantProfile = {
  serialNumber: string;
  terminalId: string;
  merchantId: string;
  walletId: string;
  phoneNumber: string;
  parentMerchantId: string;
  parentMerchantName: string;
  parentAccountNumber: string;
  parentEmail: string;
  parentAddress: string;
  parentPhone: string;
  kycProfileId?: string;
  merchantType?: string;
  /** `"00"` purchase (default), `"31"` balance check */
  transactionType?: string;
};

export type PaymentRequest = {
  amount: string;
  orderReference: string;
  paymentReference?: string;
  phoneNumber?: string | null;
  longitude?: number | null;
  latitude?: number | null;
};

export type KongaTransactionResponse = {
  responseCode?: string;
  responseDescription?: string;
  responseMessage?: string;
  resDescription?: string;
  amount?: string;
  terminalId?: string;
  transRef?: string;
  stan?: string;
  authcode?: string;
  pan?: string;
  cardType?: string;
  cardHolderName?: string;
  expireDate?: string;
  aid?: string;
  accountType?: string;
  transactionType?: string;
  transDate?: string;
  transTime?: string;
  customerName?: string;
  customerPhoneNo?: string;
  orderReferenceNumber?: string;
  paymentRequestRef?: string;
};

export type PaymentResult = {
  ok: boolean;
  resultCode: number;
  approved?: boolean;
  rawJson?: string;
  response?: KongaTransactionResponse;
  errorCode?: string;
  statusCode?: number;
  message?: string;
};

type NativeModule = {
  init(config: KongaPosConfig): void;
  setMerchant(merchant: MerchantProfile): void;
  startCardPayment(request: PaymentRequest): Promise<PaymentResult>;
  startCardPaymentOneShot(params: {
    config: KongaPosConfig;
    merchant: MerchantProfile;
    request: PaymentRequest;
  }): Promise<PaymentResult>;
};

const LINKING_ERROR =
  `KongaPayPos native module is unavailable. ` +
  `This SDK requires a custom Expo Dev Client build on an Anfu/Trendit Android POS device ` +
  `(Expo Go is not supported). Run: npx expo prebuild --platform android && npx expo run:android`;

function getNativeModule(): NativeModule {
  if (Platform.OS !== 'android') {
    throw new Error('KongaPay POS SDK is Android-only (Anfu / Trendit terminals).');
  }
  try {
    return requireNativeModule<NativeModule>('KongaPayPos');
  } catch {
    throw new Error(LINKING_ERROR);
  }
}

export function isKongaPayPosAvailable(): boolean {
  if (Platform.OS !== 'android') return false;
  try {
    requireNativeModule('KongaPayPos');
    return true;
  } catch {
    return false;
  }
}

export function init(config: KongaPosConfig = {}): void {
  getNativeModule().init({
    posType: config.posType ?? 'ANFU',
    isTestEnvironment: config.isTestEnvironment ?? false,
    appReleaseVersion: config.appReleaseVersion ?? '1.0.0',
    isMerchantApp: config.isMerchantApp ?? false,
  });
}

export function setMerchant(merchant: MerchantProfile): void {
  getNativeModule().setMerchant({
    ...merchant,
    kycProfileId: merchant.kycProfileId ?? '',
    merchantType: merchant.merchantType ?? '',
    transactionType: merchant.transactionType ?? '00',
  });
}

export function startCardPayment(request: PaymentRequest): Promise<PaymentResult> {
  return getNativeModule().startCardPayment({
    amount: request.amount,
    orderReference: request.orderReference,
    paymentReference: request.paymentReference ?? '',
    phoneNumber: request.phoneNumber ?? null,
    longitude: request.longitude ?? null,
    latitude: request.latitude ?? null,
  });
}

export function startCardPaymentOneShot(params: {
  config?: KongaPosConfig;
  merchant: MerchantProfile;
  request: PaymentRequest;
}): Promise<PaymentResult> {
  return getNativeModule().startCardPaymentOneShot({
    config: {
      posType: params.config?.posType ?? 'ANFU',
      isTestEnvironment: params.config?.isTestEnvironment ?? false,
      appReleaseVersion: params.config?.appReleaseVersion ?? '1.0.0',
      isMerchantApp: params.config?.isMerchantApp ?? false,
    },
    merchant: {
      ...params.merchant,
      kycProfileId: params.merchant.kycProfileId ?? '',
      merchantType: params.merchant.merchantType ?? '',
      transactionType: params.merchant.transactionType ?? '00',
    },
    request: {
      amount: params.request.amount,
      orderReference: params.request.orderReference,
      paymentReference: params.request.paymentReference ?? '',
      phoneNumber: params.request.phoneNumber ?? null,
      longitude: params.request.longitude ?? null,
      latitude: params.request.latitude ?? null,
    },
  });
}

export default {
  init,
  setMerchant,
  startCardPayment,
  startCardPaymentOneShot,
  isKongaPayPosAvailable,
};
