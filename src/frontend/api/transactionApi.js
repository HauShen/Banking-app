import client from "./client";

export async function getAccountSummary() {
  const { data } = await client.get("/accounts/summary");
  return data;
}