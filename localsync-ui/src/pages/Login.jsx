import { useState } from 'react'

const OAUTH_PROVIDERS = [
  {
    id: 'google',
    name: 'Continue with Google',
    icon: (
      <svg viewBox="0 0 24 24" className="social-btn-icon">
        <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
        <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
        <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
        <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
      </svg>
    ),
  },
  {
    id: 'github',
    name: 'Continue with GitHub',
    icon: (
      <svg viewBox="0 0 24 24" className="social-btn-icon" fill="currentColor">
        <path d="M12 .297c-6.63 0-12 5.373-12 12 0 5.303 3.438 9.8 8.205 11.385.6.113.82-.258.82-.577 0-.285-.01-1.04-.015-2.04-3.338.724-4.042-1.61-4.042-1.61C4.422 18.07 3.633 17.7 3.633 17.7c-1.087-.744.084-.729.084-.729 1.205.084 1.838 1.236 1.838 1.236 1.07 1.835 2.809 1.305 3.495.998.108-.776.417-1.305.76-1.605-2.665-.3-5.466-1.332-5.466-5.93 0-1.31.465-2.38 1.235-3.22-.135-.303-.54-1.523.105-3.176 0 0 1.005-.322 3.3 1.23.96-.267 1.98-.399 3-.405 1.02.006 2.04.138 3 .405 2.28-1.552 3.285-1.23 3.285-1.23.645 1.653.24 2.873.12 3.176.765.84 1.23 1.91 1.23 3.22 0 4.61-2.805 5.625-5.475 5.92.42.36.81 1.096.81 2.22 0 1.606-.015 2.896-.015 3.286 0 .315.21.69.825.57C20.565 22.092 24 17.592 24 12.297c0-6.627-5.373-12-12-12"/>
      </svg>
    ),
  },
  {
    id: 'microsoft',
    name: 'Continue with Microsoft',
    icon: (
      <svg viewBox="0 0 24 24" className="social-btn-icon">
        <path fill="#f25022" d="M1 1h10v10H1z"/>
        <path fill="#00a4ef" d="M13 1h10v10H13z"/>
        <path fill="#7fba00" d="M1 13h10v10H1z"/>
        <path fill="#ffb900" d="M13 13h10v10H13z"/>
      </svg>
    ),
  },
  {
    id: 'apple',
    name: 'Continue with Apple',
    icon: (
      <svg viewBox="0 0 24 24" className="social-btn-icon" fill="currentColor">
        <path d="M18.71 19.5c-.83 1.24-1.71 2.45-3.05 2.47-1.34.03-1.77-.79-3.29-.79-1.53 0-2 .77-3.27.82-1.31.05-2.3-1.32-3.14-2.53C4.25 17 2.94 12.45 4.7 9.39c.87-1.52 2.43-2.48 4.12-2.51 1.28-.02 2.5.87 3.29.87.78 0 2.26-1.07 3.8-.91.65.03 2.47.26 3.64 1.98-.09.06-2.17 1.28-2.15 3.81.03 3.02 2.65 4.03 2.68 4.04-.03.07-.42 1.44-1.38 2.83M13 3.5c.73-.83 1.94-1.46 2.94-1.5.13 1.17-.34 2.35-1.04 3.19-.69.85-1.83 1.51-2.95 1.42-.15-1.15.41-2.35 1.05-3.11z"/>
      </svg>
    ),
  },
]

