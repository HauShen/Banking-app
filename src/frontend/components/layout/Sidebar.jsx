import React from "react";
import { NavLink } from "react-router-dom";
import { useAuth } from "../../hooks/useAuth";

const linkClass = ({ isActive }) =>
  `nav-link rounded mb-1 ${isActive ? "active bg-primary text-white" : "text-dark"}`;

export default function Sidebar() {
  const { user } = useAuth();
  const isAdmin = user?.role === "ADMIN";

  return (
    <aside className="border-end p-3" style={{ width: 220, minHeight: "100vh" }}>
      <h5 className="mb-4">Banking App</h5>
      <nav className="nav flex-column">
        <NavLink to="/dashboard" className={linkClass}>Dashboard</NavLink>

        {/* ✅ Customer-only links */}
        {!isAdmin && (
          <>
            <NavLink to="/transactions" className={linkClass}>Transactions</NavLink>
            <NavLink to="/transfer"     className={linkClass}>Transfer</NavLink>
          </>
        )}

        {/* ✅ Admin-only links */}
        {isAdmin && (
          <>
            <NavLink to="/admin/users"    className={linkClass}>Manage Users</NavLink>
            <NavLink to="/admin/accounts" className={linkClass}>Manage Accounts</NavLink>
          </>
        )}

        {/* ✅ Shared */}
        <NavLink to="/profile" className={linkClass}>Profile</NavLink>
      </nav>
    </aside>
  );
}