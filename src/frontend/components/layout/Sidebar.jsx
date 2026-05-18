import React from "react";
import { NavLink } from "react-router-dom";

export default function Sidebar() {   // ✅ missing this wrapper
  return (
    <aside className="border-end p-3" style={{ width: 220, minHeight: "100vh" }}>
      <h5 className="mb-4">Banking App</h5>
      <nav className="nav flex-column">
        <NavLink to="/dashboard"    className={({ isActive }) => `nav-link rounded mb-1 ${isActive ? "active bg-primary text-white" : "text-dark"}`}>Dashboard</NavLink>
        <NavLink to="/transactions" className={({ isActive }) => `nav-link rounded mb-1 ${isActive ? "active bg-primary text-white" : "text-dark"}`}>Transactions</NavLink>
        <NavLink to="/transfer"     className={({ isActive }) => `nav-link rounded mb-1 ${isActive ? "active bg-primary text-white" : "text-dark"}`}>Transfer</NavLink>
        <NavLink to="/profile"      className={({ isActive }) => `nav-link rounded mb-1 ${isActive ? "active bg-primary text-white" : "text-dark"}`}>Profile</NavLink>
      </nav>
    </aside>
  );
}