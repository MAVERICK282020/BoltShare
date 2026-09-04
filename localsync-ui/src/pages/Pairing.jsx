import { useState, useEffect, useRef } from 'react'

const API = 'http://localhost:8080'

export default function Pairing({ addToast }) {
  const [qrData, setQrData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [timeLeft, setTimeLeft] = useState(300)
  const [remoteEnabled, setRemoteEnabled] = useState(true)
  const [togglingRemote, setTogglingRemote] = useState(false)
  const timerRef = useRef(null)

  const loadQr = async () => {
    setLoading(true)
    try {
      const resp = await fetch(`${API}/api/auth/generate-qr`)
      if (!resp.ok) throw new Error('Server offline')
      const data = await resp.json()
      setQrData(data)
      setTimeLeft(300)

      // Countdown
      clearInterval(timerRef.current)
      timerRef.current = setInterval(() => {
        setTimeLeft(t => {
          if (t <= 1) { clearInterval(timerRef.current); loadQr(); return 0 }
          return t - 1
        })
      }, 1000)
    } catch (e) {
      addToast?.('Server offline - start the Spring Boot server first', 'error')
    } finally {
      setLoading(false)
    }
  }

  const toggleRemoteAccess = async () => {
    setTogglingRemote(true)
    try {
      const resp = await fetch(`${API}/api/auth/toggle-remote`, { method: 'POST' })
      const data = await resp.json()
      setRemoteEnabled(data.remoteEnabled)
      addToast(data.remoteEnabled ? '🌐 Anywhere Access enabled (Mobile Data & Internet)' : '🔒 Remote Access disabled (Same Wi-Fi only)', 'info')
      loadQr() // reload QR with updated remote setting
    } catch {
      addToast('Failed to toggle remote access', 'error')
    } finally {
      setTogglingRemote(false)
    }
  }

  useEffect(() => {
    loadQr()
    fetch(`${API}/api/auth/network-status`)
      .then(r => r.json())
      .then(d => setRemoteEnabled(d.remoteEnabled))
      .catch(() => {})
    return () => clearInterval(timerRef.current)
  }, [])

  const mins = String(Math.floor(timeLeft / 60)).padStart(2, '0')
  const secs = String(timeLeft % 60).padStart(2, '0')

  return (
    <div className="pairing-container">
      <div className="pairing-card">
        <div style={{ fontSize: 36, marginBottom: 16 }}>🔗</div>
        <h1 className="pairing-title">Pair a New Device</h1>
        <p className="pairing-sub">
          Scan this QR code once from your phone. After pairing, you can access your files anytime — on the same Wi-Fi or outside over mobile data!
        </p>

        {loading ? (
          <div style={{ padding: 40 }}><div className="spinner" style={{ margin: 'auto' }} /></div>
        ) : qrData ? (
          <>
            <div className="qr-wrapper">
              <img src={qrData.qrImage} className="qr-image" alt="QR Code for pairing" />
              <div className="qr-scan-ring"></div>
            </div>
            <div className="qr-expiry">
              <span>⏱</span>
              <span>Expires in <strong>{mins}:{secs}</strong></span>
              <button onClick={loadQr} style={{ marginLeft: 8, color: 'var(--accent)', background: 'none', border: 'none', cursor: 'pointer', fontSize: 12, fontWeight: 600 }}>↻ Refresh</button>
            </div>
          </>
        ) : (
          <div className="empty-state">
            <div className="empty-icon">⚠️</div>
            <div className="empty-text">Server offline. Start LocalSync server first.</div>
            <button className="btn btn-primary" style={{ marginTop: 16 }} onClick={loadQr}>Retry</button>
          </div>
        )}

        {/* Dual Connectivity Cards */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10, margin: '20px 0', textAlign: 'left' }}>
          <div style={{ padding: '12px 14px', borderRadius: 10, background: 'var(--bg-elevated)', border: '1px solid var(--border)', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
              <div style={{ width: 32, height: 32, borderRadius: 8, background: 'var(--green-bg)', color: 'var(--green)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 16 }}>⚡</div>
              <div>
                <div style={{ fontSize: 13, fontWeight: 600, color: 'var(--text-primary)' }}>Same Wi-Fi (LAN Direct)</div>
                <div style={{ fontSize: 11, color: 'var(--text-muted)' }}>{qrData?.localUrl || 'http://localhost:8080'} · Zero data consumed</div>
              </div>
            </div>
            <span className="tag tag-green">Active</span>
          </div>

          <div style={{ padding: '12px 14px', borderRadius: 10, background: 'var(--bg-elevated)', border: '1px solid var(--border)', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
              <div style={{ width: 32, height: 32, borderRadius: 8, background: 'var(--accent-bg)', color: 'var(--accent)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 16 }}>🌐</div>
              <div>
                <div style={{ fontSize: 13, fontWeight: 600, color: 'var(--text-primary)' }}>Anywhere Access (Mobile Data / Remote)</div>
                <div style={{ fontSize: 11, color: 'var(--text-muted)' }}>
                  {remoteEnabled ? (qrData?.remoteUrl || 'Active') : 'Disabled (Local only)'}
                </div>
              </div>
            </div>
            <button 
              className={`btn btn-xs ${remoteEnabled ? 'btn-primary' : 'btn-secondary'}`}
              onClick={toggleRemoteAccess}
              disabled={togglingRemote}
            >
              {remoteEnabled ? 'Enabled' : 'Disabled'}
            </button>
          </div>
        </div>

        <div className="pairing-steps">
          <div style={{ fontSize: 11, fontWeight: 700, color: 'var(--text-muted)', marginBottom: 8, textTransform: 'uppercase', letterSpacing: 0.8 }}>How it works</div>
          {[
            'Scan this QR code once with your phone',
            'Phone stores a permanent 1-Year Device Token',
            'Same Wi-Fi: Connects directly at high speed',
            'Mobile Data / Outside: Switches to Encrypted Remote Gateway automatically',
          ].map((step, i) => (
            <div key={i} className="pairing-step">
              <div className="step-num">{i + 1}</div>
              <span>{step}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
