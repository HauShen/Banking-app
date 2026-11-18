import { describe, expect, it } from 'vitest';
import { linkEnum, visibleMenuLinks } from './MenuLinks';

const titles = (role: Parameters<typeof visibleMenuLinks>[0]) =>
  visibleMenuLinks(role).map((link) => link.title);

describe('visibleMenuLinks', () => {
  it('shows the admin-only users item to admins', () => {
    expect(titles('ADMIN')).toContain(linkEnum.Users);
  });

  it('hides the admin-only users item from customers and while the user is unknown', () => {
    expect(titles('CUSTOMER')).not.toContain(linkEnum.Users);
    expect(titles(undefined)).not.toContain(linkEnum.Users);
  });

  it('shows the common items to everyone', () => {
    expect(titles('CUSTOMER')).toEqual(titles('ADMIN').filter((t) => t !== linkEnum.Users));
    expect(titles('CUSTOMER')).toContain(linkEnum.Dashboard);
  });
});
