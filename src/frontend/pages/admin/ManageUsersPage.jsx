import React, { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { getAccountsByUserId, createAccount } from "../../api/accountApi";

// Fetch all users via the admin endpoint
async function fetchAllUsers(page = 0, size = 10) {
  const { data } = await import("../../api/client").then(m =>
    m.default.get("/users/admin/get_all", { params: { page, size } })
  );
  return data;
}

export default function ManageUsersPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [selectedUser, setSelectedUser] = useState(null);  // user to create account for
  const [accountType, setAccountType] = useState("SAVINGS");
  const [showModal, setShowModal] = useState(false);

  const { data, isLoading, isError } = useQuery({
    queryKey: ["admin-users", page],
    queryFn: () => fetchAllUsers(page, 10),
  });

  const createMutation = useMutation({
    mutationFn: ({ userId, accountType }) => createAccount({ userId, accountType }),
    onSuccess: () => {
      alert("Account created successfully!");
      setShowModal(false);
      queryClient.invalidateQueries(["admin-users"]);
    },
    onError: (e) => alert(e?.response?.data?.message || "Failed to create account"),
  });

  const users = data?.content ?? [];
  const totalPages = data?.totalPages ?? 1;

  return (
    <div className="container mt-4">
      <h2 className="mb-4">Manage Users</h2>

      {isLoading && <p>Loading users...</p>}
      {isError   && <p className="text-danger">Failed to load users.</p>}

      <div className="table-responsive">
        <table className="table table-striped table-hover align-middle">
          <thead className="table-dark">
            <tr>
              <th>Username</th>
              <th>Full Name</th>
              <th>Email</th>
              <th>Role</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {users.map((u) => (
              <tr key={u.id}>
                <td>{u.username}</td>
                <td>{u.fullName}</td>
                <td>{u.email}</td>
                <td><span className={`badge ${u.role === "ADMIN" ? "bg-dark" : "bg-primary"}`}>{u.role}</span></td>
                <td>
                  {u.role !== "ADMIN" && (
                    <button className="btn btn-sm btn-outline-success"
                      onClick={() => { setSelectedUser(u); setShowModal(true); }}>
                      + Create Account
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      <div className="d-flex justify-content-between align-items-center mt-3">
        <span className="text-muted small">Page {page + 1} of {totalPages}</span>
        <div className="btn-group">
          <button className="btn btn-sm btn-outline-secondary"
            onClick={() => setPage(p => Math.max(p - 1, 0))} disabled={page === 0}>← Prev</button>
          <button className="btn btn-sm btn-outline-secondary"
            onClick={() => setPage(p => Math.min(p + 1, totalPages - 1))} disabled={page >= totalPages - 1}>Next →</button>
        </div>
      </div>

      {/* Create Account Modal */}
      {showModal && selectedUser && (
        <div className="modal d-block" style={{ background: "rgba(0,0,0,0.4)" }}>
          <div className="modal-dialog modal-dialog-centered">
            <div className="modal-content p-4">
              <h5 className="mb-3">Create Account for <strong>{selectedUser.username}</strong></h5>
              <div className="mb-3">
                <label className="form-label">Account Type</label>
                <select className="form-select" value={accountType} onChange={e => setAccountType(e.target.value)}>
                  <option value="SAVINGS">SAVINGS</option>
                  <option value="CURRENT">CURRENT</option>
                </select>
              </div>
              <div className="d-flex gap-2">
                <button className="btn btn-primary"
                  disabled={createMutation.isPending}
                  onClick={() => createMutation.mutate({ userId: selectedUser.id, accountType })}>
                  {createMutation.isPending ? "Creating..." : "Create"}
                </button>
                <button className="btn btn-outline-secondary" onClick={() => setShowModal(false)}>Cancel</button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}