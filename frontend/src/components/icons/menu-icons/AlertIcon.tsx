import { SvgIcon, SvgIconProps } from '@mui/material';

/** Warning triangle: transaction alerts. */
const AlertIcon = (props: SvgIconProps) => {
  return (
    <SvgIcon {...props}>
      <path d="M1 21h22L12 2 1 21Zm12-3h-2v-2h2v2Zm0-4h-2v-4h2v4Z" />
    </SvgIcon>
  );
};

export default AlertIcon;
