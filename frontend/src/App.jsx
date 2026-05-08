import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
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
import OfflineMessagesPage from './pages/OfflineMessagesPage';
import OfflineMessageDetailPage from './pages/OfflineMessageDetailPage';
import DashboardPageTutorial from './pages/DashboardPage_tutorial';
import ForumPageTutorial from './pages/ForumPage_tutorial';
import PostDetailPageTutorial from './pages/PostDetailPage_tutorial';
import PostCreatePageTutorial from './pages/PostCreatePage_tutorial';
import HelpRequestsPageTutorial from './pages/HelpRequestsPage_tutorial';
import HelpRequestDetailPageTutorial from './pages/HelpRequestDetailPage_tutorial';
import HelpRequestCreatePageTutorial from './pages/HelpRequestCreatePage_tutorial';

export default function App() {
return (
  <BrowserRouter>
    <AuthProvider>
        <div className="app-controls">
          <div className="app-controls-left">
            <LanguageSelector />
          </div>
          <div className="app-controls-right">
            <HubSelector />
          </div>
        </div>
        <Routes>
          <Route path="/" element={<LandingPage />} />
          <Route path="/signup" element={<SignUpPage />} />
          <Route path="/signin" element={<SignInPage />} />
          <Route path="/tutorial" element={<DashboardPageTutorial />} />
          <Route path="/tutorial/forum" element={<ForumPageTutorial />} />
          <Route path="/tutorial/forum/posts/:id" element={<PostDetailPageTutorial />} />
          <Route path="/tutorial/forum/new" element={<PostCreatePageTutorial />} />
          <Route path="/tutorial/help-requests" element={<HelpRequestsPageTutorial />} />
          <Route path="/tutorial/help-requests/:id" element={<HelpRequestDetailPageTutorial />} />
          <Route path="/tutorial/help-requests/new" element={<HelpRequestCreatePageTutorial />} />
          <Route path="/tutorial/emergency-info" element={<EmergencyInfoPage tutorialMode />} />
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
          <Route
            path="/offline-messages"
            element={
              <ProtectedRoute>
                <OfflineMessagesPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/offline-messages/:id"
            element={
              <ProtectedRoute>
                <OfflineMessageDetailPage />
              </ProtectedRoute>
            }
          />
        </Routes>
    </AuthProvider>
  </BrowserRouter>
);
}
