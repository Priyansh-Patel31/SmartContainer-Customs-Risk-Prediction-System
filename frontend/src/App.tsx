import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider, keepPreviousData } from '@tanstack/react-query';
import { Toaster } from 'react-hot-toast';
import { Loader2 } from 'lucide-react';
import type { ReactNode } from 'react';
import { AuthProvider, useAuth } from '@/context/AuthContext';
import { NotificationProvider } from '@/context/NotificationContext';
import { SocketProvider } from '@/context/SocketContext';
import AppLayout from '@/components/layout/AppLayout';
import Dashboard from '@/pages/Dashboard';
import Upload from '@/pages/Upload';
import Predict from '@/pages/Predict';
import MapPage from '@/pages/Map';
import Tracking from '@/pages/Tracking';
import Login from '@/pages/Login';
import Profile from '@/pages/Profile';
import AccountSettings from '@/pages/AccountSettings';
import SystemAccess from '@/pages/SystemAccess';
import Dossier from '@/pages/Dossier';
import Analytics from '@/pages/Analytics';
import Simulator from '@/pages/Simulator';
import Chat from '@/pages/Chat';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 5,     // 5 min — serve from cache without refetch
      gcTime: 1000 * 60 * 15,       // 15 min — keep data in memory
      placeholderData: keepPreviousData, // show stale data instantly, no loading flash
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

function ProtectedRoute({ children, allowedRoles }: { children: ReactNode, allowedRoles?: string[] }) {
  const { user, isAuthenticated, isLoading } = useAuth();
  if (isLoading) {
    return (
      <div className="flex h-screen items-center justify-center bg-background">
        <Loader2 className="w-8 h-8 animate-spin text-primary" />
      </div>
    );
  }
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  if (allowedRoles && user?.role && !allowedRoles.includes(user.role)) {
      if (user.role === 'exporter') return <Navigate to="/exporter" replace />;
      return <Navigate to="/" replace />;
  }
  return <>{children}</>;
}

export default function App() {

  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <SocketProvider>
        <NotificationProvider>
          <BrowserRouter>
            <Routes>
              <Route path="/login" element={<Login />} />

              {/* Admin Portal (Standard Base) */}
              <Route element={<ProtectedRoute allowedRoles={['admin', 'officer']}><AppLayout /></ProtectedRoute>}>
                <Route path="/" element={<Dashboard />} />
                <Route path="/upload" element={<Upload />} />
                <Route path="/predict" element={<Predict />} />
                <Route path="/map" element={<MapPage />} />
                <Route path="/tracking" element={<Tracking />} />
                <Route path="/analytics" element={<Analytics />} />
                <Route path="/simulator" element={<Simulator />} />
                <Route path="/chat" element={<Chat />} />
                <Route path="/profile" element={<Profile />} />
                <Route path="/account-settings" element={<AccountSettings />} />
                <Route path="/system-access" element={<SystemAccess />} />
                <Route path="/dossier/:id" element={<Dossier />} />
              </Route>

              {/* Exporter Portal */}
              <Route path="/exporter" element={<ProtectedRoute allowedRoles={['exporter']}><AppLayout /></ProtectedRoute>}>
                <Route index element={<Dashboard />} />
                <Route path="tracking" element={<Tracking />} />
                <Route path="analytics" element={<Analytics />} />
                <Route path="chat" element={<Chat />} />
                <Route path="profile" element={<Profile />} />
                <Route path="account-settings" element={<AccountSettings />} />
              </Route>
            </Routes>
          </BrowserRouter>
          <Toaster
            position="top-right"
            toastOptions={{
              style: {
                background: 'var(--card)',
                color: 'var(--foreground)',
                border: '1px solid var(--border)',
              },
            }}
          />
        </NotificationProvider>
        </SocketProvider>
      </AuthProvider>
    </QueryClientProvider>
  );
}

