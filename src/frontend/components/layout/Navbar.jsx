import { useNavigate } from "react-router-dom";
import { useAuth } from "../../hooks/useAuth";

export default function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  return (
    <header style={{ borderBottom: "1px solid #eee", padding: 16, display: "flex", justifyContent: "space-between" }}>
      <strong>Welcome, {user?.name || "User"}</strong>
      <button
        onClick={() => {
          logout();
          navigate("/login");
        }}
      >
        Logout
      </button>
    </header>
  );