import '@testing-library/jest-dom/vitest';
import { cleanup, configure } from '@testing-library/react';
import { afterAll, afterEach, beforeAll, vi } from 'vitest';
import { server } from './server';

// findBy*/waitFor wait up to 5 s instead of 1 s: the first test that renders MUI's DataGrid spends
// ~2.5 s loading it, and CI runners are slower than a laptop.
configure({ asyncUtilTimeout: 5000 });

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
