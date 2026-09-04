import { useState, useCallback, useEffect } from 'react'
import Sidebar from './components/Sidebar'
import Dashboard from './pages/Dashboard'
import FileBrowser from './pages/FileBrowser'
import Pairing from './pages/Pairing'
import Devices from './pages/Devices'
import Transfers from './pages/Transfers'
import Login from './pages/Login'

export const API_BASE = 'http://localhost:8080'

export const api = {
  get: (path) => {
    const token = localStorage.getItem('ls_token') || localStorage.getItem('localsync_jwt')
    const headers = {}
    if (token && token !== 'null' && token !== 'undefined') {
      headers['Authorization'] = `Bearer ${token}`
    }
    return fetch(API_BASE + path, { headers })
      .then(async r => {
        if (!r.ok) return []
        try { return await r.json() } catch { return [] }
      })
      .catch(() => [])
  },
  post: (path, body) => {
    const token = localStorage.getItem('ls_token') || localStorage.getItem('localsync_jwt')
    const headers = { 'Content-Type': 'application/json' }
    if (token && token !== 'null' && token !== 'undefined') {
      headers['Authorization'] = `Bearer ${token}`
    }
    return fetch(API_BASE + path, {
      method: 'POST',
      headers,
      body: JSON.stringify(body)
    }).then(async r => {
      try { return await r.json() } catch { return {} }
    }).catch(() => ({}))
  },
  delete: (path) => {
    const token = localStorage.getItem('ls_token') || localStorage.getItem('localsync_jwt')
    const headers = {}
    if (token && token !== 'null' && token !== 'undefined') {
      headers['Authorization'] = `Bearer ${token}`
    }
    return fetch(API_BASE + path, {
      method: 'DELETE',
      headers
    }).then(async r => {
      try { return await r.json() } catch { return {} }
    }).catch(() => ({}))
  },
}

export function formatBytes(bytes) {
  if (!bytes || bytes < 0) return '—'
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
}

export function formatDate(ts) {
  if (!ts) return '—'
  return new Date(ts).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })
}

const PAGES = {
  dashboard: Dashboard,
  files: FileBrowser,
  pairing: Pairing,
  devices: Devices,
  transfers: Transfers,
}

