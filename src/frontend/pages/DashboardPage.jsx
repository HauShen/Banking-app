import React from "react";
import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { getAccountSummary } from "../api/accountApi";
import { useAuth } from "../hooks/useAuth";

function formatCurrency(amount, currency = "USD") {
  return new Intl.NumberFormat("en-US", { style: "currency", currency }).format(amount);
}

function statusBadge(status) {
  const color = status === "ACTIVE" ? "success" : "danger";
  return <span className={`badge bg-${color}`}>{status}</span>;
}

export default function DashboardPage() {
  const { user } = useAuth();

  const { data, isLoading, isError } = useQuery({
    queryKey: ["account-summary"],
    queryFn: getAccountSummary,
  });

  if (isLoading) return <p className="p-4">Loading dashboard...</p>;
  if (isError)   return <p className="p-4 text-danger">Failed to load dashboard.</p>;

  const accounts = data?.accounts ?? [];

  return (
    <div className="container mt-4">
      {/* Welcome header */}
      <h2 className="mb-1">Welcome, {user?.fullName ?? user?.username ?? "User"}</h2>
      <p className="text-muted mb-4">Here's an overview of your accounts.</p>

      {/* Summary cards */}
      <div className="row g-3 mb-4">
        <div className="col-md-4">
          <div className="card p-3 shadow-sm border-0 bg-primary text-white">
            <h6>Total Balance</h6>
            <h3>{formatCurrency(data?.balance ?? 0)}</h3>
          </div>
        </div>
        <div className="col-md-4">
          <div className="card p-3 shadow-sm border-0">
            <h6 className="text-muted">Total Accounts</h6>
            <h3>{accounts.length}</h3>
          </div>
        </div>
        <div className="col-md-4">
          <div className="card p-3 shadow-sm border-0">
            <h6 className="text-muted">Quick Actions</h6>
            <Link to="/transfer" className="btn btn-sm btn-outline-primary w-100">
              + New Transfer
            </Link>
          </div>
        </div>
      </div>

      {/* Accounts list */}
      <h5 className="mb-3">Your Accounts</h5>
      {accounts.length === 0 ? (
        <div className="alert alert-info">
          You have no accounts yet. Contact your admin to create one.
        </div>
      ) : (
        <div className="row g-3">
          {accounts.map((acc) => (
            <div className="col-md-6" key={acc.accountId}>
              <div className="card p-3 shadow-sm h-100">
                <div className="d-flex justify-content-between align-items-center mb-2">
                  <span className="fw-bold">{acc.accountType}</span>
                  {statusBadge(acc.accountStatus)}
                </div>
                <div className="text-muted small mb-1">
                  Account No: <span className="fw-semibold text-dark">{acc.accountNumber}</span>
                </div>
                <div className="text-muted small mb-2">
                  Currency: <span className="fw-semibold text-dark">{acc.accountCurrency}</span>
                </div>
                <h4 className="mb-3">{formatCurrency(acc.balance ?? 0, acc.accountCurrency)}</h4>
                <Link
                  to={`/transactions?account=${acc.accountNumber}`}
                  className="btn btn-sm btn-outline-secondary"
                >
                  View Transactions
                </Link>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}