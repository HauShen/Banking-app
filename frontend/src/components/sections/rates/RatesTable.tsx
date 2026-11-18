import {
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import type { ExchangeRate } from 'api/fx';
import { formatDateTime } from 'lib/dates';
import { formatRate } from 'lib/money';

/** MYR per 1 unit of each currency. "You pay" = the bank's sell rate; "You receive" = its buy rate. */
const RatesTable = ({ rates }: { rates: ExchangeRate[] }) => (
  <>
    <TableContainer>
      <Table aria-label="Exchange rates">
        <TableHead>
          <TableRow>
            <TableCell>Currency</TableCell>
            <TableCell align="right">You pay in it (RM per unit)</TableCell>
            <TableCell align="right">You receive it (RM per unit)</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {rates.map((rate) => (
            <TableRow key={rate.currency}>
              <TableCell sx={{ fontWeight: 600 }}>{rate.currency}</TableCell>
              <TableCell align="right">{formatRate(rate.sellRate)}</TableCell>
              <TableCell align="right">{formatRate(rate.buyRate)}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
    {rates[0] && (
      <Typography variant="caption" sx={{ color: 'text.secondary', display: 'block', mt: 1.5 }}>
        Rates as of {formatDateTime(rates[0].asOf)}. Paying in a foreign currency uses the first
        rate; money arriving from abroad uses the second.
      </Typography>
    )}
  </>
);

export default RatesTable;
