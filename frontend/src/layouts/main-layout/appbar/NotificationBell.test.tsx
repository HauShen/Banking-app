import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useAccountSummary } from 'api/accounts';
import { clearAccessToken } from 'api/tokenStore';
import type { AppNotification } from 'api/types';
import { http, HttpResponse } from 'msw';
import { mockSession, renderApp } from 'test/renderApp';
import { API, server } from 'test/server';
import { beforeEach, describe, expect, it } from 'vitest';
import NotificationBell from './NotificationBell';

const customer = {
  id: 'u1',
  username: 'hau',
  fullName: 'Hau Shen',
  email: 'h@b.local',
  role: 'CUSTOMER',
};

const received: AppNotification = {
  id: 2,
  type: 'MONEY_RECEIVED',
  message: 'You received RM 30.00 from account ****0001.',
  reference: 'TXN-2',
  amount: 30,
  createdAt: '2026-11-21T08:05:00Z',
  read: false,
};
const deposited: AppNotification = {
  id: 1,
  type: 'CASH_DEPOSITED',
  message: 'RM 50.00 was deposited into your account ****0002.',
  reference: 'CDM-1',
  amount: 50,
  createdAt: '2026-11-21T08:00:00Z',
  read: true,
};

/**
 * A small notification backend: the list and the count follow the PATCH / POST calls, and `arrive()` adds a new
 * unread notification as if Kafka had just delivered one. Returns the calls it received.
 */
const mockNotifications = (initial: AppNotification[]) => {
  let rows = initial.map((n) => ({ ...n }));
  const calls: string[] = [];
  server.use(
    http.get(`${API}/api/notifications/unread-count`, () =>
      HttpResponse.json({ unread: rows.filter((n) => !n.read).length }),
    ),
    http.get(`${API}/api/notifications`, ({ request }) => {
      calls.push(`GET ${new URL(request.url).search}`);
      return HttpResponse.json({
        content: rows,
        totalElements: rows.length,
        totalPages: 1,
        number: 0,
        size: 10,
      });
    }),
    http.patch(`${API}/api/notifications/:id/read`, ({ params }) => {
      calls.push(`PATCH ${params.id}`);
      rows = rows.map((n) => (n.id === Number(params.id) ? { ...n, read: true } : n));
      return new HttpResponse(null, { status: 204 });
    }),
    http.post(`${API}/api/notifications/read-all`, () => {
      calls.push('POST read-all');
      const marked = rows.filter((n) => !n.read).length;
      rows = rows.map((n) => ({ ...n, read: true }));
      return HttpResponse.json({ marked });
    }),
  );
  return {
    calls,
    arrive: (notification: AppNotification) => {
      rows = [notification, ...rows];
    },
  };
};

const openBell = async () => {
  const user = userEvent.setup();
  await user.click(await screen.findByRole('button', { name: /^notifications/i }));
  return { user, list: await screen.findByRole('list', { name: 'Latest notifications' }) };
};

beforeEach(() => {
  clearAccessToken();
  mockSession(customer);
});

