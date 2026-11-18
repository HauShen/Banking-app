import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { clearAccessToken } from 'api/tokenStore';
import paths from 'routes/path';
import { mockQuotes, mockRates } from 'test/fx';
import { mockSession, renderApp } from 'test/renderApp';
import { beforeEach, describe, expect, it } from 'vitest';
import RatesPage from '.';

const customer = {
  id: 'u1',
  username: 'hau',
  fullName: 'Hau Shen',
  email: 'h@b.local',
  role: 'CUSTOMER',
};
const plain = (text: string | null | undefined) => (text ?? '').replace(/\s/g, ' ');

beforeEach(() => {
  clearAccessToken();
  mockSession(customer);
  mockRates();
  mockQuotes();
});

const renderRates = () =>
  renderApp(paths.rates, {}, <p>Dashboard</p>, { [paths.rates]: <RatesPage /> });

describe('exchange rates page', () => {
  it('lists the sell (you pay) and buy (you receive) rate of every currency', async () => {
    renderRates();
    const table = await screen.findByRole('table', { name: /exchange rates/i });
    const usd = within(table).getByText('USD').closest('tr') as HTMLElement;
    expect(plain(usd.textContent)).toBe('USD4.23504.1650');
    expect(within(table).getAllByRole('row')).toHaveLength(5); // header + 4 currencies
  });

  it('converts an amount with the right rate for paying or receiving', async () => {
    renderRates();
    const user = userEvent.setup();

    // Default: 100 of the first currency (EUR), paying: 100 × 4.92.
    expect(await screen.findByText(/= RM 492\.00/)).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /i receive/i }));
    expect(await screen.findByText(/= RM 483\.00/)).toBeInTheDocument();

    const amount = screen.getByLabelText(/^amount/i);
    await user.clear(amount);
    await user.type(amount, '1.234');
    expect(await screen.findByText('Enter a valid EUR amount.')).toBeInTheDocument();
  });
});
