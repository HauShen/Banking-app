import client from "./client";

export async function getAccountSummary() {
  const { data } = await client.get("/accounts/summary");
  return data;
}

export async function getTransactions() {
  const { data } = await client.get("/transactions"); // adjust endpoint if needed
  return data;
}