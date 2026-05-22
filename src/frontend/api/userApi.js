import client from "./client";

export async function deleteUser(userId) {
  await client.delete(`/users/admin/${userId}`);
}