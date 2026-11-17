import '@testing-library/jest-dom/vitest';
import { cleanup } from '@testing-library/react';
import { afterAll, afterEach, beforeAll, vi } from 'vitest';
import { server } from './server';

// Icons load their SVG data from the Iconify API at runtime; tests stay offline.
vi.mock('components/base/IconifyIcon', () => ({ default: () => null }));

// SimpleBar asks for a scrollbar pseudo-element style, which jsdom doesn't implement (it logs an error).
const { getComputedStyle } = window;
window.getComputedStyle = (element: Element) => getComputedStyle(element);

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
afterEach(() => {
  server.resetHandlers();
  cleanup();
});
afterAll(() => server.close());
