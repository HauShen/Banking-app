import client from "./client";

export async function getAccountSummary() {
  const { data } = await client.get("/accounts/summary");
  return data;
}

export async function getAccountsByUserId(userId) {
  const { data } = await client.get(`/accounts/get_all/${userId}`);
  return data;
}

export async function getAllAccounts() {
  const { data } = await client.get("/accounts/get_all");
  return data;
}

// Admin: create account for a customer
export async function createAccount(payload) {
  // payload: { userId, accountType }
  const { data } = await client.post("/accounts/create", payload);
  return data;
}

// Customer: top up own account
export async function topUpAccount(accountId, amount) {
  const { data } = await client.patch(`/accounts/${accountId}/topup`, null, {
    params: { amount },
  });
  return data;
}

export async function updateAccountStatus(accountId, status) {
  const { data } = await client.patch(`/accounts/${accountId}/status`, null, {
    params: { status },
  });
  return data;
}