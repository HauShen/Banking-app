import React from "react";
import { Navigate, Route, Routes } from "react-router-dom";
import LoginPage from "../pages/LoginPage";
import RegisterPage from "../pages/RegisterPage";
import DashboardPage from "../pages/DashboardPage";
import TransactionsPage from "../pages/TransactionsPage";
import TransferPage from "../pages/TransferPage";
import ProfilePage from "../pages/ProfilePage";
import ManageUsersPage from "../pages/admin/ManageUsersPage";
import ManageAccountsPage from "../pages/admin/ManageAccountsPage";
import UserDetailPage from "../pages/admin/UserDetailPage";
import ProtectedRoute from "./ProtectedRoute";
import AppLayout from "../components/layout/AppLayout";

export default function AppRoutes() {
  return (
    <Routes>
      <Route path="/login"    element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />

      <Route element={<ProtectedRoute><AppLayout /></ProtectedRoute>}>
        <Route path="/dashboard"            element={<DashboardPage />} />
        <Route path="/transactions"         element={<TransactionsPage />} />
        <Route path="/transfer"             element={<TransferPage />} />
        <Route path="/profile"              element={<ProfilePage />} />
        <Route path="/admin/users"          element={<ManageUsersPage />} />
        <Route path="/admin/accounts"       element={<ManageAccountsPage />} />
        <Route path="/admin/users/:userId"  element={<UserDetailPage />} />
      </Route>

      <Route path="/"  element={<Navigate to="/dashboard" replace />} />
      <Route path="*"  element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
}