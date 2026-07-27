# KongaPay POS — Expo / React Native demo

Yes — external partners **can** integrate this SDK from a React Native Expo app, with these constraints:

| Constraint | Detail |
|------------|--------|
| Platform | **Android only** (Anfu / Trendit POS hardware) |
| Expo Go | **Not supported** — local AARs + custom `Application` |
| Required | **Expo Dev Client** + `npx expo prebuild` / EAS Build |
| Bridge | Local Expo module `modules/kongapay-pos` |

The native Android AAR cannot run in pure JS. This demo wraps `KongaPos` in an Expo Module and a config plugin that:

1. Copies `pos_utils` + Anfu AARs into the Android app
2. Adds Gradle / JitPack dependencies
3. Makes `MainApplication` extend `ApplicationClass` (required by the SDK)

---

## Quick start

```bash
cd expo-demo
npm install

# Generate the native Android project (Dev Client)
npx expo prebuild --platform android

# Install on a connected Anfu/Trendit device (or compatible Android device for compile checks)
npx expo run:android
```

`expo-build-properties` pins Android `minSdkVersion` to **29**. Update merchant credentials in `App.tsx` (`DEFAULT_MERCHANT`) before real terminal tests.

---

## JS API

```ts
import KongaPayPos from 'kongapay-pos';

KongaPayPos.init({
  posType: 'ANFU', // or 'Trendit'
  isTestEnvironment: false,
  appReleaseVersion: '1.0.0',
});

KongaPayPos.setMerchant({
  serialNumber: '...',
  terminalId: '...',
  merchantId: '...',
  walletId: '...',
  phoneNumber: '...',
  parentMerchantId: '...',
  parentMerchantName: '...',
  parentAccountNumber: '...',
  parentEmail: '...',
  parentAddress: '...',
  parentPhone: '...',
  transactionType: '00',
});

const result = await KongaPayPos.startCardPayment({
  amount: '50',
  orderReference: `ORD-${Date.now()}`,
});

if (result.ok && result.approved) {
  // responseCode === "00"
  console.log(result.response);
}
```

Treat only `result.approved === true` (i.e. `responseCode === "00"`) as approved.

---

## Project layout

```text
expo-demo/
├── App.tsx                          # Demo UI
├── app.json                         # plugins: expo-dev-client + kongapay-pos
└── modules/kongapay-pos/
    ├── app.plugin.js                # Config plugin (AARs, ApplicationClass, deps)
    ├── src/index.ts                 # JS/TS API
    └── android/
        ├── build.gradle
        ├── libs/                    # Partner AARs (copied from ../app/libs)
        └── src/main/java/.../KongaPayPosModule.kt
```

---

## Partner integration checklist

- [ ] Use **custom Dev Client** / EAS Build — not Expo Go
- [ ] Android `minSdk` ≥ **29**
- [ ] Ship both `pos_utils-*.aar` and `AFSDKInterface_*.aar` for Anfu
- [ ] `MainApplication` extends `ApplicationClass` (handled by the config plugin)
- [ ] Call `init` → `setMerchant` → `startCardPayment`
- [ ] Unique `orderReference` per attempt
- [ ] Physical Anfu or Trendit terminal for real card/EMV flows

---

## EAS Build (optional)

```bash
npx eas-cli build --platform android --profile development
```

Use a development profile so you get a Dev Client you can iterate against with Metro.

---

## Support

Same as the native Android SDK — contact KongaPay POS support with partner name, device serial/TID, app version, and `orderReference`.
