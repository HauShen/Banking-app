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
    <div>
      <h1>Dashboard</h1>
      <p>Balance: RM {data?.balance ?? 0}</p>
      <p>Account Type: {data?.accountType ?? "Savings"}</p>
    </div>
  );
}