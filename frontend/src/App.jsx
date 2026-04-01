import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { HubProvider } from './context/HubContext';
import ProtectedRoute from './components/ProtectedRoute';
import HubSelector from './components/HubSelector';

import LandingPage from './pages/LandingPage';
import SignUpPage from './pages/SignUpPage';
import SignInPage from './pages/SignInPage';
import DashboardPage from './pages/DashboardPage';
import ForumPage from './pages/ForumPage';
import PostDetailPage from './pages/PostDetailPage';
import PostCreatePage from './pages/PostCreatePage';

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <HubProvider>
          <div className="hub-selector-bar">
            <HubSelector />
          </div>
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
            <Route path="/forum" element={<ForumPage />} />
            <Route path="/forum/posts/:id" element={<PostDetailPage />} />
            <Route
              path="/forum/new"
              element={
                <ProtectedRoute>
                  <PostCreatePage />
                </ProtectedRoute>
              }
            />
          </Routes>
        </HubProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}
