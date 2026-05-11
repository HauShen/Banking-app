import { createContext, useEffect, useMemo, useState } from "react";
import { getMe, login as loginApi, register as registerApi } from "../api/authApi";

export const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  const isAuthenticated = !!localStorage.getItem("token");

  useEffect(() => {
    async function bootstrap() {
      try {
        if (!localStorage.getItem("token")) return;
        const me = await getMe();
        setUser(me);
      } catch (err) {
        localStorage.removeItem("token");
        setUser(null);
      } finally {
        setLoading(false);
      }
    }
    bootstrap();
  }, []);

  const login = async (payload) => {
    const data = await loginApi(payload);
    localStorage.setItem("token", data.token);
    const me = await getMe();
    setUser(me);
    return me;
  };

  const register = async (payload) => {
    const data = await registerApi(payload);
    localStorage.setItem("token", data.token);
    const me = await getMe();
    setUser(me);
    return me;
  };

  const logout = () => {
    localStorage.removeItem("token");
    setUser(null);
  };

  const value = useMemo(
    () => ({ user, loading, isAuthenticated: !!localStorage.getItem("token"), login, register, logout }),
    [user, loading]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}