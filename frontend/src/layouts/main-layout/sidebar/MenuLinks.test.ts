import { describe, expect, it } from 'vitest';
import { linkEnum, visibleMenuLinks } from './MenuLinks';

const titles = (role: Parameters<typeof visibleMenuLinks>[0]) =>
  visibleMenuLinks(role).map((link) => link.title);

describe('visibleMenuLinks', () => {
  it('shows the admin item to admins', () => {
    expect(titles('ADMIN')).toContain(linkEnum.Admin);
  });

  it('hides the admin item from customers and while the user is unknown', () => {
    expect(titles('CUSTOMER')).not.toContain(linkEnum.Admin);
    expect(titles(undefined)).not.toContain(linkEnum.Admin);
  });

  it('shows the common items to everyone', () => {
    expect(titles('CUSTOMER')).toEqual(titles('ADMIN').filter((t) => t !== linkEnum.Admin));
    expect(titles('CUSTOMER')).toContain(linkEnum.Dashboard);
  });
});
