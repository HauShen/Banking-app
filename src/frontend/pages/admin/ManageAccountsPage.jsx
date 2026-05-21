import React, { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { getAllAccounts, updateAccountStatus } from "../../api/accountApi";

function formatCurrency(amount) {
  return new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" }).format(amount ?? 0);
}

export default function ManageAccountsPage() {
  const queryClient = useQueryClient();

  const { data: accounts, isLoading, isError } = useQuery({
    queryKey: ["admin-accounts"],
    queryFn: getAllAccounts,
  });

  const statusMutation = useMutation({
    mutationFn: ({ accountId, status }) => updateAccountStatus(accountId, status),
    onSuccess: () => queryClient.invalidateQueries(["admin-accounts"]),
    onError: (e) => alert(e?.response?.data?.message || "Failed to update status"),
  });

  if (isLoading) return <p className="p-4">Loading accounts...</p>;
  if (isError)   return <p className="p-4 text-danger">Failed to load accounts.</p>;

  return (
    <div className="container mt-4">
      <h2 className="mb-4">Manage Accounts</h2>
      <div className="table-responsive">
        <table className="table table-striped table-hover align-middle">
          <thead className="table-dark">
            <tr>
              <th>Account No</th>
              <th>Type</th>
              <th>Balance</th>
              <th>Currency</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {(accounts ?? []).map((acc) => (
              <tr key={acc.accountId}>
                <td className="font-monospace">{acc.accountNumber}</td>
                <td>{acc.accountType}</td>
                <td>{formatCurrency(acc.balance)}</td>
                <td>{acc.accountCurrency}</td>
                <td>
                  <span className={`badge ${acc.accountStatus === "ACTIVE" ? "bg-success" : "bg-danger"}`}>
                    {acc.accountStatus}
                  </span>
                </td>
                <td>
                  {acc.accountStatus === "ACTIVE" ? (
                    <button className="btn btn-sm btn-outline-danger"
                      disabled={statusMutation.isPending}
                      onClick={() => statusMutation.mutate({ accountId: acc.accountId, status: "LOCKED" })}>
                      Lock
                    </button>
                  ) : (
                    <button className="btn btn-sm btn-outline-success"
                      disabled={statusMutation.isPending}
                      onClick={() => statusMutation.mutate({ accountId: acc.accountId, status: "ACTIVE" })}>
                      Activate
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}