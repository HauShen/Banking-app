import { useAuth } from "../hooks/useAuth";

export default function ProfilePage() {
  const { user } = useAuth();
  return (
    <div>
      <h1>Profile</h1>
      <p>Name: {user?.name}</p>
      <p>Email: {user?.email}</p>
    </div>
  );
}