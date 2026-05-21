import client from "./client";

// GET /api/transfers/account/{accountNumber}?page=0&size=10
export async function getTransactionsByAccount(accountNumber, page = 0, size = 10) {
  const { data } = await client.get(`/transfers/account/${accountNumber}`, {
    params: { page, size },
  });
  return data; // returns Page<TransactionResponseBody>
}

// POST /api/transfers
export async function createTransfer(payload) {
  const { data } = await client.post("/transfers", payload);
  return data;
}