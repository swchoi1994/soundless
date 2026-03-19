import React from 'react';
import {render, fireEvent} from '@testing-library/react-native';
import App from '../App';
import {useShutterSound} from '../src/useShutterSound';

jest.mock('../src/useShutterSound');
jest.mock('react-native-safe-area-context', () => {
  const RN = require('react-native');
  const MockReact = require('react');
  return {
    SafeAreaProvider: ({children, ...rest}: any) =>
      MockReact.createElement(RN.View, rest, children),
    SafeAreaView: ({children, ...rest}: any) =>
      MockReact.createElement(RN.View, rest, children),
  };
});

const mockUseShutterSound = useShutterSound as jest.MockedFunction<
  typeof useShutterSound
>;

const baseReturn = {
  soundEnabled: null,
  status: 'loading' as const,
  toggling: false,
  toggleSound: jest.fn(),
  requestPermission: jest.fn(),
  refresh: jest.fn(),
};

describe('App', () => {
  it('renders loading state', () => {
    mockUseShutterSound.mockReturnValue({...baseReturn, status: 'loading'});
    const {toJSON} = render(<App />);
    expect(toJSON()).toBeTruthy();
  });

  it('renders not_supported state', () => {
    mockUseShutterSound.mockReturnValue({
      ...baseReturn,
      status: 'not_supported',
    });
    const {getByText} = render(<App />);
    expect(getByText('지원되지 않는 기기')).toBeTruthy();
  });

  it('renders error state with retry button', () => {
    const mockRefresh = jest.fn();
    mockUseShutterSound.mockReturnValue({
      ...baseReturn,
      status: 'error',
      refresh: mockRefresh,
    });
    const {getByText} = render(<App />);
    expect(getByText('오류가 발생했습니다')).toBeTruthy();

    const retryButton = getByText('다시 시도');
    expect(retryButton).toBeTruthy();
    fireEvent.press(retryButton);
    expect(mockRefresh).toHaveBeenCalledTimes(1);
  });

  it('renders blocked state with Knox message', () => {
    mockUseShutterSound.mockReturnValue({
      ...baseReturn,
      status: 'blocked',
    });
    const {getByText} = render(<App />);
    expect(getByText('설정 변경이 차단됨')).toBeTruthy();
  });

  it('renders no_permission state with button', () => {
    mockUseShutterSound.mockReturnValue({
      ...baseReturn,
      status: 'no_permission',
    });
    const {getByText} = render(<App />);
    expect(getByText('권한이 필요합니다')).toBeTruthy();
    expect(getByText('권한 설정 열기')).toBeTruthy();
  });

  it('renders ready state with sound ON', () => {
    mockUseShutterSound.mockReturnValue({
      ...baseReturn,
      status: 'ready',
      soundEnabled: true,
    });
    const {getByText} = render(<App />);
    expect(getByText('셔터음 켜짐')).toBeTruthy();
    expect(getByText('셔터음 끄기')).toBeTruthy();
  });

  it('renders ready state with sound OFF', () => {
    mockUseShutterSound.mockReturnValue({
      ...baseReturn,
      status: 'ready',
      soundEnabled: false,
    });
    const {getByText} = render(<App />);
    expect(getByText('셔터음 꺼짐')).toBeTruthy();
    expect(getByText('셔터음 켜기')).toBeTruthy();
  });

  it('shows spinner instead of text when toggling', () => {
    mockUseShutterSound.mockReturnValue({
      ...baseReturn,
      status: 'ready',
      soundEnabled: true,
      toggling: true,
    });
    const {queryByText} = render(<App />);
    expect(queryByText('셔터음 끄기')).toBeNull();
  });
});
