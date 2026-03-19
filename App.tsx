import React from 'react';
import {
  ActivityIndicator,
  Pressable,
  StatusBar,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import {SafeAreaProvider, SafeAreaView} from 'react-native-safe-area-context';
import {useShutterSound} from './src/useShutterSound';

const COLORS = {
  bg: '#0F0F0F',
  card: '#1A1A1A',
  cardBorder: '#2A2A2A',
  primary: '#4A90D9',
  primaryPressed: '#3A7BC8',
  danger: '#E54D42',
  success: '#34C759',
  successPressed: '#2DB14D',
  textPrimary: '#FFFFFF',
  textSecondary: '#8E8E93',
  textMuted: '#636366',
  disabled: '#3A3A3C',
} as const;

function App() {
  return (
    <SafeAreaProvider>
      <StatusBar barStyle="light-content" backgroundColor={COLORS.bg} />
      <SafeAreaView style={styles.container}>
        <AppContent />
      </SafeAreaView>
    </SafeAreaProvider>
  );
}

function AppContent() {
  const {status, soundEnabled, toggling, toggleSound, requestPermission, refresh} =
    useShutterSound();

  if (status === 'loading') {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" color={COLORS.primary} />
      </View>
    );
  }

  if (status === 'not_supported') {
    return (
      <View style={styles.center}>
        <Text style={styles.title}>Soundless</Text>
        <View style={[styles.card, styles.cardSpaced]}>
          <Text style={styles.statusIcon}>&#x26A0;</Text>
          <Text style={styles.cardTitle}>지원되지 않는 기기</Text>
          <Text style={styles.cardDesc}>
            이 기기는 셔터음 설정을 지원하지 않습니다.{'\n'}
            삼성 갤럭시 등 한국 출시 기기에서만 사용 가능합니다.
          </Text>
        </View>
      </View>
    );
  }

  if (status === 'error') {
    return (
      <View style={styles.center}>
        <Text style={styles.title}>Soundless</Text>
        <View style={[styles.card, styles.cardSpaced]}>
          <Text style={styles.statusIcon}>&#x26A0;</Text>
          <Text style={styles.cardTitle}>오류가 발생했습니다</Text>
          <Text style={styles.cardDesc}>
            셔터음 설정을 읽는 중 오류가 발생했습니다.
          </Text>
          <Pressable
            style={({pressed}) => [
              styles.permButton,
              pressed && {backgroundColor: COLORS.primaryPressed},
            ]}
            onPress={refresh}
            accessibilityLabel="다시 시도"
            accessibilityRole="button">
            <Text style={styles.permButtonText}>다시 시도</Text>
          </Pressable>
        </View>
      </View>
    );
  }

  if (status === 'blocked') {
    return (
      <View style={styles.center}>
        <Text style={styles.title}>Soundless</Text>
        <View style={[styles.card, styles.cardSpaced]}>
          <Text style={styles.statusIcon}>&#x1F6E1;</Text>
          <Text style={styles.cardTitle}>설정 변경이 차단됨</Text>
          <Text style={styles.cardDesc}>
            기기 보안 정책(Knox)에 의해 셔터음 설정 변경이{'\n'}
            차단되었습니다. ADB를 통해 변경해주세요:{'\n\n'}
            adb shell settings put system{'\n'}
            csc_pref_camera_forced_shuttersound_key 0
          </Text>
        </View>
      </View>
    );
  }

  if (status === 'no_permission') {
    return (
      <View style={styles.content}>
        <Text style={styles.title}>Soundless</Text>
        <Text style={styles.subtitle}>카메라 셔터음 제어</Text>

        <View style={styles.card}>
          <Text style={styles.statusIcon}>&#x1F512;</Text>
          <Text style={styles.cardTitle}>권한이 필요합니다</Text>
          <Text style={styles.cardDesc}>
            시스템 설정 변경 권한을 허용해주세요.{'\n'}
            설정 화면에서 "시스템 설정 변경 허용"을 켜주세요.
          </Text>
          <Pressable
            style={({pressed}) => [
              styles.permButton,
              pressed && {backgroundColor: COLORS.primaryPressed},
            ]}
            onPress={requestPermission}
            accessibilityLabel="시스템 설정 변경 권한 허용 화면 열기"
            accessibilityRole="button">
            <Text style={styles.permButtonText}>권한 설정 열기</Text>
          </Pressable>
        </View>

        <Text style={styles.info}>
          이 앱은 카메라 셔터음을 끄거나 켤 수 있는 간단한 유틸리티입니다.{'\n'}
          시스템 설정만 변경하며, 기본 카메라 앱에도 적용됩니다.
        </Text>
      </View>
    );
  }

  const isOff = soundEnabled === false;

  return (
    <View style={styles.content}>
      <Text style={styles.title}>Soundless</Text>
      <Text style={styles.subtitle}>카메라 셔터음 제어</Text>

      <View style={[styles.card, isOff ? styles.cardOff : styles.cardOn]}>
        <Text style={styles.statusIcon}>{isOff ? '🔇' : '🔊'}</Text>
        <Text style={styles.statusText}>
          {isOff ? '셔터음 꺼짐' : '셔터음 켜짐'}
        </Text>
        <Text style={styles.statusDesc}>
          {isOff
            ? '카메라 셔터음이 비활성화되어 있습니다'
            : '카메라 셔터음이 활성화되어 있습니다'}
        </Text>
      </View>

      <Pressable
        style={({pressed}) => [
          styles.toggleButton,
          isOff ? styles.btnEnable : styles.btnDisable,
          pressed && {
            backgroundColor: isOff
              ? COLORS.primaryPressed
              : COLORS.successPressed,
          },
          toggling && {backgroundColor: COLORS.disabled},
        ]}
        onPress={toggleSound}
        disabled={toggling}
        accessibilityLabel={isOff ? '셔터음 켜기' : '셔터음 끄기'}
        accessibilityRole="button"
        accessibilityState={{disabled: toggling}}>
        {toggling ? (
          <ActivityIndicator size="small" color={COLORS.textPrimary} />
        ) : (
          <Text style={styles.toggleButtonText}>
            {isOff ? '셔터음 켜기' : '셔터음 끄기'}
          </Text>
        )}
      </Pressable>

      <Text style={styles.info}>
        시스템 설정을 변경하여 기본 카메라 앱의 셔터음을 제어합니다.{'\n'}
        PC나 ADB 연결 없이 바로 적용됩니다.
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: COLORS.bg,
  },
  center: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 24,
  },
  content: {
    flex: 1,
    alignItems: 'center',
    paddingHorizontal: 24,
    paddingTop: 48,
  },
  title: {
    fontSize: 36,
    fontWeight: '700',
    color: COLORS.textPrimary,
    letterSpacing: 2,
  },
  subtitle: {
    fontSize: 16,
    color: COLORS.textSecondary,
    marginTop: 4,
    marginBottom: 40,
  },
  card: {
    width: '100%',
    backgroundColor: COLORS.card,
    borderRadius: 16,
    padding: 28,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: COLORS.cardBorder,
  },
  cardOn: {
    borderColor: COLORS.danger,
  },
  cardOff: {
    borderColor: COLORS.success,
  },
  cardSpaced: {
    marginTop: 24,
  },
  statusIcon: {
    fontSize: 48,
    marginBottom: 12,
  },
  statusText: {
    fontSize: 22,
    fontWeight: '600',
    color: COLORS.textPrimary,
    marginBottom: 6,
  },
  statusDesc: {
    fontSize: 14,
    color: COLORS.textSecondary,
    textAlign: 'center',
  },
  toggleButton: {
    width: '100%',
    paddingVertical: 18,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 24,
    minHeight: 56,
  },
  btnDisable: {
    backgroundColor: COLORS.success,
  },
  btnEnable: {
    backgroundColor: COLORS.primary,
  },
  toggleButtonText: {
    fontSize: 18,
    fontWeight: '700',
    color: COLORS.textPrimary,
  },
  cardTitle: {
    fontSize: 20,
    fontWeight: '600',
    color: COLORS.textPrimary,
    marginBottom: 8,
  },
  cardDesc: {
    fontSize: 14,
    color: COLORS.textSecondary,
    textAlign: 'center',
    lineHeight: 22,
  },
  permButton: {
    backgroundColor: COLORS.primary,
    paddingVertical: 14,
    paddingHorizontal: 32,
    borderRadius: 12,
    marginTop: 20,
  },
  permButtonText: {
    fontSize: 16,
    fontWeight: '600',
    color: COLORS.textPrimary,
  },
  info: {
    fontSize: 12,
    color: COLORS.textMuted,
    textAlign: 'center',
    lineHeight: 20,
    marginTop: 32,
    paddingHorizontal: 12,
  },
});

export default App;
