import React from "react";
import { useAuth } from "../hooks/useAuth";

export default function ProfilePage() {
  const { user } = useAuth();

  return (
    <div className="container mt-4" style={{ maxWidth: 500 }}>
      <h2 className="mb-4">Profile</h2>
      <div className="card p-4 shadow-sm">
        <div className="mb-3 pb-3 border-bottom">
          <small className="text-muted text-uppercase">Username</small>

          <p className="mb-0 fw-semibold">{user?.username ?? "—"}</p>
        </div>
        <div className="mb-3 pb-3 border-bottom">
          <small className="text-muted text-uppercase">Full Name</small>

          <p className="mb-0 fw-semibold">{user?.fullName ?? "—"}</p>
        </div>
        <div className="mb-3 pb-3 border-bottom">
          <small className="text-muted text-uppercase">Email</small>
          <p className="mb-0 fw-semibold">{user?.email ?? "—"}</p>
        </div>
        <div className="mb-0">
          <small className="text-muted text-uppercase">Role</small>
          <p className="mb-0">
            <span className={`badge ${user?.role === "ADMIN" ? "bg-dark" : "bg-primary"}`}>
              {user?.role ?? "—"}
            </span>
          </p>
        </div>
      </div>
    </div>
  );
}