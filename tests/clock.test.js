import { describe, it, expect, beforeEach } from 'vitest';
import { pad, NervClock } from './clock-logic.js';

describe('NervClock', () => {
	let clock;

	beforeEach(() => {
		clock = new NervClock();
	});

	describe('pad', () => {
		it('should pad single digit numbers with zero', () => {
			expect(pad(5)).toBe('05');
			expect(pad(0)).toBe('00');
			expect(pad(9)).toBe('09');
		});

		it('should not pad two digit numbers', () => {
			expect(pad(10)).toBe('10');
			expect(pad(59)).toBe('59');
		});

		it('should handle larger numbers', () => {
			expect(pad(100)).toBe('100');
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
	});
});
