import React from "react";
import { useQuery } from "@tanstack/react-query";
import { getAccountSummary } from "../api/accountApi";

export default function DashboardPage() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ["account-summary"],
    queryFn: getAccountSummary,
  });

  if (isLoading) return <p>Loading dashboard...</p>;
  if (isError) return <p>Failed to load dashboard.</p>;

  return (
    <div className="container mt-4">
      <h2 className="mb-4">Dashboard</h2>
      <div className="row g-3">
        <div className="col-md-4">
          <div className="card p-3 shadow-sm">
            <h6 className="text-muted">Balance</h6>
            <h4>{formatCurrency(data?.balance ?? 0)}</h4>
          </div>
        </div>
        <div className="col-md-4">
          <div className="card p-3 shadow-sm">
            <h6 className="text-muted">Account Type</h6>
            <h4>{data?.accountType ?? "Savings"}</h4>
          </div>
        </div>
      </div>
    </div>
  );
}