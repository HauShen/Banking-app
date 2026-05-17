import React from "react";
import { useAuth } from "../hooks/useAuth";

export default function ProfilePage() {
  const { user } = useAuth();
  return (
    <div className="container mt-4" style={{ maxWidth: 500 }}>
      <h2 className="mb-3">Profile</h2>
      <div className="card p-4 shadow-sm">
        <p className="mb-2"><strong>Name:</strong> {user?.name}</p>
        <p className="mb-0"><strong>Email:</strong> {user?.email}</p>
      </div>
    </div>
  );
}