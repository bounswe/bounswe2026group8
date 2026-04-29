import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import StaffRoute from './components/StaffRoute';
import HubSelector from './components/HubSelector';
import LanguageSelector from './components/LanguageSelector';

import LandingPage from './pages/LandingPage';
import SignUpPage from './pages/SignUpPage';
import SignInPage from './pages/SignInPage';
import DashboardPage from './pages/DashboardPage';
import ProfilePage from './pages/ProfilePage';
import ForumPage from './pages/ForumPage';
import PostDetailPage from './pages/PostDetailPage';
import PostCreatePage from './pages/PostCreatePage';
import HelpRequestsPage from './pages/HelpRequestsPage';
import HelpRequestCreatePage from './pages/HelpRequestCreatePage';
import HelpRequestDetailPage from './pages/HelpRequestDetailPage';
import MyPostsPage from './pages/MyPostsPage';
import UserProfilePage from './pages/UserProfilePage';
import EmergencyInfoPage from './pages/EmergencyInfoPage';
import EmergencyMapPage from './pages/EmergencyMapPage';
import StaffDashboardPage from './pages/StaffDashboardPage';
import AdminUsersPage from './pages/AdminUsersPage';
import AdminHubsPage from './pages/AdminHubsPage';
import AdminAuditLogPage from './pages/AdminAuditLogPage';
import ForumModerationPage from './pages/ForumModerationPage';
import HelpModerationPage from './pages/HelpModerationPage';
import ExpertiseVerificationPage from './pages/ExpertiseVerificationPage';
import { STAFF_ROLE } from './utils/staffRoles';

export default function App() {
return (
  <BrowserRouter>
    <AuthProvider>
        <HubSelector />
        <LanguageSelector />
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
          <Route
            path="/profile"
            element={
              <ProtectedRoute>
                <ProfilePage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/my-posts"
            element={
              <ProtectedRoute>
                <MyPostsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/users/:id"
            element={
              <ProtectedRoute>
                <UserProfilePage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/emergency-info"
            element={
              <ProtectedRoute>
                <EmergencyInfoPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/emergency-info/map"
            element={
              <ProtectedRoute>
                <EmergencyMapPage />
              </ProtectedRoute>
            }
          />

          {/* ── Staff: any role with elevated authority ──────────────────── */}
          <Route
            path="/staff"
            element={
              <StaffRoute
                allowedStaffRoles={[STAFF_ROLE.MODERATOR, STAFF_ROLE.VERIFICATION_COORDINATOR]}
              >
                <StaffDashboardPage />
              </StaffRoute>
            }
          />

          {/* ── Admin-only ───────────────────────────────────────────────── */}
          <Route
            path="/staff/users"
            element={
              <StaffRoute>
                <AdminUsersPage />
              </StaffRoute>
            }
          />
          <Route
            path="/staff/hubs"
            element={
              <StaffRoute>
                <AdminHubsPage />
              </StaffRoute>
            }
          />
          <Route
            path="/staff/audit-logs"
            element={
              <StaffRoute>
                <AdminAuditLogPage />
              </StaffRoute>
            }
          />

          {/* ── Moderator / Admin ────────────────────────────────────────── */}
          <Route
            path="/staff/moderation/forum"
            element={
              <StaffRoute allowedStaffRoles={[STAFF_ROLE.MODERATOR]}>
                <ForumModerationPage />
              </StaffRoute>
            }
          />
          <Route
            path="/staff/moderation/help"
            element={
              <StaffRoute allowedStaffRoles={[STAFF_ROLE.MODERATOR]}>
                <HelpModerationPage />
              </StaffRoute>
            }
          />

          {/* ── Verification coordinator / Admin ─────────────────────────── */}
          <Route
            path="/staff/verification/expertise"
            element={
              <StaffRoute allowedStaffRoles={[STAFF_ROLE.VERIFICATION_COORDINATOR]}>
                <ExpertiseVerificationPage />
              </StaffRoute>
            }
          />
        </Routes>
    </AuthProvider>
  </BrowserRouter>
);
}
