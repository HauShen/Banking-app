import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { clearAccessToken } from 'api/tokenStore';
import type { TransactionAlert } from 'api/types';
import { http, HttpResponse } from 'msw';
import paths from 'routes/path';
import RequireRole from 'routes/RequireRole';
import { mockSession, renderApp } from 'test/renderApp';
import { API, server } from 'test/server';
import { beforeEach, describe, expect, it } from 'vitest';
import AdminAlertsPage from '.';

const admin = {
  id: 'a1',
  username: 'devadmin',
  fullName: 'Dev Admin',
  email: 'a@b.local',
  role: 'ADMIN',
};
const customer = {
  id: 'c1',
  username: 'hau',
  fullName: 'Hau Shen',
  email: 'h@b.local',
  role: 'CUSTOMER',
};

const alert = (id: number, overrides: Partial<TransactionAlert> = {}): TransactionAlert => ({
  id,
  rule: 'LARGE_TRANSFER',
  status: 'OPEN',
  accountNumber: '1111111111',
  ownerId: 'c1',
  reference: `TXN-${id}`,
  amount: 12000,
  detail: 'Transfer of RM 12000.00 is at or above the RM 10000.00 limit',
  createdAt: `2026-11-21T08:0${id}:00Z`,
  reviewedAt: null,
  reviewedBy: null,
  ...overrides,
});

const large = alert(2);
const rapid = alert(1, {
  rule: 'RAPID_TRANSFERS',
  accountNumber: '2222222222',
  amount: 10,
  detail: '6 transfers from this account within 10 minutes (limit 5)',
});
const reviewed = alert(3, {
  status: 'REVIEWED',
  reviewedAt: '2026-11-21T09:00:00Z',
  reviewedBy: 'a1',
});

/**
 * The alert backend in memory: list (status filter, newest first, paging), open count and review all work on
 * it. Returns the requests it received; `reviewReply` can replace the review's answer.
 */
const mockAlertsApi = (initial: TransactionAlert[], reviewReply?: () => Response) => {
  let store = initial.map((a) => ({ ...a }));
  const requests: string[] = [];
  server.use(
    http.get(`${API}/api/admin/alerts/open-count`, () =>
      HttpResponse.json({ open: store.filter((a) => a.status === 'OPEN').length }),
    ),
    http.get(`${API}/api/admin/alerts`, ({ request }) => {
      const url = new URL(request.url);
      requests.push(`GET ${url.search}`);
      const status = url.searchParams.get('status');
      const page = Number(url.searchParams.get('page'));
      const size = Number(url.searchParams.get('size'));
      const matching = store
        .filter((a) => !status || a.status === status)
        .sort((a, b) => b.createdAt.localeCompare(a.createdAt));
      return HttpResponse.json({
        content: matching.slice(page * size, page * size + size),
        totalElements: matching.length,
        totalPages: Math.ceil(matching.length / size),
        number: page,
        size,
      });
    }),
    http.patch(`${API}/api/admin/alerts/:id/review`, ({ params }) => {
      requests.push(`PATCH ${params.id}`);
      if (reviewReply) return reviewReply();
      store = store.map((a) =>
        a.id === Number(params.id)
          ? {
              ...a,
              status: 'REVIEWED' as const,
              reviewedAt: '2026-11-22T01:00:00Z',
              reviewedBy: 'a1',
            }
          : a,
      );
      return HttpResponse.json(store.find((a) => a.id === Number(params.id)));
    }),
  );
  return requests;
};

const renderAlerts = () =>
  renderApp(paths.adminAlerts, {}, <p>Dashboard</p>, {
    [paths.adminAlerts]: (
      <RequireRole role="ADMIN">
        <AdminAlertsPage />
      </RequireRole>
    ),
  });

/** The data rows of the grid (without the header row). */
const rows = async () => {
  await screen.findByRole('grid');
  return screen.getAllByRole('row').slice(1);
};

beforeEach(() => {
  clearAccessToken();
  mockSession(admin);
});

