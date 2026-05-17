import React, { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";
import Input from "../components/ui/Input";
import Button from "../components/ui/Button";
import { getBootstrapStatus, registerBootstrapAdmin } from "../api/authAPI";

export default function RegisterPage() {
  const { register, handleSubmit, watch } = useForm({
    defaultValues: { asBootstrapAdmin: false },
  });
  const auth = useAuth();
  const navigate = useNavigate();

  const [bootstrapAllowed, setBootstrapAllowed] = useState(false);
  const [loadingStatus, setLoadingStatus] = useState(true);

  const asBootstrapAdmin = watch("asBootstrapAdmin");

  useEffect(() => {
    let mounted = true;
    (async () => {
      try {
        const status = await getBootstrapStatus();
        // expected response: { bootstrapEnabled, adminExists, userCount, allowed }
        if (mounted) setBootstrapAllowed(!!status?.allowed);
      } catch (e) {
        // fail closed: hide bootstrap option if status endpoint fails
        if (mounted) setBootstrapAllowed(false);
      } finally {
        if (mounted) setLoadingStatus(false);
      }
    })();

    return () => {
      mounted = false;
    };
  }, []);

  const onSubmit = async (values) => {
    try {
      const payload = {
        username: values.username,
        fullName: values.fullName, // must match backend AuthenticationRequest
        email: values.email,
        password: values.password,
      };

      if (bootstrapAllowed && values.asBootstrapAdmin) {
        // 1) Create first admin
        await registerBootstrapAdmin(payload);

        // 2) Login immediately (your backend login expects username/password)
        await auth.login({
          username: values.username,
          password: values.password,
        });
      } else {
        // normal register endpoint
        await auth.register(payload);
      }

      navigate("/dashboard", { replace: true });
    } catch (e) {
      console.error("Registration error:", e?.response?.status, e?.response?.data || e.message);
      alert(e?.response?.data?.message || "Registration failed");
    }
  };

  return (
    <div className="container d-flex justify-content-center align-items-center min-vh-100">
      <div className="card p-4 shadow-sm" style={{ width: "100%", maxWidth: 420 }}>
        <h2 className="mb-3">Register</h2>
        <form onSubmit={handleSubmit(onSubmit)}>
          <div className="mb-3"><Input placeholder="Username" {...register("username", { required: true })} /></div>
          <div className="mb-3"><Input placeholder="Full name" {...register("fullName", { required: true })} /></div>
          <div className="mb-3"><Input placeholder="Email" type="email" {...register("email", { required: true })} /></div>
          <div className="mb-3"><Input placeholder="Password" type="password" {...register("password", { required: true, minLength: 6 })} /></div>

          {!loadingStatus && bootstrapAllowed && (
            <div className="form-check mb-3">
              <input className="form-check-input" type="checkbox" {...register("asBootstrapAdmin")} id="bootstrapCheck" />
              <label className="form-check-label" htmlFor="bootstrapCheck">
                Create first admin account (bootstrap)
              </label>
            </div>
          )}

          <Button type="submit" className="w-100">
            {bootstrapAllowed && asBootstrapAdmin ? "Create Admin Account" : "Create Account"}
          </Button>
        </form>
        <p className="mt-3 mb-0">Already have an account? <Link to="/login">Login</Link></p>
      </div>
    </div>
  );
}