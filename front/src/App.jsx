import React, { useState, lazy, Suspense } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate, Outlet } from 'react-router-dom';
import { ProtectedRoute } from './components/shared/ProtectedRoute';
import LoadingSpinner from './components/shared/LoadingSpinner';
import Sidebar from './components/shared/Sidebar';
import Topbar from './components/shared/Topbar';
import ChatbotWidget from './components/member/ChatbotWidget';
import ErrorBoundary from './components/shared/ErrorBoundary';

// Lazy load pages for performance
const LandingPage = lazy(() => import('./pages/auth/LandingPage'));

// Admin Pages
const AdminDashboard = lazy(() => import('./pages/admin/AdminDashboard'));
const AdminStaffPage = lazy(() => import('./pages/admin/AdminStaffPage'));
const AdminRolePage = lazy(() => import('./pages/admin/AdminRolePage'));
const AdminMembersPage = lazy(() => import('./pages/admin/AdminMembersPage'));
const AdminBooksPage = lazy(() => import('./pages/admin/AdminBooksPage'));
const AdminAuthorsPage = lazy(() => import('./pages/admin/AdminAuthorsPage'));
const AdminCategoriesPage = lazy(() => import('./pages/admin/AdminCategoriesPage'));
const AdminLoansPage = lazy(() => import('./pages/admin/AdminLoansPage'));
const AdminFinesPage = lazy(() => import('./pages/admin/AdminFinesPage'));
const AdminReportsPage = lazy(() => import('./pages/admin/AdminReportsPage'));
const AdminAnalyticsPage = lazy(() => import('./pages/admin/AdminAnalyticsPage'));
const AdminConfigPage = lazy(() => import('./pages/admin/AdminConfigPage'));
const AdminProfilePage = lazy(() => import('./pages/admin/AdminProfilePage'));

// Analytics Engineer Pages
import AnalyticsEngineerDashboardPage from './pages/analytics-engineer/AnalyticsEngineerDashboardPage';
import AnalyticsEngineerOperationsPage from './pages/analytics-engineer/AnalyticsEngineerOperationsPage';
import AnalyticsEngineerReportsPage from './pages/analytics-engineer/AnalyticsEngineerReportsPage';
import AnalyticsEngineerMembersPage from './pages/analytics-engineer/AnalyticsEngineerMembersPage';
import AnalyticsEngineerProfilePage from './pages/analytics-engineer/AnalyticsEngineerProfilePage';

// Staff Pages
const StaffDashboard = lazy(() => import('./pages/staff/StaffDashboard'));
const StaffIssuePage = lazy(() => import('./pages/staff/StaffIssuePage'));
const StaffReturnPage = lazy(() => import('./pages/staff/StaffReturnPage'));
const StaffBooksPage = lazy(() => import('./pages/staff/StaffBooksPage'));
const StaffMembersPage = lazy(() => import('./pages/staff/StaffMembersPage'));
import StaffNotificationsPage from './pages/staff/StaffNotificationsPage';
const StaffFinesPage = lazy(() => import('./pages/staff/StaffFinesPage'));
const StaffProfilePage = lazy(() => import('./pages/staff/StaffProfilePage'));

// Member Pages
const MemberHomePage = lazy(() => import('./pages/member/MemberHomePage'));
const MemberBrowsePage = lazy(() => import('./pages/member/MemberBrowsePage'));
const MemberBookDetailPage = lazy(() => import('./pages/member/MemberBookDetailPage'));
const MemberAuthorPage = lazy(() => import('./pages/member/MemberAuthorPage'));
const AIRecommendationEngine = lazy(() => import('./pages/member/AIRecommendationEngine'));
const Chatbot = lazy(() => import('./pages/member/Chatbot'));
const MemberLoansPage = lazy(() => import('./pages/member/MemberLoansPage'));
const MemberHistoryPage = lazy(() => import('./pages/member/MemberHistoryPage'));
const MemberWishlistPage = lazy(() => import('./pages/member/MemberWishlistPage'));
const MemberReservationsPage = lazy(() => import('./pages/member/MemberReservationsPage'));
const MemberReviewsPage = lazy(() => import('./pages/member/MemberReviewsPage'));
const MemberReadingGoalPage = lazy(() => import('./pages/member/MemberReadingGoalPage'));
const MemberBookRequestPage = lazy(() => import('./pages/member/MemberBookRequestPage'));
const MemberRenewalRequestPage = lazy(() => import('./pages/member/MemberRenewalRequestPage'));
const MemberNotificationsPage = lazy(() => import('./pages/member/MemberNotificationsPage'));
const MemberProfilePage = lazy(() => import('./pages/member/MemberProfilePage'));

const SidebarWrapper = ({ portal, children, bgClass }) => {
  const [isOpen, setIsOpen] = useState(true);

  return (
    <div className="flex min-h-screen font-sans">
      <aside className={`h-screen fixed top-0 left-0 z-40 ${bgClass} overflow-hidden flex-shrink-0 transition-all duration-300 ${isOpen ? 'w-64' : 'w-16'}`}>
        <Sidebar portal={portal} isOpen={isOpen} />
      </aside>
      <div className={`flex-1 flex flex-col min-h-screen w-full transition-all duration-300 ${isOpen ? 'ml-64' : 'ml-16'}`}>
        {children}
      </div>
    </div>
  );
};

