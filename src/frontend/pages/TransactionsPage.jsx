import React, { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useSearchParams } from "react-router-dom";
import { getTransactionsByAccount } from "../api/transactionApi";
import { useAuth } from "../hooks/useAuth";

function formatCurrency(amount, currency = "USD") {
  return new Intl.NumberFormat("en-US", { style: "currency", currency }).format(amount ?? 0);
}

function formatDate(instant) {
  return instant ? new Date(instant).toLocaleString() : "—";
}

export default function TransactionsPage() {
  const { user } = useAuth();
  const [searchParams] = useSearchParams();
  const accountNumber = searchParams.get("account");
  const [page, setPage] = useState(0);
  const PAGE_SIZE = 10;

  const { data, isLoading, isError } = useQuery({
    queryKey: ["transactions", accountNumber, page],
    queryFn: () => getTransactionsByAccount(accountNumber, page, PAGE_SIZE),
    enabled: !!accountNumber,
  });

  if (!accountNumber) return (
    <div className="container mt-4">
      <div className="alert alert-warning">
        No account selected. Go to <a href="/dashboard">Dashboard</a> and click "View Transactions".
      </div>
    </div>
  );

  if (isLoading) return <p className="p-4">Loading transactions...</p>;
  if (isError)   return <p className="p-4 text-danger">Failed to load transactions.</p>;

  const transactions = data?.content ?? [];
  const totalPages   = data?.totalPages ?? 1;

  return (
    <div className="container mt-4">
      <h2 className="mb-1">Transactions</h2>
      <p className="text-muted mb-4">Account: <strong>{accountNumber}</strong></p>

      {transactions.length === 0 ? (
        <div className="alert alert-info">No transactions found for this account.</div>
      ) : (
        <>
          <div className="table-responsive">
            <table className="table table-striped table-hover align-middle">
              <thead className="table-dark">
                <tr>
                  <th>Direction</th>
                  <th>From</th>
                  <th>To</th>
                  <th>Amount</th>
                  <th>Currency</th>
                  <th>Status</th>
                  <th>Description</th>
                  <th>Date</th>
                </tr>
              </thead>
              <tbody>
                {transactions.map((tx) => {
                  const isOutgoing = tx.fromAccountNumber === accountNumber;
                  return (
                    <tr key={tx.reference}>
                      {/* Direction indicator */}
                      <td>
                        {isOutgoing
                          ? <span className="text-danger fw-bold">▼ Out</span>
                          : <span className="text-success fw-bold">▲ In</span>}
                      </td>
                      <td className="font-monospace small">{tx.fromAccountNumber}</td>
                      <td className="font-monospace small">{tx.toAccountNumber}</td>
                      {/* Amount: red for outgoing, green for incoming */}
                      <td className={isOutgoing ? "text-danger fw-semibold" : "text-success fw-semibold"}>
                        {isOutgoing ? "−" : "+"}{formatCurrency(tx.amount, tx.currency)}
                      </td>
                      <td>{tx.currency ?? "—"}</td>
                      <td>
                        <span className={`badge ${
                          tx.status === "SUCCESS" ? "bg-success"
                          : tx.status === "FAILED"  ? "bg-danger"
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
          </div>

          {/* Pagination */}
          <div className="d-flex justify-content-between align-items-center mt-3">
            <span className="text-muted small">
              Page {page + 1} of {totalPages} &nbsp;|&nbsp; {data?.totalElements} transactions
            </span>
            <div className="btn-group">
              <button className="btn btn-sm btn-outline-secondary"
                onClick={() => setPage(p => Math.max(p - 1, 0))}
                disabled={page === 0}>
                ← Previous
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
  );
}