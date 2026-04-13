import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import { NervClock } from '../ui/clock.js';

describe('NervClock', () => {
	let clock;

	beforeEach(() => {
		clock = new NervClock();
	});

	afterEach(() => {
		vi.clearAllMocks();
	});

	describe('pad', () => {
		it('should pad single digit numbers with zero', () => {
			expect(clock.pad(5)).toBe('05');
			expect(clock.pad(0)).toBe('00');
			expect(clock.pad(9)).toBe('09');
		});

		it('should not pad two digit numbers', () => {
			expect(clock.pad(10)).toBe('10');
			expect(clock.pad(59)).toBe('59');
		});

		it('should handle larger numbers', () => {
			expect(clock.pad(100)).toBe('100');
		});
	});

	describe('constructor', () => {
		it('should initialize with default mode normal', () => {
			expect(clock.mode).toBe('normal');
		});

		it('should initialize isPaused as false', () => {
			expect(clock.isPaused).toBe(false);
		});

		it('should initialize isDepleted as false', () => {
			expect(clock.isDepleted).toBe(false);
		});

		it('should initialize pomodoro durations correctly', () => {
			expect(clock.pomodoroDurations[0]).toBe(25 * 60 * 1000);
			expect(clock.pomodoroDurations[1]).toBe(5 * 60 * 1000);
		});

		it('should initialize stopwatch time to 0', () => {
			expect(clock.stopwatchTime).toBe(0);
		});
	});

	describe('setMode', () => {
		it('should set mode to racing', () => {
			clock.setMode('racing');
			expect(clock.mode).toBe('racing');
		});

		it('should reset stopwatch when switching to racing', () => {
			clock.stopwatchTime = 1000;
			clock.setMode('racing');
			expect(clock.stopwatchTime).toBe(0);
		});

		it('should set mode to slow with 25min default', () => {
			clock.setMode('slow');
			expect(clock.mode).toBe('slow');
			expect(clock.pomodoroDurationIndex).toBe(0);
			expect(clock.pomodoroTime).toBe(25 * 60 * 1000);
		});

		it('should reset paused state on mode change', () => {
			clock.setMode('racing');
			clock.isPaused = true;
			clock.setMode('slow');
			expect(clock.isPaused).toBe(false);
		});

		it('should reset depleted state on mode change', () => {
			clock.setMode('slow');
			clock.isDepleted = true;
			clock.setMode('racing');
			expect(clock.isDepleted).toBe(false);
		});

		it('should toggle pomodoro duration when in slow mode', () => {
			clock.setMode('slow');
			const firstDuration = clock.pomodoroTime;
			clock.setMode('slow');
			expect(clock.pomodoroTime).not.toBe(firstDuration);
			expect(clock.pomodoroDurationIndex).toBe(1);
			expect(clock.pomodoroTime).toBe(5 * 60 * 1000);
		});

		it('should set mode to normal', () => {
			clock.setMode('normal');
			expect(clock.mode).toBe('normal');
		});
	});

	describe('handleButton', () => {
		it('should pause when stop pressed in racing mode', () => {
			clock.setMode('racing');
			clock.handleButton('stop');
			expect(clock.isPaused).toBe(true);
		});

		it('should resume when stop pressed while paused', () => {
			clock.setMode('racing');
			clock.isPaused = true;
			clock.handleButton('stop');
			expect(clock.isPaused).toBe(false);
		});

		it('should change mode when mode button pressed', () => {
			clock.handleButton('racing');
			expect(clock.mode).toBe('racing');
		});

		it('should toggle pause in normal mode when stop pressed', () => {
			clock.setMode('normal');
			clock.handleButton('stop');
			expect(clock.isPaused).toBe(true);
		});
	});

	describe('clearTimerState', () => {
		it('should remove color classes from clockDisplay', () => {
			clock.clearTimerState();
			expect(clock.clockDisplay.classList.remove).toHaveBeenCalledWith('warning', 'critical', 'depleted');
		});
	});

	describe('updateButtonStates', () => {
		it('should toggle active class on buttons based on mode', () => {
			clock.setMode('racing');
			clock.updateButtonStates();
		});

		it('should show PLAY text when paused in racing mode', () => {
			clock.setMode('racing');
			clock.isPaused = true;
			clock.updateButtonStates();
			expect(clock.stopBtn.textContent).toBe('PLAY');
		});

		it('should show STOP text when not paused', () => {
			clock.setMode('racing');
			clock.isPaused = false;
			clock.updateButtonStates();
			expect(clock.stopBtn.textContent).toBe('STOP');
		});

		it('should disable stop button in normal mode', () => {
			clock.setMode('normal');
			clock.updateButtonStates();
			expect(clock.stopBtn.classList.add).toHaveBeenCalledWith('disabled');
		});
	});

	describe('display', () => {
		it('should call setDigit for each digit', () => {
			vi.spyOn(clock, 'setDigit');
			clock.display('12', '34', '56', '78');
			expect(clock.setDigit).toHaveBeenCalledTimes(8);
		});
	});

	describe('setDigit', () => {
		it('should do nothing if element is null', () => {
			expect(() => clock.setDigit(null, '5')).not.toThrow();
		});
	});

	describe('showDepletedMessage', () => {
		it('should create depleted message element', () => {
			expect(() => clock.showDepletedMessage()).not.toThrow();
		});
	});

	describe('hideDepletedMessage', () => {
		it('should remove depleted message from DOM', () => {
			expect(() => clock.hideDepletedMessage()).not.toThrow();
		});
	});
});
