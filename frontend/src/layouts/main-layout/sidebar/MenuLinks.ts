import { SvgIconProps } from '@mui/material';
import type { Role } from 'api/types';
import AdminIcon from 'components/icons/menu-icons/AdminIcon';
import HomeIcon from 'components/icons/menu-icons/HomeIcon';
import SettingsIcon from 'components/icons/menu-icons/SettingsIcon';
import TransferIcon from 'components/icons/menu-icons/TransferIcon';
import UserIcon from 'components/icons/menu-icons/UserIcon';

export enum linkEnum {
  Dashboard = 'dashboard',
  Transactions = 'transactions',
  Accounts = 'accounts',
  Setting = 'Setting',
  Admin = 'admin',
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
    id: 2,
    title: linkEnum.Transactions,
    link: '#!',
    icon: TransferIcon,
    available: false,
  },
  {
    id: 3,
    title: linkEnum.Accounts,
    link: '#!',
    icon: UserIcon,
    available: false,
  },
  {
    id: 4,
    title: linkEnum.Setting,
    link: '#!',
    icon: SettingsIcon,
    available: false,
  },
  {
    id: 5,
    title: linkEnum.Admin,
    link: '/admin',
    icon: AdminIcon,
    available: false, // admin pages come in phase F3
    roles: ['ADMIN'],
  },
];

/** The menu items a user with this role may see. */
export const visibleMenuLinks = (role: Role | undefined): MenuLinkType[] =>
  menuLinks.filter((link) => !link.roles || (role !== undefined && link.roles.includes(role)));