describe('notification bell', () => {
  it('shows the number of unread notifications on the bell', async () => {
    mockNotifications([received, deposited]);
    renderApp('/', {}, <NotificationBell />);

    expect(
      await screen.findByRole('button', { name: 'Notifications, 1 unread' }),
    ).toBeInTheDocument();
  });

  it('opens the latest notifications, newest first, with the unread one marked', async () => {
    const backend = mockNotifications([received, deposited]);
    renderApp('/', {}, <NotificationBell />);

    const { list } = await openBell();

    const items = within(list).getAllByRole('button');
    expect(items).toHaveLength(2);
    expect(items[0]).toHaveTextContent('Unread: You received RM 30.00 from account ****0001.');
    expect(items[1]).toHaveTextContent('RM 50.00 was deposited into your account ****0002.');
    expect(items[1]).not.toHaveTextContent('Unread');
    expect(backend.calls).toEqual(['GET ?page=0&size=10']);
  });

  it('opening an unread notification marks it as read and the badge goes away', async () => {
    const backend = mockNotifications([received, deposited]);
    renderApp('/', {}, <NotificationBell />);
    const { user, list } = await openBell();

    await user.click(within(list).getByText(/You received RM 30.00/));

    await waitFor(() => expect(backend.calls).toContain('PATCH 2'));
    // while the dropdown is open, MUI hides the rest of the page from screen readers: hence hidden: true
    expect(
      await screen.findByRole('button', { name: 'Notifications', hidden: true }),
    ).toBeInTheDocument();
    await waitFor(() =>
      expect(within(list).getAllByRole('button')[0]).not.toHaveTextContent('Unread'),
    );
  });

  it('opening a notification that is already read sends nothing', async () => {
    const backend = mockNotifications([deposited]);
    renderApp('/', {}, <NotificationBell />);
    const { user, list } = await openBell();

    await user.click(within(list).getByText(/RM 50.00 was deposited/));

    expect(backend.calls).toEqual(['GET ?page=0&size=10']);
  });

  it('"Mark all as read" marks everything; it is disabled when nothing is unread', async () => {
    const backend = mockNotifications([received, { ...received, id: 3, reference: 'TXN-3' }]);
    renderApp('/', {}, <NotificationBell />);
    const { user } = await openBell();
    const markAll = screen.getByRole('button', { name: 'Mark all as read' });

    await user.click(markAll);

    await waitFor(() => expect(backend.calls).toContain('POST read-all'));
    await waitFor(() => expect(markAll).toBeDisabled());
    expect(
      await screen.findByRole('button', { name: 'Notifications', hidden: true }),
    ).toBeInTheDocument();
  });

  it('says so when there are no notifications yet', async () => {
    mockNotifications([]);
    renderApp('/', {}, <NotificationBell />);

    await userEvent.setup().click(await screen.findByRole('button', { name: 'Notifications' }));

    expect(await screen.findByText('No notifications yet')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Mark all as read' })).toBeDisabled();
  });

  it('keeps the disabled "Mark all as read" label readable (no grey box hiding the text)', async () => {
    mockNotifications([]);
    renderApp('/', {}, <NotificationBell />);

    await userEvent.setup().click(await screen.findByRole('button', { name: 'Notifications' }));
    await screen.findByText('No notifications yet');

    const markAll = screen.getByRole('button', { name: 'Mark all as read' });
    expect(markAll).toBeDisabled();
    const { color, backgroundColor } = getComputedStyle(markAll);
    expect(backgroundColor).toMatch(/^(transparent|rgba\(0, 0, 0, 0\))$/);
    expect(color).not.toBe(backgroundColor);
  });

  it('when the list fails to load: a message and "Try again", which loads it', async () => {
    mockNotifications([received]);
    let fail = true;
    server.use(
      http.get(`${API}/api/notifications`, () =>
        fail
          ? HttpResponse.json({ status: 400, message: 'Bad request' }, { status: 400 })
          : HttpResponse.json({
              content: [received],
              totalElements: 1,
              totalPages: 1,
              number: 0,
              size: 10,
            }),
      ),
    );
    renderApp('/', {}, <NotificationBell />);
    const user = userEvent.setup();
    await user.click(await screen.findByRole('button', { name: /^notifications/i }));

    expect(await screen.findByText("Couldn't load your notifications.")).toBeInTheDocument();
    fail = false;
    await user.click(screen.getByRole('button', { name: 'Try again' }));

    expect(await screen.findByText(/You received RM 30.00/)).toBeInTheDocument();
  });

  it('polls the count: a new notification shows up on the bell and the balances are re-fetched', async () => {
    const backend = mockNotifications([deposited]);
    let summaryRequests = 0;
    server.use(
      http.get(`${API}/api/accounts/summary`, () => {
        summaryRequests += 1;
        return HttpResponse.json({ balance: 100, accountType: 'SAVINGS', accounts: [] });
      }),
    );
    /** Stands in for the dashboard: anything that shows balances uses this query. */
    const SummaryUser = () => {
      useAccountSummary();
      return null;
    };
    renderApp(
      '/',
      {},
      <>
        <NotificationBell pollMs={100} />
        <SummaryUser />
      </>,
    );
    await screen.findByRole('button', { name: 'Notifications' });
    await waitFor(() => expect(summaryRequests).toBe(1));

    backend.arrive(received); // Kafka delivered a transfer to this user

    expect(
      await screen.findByRole('button', { name: 'Notifications, 1 unread' }),
    ).toBeInTheDocument();
    await waitFor(() => expect(summaryRequests).toBe(2));
  });
});
