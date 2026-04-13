import { defineConfig } from 'vitest/config';

export default defineConfig({
	test: {
		environment: 'jsdom',
		globals: true,
		include: ['tests/**/*.test.js'],
		coverage: {
			provider: 'v8',
			reporter: ['text', 'json', 'html'],
			include: ['tests/clock-logic.js'],
			exclude: ['**/*.test.js', '**/setup.js']
		},
		setupFiles: ['./tests/setup.js']
	}
});
