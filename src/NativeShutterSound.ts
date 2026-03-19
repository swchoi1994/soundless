import {NativeEventEmitter, NativeModules, Platform} from 'react-native';

interface ShutterSoundInterface {
  hasWriteSettingsPermission(): Promise<boolean>;
  getShutterSoundEnabled(): Promise<boolean>;
  setShutterSoundEnabled(enabled: boolean): Promise<void>;
  openWriteSettingsPermission(): Promise<void>;
  addListener(eventName: string): void;
  removeListeners(count: number): void;
}

const module =
  Platform.OS === 'android'
    ? (NativeModules.ShutterSoundModule as ShutterSoundInterface)
    : null;

if (__DEV__ && Platform.OS === 'android' && !module) {
  console.error(
    'ShutterSoundModule is not available. Did you forget to link the native module?',
  );
}

export const ShutterSoundModule = module;
export const ShutterSoundEmitter = module
  ? new NativeEventEmitter(NativeModules.ShutterSoundModule)
  : null;
