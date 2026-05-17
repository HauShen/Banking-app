import React from "react";
import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";

export default function ProtectedRoute({ children }) {
  const { isAuthenticated, loading } = useAuth();
  const location = useLocation();

  if (loading) return <div className="d-flex justify-content-center align-items-center min-vh-100">
                        <div className="spinner-border text-primary" role="status">
                          <span className="visually-hidden">Checking session...</span>
                        </div>
                      </div>;

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return children;
}