import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import Footer from './Footer';

describe('footer', () => {
  it('links to the source code and credits the UI template, both in a new tab', () => {
    render(<Footer />);

    expect(screen.getByRole('link', { name: 'Source on GitHub' })).toHaveAttribute(
      'href',
      'https://github.com/HauShen/Banking-app',
    );
    const credit = screen.getByRole('link', { name: 'UI: BankDash by ThemeWagon' });
    expect(credit).toHaveAttribute('href', 'https://github.com/themewagon/bankdash');
    expect(credit).toHaveAttribute('target', '_blank');
    expect(credit).toHaveAttribute('rel', 'noopener noreferrer');
  });

  it('says that this is a demo, without template placeholder text', () => {
    render(<Footer />);

    expect(screen.getByText(/a demo bank, no real money moves/)).toBeInTheDocument();
    expect(screen.queryByText(/your company/i)).not.toBeInTheDocument();
  });
});
