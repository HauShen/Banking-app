import React, { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { getAccountSummary, topUpAccount } from "../api/accountApi";
import { useAuth } from "../hooks/useAuth";

function formatCurrency(amount, currency = "USD") {
  return new Intl.NumberFormat("en-US", { style: "currency", currency }).format(amount ?? 0);
}
function statusBadge(status) {
  return <span className={`badge bg-${status === "ACTIVE" ? "success" : "danger"}`}>{status}</span>;
}

function AdminDashboard({ user }) {
  return (
    <div className="container mt-4">
      <h2 className="mb-1">Admin Dashboard</h2>
      <p className="text-muted mb-4">Manage users and accounts.</p>
      <div className="row g-3">
        <div className="col-md-4">
          <div className="card p-3 shadow-sm border-0 bg-dark text-white">
            <h6>Role</h6><h4>Administrator</h4>
          </div>
        </div>
        <div className="col-md-4">
          <div className="card p-3 shadow-sm border-0">
            <h6 className="text-muted">Quick Actions</h6>
            <div className="d-flex flex-column gap-2">
              <Link to="/admin/users"    className="btn btn-sm btn-outline-primary">Manage Users</Link>
              <Link to="/admin/accounts" className="btn btn-sm btn-outline-secondary">Manage Accounts</Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function TopUpModal({ account, onClose, onSuccess }) {
  const [amount, setAmount] = useState("");
  const mutation = useMutation({
    mutationFn: () => topUpAccount(account.accountId, parseFloat(amount)),
    onSuccess: () => { onSuccess(); onClose(); },
    onError: (e) => alert(e?.response?.data?.message || "Top-up failed"),
  });

  return (
    <div className="modal d-block" style={{ background: "rgba(0,0,0,0.4)" }}>
      <div className="modal-dialog modal-dialog-centered">
        <div className="modal-content p-4">
          <h5 className="mb-1">Top Up Account</h5>
          <p className="text-muted small mb-3">{account.accountNumber}</p>
          <div className="mb-3">
            <label className="form-label">Amount</label>
            <input className="form-control" type="number" min="1" step="0.01"
              placeholder="Enter amount" value={amount}
              onChange={e => setAmount(e.target.value)} />
          </div>
          <div className="d-flex gap-2">
            <button className="btn btn-primary" disabled={!amount || mutation.isPending}
              onClick={() => mutation.mutate()}>
              {mutation.isPending ? "Processing..." : "Top Up"}
            </button>
            <button className="btn btn-outline-secondary" onClick={onClose}>Cancel</button>
          </div>
        </div>
      </div>
    </div>
  );
}

function CustomerDashboard({ user }) {
  const queryClient = useQueryClient();
  const [topUpTarget, setTopUpTarget] = useState(null);

  const { data, isLoading, isError } = useQuery({
    queryKey: ["account-summary"],
    queryFn: getAccountSummary,
  });

  if (isLoading) return <p className="p-4">Loading dashboard...</p>;
  if (isError)   return <p className="p-4 text-danger">Failed to load dashboard.</p>;

  const accounts = data?.accounts ?? [];

  return (
    <div className="container mt-4">
      <p className="text-muted mb-4">Here's an overview of your accounts.</p>
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
            <Link to="/transfer" className="btn btn-sm btn-outline-primary w-100">+ New Transfer</Link>
          </div>
        </div>
      </div>

      <h5 className="mb-3">Your Accounts</h5>
      {accounts.length === 0 ? (
        <div className="alert alert-info">You have no accounts yet. Contact your admin to create one.</div>
      ) : (
        <div className="row g-3">
          {accounts.map((acc) => (
            <div className="col-md-6" key={acc.accountId}>
              <div className="card p-3 shadow-sm h-100">
                <div className="d-flex justify-content-between align-items-center mb-2">
                  <span className="fw-bold">{acc.accountType}</span>
                  {statusBadge(acc.accountStatus)}
                </div>
                <div className="text-muted small mb-1">Account No: <span className="fw-semibold text-dark">{acc.accountNumber}</span></div>
                <div className="text-muted small mb-2">Currency: <span className="fw-semibold text-dark">{acc.accountCurrency}</span></div>
                <h4 className="mb-3">{formatCurrency(acc.balance ?? 0, acc.accountCurrency)}</h4>
                <div className="d-flex gap-2">
                  <Link to={`/transactions?account=${acc.accountNumber}`} className="btn btn-sm btn-outline-secondary flex-grow-1">
                    View Transactions
                  </Link>
                  {/* ✅ Top-Up button — only for ACTIVE accounts */}
                  {acc.accountStatus === "ACTIVE" && (
                    <button className="btn btn-sm btn-outline-success flex-grow-1"
                      onClick={() => setTopUpTarget(acc)}>
                      + Top Up
                    </button>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Top-Up Modal */}
      {topUpTarget && (
        <TopUpModal
          account={topUpTarget}
          onClose={() => setTopUpTarget(null)}
          onSuccess={() => queryClient.invalidateQueries(["account-summary"])}
        />
      )}
    </div>
  );
}

export default function DashboardPage() {
  const { user } = useAuth();
  const isAdmin = user?.role === "ADMIN";
  return (
    <div>
      <div className="container mt-4">
        <h2 className="mb-3">Welcome, {user?.fullName ?? user?.username ?? "User"}</h2>
      </div>
      {isAdmin ? <AdminDashboard user={user} /> : <CustomerDashboard user={user} />}
    </div>
  );
}