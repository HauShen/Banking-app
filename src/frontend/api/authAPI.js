import client from "./client";

export async function login(payload) {
  const { data } = await client.post("/auth/login", payload);
  return data;
}

export async function register(payload) {
  const { data } = await client.post("/auth/register", payload);
  return data;
}

export async function getMe() {
  const { data } = await client.get("/auth/me");
  return data;
}

export async function getBootstrapStatus() {
  const { data } = await client.get("/auth/bootstrap/status");  // ← was /auth/bootstrap-status
  return data;
}

export async function registerBootstrapAdmin(payload) {
  const { data } = await client.post("/auth/bootstrap/register", payload);  // ← was /auth/bootstrap-admin/register
  return data;
}