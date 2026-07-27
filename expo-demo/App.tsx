import { useEffect, useMemo, useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  Platform,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import { StatusBar } from 'expo-status-bar';
import KongaPayPos, {
  isKongaPayPosAvailable,
  type PaymentResult,
} from 'kongapay-pos';

const DEFAULT_MERCHANT = {
  serialNumber: '1492430209',
  terminalId: '2101MN91',
  merchantId: '31446',
  walletId: '2001684006',
  phoneNumber: '2348044433322',
  parentMerchantId: '31446',
  parentMerchantName: 'Barkey',
  parentAccountNumber: '10000312123',
  parentEmail: 'merchant@example.com',
  parentAddress: 'Lagos',
  parentPhone: '2348160764301',
  kycProfileId: '19',
  merchantType: 'company',
  transactionType: '00',
};

export default function App() {
  const [amount, setAmount] = useState('50');
  const [orderReference, setOrderReference] = useState(
    () => `ORD-${Date.now()}`
  );
  const [ready, setReady] = useState(false);
  const [busy, setBusy] = useState(false);
  const [lastResult, setLastResult] = useState<PaymentResult | null>(null);
  const [initError, setInitError] = useState<string | null>(null);

  const available = useMemo(() => isKongaPayPosAvailable(), []);

  useEffect(() => {
    if (!available) return;
    try {
      KongaPayPos.init({
        posType: 'ANFU',
        isTestEnvironment: false,
        appReleaseVersion: '1.0.0',
        isMerchantApp: false,
      });
      KongaPayPos.setMerchant(DEFAULT_MERCHANT);
      setReady(true);
    } catch (e) {
      setInitError(e instanceof Error ? e.message : String(e));
    }
  }, [available]);

  async function onPay() {
    if (!ready || busy) return;
    setBusy(true);
    setLastResult(null);
    try {
      const result = await KongaPayPos.startCardPayment({
        amount: amount.trim(),
        orderReference: orderReference.trim() || `ORD-${Date.now()}`,
        paymentReference: '',
      });
      setLastResult(result);
      setOrderReference(`ORD-${Date.now()}`);

      if (result.ok && result.approved) {
        Alert.alert(
          'Approved',
          result.response?.responseDescription ??
            result.response?.responseMessage ??
            'Payment approved'
        );
      } else if (result.ok) {
        Alert.alert(
          result.response?.responseCode ?? 'Declined',
          result.response?.responseDescription ??
            result.response?.responseMessage ??
            'Payment declined'
        );
      } else {
        Alert.alert(result.errorCode ?? 'Error', result.message ?? 'Payment failed');
      }
    } catch (e) {
      Alert.alert('Payment error', e instanceof Error ? e.message : String(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <SafeAreaView style={styles.safe}>
      <StatusBar style="dark" />
      <ScrollView contentContainerStyle={styles.container}>
        <Text style={styles.brand}>KongaPay POS</Text>
        <Text style={styles.subtitle}>Expo / React Native demo</Text>

        <View style={styles.card}>
          <Text style={styles.label}>Platform</Text>
          <Text style={styles.value}>{Platform.OS}</Text>

          <Text style={styles.label}>Native module</Text>
          <Text style={[styles.value, available ? styles.ok : styles.bad]}>
            {available ? 'Linked' : 'Not available (need Dev Client build)'}
          </Text>

          <Text style={styles.label}>SDK init</Text>
          <Text style={[styles.value, ready ? styles.ok : styles.bad]}>
            {initError ?? (ready ? 'Ready' : available ? 'Initializing…' : 'Skipped')}
          </Text>
        </View>

        {!available && (
          <View style={styles.notice}>
            <Text style={styles.noticeTitle}>Build required</Text>
            <Text style={styles.noticeBody}>
              This SDK uses Android AARs and must run on an Anfu/Trendit POS device
              with a custom Expo Dev Client — Expo Go will not work.
            </Text>
            <Text style={styles.code}>
              {`npx expo prebuild --platform android\nnpx expo run:android`}
            </Text>
          </View>
        )}

        <Text style={styles.section}>Payment</Text>
        <Text style={styles.fieldLabel}>Amount</Text>
        <TextInput
          style={styles.input}
          keyboardType="decimal-pad"
          value={amount}
          onChangeText={setAmount}
          placeholder="50"
        />
        <Text style={styles.fieldLabel}>Order reference</Text>
        <TextInput
          style={styles.input}
          value={orderReference}
          onChangeText={setOrderReference}
          autoCapitalize="characters"
        />

        <TouchableOpacity
          style={[styles.button, (!ready || busy) && styles.buttonDisabled]}
          disabled={!ready || busy}
          onPress={onPay}
        >
          {busy ? (
            <ActivityIndicator color="#fff" />
          ) : (
            <Text style={styles.buttonText}>Start card payment</Text>
          )}
        </TouchableOpacity>

        {lastResult && (
          <View style={styles.result}>
            <Text style={styles.section}>Last result</Text>
            <Text style={styles.mono}>{JSON.stringify(lastResult, null, 2)}</Text>
          </View>
        )}

        <Text style={styles.hint}>
          Update DEFAULT_MERCHANT in App.tsx with credentials from KongaPay before
          testing on a real terminal.
        </Text>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: '#F7F8FA' },
  container: { padding: 20, paddingBottom: 48 },
  brand: {
    fontSize: 28,
    fontWeight: '700',
    color: '#111827',
    letterSpacing: -0.5,
  },
  subtitle: { marginTop: 4, color: '#6B7280', marginBottom: 20 },
  card: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 16,
    borderWidth: 1,
    borderColor: '#E5E7EB',
    marginBottom: 16,
  },
  label: { fontSize: 12, color: '#6B7280', marginTop: 8 },
  value: { fontSize: 15, color: '#111827', marginTop: 2 },
  ok: { color: '#047857' },
  bad: { color: '#B91C1C' },
  notice: {
    backgroundColor: '#FFF7ED',
    borderColor: '#FDBA74',
    borderWidth: 1,
    borderRadius: 12,
    padding: 14,
    marginBottom: 16,
  },
  noticeTitle: { fontWeight: '700', color: '#9A3412', marginBottom: 6 },
  noticeBody: { color: '#9A3412', lineHeight: 20 },
  code: {
    marginTop: 10,
    fontFamily: Platform.select({ ios: 'Menlo', android: 'monospace', default: 'monospace' }),
    fontSize: 12,
    color: '#7C2D12',
  },
  section: {
    fontSize: 18,
    fontWeight: '600',
    color: '#111827',
    marginBottom: 10,
    marginTop: 8,
  },
  fieldLabel: { fontSize: 13, color: '#4B5563', marginBottom: 6 },
  input: {
    backgroundColor: '#fff',
    borderWidth: 1,
    borderColor: '#D1D5DB',
    borderRadius: 10,
    paddingHorizontal: 14,
    paddingVertical: 12,
    fontSize: 16,
    marginBottom: 12,
  },
  button: {
    backgroundColor: '#0F766E',
    borderRadius: 12,
    paddingVertical: 14,
    alignItems: 'center',
    marginTop: 4,
  },
  buttonDisabled: { opacity: 0.5 },
  buttonText: { color: '#fff', fontSize: 16, fontWeight: '600' },
  result: {
    marginTop: 20,
    backgroundColor: '#111827',
    borderRadius: 12,
    padding: 14,
  },
  mono: {
    color: '#E5E7EB',
    fontFamily: Platform.select({ ios: 'Menlo', android: 'monospace', default: 'monospace' }),
    fontSize: 12,
    lineHeight: 18,
  },
  hint: { marginTop: 18, color: '#6B7280', lineHeight: 20, fontSize: 13 },
});
