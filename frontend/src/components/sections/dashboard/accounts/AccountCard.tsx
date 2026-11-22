import {
  Card,
  CardActionArea,
  Chip,
  Grid,
  Palette,
  Stack,
  Typography,
  useTheme,
} from '@mui/material';
import type { Account } from 'api/types';
import BankLogoAlt from 'assets/bank-logo-alt.svg';
import BankLogo from 'assets/bank-logo.svg';
import ChipCardBlack from 'assets/chip_black.png';
import ChipCardWhite from 'assets/chip_white.png';
import Image from 'components/base/Image';
import { formatMoney } from 'lib/money';
import { formatAccountNumber } from 'lib/transactions';

const ACCOUNT_TYPE_LABEL: Record<Account['accountType'], string> = {
  SAVINGS: 'Savings',
  CURRENT: 'Current',
};

/** The template's card look: the selected account is blue, the others white. */
const themeStyles = (selected: boolean, palette: Palette) =>
  selected
    ? {
        cardBg: palette.gradients.blueGradient,
        textColor: palette.common.white,
        labelColor: palette.common.white,
        chipCard: ChipCardWhite,
        bankLogo: BankLogo,
        footerBg: palette.gradients.whiteGradient,
        footerBorder: 'none',
      }
    : {
        cardBg: palette.common.white,
        textColor: palette.primary.darker,
        labelColor: palette.primary.light,
        chipCard: ChipCardBlack,
        bankLogo: BankLogoAlt,
        footerBg: palette.gradients.whiteCardGradient,
        footerBorder: 1,
      };

interface AccountCardProps {
  account: Account;
  holderName: string;
  selected: boolean;
  onSelect: () => void;
}

const Label = ({ color, children }: { color: string; children: string }) => (
  <Typography sx={{ color, fontSize: { xs: 'overline.fontSize', md: 'caption.fontSize' } }}>
    {children}
  </Typography>
);

const AccountCard = ({ account, holderName, selected, onSelect }: AccountCardProps) => {
  const { palette } = useTheme();
  const { cardBg, textColor, labelColor, chipCard, bankLogo, footerBg, footerBorder } = themeStyles(
    selected,
    palette,
  );
  const locked = account.accountStatus === 'LOCKED';

  return (
    <Card
      sx={{
        flex: '1 1 0',
        minWidth: 320,
        overflow: 'hidden',
        background: cardBg,
        color: textColor,
        border: 1,
        borderColor: 'action.focus',
      }}
    >
      <CardActionArea
        onClick={onSelect}
        aria-pressed={selected}
        aria-label={`${ACCOUNT_TYPE_LABEL[account.accountType]} account ${account.accountNumber}`}
      >
        <Stack sx={{ gap: 4, px: { xs: 2.5, md: 3 }, pt: 3, pb: { xs: 2, md: 3 } }}>
          <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <Label color={labelColor}>Balance</Label>
              <Typography sx={{ fontSize: { xs: 'body2.fontSize', md: 'h4.fontSize' } }}>
                {formatMoney(account.balance, account.accountCurrency)}
              </Typography>
            </div>
            <Stack direction="row" alignItems="center" gap={1.5}>
              {locked && <Chip label="Locked" color="error" size="small" />}
              <Image src={chipCard} alt="" sx={{ width: { xs: 30, md: 35 } }} />
            </Stack>
          </Stack>
          <Grid container spacing={5}>
            <Grid item xs={6}>
              <Stack sx={{ gap: 0.5 }}>
                <Label color={labelColor}>ACCOUNT HOLDER</Label>
                <Typography
                  sx={{ fontSize: { xs: 'subtitle1.fontSize', md: 'body1.fontSize' } }}
                  fontWeight={600}
                  noWrap
                >
                  {holderName}
                </Typography>
              </Stack>
            </Grid>
            <Grid item xs={6}>
              <Stack sx={{ gap: 0.5 }}>
                <Label color={labelColor}>ACCOUNT TYPE</Label>
                <Typography
                  sx={{ fontSize: { xs: 'subtitle1.fontSize', md: 'body1.fontSize' } }}
                  fontWeight={600}
                >
                  {ACCOUNT_TYPE_LABEL[account.accountType]}
                </Typography>
              </Stack>
            </Grid>
          </Grid>
        </Stack>
        <Stack
          direction="row"
          gap={2}
          sx={{
            background: footerBg,
            justifyContent: 'space-between',
            alignItems: 'center',
            mt: 1.25,
            px: 3,
            pt: { xs: 2, md: 3 },
            pb: { xs: 2, md: 4 },
            borderTop: footerBorder,
            borderColor: 'action.focus',
          }}
        >
          <Typography sx={{ fontSize: { xs: 'body1.fontSize', md: 'h3.fontSize' } }}>
            {formatAccountNumber(account.accountNumber)}
          </Typography>
          <Image src={bankLogo} alt="" sx={{ width: 50 }} />
        </Stack>
      </CardActionArea>
    </Card>
  );
};

export default AccountCard;
