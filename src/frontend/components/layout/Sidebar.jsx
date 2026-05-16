import React from "react";
import { NavLink } from "react-router-dom";

const linkStyle = ({ isActive }) => ({
  display: "block",
  padding: "10px 12px",
  borderRadius: 8,
  marginBottom: 8,
  textDecoration: "none",
  color: isActive ? "white" : "#111",
  background: isActive ? "#2563eb" : "transparent",
});

export default function Sidebar() {
  return (
    <aside style={{ borderRight: "1px solid #eee", padding: 16 }}>
      <h2>Banking App</h2>
      <nav style={{ marginTop: 16 }}>
        <NavLink to="/dashboard" style={linkStyle}>Dashboard</NavLink>
        <NavLink to="/transactions" style={linkStyle}>Transactions</NavLink>
        <NavLink to="/transfer" style={linkStyle}>Transfer</NavLink>
        <NavLink to="/profile" style={linkStyle}>Profile</NavLink>
      </nav>
    </aside>
  );
}