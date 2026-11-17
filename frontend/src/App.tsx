import AuthProvider from 'providers/AuthProvider';
import { Outlet } from 'react-router-dom';

const App = () => {
  return (
    <AuthProvider>
      <Outlet />
    </AuthProvider>
  );
};

export default App;