function App() {
  return (
    <Router>
      <Suspense fallback={<LoadingSpinner fullPage />}>
        <Routes>
          <Route path="/" element={<LandingPage />} />
          <Route path="/admin" element={<ProtectedRoute allowedRoles={['ROLE_ADMIN']}><AdminLayout /></ProtectedRoute>}>
            <Route index element={<Navigate to="/admin/dashboard" replace />} />
            <Route path="dashboard" element={<AdminDashboard />} />
            <Route path="staff" element={<AdminStaffPage />} />
            <Route path="roles" element={<AdminRolePage />} />
            <Route path="members" element={<AdminMembersPage />} />
            <Route path="books" element={<AdminBooksPage />} />
            <Route path="authors" element={<AdminAuthorsPage />} />
            <Route path="categories" element={<AdminCategoriesPage />} />
            <Route path="loans" element={<AdminLoansPage />} />
            <Route path="fines" element={<AdminFinesPage />} />
            <Route path="reports" element={<AdminReportsPage />} />
            <Route path="analytics" element={<AdminAnalyticsPage />} />
            <Route path="config" element={<AdminConfigPage />} />
            <Route path="profile" element={<AdminProfilePage />} />
          </Route>
          <Route path="/staff" element={<ProtectedRoute allowedRoles={['ROLE_STAFF']}><StaffLayout /></ProtectedRoute>}>
            <Route index element={<Navigate to="/staff/dashboard" replace />} />
            <Route path="dashboard" element={<StaffDashboard />} />
            <Route path="issue" element={<StaffIssuePage />} />
            <Route path="return" element={<StaffReturnPage />} />
            <Route path="books" element={<StaffBooksPage />} />
            <Route path="members" element={<StaffMembersPage />} />
            <Route path="notifications" element={<StaffNotificationsPage />} />
            <Route path="fines" element={<StaffFinesPage />} />
            <Route path="profile" element={<StaffProfilePage />} />
          </Route>
          <Route path="/analytics-engineer" element={<ProtectedRoute allowedRoles={['ROLE_ANALYTICS_ENGINEER']}><AnalyticsEngineerLayout /></ProtectedRoute>}>
            <Route index element={<Navigate to="/analytics-engineer/dashboard" replace />} />
            <Route path="dashboard" element={<AnalyticsEngineerDashboardPage />} />
            <Route path="operations" element={<AnalyticsEngineerOperationsPage />} />
            <Route path="reports" element={<AnalyticsEngineerReportsPage />} />
            <Route path="members" element={<AnalyticsEngineerMembersPage />} />
            <Route path="profile" element={<AnalyticsEngineerProfilePage />} />
          </Route>
          <Route path="/member" element={<ProtectedRoute allowedRoles={['ROLE_MEMBER']}><MemberLayout /></ProtectedRoute>}>
            <Route index element={<Navigate to="/member/home" replace />} />
            <Route path="home" element={<MemberHomePage />} />
            <Route path="browse" element={<MemberBrowsePage />} />
            <Route path="books/:id" element={<MemberBookDetailPage />} />
            <Route path="authors/:id" element={<MemberAuthorPage />} />
            <Route path="ai-recommend" element={<AIRecommendationEngine />} />
            <Route path="chatbot" element={<Chatbot />} />
            <Route path="loans" element={<MemberLoansPage />} />
            <Route path="history" element={<MemberHistoryPage />} />
            <Route path="wishlist" element={<MemberWishlistPage />} />
            <Route path="reservations" element={<MemberReservationsPage />} />
            <Route path="reviews" element={<MemberReviewsPage />} />
            <Route path="reading-goal" element={<MemberReadingGoalPage />} />
            <Route path="book-requests" element={<MemberBookRequestPage />} />
            <Route path="renewal-requests" element={<MemberRenewalRequestPage />} />
            <Route path="notifications" element={<MemberNotificationsPage />} />
            <Route path="profile" element={<MemberProfilePage />} />
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </Suspense>
    </Router>
  );
}

const AdminLayout = () => (
  <SidebarWrapper portal="admin" bgClass="bg-gray-900">
    <div className="flex-1 flex flex-col">
      <Topbar portal="admin" />
      <main className="flex-1 p-6 overflow-y-auto bg-white">
        <ErrorBoundary>
          <Outlet />
        </ErrorBoundary>
      </main>
    </div>
  </SidebarWrapper>
);

const StaffLayout = () => (
  <SidebarWrapper portal="staff" bgClass="bg-gray-900">
    <div className="flex-1 flex flex-col">
      <Topbar portal="staff" />
      <main className="flex-1 p-6 overflow-y-auto bg-white">
        <ErrorBoundary>
          <Outlet />
        </ErrorBoundary>
      </main>
    </div>
  </SidebarWrapper>
);

const AnalyticsEngineerLayout = () => (
  <SidebarWrapper portal="analyticsEngineer" bgClass="bg-gray-900">
    <div className="flex-1 flex flex-col">
      <Topbar portal="analyticsEngineer" />
      <main className="flex-1 p-6 overflow-y-auto bg-white">
        <ErrorBoundary>
          <Outlet />
        </ErrorBoundary>
      </main>
    </div>
  </SidebarWrapper>
);

const MemberLayout = () => (
  <SidebarWrapper portal="member" bgClass="bg-amber-50">
    <div className="flex-1 flex flex-col min-h-screen bg-amber-50">
      <Topbar portal="member" />
      <main className="flex-1 p-6 overflow-y-auto bg-amber-50">
        <Outlet />
      </main>
      <ChatbotWidget />
    </div>
  </SidebarWrapper>
);

export default App;