export default function Login({ onLogin }) {
  const [serverIp, setServerIp] = useState('192.168.')
  const [loading, setLoading] = useState(null)
  const [localLoading, setLocalLoading] = useState(false)

  const handleSocialAuth = async (provider) => {
    setLoading(provider)
    // Simulate OAuth — in real app, redirect to OAuth provider
    await new Promise(r => setTimeout(r, 1200))
    setLoading(null)
    onLogin({
      name: provider === 'google' ? 'Akshat Kumar' : provider === 'github' ? 'akshat-dev' : 'Akshat',
      email: `akshat@${provider === 'google' ? 'gmail.com' : provider === 'github' ? 'github.com' : 'example.com'}`,
      provider,
      avatar: provider.charAt(0).toUpperCase(),
    })
  }

  const handleLocalConnect = async (e) => {
    e.preventDefault()
    setLocalLoading(true)
    try {
      const res = await fetch(`http://${serverIp}:8080/api/health`)
      const data = await res.json()
      onLogin({
        name: 'Local User',
        email: `Connected to ${data.host}`,
        provider: 'local',
        avatar: 'L',
      })
    } catch {
      alert('Cannot connect to LocalSync server at ' + serverIp + ':8080\n\nMake sure the server is running.')
    } finally {
      setLocalLoading(false)
    }
  }

  return (
    <div className="login-page">
      {/* Left panel */}
      <div className="login-left">
        <div className="login-brand">
          <div className="login-brand-icon">⚡</div>
          <div className="login-brand-name">LocalSync</div>
        </div>

        <div className="login-hero">
          <h1 className="login-hero-title">
            Your files,<br />your network,<br />your control.
          </h1>
          <p className="login-hero-sub">
            Transfer files between your phone and laptop instantly over local Wi-Fi — no cloud, no cables, fully encrypted.
          </p>
          <div className="login-features">
            {[
              ['🔒', 'End-to-end encrypted transfers'],
              ['⚡', 'Resume interrupted transfers'],
              ['🤖', 'AI duplicate detection & insights'],
              ['📱', 'Android app pairing via QR code'],
            ].map(([icon, text]) => (
              <div key={text} className="login-feature">
                <div className="login-feature-icon">{icon}</div>
                {text}
              </div>
            ))}
          </div>
        </div>

        {/* Floating decorative cards */}
        <div className="login-floating-cards" style={{ right: 24 }}>
          <div className="login-float-card">🎬 vacation.mp4 · 15.4 GB · 78%</div>
          <div className="login-float-card">📸 4,832 photos detected</div>
          <div className="login-float-card">✅ Paired: Akshat's Phone</div>
        </div>

        <div className="login-footer-text">© 2026 LocalSync · Local-first · Privacy-by-design</div>
      </div>

      {/* Right panel */}
      <div className="login-right">
        <div className="login-form-box">
          <h2 className="login-form-title">Welcome back 👋</h2>
          <p className="login-form-sub">Sign in to access your LocalSync dashboard</p>

          {/* Social buttons */}
          {OAUTH_PROVIDERS.map(p => (
            <button
              key={p.id}
              className="social-btn"
              onClick={() => handleSocialAuth(p.id)}
              disabled={loading !== null}
              aria-label={p.name}
            >
              {loading === p.id ? (
                <div className="spinner" style={{ width: 20, height: 20, flexShrink: 0 }} />
              ) : p.icon}
              <span className="social-btn-text">
                {loading === p.id ? 'Signing in...' : p.name}
              </span>
            </button>
          ))}

          <div className="login-divider">or connect directly to server</div>

          {/* Local server connect */}
          <div className="login-local-box">
            <div className="login-local-title">🌐 Local Network Connection</div>
            <form onSubmit={handleLocalConnect}>
              <input
                className="login-input"
                value={serverIp}
                onChange={e => setServerIp(e.target.value)}
                placeholder="Server IP (e.g. 192.168.1.100 or localhost)"
                required
                aria-label="LocalSync server IP address"
              />
              <button
                type="submit"
                className="login-connect-btn"
                disabled={localLoading}
                aria-label="Connect to local server"
              >
                {localLoading ? (
                  <span style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8 }}>
                    <div className="spinner" style={{ width: 14, height: 14, borderWidth: 2, borderColor: 'rgba(255,255,255,0.3)', borderTopColor: 'white' }} />
                    Connecting...
                  </span>
                ) : '⚡ Connect to LocalSync Server'}
              </button>
            </form>
            <div style={{ marginTop: 8, fontSize: 11, color: 'var(--text-muted)' }}>
              Server running? Use <code style={{ background: 'var(--bg-base)', padding: '1px 5px', borderRadius: 4 }}>localhost</code> if on same machine
            </div>
          </div>

          <p className="login-terms">
            By signing in, you agree to LocalSync's{' '}
            <a href="#" aria-label="Terms of Service">Terms of Service</a>
            {' '}and{' '}
            <a href="#" aria-label="Privacy Policy">Privacy Policy</a>.
            Your data never leaves your local network.
          </p>
        </div>
      </div>
    </div>
  )
}
