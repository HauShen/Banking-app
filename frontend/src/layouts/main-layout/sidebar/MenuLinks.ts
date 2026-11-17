import { SvgIconProps } from '@mui/material';
import HomeIcon from 'components/icons/menu-icons/HomeIcon';
import SettingsIcon from 'components/icons/menu-icons/SettingsIcon';
import TransferIcon from 'components/icons/menu-icons/TransferIcon';
import UserIcon from 'components/icons/menu-icons/UserIcon';

export enum linkEnum {
  Dashboard = 'dashboard',
  Transactions = 'transactions',
  Accounts = 'accounts',
  Setting = 'Setting',
}

export interface MenuLinkType {
  id: number;
  title: string;
  link: string;
  icon?: (props: SvgIconProps) => JSX.Element;
  available: boolean;
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
];