describe('admin alerts page', () => {
  it('shows the open alerts by default, newest first, with the open count', async () => {
    const requests = mockAlertsApi([large, rapid, reviewed]);
    renderAlerts();

    expect(await screen.findByText('2 open alerts')).toBeInTheDocument();
    await screen.findByText('Rapid transfers');
    const [first, second] = await rows();
    expect(first).toHaveTextContent('Large transfer');
    expect(first).toHaveTextContent('Transfer of RM 12000.00 is at or above the RM 10000.00 limit');
    expect(first).toHaveTextContent('RM 12,000.00');
    expect(within(first).getByRole('link', { name: '1111 111 111' })).toHaveAttribute(
      'href',
      '/admin/accounts/1111111111/history',
    );
    expect(second).toHaveTextContent('6 transfers from this account within 10 minutes (limit 5)');
    expect(screen.getAllByRole('button', { name: /as reviewed/ })).toHaveLength(2);
    expect(requests).toContain('GET ?page=0&size=20&status=OPEN');
  });

  it('"Mark as reviewed" sends the review, the alert leaves the open list and the count drops', async () => {
    const requests = mockAlertsApi([large, rapid]);
    renderAlerts();
    const user = userEvent.setup();

    await user.click(await screen.findByRole('button', { name: 'Mark alert TXN-2 as reviewed' }));

    await waitFor(() => expect(requests).toContain('PATCH 2'));
    expect(await screen.findByText('Alert for TXN-2 marked as reviewed.')).toBeInTheDocument();
    expect(await screen.findByText('1 open alert')).toBeInTheDocument();
    await waitFor(() => expect(screen.queryByText('Large transfer')).not.toBeInTheDocument());
    expect(screen.getByText('Rapid transfers')).toBeInTheDocument();
  });

  it('the filter shows reviewed alerts (no button) or all of them', async () => {
    const requests = mockAlertsApi([large, reviewed]);
    renderAlerts();
    const user = userEvent.setup();
    await screen.findByText('Large transfer');

    await user.click(screen.getByRole('button', { name: 'Reviewed' }));

    await waitFor(() => expect(requests).toContain('GET ?page=0&size=20&status=REVIEWED'));
    // the open alert TXN-2 is gone; the reviewed one (also a large transfer) has a chip instead of a button
    await waitFor(() =>
      expect(
        screen.queryByRole('button', { name: 'Mark alert TXN-2 as reviewed' }),
      ).not.toBeInTheDocument(),
    );
    expect(await rows()).toHaveLength(1);
    expect(screen.getByText('Reviewed', { selector: '.MuiChip-label' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /as reviewed/ })).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'All' }));

    await waitFor(() => expect(requests).toContain('GET ?page=0&size=20'));
    expect(
      await screen.findByRole('button', { name: 'Mark alert TXN-2 as reviewed' }),
    ).toBeInTheDocument();
    expect(await rows()).toHaveLength(2);
  });

  it('says so when there is nothing to review', async () => {
    mockAlertsApi([reviewed]);
    renderAlerts();

    expect(await screen.findByText('No open alerts')).toBeInTheDocument();
    expect(screen.getByText('0 open alerts')).toBeInTheDocument();
  });

  it('pages on the server: the next page asks for page 1', async () => {
    const many = Array.from({ length: 25 }, (_, i) =>
      alert(i + 1, {
        createdAt: `2026-11-21T08:${String(i).padStart(2, '0')}:00Z`,
        reference: `TXN-${i + 1}`,
      }),
    );
    const requests = mockAlertsApi(many);
    renderAlerts();
    const user = userEvent.setup();
    await screen.findByText('25 open alerts');
    await rows();

    await user.click(screen.getByRole('button', { name: /next page/i }));

    await waitFor(() => expect(requests).toContain('GET ?page=1&size=20&status=OPEN'));
    expect(
      await screen.findByRole('button', { name: 'Mark alert TXN-1 as reviewed' }),
    ).toBeInTheDocument();
  });

  it('a failed review shows the reason and keeps the alert open', async () => {
    mockAlertsApi([large], () =>
      HttpResponse.json({ status: 404, message: 'Alert not found' }, { status: 404 }),
    );
    renderAlerts();
    const user = userEvent.setup();

    await user.click(await screen.findByRole('button', { name: 'Mark alert TXN-2 as reviewed' }));

    expect(await screen.findByText(/Alert not found|not found/i)).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: 'Mark alert TXN-2 as reviewed' }),
    ).toBeInTheDocument();
  });

  it('is not shown to a customer', async () => {
    mockSession(customer);
    renderAlerts();

    expect(await screen.findByText('Dashboard')).toBeInTheDocument();
    expect(screen.queryByText(/open alert/)).not.toBeInTheDocument();
  });
});
