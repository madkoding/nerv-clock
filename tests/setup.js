import { vi } from 'vitest';

const mockElement = (id) => ({
	id,
	textContent: '',
	className: '',
	lastChild: null,
	appendChild: vi.fn(),
	remove: vi.fn(),
	querySelectorAll: vi.fn(() => []),
	querySelector: vi.fn(() => null),
	style: {
		display: ''
	},
	classList: {
		toggle: vi.fn(),
		add: vi.fn(),
		remove: vi.fn(),
		contains: vi.fn(() => false)
	},
	addEventListener: vi.fn()
});

const createClockDisplay = () => ({
	classList: {
		toggle: vi.fn(),
		add: vi.fn(),
		remove: vi.fn(),
		contains: vi.fn((cls) => cls === 'warning' || cls === 'critical' || cls === 'depleted')
	},
	appendChild: vi.fn(),
	remove: vi.fn(),
	style: {}
});

const createButton = (mode) => ({
	dataset: { mode },
	classList: {
		toggle: vi.fn(),
		add: vi.fn(),
		remove: vi.fn(),
		contains: vi.fn(() => false)
	},
	textContent: '',
	addEventListener: vi.fn()
});

global.document = {
	getElementById: vi.fn((id) => mockElement(id)),
	querySelectorAll: vi.fn((selector) => {
		if (selector === '.ctrl-btn') {
			return [
				createButton('normal'),
				createButton('racing'),
				createButton('slow'),
				createButton('stop')
			];
		}
		if (selector === '.digit-group, .colon') {
			return [];
		}
		return [];
	}),
	querySelector: vi.fn((selector) => {
		if (selector === '.clock-display') {
			return createClockDisplay();
		}
		if (selector === '.depleted-message') {
			return null;
		}
		return createClockDisplay();
	}),
	addEventListener: vi.fn(),
	readyState: 'complete',
	createTextNode: vi.fn((text) => ({ nodeType: 3, textContent: text }))
};

global.window = {
	__TAURI__: undefined
};
