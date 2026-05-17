import React from "react";
import { useQuery } from "@tanstack/react-query";
import { getTransactions } from "../api/transactionApi";

export default function TransactionsPage() {
  const { data, isLoading } = useQuery({
    queryKey: ["transactions"],
    queryFn: () => getTransactions(),
  });

  if (isLoading) return <p>Loading transactions...</p>;

  return (
    <div>
      <h1>Transactions</h1>
      <div className="container mt-4">
        <h2 className="mb-3">Transactions</h2>
        <table className="table table-striped table-hover">
          <thead className="table-dark">
            <tr>
              <th>Type</th><th>Amount</th><th>Description</th>
            </tr>
          </thead>
          <tbody>
            {(data?.items || []).map((tx) => (
              <tr key={tx.id}>
                <td>{tx.type}</td>
                <td>{formatCurrency(tx.amount)}</td>
                <td>{tx.description}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}