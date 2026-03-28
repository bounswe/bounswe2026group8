import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';

import LandingPage from './pages/LandingPage';
import SignUpPage from './pages/SignUpPage';
import SignInPage from './pages/SignInPage';
import DashboardPage from './pages/DashboardPage';
import HelpRequestsPage from './pages/HelpRequestsPage';
import HelpRequestCreatePage from './pages/HelpRequestCreatePage';
import HelpRequestDetailPage from './pages/HelpRequestDetailPage';

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/" element={<LandingPage />} />
          <Route path="/signup" element={<SignUpPage />} />
          <Route path="/signin" element={<SignInPage />} />
          <Route
            path="/dashboard"
            element={
              <ProtectedRoute>
                <DashboardPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/help-requests"
            element={
              <ProtectedRoute>
                <HelpRequestsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/help-requests/new"
            element={
              <ProtectedRoute>
                <HelpRequestCreatePage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/help-requests/:id"
            element={
              <ProtectedRoute>
                <HelpRequestDetailPage />
              </ProtectedRoute>
            }
          />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
