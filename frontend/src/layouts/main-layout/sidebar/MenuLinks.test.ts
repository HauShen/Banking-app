import { describe, expect, it } from 'vitest';
import { linkEnum, visibleMenuLinks } from './MenuLinks';

const titles = (role: Parameters<typeof visibleMenuLinks>[0]) =>
  visibleMenuLinks(role).map((link) => link.title);

const ADMIN_ONLY: string[] = [linkEnum.Users, linkEnum.Accounts];

describe('visibleMenuLinks', () => {
  it('shows the admin pages (users, accounts) to admins', () => {
    expect(titles('ADMIN')).toEqual(expect.arrayContaining(ADMIN_ONLY));
  });

  it('hides them from customers and while the user is unknown', () => {
    for (const title of ADMIN_ONLY) {
      expect(titles('CUSTOMER')).not.toContain(title);
      expect(titles(undefined)).not.toContain(title);
    }
  });

  it('shows the customer pages to everyone', () => {
    expect(titles('CUSTOMER')).toEqual(titles('ADMIN').filter((t) => !ADMIN_ONLY.includes(t)));
    expect(titles('CUSTOMER')).toEqual(
      expect.arrayContaining([linkEnum.Dashboard, linkEnum.Transfer, linkEnum.Profile]),
    );
  });
});
