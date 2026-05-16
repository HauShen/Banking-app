import React from "react";
import { useForm } from "react-hook-form";
import { useNavigate, useLocation, Link } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";
import Button from "../components/ui/Button";
import Input from "../components/ui/Input";

export default function LoginPage() {
  const { register, handleSubmit } = useForm();
  const auth = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const onSubmit = async (values) => {
    try {
      await auth.login(values); // { username, password }
      const redirectTo = location.state?.from?.pathname || "/dashboard";
      navigate(redirectTo, { replace: true });
    } catch (e) {
      console.error("Login error:", e?.response?.status, e?.response?.data);
      alert(e?.response?.data?.message || "Login failed");
    }
  };

  return (
    <div style={{ maxWidth: 400, margin: "80px auto" }}>
      <h1>Login</h1>
      <form onSubmit={handleSubmit(onSubmit)} style={{ display: "grid", gap: 12 }}>
        <Input placeholder="Username" {...register("username", { required: true })} />
        <Input placeholder="Password" type="password" {...register("password", { required: true })} />
        <Button type="submit">Login</Button>
      </form>
      <p style={{ marginTop: 12 }}>
        No account? <Link to="/register">Register</Link>
      </p>
    </div>
  );
}