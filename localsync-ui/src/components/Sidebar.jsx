import { useState } from 'react'

const NAV_MAIN = [
  { id: 'dashboard', icon: '⚡', label: 'Dashboard' },
  { id: 'files',     icon: '📁', label: 'File Browser' },
  { id: 'transfers', icon: '↕️', label: 'Transfers' },
]
const NAV_SECURITY = [
  { id: 'devices', icon: '📱', label: 'Devices' },
  { id: 'pairing', icon: '🔗', label: 'Pair Device' },
]

export default function Sidebar({ activePage, onNavigate, onKillSwitch, user, onEditProfile, onLogout }) {
  const [showUserMenu, setShowUserMenu] = useState(false)

  const getInitials = (name) => {
    if (!name) return 'AK'
    const parts = name.trim().split(' ')
    if (parts.length >= 2) return (parts[0][0] + parts[1][0]).toUpperCase()
    return name.substring(0, 2).toUpperCase()
  }

  return (
    <aside
      className="sidebar"
      role="navigation"
      aria-label="Main navigation"
      style={{
        width: 240,
        minWidth: 240,
        height: '100vh',
        background: '#151924',
        borderRight: '1px solid #232a3d',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'space-between'
      }}
    >
      <div>
        {/* Brand */}
        <div className="sidebar-brand" style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '18px 16px' }}>
          <img
            src="/logo.png"
            alt="BoltShare"
            style={{ width: 38, height: 38, borderRadius: 10, objectFit: 'cover', boxShadow: '0 0 16px rgba(99, 102, 241, 0.45)' }}
          />
          <div>
            <div className="sidebar-brand-name" style={{ fontSize: 16, fontWeight: 700, color: '#f8fafc', letterSpacing: '-0.02em' }}>BoltShare</div>
            <div style={{ fontSize: 11, color: '#8a99b5', fontWeight: 500 }}>Storage Bridge</div>
          </div>
        </div>

        {/* Nav */}
        <nav className="sidebar-nav">
          <div className="nav-section-label" style={{ fontSize: 11, textTransform: 'uppercase', letterSpacing: '0.08em', color: '#64748b', fontWeight: 600, padding: '10px 16px 4px' }}>MAIN</div>
          {NAV_MAIN.map(item => (
            <div
              key={item.id}
              className={`nav-item ${activePage === item.id ? 'active' : ''}`}
              onClick={() => onNavigate(item.id)}
              role="button"
              tabIndex={0}
              aria-current={activePage === item.id ? 'page' : undefined}
              onKeyDown={e => e.key === 'Enter' && onNavigate(item.id)}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 12,
                padding: '10px 14px',
                borderRadius: 12,
                margin: '3px 10px',
                cursor: 'pointer',
                fontWeight: 500,
                fontSize: 13,
                color: activePage === item.id ? '#ffffff' : '#94a3b8',
                background: activePage === item.id ? '#2b3447' : 'transparent',
                border: activePage === item.id ? '1px solid #3d4a66' : '1px solid transparent',
                boxShadow: activePage === item.id ? '0 2px 10px rgba(0,0,0,0.25)' : 'none',
                transition: 'all 0.15s ease'
              }}
              onMouseEnter={e => {
                if (activePage !== item.id) e.currentTarget.style.background = '#1c2232'
              }}
              onMouseLeave={e => {
                if (activePage !== item.id) e.currentTarget.style.background = 'transparent'
              }}
            >
              <span className="nav-icon" aria-hidden="true" style={{ fontSize: 16 }}>{item.icon}</span>
              {item.label}
            </div>
          ))}

          <div className="nav-section-label" style={{ fontSize: 11, textTransform: 'uppercase', letterSpacing: '0.08em', color: '#64748b', fontWeight: 600, padding: '16px 16px 4px' }}>SECURITY</div>
          {NAV_SECURITY.map(item => (
            <div
              key={item.id}
              className={`nav-item ${activePage === item.id ? 'active' : ''}`}
              onClick={() => onNavigate(item.id)}
              role="button"
              tabIndex={0}
              aria-current={activePage === item.id ? 'page' : undefined}
              onKeyDown={e => e.key === 'Enter' && onNavigate(item.id)}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 12,
                padding: '10px 14px',
                borderRadius: 12,
                margin: '3px 10px',
                cursor: 'pointer',
                fontWeight: 500,
                fontSize: 13,
                color: activePage === item.id ? '#ffffff' : '#94a3b8',
                background: activePage === item.id ? '#2b3447' : 'transparent',
                border: activePage === item.id ? '1px solid #3d4a66' : '1px solid transparent',
                transition: 'all 0.15s ease'
              }}
              onMouseEnter={e => {
                if (activePage !== item.id) e.currentTarget.style.background = '#1c2232'
              }}
              onMouseLeave={e => {
                if (activePage !== item.id) e.currentTarget.style.background = 'transparent'
              }}
            >
              <span className="nav-icon" aria-hidden="true" style={{ fontSize: 16 }}>{item.icon}</span>
              {item.label}
            </div>
          ))}
        </nav>
      </div>

      <div>
        {/* Kill Switch Button matching Image 2 */}
        <div className="sidebar-footer" style={{ padding: '12px 16px' }}>
          <button
            className="kill-switch-btn"
            onClick={onKillSwitch}
            aria-label="Emergency kill switch — disconnect all devices"
            title="Disconnect all paired devices immediately"
            style={{
              width: '100%',
              padding: '10px 14px',
              borderRadius: 12,
              background: 'rgba(239, 68, 68, 0.1)',
              border: '1px solid rgba(239, 68, 68, 0.45)',
              color: '#f87171',
              fontWeight: 600,
              fontSize: 12,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: 8,
              cursor: 'pointer',
              transition: 'all 0.2s'
            }}
          >
            <span aria-hidden="true" style={{ width: 8, height: 8, borderRadius: '50%', background: '#ef4444', display: 'inline-block' }} />
            Kill Switch
          </button>
        </div>

        {/* User Section (Editable) */}
        <div className="sidebar-user" style={{ padding: '12px 16px', borderTop: '1px solid #232a3d' }}>
          <div
            className="user-row"
            onClick={onEditProfile}
            role="button"
            tabIndex={0}
            aria-label="User profile — click to edit"
            title="Click to edit user profile details"
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 12,
              cursor: 'pointer',
              padding: '6px 8px',
              borderRadius: 10,
              transition: 'background 0.15s'
            }}
            onMouseEnter={e => e.currentTarget.style.background = '#1e2535'}
            onMouseLeave={e => e.currentTarget.style.background = 'transparent'}
          >
            <div
              className="user-avatar"
              style={{
                width: 38,
                height: 38,
                borderRadius: '50%',
                background: '#283146',
                color: '#f8fafc',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontWeight: 700,
                fontSize: 14,
                border: '1px solid #3d4a66'
              }}
            >
              {getInitials(user?.name)}
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div className="user-name" style={{ fontSize: 13, fontWeight: 600, color: '#f8fafc', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                {user?.name || 'Akshat Kumar'}
              </div>
              <div className="user-email" style={{ fontSize: 11, color: '#8a99b5', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                {user?.email || 'akshat@gmail.com'}
              </div>
            </div>
            <span style={{ fontSize: 14, color: '#64748b' }}>›</span>
          </div>
        </div>
      </div>
    </aside>
  )
}
