import { SvgIconProps } from '@mui/material';
import type { Role } from 'api/types';
import AdminIcon from 'components/icons/menu-icons/AdminIcon';
import ExchangeIcon from 'components/icons/menu-icons/ExchangeIcon';
import HomeIcon from 'components/icons/menu-icons/HomeIcon';
import SendIcon from 'components/icons/menu-icons/SendIcon';
import SettingsIcon from 'components/icons/menu-icons/SettingsIcon';
import TransferIcon from 'components/icons/menu-icons/TransferIcon';
import UserIcon from 'components/icons/menu-icons/UserIcon';

export enum linkEnum {
  Dashboard = 'dashboard',
  Transfer = 'transfer',
  Transactions = 'transactions',
  Rates = 'exchange rates',
  Accounts = 'accounts',
  Profile = 'profile',
  Users = 'users',
}

export interface MenuLinkType {
  id: number;
  title: string;
  link: string;
  icon?: (props: SvgIconProps) => JSX.Element;
  available: boolean;
  /** Only these roles see the item; undefined = everyone. */
  roles?: Role[];
}
export const menuLinks: MenuLinkType[] = [
  {
    id: 1,
    title: linkEnum.Dashboard,
    link: '/',
    icon: HomeIcon,
    available: true,
  },
  {
    id: 6,
    title: linkEnum.Transfer,
    link: '/transfer',
    icon: SendIcon,
    available: true,
  },
  {
    id: 2,
    title: linkEnum.Transactions,
    link: '/transactions',
    icon: TransferIcon,
    available: true,
  },
  {
    id: 7,
    title: linkEnum.Rates,
    link: '/rates',
    icon: ExchangeIcon,
    available: true,
  },
  {
    id: 3,
    title: linkEnum.Accounts,
    link: '/admin/accounts',
    icon: UserIcon,
    available: true,
    roles: ['ADMIN'],
  },
  {
    id: 4,
    title: linkEnum.Profile,
    link: '/profile',
    icon: SettingsIcon,
    available: true,
  },
  {
    id: 5,
    title: linkEnum.Users,
    link: '/admin/users',
    icon: AdminIcon,
    available: true,
    roles: ['ADMIN'],
  },
];

/** The menu items a user with this role may see. */
export const visibleMenuLinks = (role: Role | undefined): MenuLinkType[] =>
  menuLinks.filter((link) => !link.roles || (role !== undefined && link.roles.includes(role)));
