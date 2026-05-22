import React, { useState } from "react";
import { useParams, Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { getAccountsByUserId } from "../../api/accountApi";
import { getTransactionsByAccount } from "../../api/transactionApi";

function formatCurrency(amount, currency = "USD") {
  return new Intl.NumberFormat("en-US", { style: "currency", currency }).format(amount ?? 0);
}
function formatDate(instant) {
  return instant ? new Date(instant).toLocaleString() : "—";
}

// ── Transactions sub-panel for one account ───────────────────────
function AccountTransactions({ account }) {
  const [page, setPage] = useState(0);
  const [open, setOpen] = useState(false);
  const PAGE_SIZE = 5;

  const { data, isLoading } = useQuery({
    queryKey: ["admin-txns", account.accountNumber, page],
    queryFn: () => getTransactionsByAccount(account.accountNumber, page, PAGE_SIZE),
    enabled: open,   // only fetch when expanded
  });

  const transactions = data?.content ?? [];
  const totalPages   = data?.totalPages ?? 1;

  return (
    <div className="mt-2">
      <button className="btn btn-sm btn-outline-secondary"
        onClick={() => setOpen(o => !o)}>
        {open ? "▲ Hide Transactions" : "▼ View Transactions"}
      </button>

      {open && (
        <div className="mt-2">
          {isLoading ? <p className="small">Loading...</p> : (
            <>
              {transactions.length === 0 ? (
                <p className="text-muted small">No transactions found.</p>
              ) : (
                <table className="table table-sm table-bordered mt-2">
                  <thead className="table-secondary">
                    <tr>
                      <th>Dir</th>
                      <th>From</th>
                      <th>To</th>
                      <th>Amount</th>
                      <th>Status</th>
                      <th>Description</th>
                      <th>Date</th>
                    </tr>
                  </thead>
                  <tbody>
                    {transactions.map((tx) => {
                      const isOut = tx.fromAccountNumber === account.accountNumber;
                      return (
                        <tr key={tx.reference}>
                          <td>{isOut
                            ? <span className="text-danger fw-bold">▼ Out</span>
                            : <span className="text-success fw-bold">▲ In</span>}
                          </td>
                          <td className="font-monospace small">{tx.fromAccountNumber}</td>
                          <td className="font-monospace small">{tx.toAccountNumber}</td>
                          <td className={isOut ? "text-danger" : "text-success"}>
                            {isOut ? "−" : "+"}{formatCurrency(tx.amount, tx.currency)}
                          </td>
                          <td>
                            <span className={`badge ${
                              tx.status === "SUCCESS" ? "bg-success"
                              : tx.status === "FAILED" ? "bg-danger"
                              : "bg-warning text-dark"}`}>
                              {tx.status}
                            </span>
                          </td>
                          <td>{tx.description ?? "—"}</td>
                          <td className="small text-muted">{formatDate(tx.createdAt)}</td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              )}

              {/* Pagination */}
              <div className="d-flex justify-content-between align-items-center">
                <span className="text-muted small">
                  Page {page + 1} of {totalPages} | {data?.totalElements} total
                </span>
                <div className="btn-group">
                  <button className="btn btn-sm btn-outline-secondary"
                    onClick={() => setPage(p => Math.max(p - 1, 0))} disabled={page === 0}>
                    ← Prev
                  </button>
                  <button className="btn btn-sm btn-outline-secondary"
                    onClick={() => setPage(p => Math.min(p + 1, totalPages - 1))}
                    disabled={page >= totalPages - 1}>
                    Next →
                  </button>
                </div>
              </div>
            </>
          )}
        </div>
      )}
    </div>
  );
}

// ── Main page ────────────────────────────────────────────────────
export default function UserDetailPage() {
  const { userId } = useParams();

  const { data: accounts, isLoading, isError } = useQuery({
    queryKey: ["admin-user-accounts", userId],
    queryFn: () => getAccountsByUserId(userId),
  });

  return (
    <div className="container mt-4">
      {/* Back button */}
      <Link to="/admin/users" className="btn btn-sm btn-outline-secondary mb-3">
        ← Back to Users
      </Link>

      <h2 className="mb-4">User Accounts & Transactions</h2>

      {isLoading && <p>Loading accounts...</p>}
      {isError   && <p className="text-danger">Failed to load accounts.</p>}

      {!isLoading && !isError && (accounts ?? []).length === 0 && (
        <div className="alert alert-info">This user has no accounts yet.</div>
      )}

      <div className="row g-3">
        {(accounts ?? []).map((acc) => (
          <div className="col-12" key={acc.accountId}>
            <div className="card p-3 shadow-sm">
              {/* Account header */}
              <div className="d-flex justify-content-between align-items-start mb-2">
                <div>
                  <span className="fw-bold fs-5">{acc.accountType}</span>
                  <span className={`badge ms-2 ${acc.accountStatus === "ACTIVE" ? "bg-success" : "bg-danger"}`}>
                    {acc.accountStatus}
                  </span>
                </div>
                <h4 className="mb-0">{formatCurrency(acc.balance ?? 0, acc.accountCurrency)}</h4>
              </div>

              {/* Account details */}
              <div className="row g-2 text-muted small mb-3">
                <div className="col-auto">
                  Account No: <span className="fw-semibold text-dark font-monospace">{acc.accountNumber}</span>
                </div>
                <div className="col-auto">
                  Currency: <span className="fw-semibold text-dark">{acc.accountCurrency}</span>
                </div>
              </div>

              {/* Expandable transactions */}
              <AccountTransactions account={acc} />
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}