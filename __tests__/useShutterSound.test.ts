import {renderHook, act, waitFor} from '@testing-library/react-native';

let mockEmitterCallback: (() => void) | null = null;
const mockRemove = jest.fn();

jest.mock('../src/NativeShutterSound', () => ({
  __esModule: true,
  ShutterSoundModule: {
    hasWriteSettingsPermission: jest.fn(),
    getShutterSoundEnabled: jest.fn(),
    setShutterSoundEnabled: jest.fn(),
    openWriteSettingsPermission: jest.fn(),
    addListener: jest.fn(),
    removeListeners: jest.fn(),
  },
  ShutterSoundEmitter: {
    addListener: jest.fn((_event: string, cb: () => void) => {
      mockEmitterCallback = cb;
      return {remove: mockRemove};
    }),
  },
}));

import {useShutterSound} from '../src/useShutterSound';
import {ShutterSoundModule} from '../src/NativeShutterSound';

const mockModule = ShutterSoundModule as jest.Mocked<
  NonNullable<typeof ShutterSoundModule>
>;

beforeEach(() => {
  jest.clearAllMocks();
  mockEmitterCallback = null;
});

describe('useShutterSound', () => {
  it('starts in loading state', () => {
    mockModule.hasWriteSettingsPermission.mockReturnValue(new Promise(() => {}));
    const {result} = renderHook(() => useShutterSound());
    expect(result.current.status).toBe('loading');
    expect(result.current.soundEnabled).toBeNull();
    expect(result.current.toggling).toBe(false);
  });

  it('transitions to no_permission when permission denied', async () => {
    mockModule.hasWriteSettingsPermission.mockResolvedValue(false);
    const {result} = renderHook(() => useShutterSound());

    await waitFor(() => expect(result.current.status).toBe('no_permission'));
    expect(result.current.soundEnabled).toBeNull();
  });

  it('transitions to ready when permission granted and setting exists', async () => {
    mockModule.hasWriteSettingsPermission.mockResolvedValue(true);
    mockModule.getShutterSoundEnabled.mockResolvedValue(true);
    const {result} = renderHook(() => useShutterSound());

    await waitFor(() => expect(result.current.status).toBe('ready'));
    expect(result.current.soundEnabled).toBe(true);
  });

  it('transitions to not_supported when setting key missing', async () => {
    mockModule.hasWriteSettingsPermission.mockResolvedValue(true);
    mockModule.getShutterSoundEnabled.mockRejectedValue({code: 'NOT_SUPPORTED'});
    const {result} = renderHook(() => useShutterSound());

    await waitFor(() => expect(result.current.status).toBe('not_supported'));
  });

  it('transitions to error on unknown read failure', async () => {
    mockModule.hasWriteSettingsPermission.mockResolvedValue(true);
    mockModule.getShutterSoundEnabled.mockRejectedValue({code: 'READ_FAILED'});
    const {result} = renderHook(() => useShutterSound());

    await waitFor(() => expect(result.current.status).toBe('error'));
    expect(result.current.soundEnabled).toBeNull();
  });

  it('toggles sound off -> on', async () => {
    mockModule.hasWriteSettingsPermission.mockResolvedValue(true);
    mockModule.getShutterSoundEnabled.mockResolvedValue(false);
    mockModule.setShutterSoundEnabled.mockResolvedValue(undefined);
    const {result} = renderHook(() => useShutterSound());

    await waitFor(() => expect(result.current.status).toBe('ready'));
    expect(result.current.soundEnabled).toBe(false);

    await act(async () => {
      await result.current.toggleSound();
    });

    expect(mockModule.setShutterSoundEnabled).toHaveBeenCalledWith(true);
    expect(result.current.soundEnabled).toBe(true);
  });

  it('toggles sound on -> off', async () => {
    mockModule.hasWriteSettingsPermission.mockResolvedValue(true);
    mockModule.getShutterSoundEnabled.mockResolvedValue(true);
    mockModule.setShutterSoundEnabled.mockResolvedValue(undefined);
    const {result} = renderHook(() => useShutterSound());

    await waitFor(() => expect(result.current.status).toBe('ready'));

    await act(async () => {
      await result.current.toggleSound();
    });

    expect(mockModule.setShutterSoundEnabled).toHaveBeenCalledWith(false);
    expect(result.current.soundEnabled).toBe(false);
  });

  it('sets blocked status when Knox blocks the write', async () => {
    mockModule.hasWriteSettingsPermission.mockResolvedValue(true);
    mockModule.getShutterSoundEnabled.mockResolvedValue(true);
    mockModule.setShutterSoundEnabled.mockRejectedValue({code: 'WRITE_BLOCKED'});
    const {result} = renderHook(() => useShutterSound());

    await waitFor(() => expect(result.current.status).toBe('ready'));

    await act(async () => {
      await result.current.toggleSound();
    });

    expect(result.current.status).toBe('blocked');
    expect(result.current.toggling).toBe(false);
  });

  it('falls back to checkState when toggle fails', async () => {
    mockModule.hasWriteSettingsPermission.mockResolvedValue(true);
    mockModule.getShutterSoundEnabled.mockResolvedValue(true);
    mockModule.setShutterSoundEnabled.mockRejectedValue(new Error('fail'));
    const {result} = renderHook(() => useShutterSound());

    await waitFor(() => expect(result.current.status).toBe('ready'));

    await act(async () => {
      await result.current.toggleSound();
    });

    expect(mockModule.hasWriteSettingsPermission).toHaveBeenCalledTimes(2);
    expect(result.current.toggling).toBe(false);
  });

  it('calls openWriteSettingsPermission on requestPermission', async () => {
    mockModule.hasWriteSettingsPermission.mockResolvedValue(false);
    mockModule.openWriteSettingsPermission.mockResolvedValue(undefined);
    const {result} = renderHook(() => useShutterSound());

    await waitFor(() => expect(result.current.status).toBe('no_permission'));

    await act(async () => {
      await result.current.requestPermission();
    });

    expect(mockModule.openWriteSettingsPermission).toHaveBeenCalledTimes(1);
  });

  it('re-checks state on onAppResume event', async () => {
    mockModule.hasWriteSettingsPermission.mockResolvedValue(true);
    mockModule.getShutterSoundEnabled.mockResolvedValue(true);
    const {result} = renderHook(() => useShutterSound());

    await waitFor(() => expect(result.current.status).toBe('ready'));
    expect(mockModule.hasWriteSettingsPermission).toHaveBeenCalledTimes(1);

    await act(async () => {
      mockEmitterCallback?.();
    });

    expect(mockModule.hasWriteSettingsPermission).toHaveBeenCalledTimes(2);
  });

  it('cleans up emitter subscription on unmount', async () => {
    mockModule.hasWriteSettingsPermission.mockReturnValue(new Promise(() => {}));
    const {unmount} = renderHook(() => useShutterSound());

    unmount();

    expect(mockRemove).toHaveBeenCalledTimes(1);
  });
});