export default function App() {
  // User profile with persistence in localStorage (default to Akshat Kumar / akshat@gmail.com)
  const [user, setUser] = useState(() => {
    try {
      const saved = localStorage.getItem('boltshare_user') || sessionStorage.getItem('ls_user')
      if (saved) return JSON.parse(saved)
    } catch {}
    return { name: 'Akshat Kumar', email: 'akshat@gmail.com', avatar: 'AK' }
  })

  const [page, setPage] = useState('files') // Default to FileBrowser as shown in Image 2
  const [toasts, setToasts] = useState([])
  const [showProfileModal, setShowProfileModal] = useState(false)
  const [editName, setEditName] = useState('')
  const [editEmail, setEditEmail] = useState('')
  const [globalSearch, setGlobalSearch] = useState('')
  const [connectedDevice, setConnectedDevice] = useState(null)

  const addToast = useCallback((msg, type = 'info') => {
    const id = Date.now()
    setToasts(t => [...t, { id, msg, type }])
    setTimeout(() => setToasts(t => t.filter(x => x.id !== id)), 4000)
  }, [])

  // Poll connected device
  useEffect(() => {
    const checkDevice = () => {
      fetch('http://localhost:8080/api/auth/devices')
        .then(r => r.json())
        .then(d => {
          if (Array.isArray(d) && d.length > 0) {
            setConnectedDevice(d[0])
          } else {
            setConnectedDevice(null)
          }
        })
        .catch(() => setConnectedDevice(null))
    }
    checkDevice()
    const interval = setInterval(checkDevice, 4000)
    return () => clearInterval(interval)
  }, [])

  const openProfileEditor = () => {
    setEditName(user?.name || 'Akshat Kumar')
    setEditEmail(user?.email || 'akshat@gmail.com')
    setShowProfileModal(true)
  }

  const saveProfile = (e) => {
    e.preventDefault()
    const updated = {
      ...user,
      name: editName.trim() || 'Akshat Kumar',
      email: editEmail.trim() || 'akshat@gmail.com',
    }
    setUser(updated)
    localStorage.setItem('boltshare_user', JSON.stringify(updated))
    setShowProfileModal(false)
    addToast('Profile updated successfully!', 'success')
  }

  const handleLogout = () => {
    setUser(null)
    sessionStorage.removeItem('ls_user')
    localStorage.removeItem('ls_token')
  }

  const killSwitch = async () => {
    if (!confirm('⚠️ Revoke ALL active sessions? All devices will be disconnected immediately.')) return
    try {
      await api.post('/api/auth/revoke-all', {})
      addToast('🔴 Kill switch activated — all sessions revoked', 'error')
    } catch {
      addToast('Kill switch failed — check server connection', 'error')
    }
  }

  const getInitials = (name) => {
    if (!name) return 'AK'
    const parts = name.trim().split(' ')
    if (parts.length >= 2) return (parts[0][0] + parts[1][0]).toUpperCase()
    return name.substring(0, 2).toUpperCase()
  }

  const PageComponent = PAGES[page] || FileBrowser

  return (
    <div className="app-layout" style={{ display: 'flex', height: '100vh', width: '100vw', background: '#1e2535', color: '#f8fafc', overflow: 'hidden' }}>
      <Sidebar
        activePage={page}
        onNavigate={setPage}
        onKillSwitch={killSwitch}
        user={user}
        onEditProfile={openProfileEditor}
        onLogout={handleLogout}
      />

      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', minWidth: 0, height: '100vh', overflow: 'hidden' }}>
        {/* Top Header matching Image 2 */}
        <header
          style={{
            height: 64,
            background: '#1e2535',
            borderBottom: '1px solid #2d364a',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '0 28px',
            flexShrink: 0
          }}
        >
          {/* Global Search Bar */}
          <div style={{ position: 'relative', width: 380 }}>
            <span style={{ position: 'absolute', left: 14, top: '50%', transform: 'translateY(-50%)', color: '#8a99b5', fontSize: 14 }}>🔍</span>
            <input
              type="text"
              placeholder="Search files, folders..."
              value={globalSearch}
              onChange={e => setGlobalSearch(e.target.value)}
              style={{
                width: '100%',
                height: 38,
                paddingLeft: 38,
                paddingRight: 75,
                background: '#242c3d',
                border: '1px solid #37435e',
                borderRadius: 10,
                color: '#ffffff',
                fontSize: 13,
                outline: 'none'
              }}
            />
            <span
              style={{
                position: 'absolute',
                right: 8,
                top: '50%',
                transform: 'translateY(-50%)',
                background: '#2c3549',
                border: '1px solid #3e4b67',
                borderRadius: 6,
                padding: '2px 6px',
                fontSize: 10,
                color: '#94a3b8',
                fontWeight: 600
              }}
            >
              Ctrl + K
            </span>
          </div>

          {/* Right Header Status & Actions */}
          <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
            {/* Wi-Fi Icon */}
            <span style={{ fontSize: 16, color: '#94a3b8', cursor: 'pointer' }} title="LAN Wi-Fi Direct">📶</span>

            {/* Connected Badge */}
            {connectedDevice ? (
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 8,
                  background: 'rgba(16, 185, 129, 0.15)',
                  border: '1px solid rgba(16, 185, 129, 0.35)',
                  borderRadius: 20,
                  padding: '5px 14px',
                  fontSize: 12,
                  color: '#34d399',
                  fontWeight: 500
                }}
              >
                <span style={{ width: 7, height: 7, borderRadius: '50%', background: '#10b981', display: 'inline-block', boxShadow: '0 0 8px #10b981' }}></span>
                Connected: {connectedDevice.deviceName} ({connectedDevice.ipAddress})
              </div>
            ) : (
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 8,
                  background: 'rgba(148, 163, 184, 0.1)',
                  border: '1px solid rgba(148, 163, 184, 0.25)',
                  borderRadius: 20,
                  padding: '5px 14px',
                  fontSize: 12,
                  color: '#94a3b8',
                  fontWeight: 500
                }}
              >
                <span style={{ width: 7, height: 7, borderRadius: '50%', background: '#64748b', display: 'inline-block' }}></span>
                No Device Connected
              </div>
            )}

            {/* Notification Bell */}
            <span style={{ fontSize: 16, color: '#94a3b8', cursor: 'pointer' }} title="Notifications">🔔</span>

            {/* Avatar Circle */}
            <div
              onClick={openProfileEditor}
              title="Click to edit profile"
              style={{
                width: 34,
                height: 34,
                borderRadius: '50%',
                background: '#283146',
                color: '#ffffff',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontWeight: 700,
                fontSize: 13,
                cursor: 'pointer',
                border: '1px solid #3d4a66'
              }}
            >
              {getInitials(user?.name)}
            </div>
          </div>
        </header>

        {/* Main Content Area matching Image 2 */}
        <main style={{ flex: 1, overflowY: 'auto', background: '#dce3ec' }}>
          <PageComponent addToast={addToast} globalSearch={globalSearch} connectedDevice={connectedDevice} onNavigate={setPage} />
        </main>
      </div>

      {/* Edit Profile Modal */}
      {showProfileModal && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            background: 'rgba(0, 0, 0, 0.75)',
            backdropFilter: 'blur(5px)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 9999
          }}
          onClick={() => setShowProfileModal(false)}
        >
          <div
            style={{
              background: '#252d3e',
              border: '1px solid #3a4661',
              borderRadius: 18,
              padding: 24,
              width: 420,
              boxShadow: '0 20px 40px rgba(0,0,0,0.5)'
            }}
            onClick={e => e.stopPropagation()}
          >
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 18 }}>
              <h3 style={{ margin: 0, fontSize: 18, color: '#f8fafc', fontWeight: 700 }}>Edit User Profile</h3>
              <span style={{ cursor: 'pointer', color: '#8a99b5', fontSize: 18 }} onClick={() => setShowProfileModal(false)}>✕</span>
            </div>

            <form onSubmit={saveProfile}>
              <div style={{ marginBottom: 14 }}>
                <label style={{ display: 'block', fontSize: 12, color: '#8a99b5', marginBottom: 6, fontWeight: 500 }}>Full Name</label>
                <input
                  type="text"
                  value={editName}
                  onChange={e => setEditName(e.target.value)}
                  style={{
                    width: '100%',
                    height: 40,
                    padding: '0 12px',
                    background: '#1c2230',
                    border: '1px solid #354058',
                    borderRadius: 10,
                    color: '#ffffff',
                    fontSize: 14,
                    outline: 'none'
                  }}
                  required
                />
              </div>

              <div style={{ marginBottom: 20 }}>
                <label style={{ display: 'block', fontSize: 12, color: '#8a99b5', marginBottom: 6, fontWeight: 500 }}>Email ID</label>
                <input
                  type="email"
                  value={editEmail}
                  onChange={e => setEditEmail(e.target.value)}
                  style={{
                    width: '100%',
                    height: 40,
                    padding: '0 12px',
                    background: '#1c2230',
                    border: '1px solid #354058',
                    borderRadius: 10,
                    color: '#ffffff',
                    fontSize: 14,
                    outline: 'none'
                  }}
                  required
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10 }}>
                <button
                  type="button"
                  onClick={() => setShowProfileModal(false)}
                  style={{
                    padding: '8px 16px',
                    borderRadius: 10,
                    background: 'transparent',
                    border: '1px solid #354058',
                    color: '#8a99b5',
                    fontSize: 13,
                    cursor: 'pointer'
                  }}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  style={{
                    padding: '8px 18px',
                    borderRadius: 10,
                    background: '#6366f1',
                    border: 'none',
                    color: '#ffffff',
                    fontWeight: 600,
                    fontSize: 13,
                    cursor: 'pointer'
                  }}
                >
                  Save Changes
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Floating Toast Notifications bottom right matching Image 2 */}
      <div
        className="toast-container"
        style={{
          position: 'fixed',
          bottom: 24,
          right: 24,
          display: 'flex',
          flexDirection: 'column',
          gap: 8,
          zIndex: 99999
        }}
      >
        {toasts.map(t => (
          <div
            key={t.id}
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              gap: 12,
              background: '#f1f5f9',
              border: '1px solid #cbd5e1',
              borderRadius: 10,
              padding: '10px 16px',
              color: '#1e293b',
              fontSize: 12,
              fontWeight: 500,
              boxShadow: '0 8px 24px rgba(0,0,0,0.18)',
              animation: 'fadeIn 0.2s ease'
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <span style={{ 
                display: 'inline-flex', 
                alignItems: 'center', 
                justifyContent: 'center', 
                width: 18, 
                height: 18, 
                borderRadius: '50%', 
                background: '#10b981', 
                color: '#ffffff', 
                fontSize: 11, 
                fontWeight: 700 
              }}>✓</span>
              <span>{t.msg}</span>
            </div>
            <span
              style={{ cursor: 'pointer', color: '#64748b', fontWeight: 700, fontSize: 14 }}
              onClick={() => setToasts(list => list.filter(x => x.id !== t.id))}
            >
              ×
            </span>
          </div>
        ))}
      </div>
    </div>
  )
}
