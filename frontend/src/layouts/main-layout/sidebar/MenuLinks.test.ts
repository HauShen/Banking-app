import { describe, expect, it } from 'vitest';
import { linkEnum, visibleMenuLinks } from './MenuLinks';

const titles = (role: Parameters<typeof visibleMenuLinks>[0]) =>
  visibleMenuLinks(role).map((link) => link.title);

const ADMIN_ONLY: string[] = [linkEnum.Users, linkEnum.Accounts, linkEnum.Alerts];
/** Moving money is for customers: admins don't transfer or see the customer transaction pages. */
const CUSTOMER_ONLY: string[] = [linkEnum.Transfer, linkEnum.Transactions];
const EVERYONE: string[] = [linkEnum.Dashboard, linkEnum.Rates, linkEnum.Profile];

describe('visibleMenuLinks', () => {
  it('shows the admin pages (users, accounts, alerts) to admins', () => {
    expect(titles('ADMIN')).toEqual(expect.arrayContaining(ADMIN_ONLY));
  });

  it('hides them from customers and while the user is unknown', () => {
    for (const title of ADMIN_ONLY) {
      expect(titles('CUSTOMER')).not.toContain(title);
      expect(titles(undefined)).not.toContain(title);
    }
  });

  it('shows transfer and transactions to customers only', () => {
    expect(titles('CUSTOMER')).toEqual(expect.arrayContaining(CUSTOMER_ONLY));
    for (const title of CUSTOMER_ONLY) {
      expect(titles('ADMIN')).not.toContain(title);
      expect(titles(undefined)).not.toContain(title);
    }
  });

  it('shows dashboard, exchange rates and profile to everyone', () => {
    expect(titles('CUSTOMER')).toEqual(expect.arrayContaining(EVERYONE));
    expect(titles('ADMIN')).toEqual(expect.arrayContaining(EVERYONE));
  });

  it('gives each role nothing beyond its own pages', () => {
    expect([...titles('CUSTOMER')].sort()).toEqual([...EVERYONE, ...CUSTOMER_ONLY].sort());
    expect([...titles('ADMIN')].sort()).toEqual([...EVERYONE, ...ADMIN_ONLY].sort());
  });
});
