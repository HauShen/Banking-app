import React from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../hooks/useAuth";

export default function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  return (
    <header className="navbar navbar-light bg-light border-bottom px-3 d-flex justify-content-between align-items-center">
          <strong>Welcome, {user?.name || "User"}</strong>
          <button
            className="btn btn-outline-secondary btn-sm"
            onClick={() => {        // ✅ real handler, not ...
              logout();
              navigate("/login");
            }}
          >
            Logout
          </button>
        </header>
  );
}