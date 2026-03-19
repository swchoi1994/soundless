import {useCallback, useEffect, useRef, useState} from 'react';
import {ShutterSoundEmitter, ShutterSoundModule} from './NativeShutterSound';

type Status =
  | 'loading'
  | 'no_permission'
  | 'not_supported'
  | 'ready'
  | 'error'
  | 'blocked';

export function useShutterSound() {
  const [soundEnabled, setSoundEnabled] = useState<boolean | null>(null);
  const [status, setStatus] = useState<Status>('loading');
  const [toggling, setToggling] = useState(false);
  const togglingRef = useRef(false);

  const checkState = useCallback(async () => {
    if (!ShutterSoundModule) {
      setStatus('not_supported');
      return;
    }

    try {
      const perm = await ShutterSoundModule.hasWriteSettingsPermission();

      if (!perm) {
        setStatus('no_permission');
        return;
      }

      try {
        const enabled = await ShutterSoundModule.getShutterSoundEnabled();
        setSoundEnabled(enabled);
        setStatus('ready');
      } catch (e: any) {
        if (e.code === 'NOT_SUPPORTED') {
          setStatus('not_supported');
        } else {
          setSoundEnabled(null);
          setStatus('error');
        }
      }
    } catch {
      setStatus('not_supported');
    }
  }, []);

  useEffect(() => {
    checkState();
  }, [checkState]);

  useEffect(() => {
    if (!ShutterSoundEmitter) return;
    const sub = ShutterSoundEmitter.addListener('onAppResume', () => {
      checkState();
    });
    return () => sub.remove();
  }, [checkState]);

  const toggleSound = useCallback(async () => {
    if (!ShutterSoundModule || soundEnabled === null || togglingRef.current) {
      return;
    }
    togglingRef.current = true;
    setToggling(true);
    try {
      await ShutterSoundModule.setShutterSoundEnabled(!soundEnabled);
      setSoundEnabled(!soundEnabled);
    } catch (e: any) {
      if (e.code === 'WRITE_BLOCKED') {
        setStatus('blocked');
      } else {
        await checkState();
      }
    } finally {
      togglingRef.current = false;
      setToggling(false);
    }
  }, [soundEnabled, checkState]);

  const requestPermission = useCallback(async () => {
    if (!ShutterSoundModule) return;
    try {
      await ShutterSoundModule.openWriteSettingsPermission();
    } catch {
      // User will come back to app and we'll re-check via onAppResume
    }
  }, []);

  return {
    soundEnabled,
    status,
    toggling,
    toggleSound,
    requestPermission,
    refresh: checkState,
  };
}
