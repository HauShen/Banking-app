import '@testing-library/jest-dom/vitest';
import { cleanup, configure } from '@testing-library/react';
import { afterAll, afterEach, beforeAll, vi } from 'vitest';
import { server } from './server';

// findBy*/waitFor wait up to 10 s instead of 1 s: the first DataGrid render in a test file takes
// ~2.5 s on a quiet machine and over 7 s when many test files run in parallel (as on CI runners).
configure({ asyncUtilTimeout: 10000 });

// Icons load their SVG data from the Iconify API at runtime; tests stay offline.
vi.mock('components/base/IconifyIcon', () => ({ default: () => null }));

// SimpleBar asks for a scrollbar pseudo-element style, which jsdom doesn't implement (it logs an error).
const { getComputedStyle } = window;
window.getComputedStyle = (element: Element) => getComputedStyle(element);

// jsdom has no ResizeObserver; MUI's DataGrid needs one to measure itself.
globalThis.ResizeObserver ??= class {
  observe() {}
  unobserve() {}
  disconnect() {}
};

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
afterEach(() => {
  server.resetHandlers();
  cleanup();
});
afterAll(() => server.close());
