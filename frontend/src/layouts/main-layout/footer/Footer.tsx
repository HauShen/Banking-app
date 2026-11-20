import { Box, Link, Stack, Typography } from '@mui/material';

const links = [
  { href: 'https://github.com/HauShen/Banking-app', title: 'Source on GitHub' },
  { href: 'https://github.com/themewagon/bankdash', title: 'UI: BankDash by ThemeWagon' },
];

const Footer = () => (
  <Box component="footer" mt="auto" pt={2.5} pb={2.5}>
    <Stack
      direction={{ xs: 'column', lg: 'row' }}
      justifyContent="space-between"
      alignItems="center"
      gap={1}
    >
      <Typography sx={{ fontSize: { xs: 'caption.fontSize', md: 'body2.fontSize' } }}>
        &copy; {new Date().getFullYear()} Banking App · a demo bank, no real money moves
      </Typography>
      <Stack direction="row" flexWrap="wrap" justifyContent="center" gap={3}>
        {links.map((link) => (
          <Link
            key={link.href}
            href={link.href}
            target="_blank"
            rel="noopener noreferrer"
            color="text.secondary"
            sx={{
              fontSize: { xs: 'caption.fontSize', md: 'body2.fontSize' },
              '&:hover': { color: 'primary.main' },
            }}
          >
            {link.title}
          </Link>
        ))}
      </Stack>
    </Stack>
  </Box>
);

export default Footer;
