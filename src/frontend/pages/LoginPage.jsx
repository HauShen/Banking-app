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
    <div className="container d-flex justify-content-center align-items-center min-vh-100">
      <div className="card p-4 shadow-sm" style={{ width: "100%", maxWidth: 400 }}>
        <h2 className="mb-3">Login</h2>
        <form onSubmit={handleSubmit(onSubmit)}>
          <div className="mb-3">
            <Input placeholder="Username" {...register("username", { required: true })} />
          </div>
          <div className="mb-3">
            <Input placeholder="Password" type="password" {...register("password", { required: true })} />
          </div>
          <Button type="submit" className="w-100">Login</Button>
        </form>
        <p className="mt-3 mb-0">No account? <Link to="/register">Register</Link></p>
      </div>
    </div>
  );
}