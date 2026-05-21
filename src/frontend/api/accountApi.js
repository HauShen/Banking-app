import client from "./client";

// GET /api/accounts/summary  (uses JWT to identify current user)
export async function getAccountSummary() {
  const { data } = await client.get("/accounts/summary");
  return data; // { balance, accountType, accounts: [...] }
}

// GET /api/accounts/get_all/{userId}
export async function getAccountsByUserId(userId) {
  const { data } = await client.get(`/accounts/get_all/${userId}`);
  return data;
}