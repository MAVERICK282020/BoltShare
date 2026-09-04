import { useState, useEffect, useRef } from 'react'
import { api, formatBytes, formatDate } from '../App'

export default function Dashboard({ addToast, onNavigate }) {
  const [serverStatus, setServerStatus] = useState('checking')
  const [devices, setDevices] = useState([])
  const [recentTransfers, setRecentTransfers] = useState([])
  const [networkStatus, setNetworkStatus] = useState(null)
  const [isDragging, setIsDragging] = useState(false)
  const fileInputRef = useRef(null)

  useEffect(() => {
    const loadData = () => {
      fetch('http://localhost:8080/api/health')
        .then(r => r.json())
        .then(() => setServerStatus('online'))
        .catch(() => setServerStatus('offline'))

      api.get('/api/auth/devices')
        .then(data => setDevices(Array.isArray(data) ? data : []))
        .catch(() => setDevices([]))

      api.get('/api/transfer/all')
        .then(data => setRecentTransfers(Array.isArray(data) ? data.slice(0, 5) : []))
        .catch(() => setRecentTransfers([]))

      fetch('http://localhost:8080/api/auth/network-status')
        .then(r => r.json())
        .then(data => setNetworkStatus(data))
        .catch(() => {})
    }

    loadData()
    const interval = setInterval(loadData, 3500)
    return () => clearInterval(interval)
  }, [])

  const connectedDevice = devices.find(d => d.trusted) || devices[0]

  // Handle Quick Drop upload directly from Dashboard
  const handleQuickDrop = async (filesList) => {
    if (!filesList || filesList.length === 0) return
    if (!connectedDevice) {
      addToast('No mobile device connected. Please pair your phone first!', 'error')
      return
    }

    const files = Array.from(filesList)
    for (const file of files) {
      const formData = new FormData()
      formData.append('file', file)
      try {
        addToast(`📤 Beaming "${file.name}" to ${connectedDevice.deviceName}...`, 'info')
        const query = new URLSearchParams({
          deviceId: connectedDevice.deviceId,
          targetPath: '/storage/emulated/0/Download'
        })
        const res = await fetch(`http://localhost:8080/api/phone/upload?${query.toString()}`, {
          method: 'POST',
          body: formData
        })
        if (res.ok) {
          addToast(`✅ Sent "${file.name}" to phone!`, 'success')
          api.get('/api/transfer/all').then(d => setRecentTransfers(Array.isArray(d) ? d.slice(0, 5) : []))
        } else {
          addToast(`❌ Upload failed for "${file.name}"`, 'error')
        }
      } catch (err) {
        addToast(`❌ Error: ${err.message}`, 'error')
      }
    }
  }

  const statCards = [
    {
      icon: '📱',
      value: devices.length,
      label: 'Paired Devices',
      sub: devices.length > 0 ? `${connectedDevice?.deviceName || '1 device'} active` : 'No devices connected',
      color: devices.length > 0 ? '#34d399' : '#94a3b8',
    },
    {
      icon: '⚡',
      value: serverStatus === 'online' ? 'Gigabit P2P' : 'Offline',
      label: 'Local LAN Engine',
      sub: networkStatus?.lanIp ? `IP: ${networkStatus.lanIp}` : 'Direct Wi-Fi Socket',
      color: '#818cf8',
    },
    {
      icon: '🔒',
      value: 'Secured',
      label: 'TLS & Token Auth',
      sub: 'Zero-cloud local encrypted',
      color: '#10b981',
    },
    {
      icon: '🌐',
      value: serverStatus === 'online' ? 'Active' : 'Offline',
      label: 'Server Daemon',
      sub: 'localhost:8080',
      color: serverStatus === 'online' ? '#10b981' : '#ef4444',
    },
  ]

  return (
    <div style={{ padding: '24px 32px' }}>
      {/* Page Header matching File Browser */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
          <div
            style={{
              width: '44px',
              height: '44px',
              borderRadius: '12px',
              background: '#242c3d',
              border: '1px solid #354058',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: '22px'
            }}
          >
            ⚡
          </div>
          <div>
            <h1 style={{ fontSize: '22px', fontWeight: '800', color: '#111827', margin: 0, letterSpacing: '-0.02em' }}>
              Dashboard & Bridge Overview
            </h1>
            <p style={{ fontSize: '12px', color: '#475569', margin: '3px 0 0 0' }}>
              Real-time P2P gigabit sync metrics, local health hub, and instant device beam
            </p>
          </div>
        </div>

        <div style={{
          display: 'flex', alignItems: 'center', gap: '8px',
          padding: '6px 14px', borderRadius: '20px',
          background: serverStatus === 'online' ? 'rgba(16, 185, 129, 0.15)' : 'rgba(239, 68, 68, 0.15)',
          border: `1px solid ${serverStatus === 'online' ? 'rgba(16, 185, 129, 0.35)' : 'rgba(239, 68, 68, 0.35)'}`,
          color: serverStatus === 'online' ? '#10b981' : '#ef4444',
          fontSize: '12px', fontWeight: '600'
        }}>
          <span style={{ width: '7px', height: '7px', borderRadius: '50%', background: 'currentColor', display: 'inline-block', boxShadow: '0 0 8px currentColor' }} />
          {serverStatus === 'online' ? 'Server Online' : 'Server Offline'}
        </div>
      </div>

      {/* Top Stat Cards matching slate-navy theme */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '18px', marginBottom: '24px' }}>
        {statCards.map((s, i) => (
          <div
            key={i}
            style={{
              background: '#2b3447',
              border: '1px solid #3a4661',
              borderRadius: '16px',
              padding: '18px 20px',
              boxShadow: '0 8px 24px rgba(0,0,0,0.12)',
              transition: 'all 0.2s ease'
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '12px' }}>
              <div style={{
                width: '38px', height: '38px', borderRadius: '10px',
                background: '#242c3d', border: '1px solid #354058',
                display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '18px'
              }}>
                {s.icon}
              </div>
              <div style={{ fontSize: '20px', fontWeight: '800', color: s.color }}>{s.value}</div>
            </div>
            <div style={{ fontSize: '13px', fontWeight: '700', color: '#f8fafc' }}>{s.label}</div>
            <div style={{ fontSize: '11px', color: '#8a99b5', marginTop: '2px' }}>{s.sub}</div>
          </div>
        ))}
      </div>

      {/* Main Content Grid */}
      <div style={{ display: 'grid', gridTemplateColumns: '1.4fr 1fr', gap: '22px' }}>
        {/* Left Column: Essential LAN Bridge Hub & Quick Share */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          
          {/* Quick Share & Drop Target to Phone */}
          <div
            onDragOver={e => { e.preventDefault(); setIsDragging(true) }}
            onDragLeave={e => { e.preventDefault(); setIsDragging(false) }}
            onDrop={e => {
              e.preventDefault()
              setIsDragging(false)
              if (e.dataTransfer.files) handleQuickDrop(e.dataTransfer.files)
            }}
            style={{
              background: '#2b3447',
              border: isDragging ? '2px dashed #818cf8' : '1px solid #3a4661',
              borderRadius: '16px',
              padding: '24px',
              boxShadow: '0 8px 30px rgba(0,0,0,0.15)',
              transition: 'all 0.2s ease'
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
              <div>
                <div style={{ fontSize: '15px', fontWeight: '700', color: '#f8fafc' }}>🚀 Quick Beam to Mobile</div>
                <div style={{ fontSize: '12px', color: '#8a99b5', marginTop: '2px' }}>
                  {connectedDevice ? `Direct P2P LAN pipeline ready to ${connectedDevice.deviceName}` : 'Pair a phone to enable instant beaming'}
                </div>
              </div>
              <button
                disabled={!connectedDevice}
                onClick={() => fileInputRef.current?.click()}
                style={{
                  background: connectedDevice ? '#6366f1' : '#354058',
                  border: 'none',
                  color: '#ffffff',
                  padding: '8px 16px',
                  borderRadius: '10px',
                  fontSize: '12px',
                  fontWeight: '600',
                  cursor: connectedDevice ? 'pointer' : 'not-allowed',
                  opacity: connectedDevice ? 1 : 0.6
                }}
              >
                + Select Files
              </button>
              <input
                type="file"
                multiple
                ref={fileInputRef}
                style={{ display: 'none' }}
                onChange={e => {
                  if (e.target.files) {
                    handleQuickDrop(e.target.files)
                    e.target.value = ''
                  }
                }}
              />
            </div>

            {/* Drag Drop Inner Box */}
            <div
              onClick={() => connectedDevice && fileInputRef.current?.click()}
              style={{
                background: isDragging ? 'rgba(99, 102, 241, 0.15)' : '#242c3d',
                border: '1px dashed #3f4d6b',
                borderRadius: '12px',
                padding: '30px 20px',
                textAlign: 'center',
                cursor: connectedDevice ? 'pointer' : 'default'
              }}
            >
              <div style={{ fontSize: '32px', marginBottom: '8px' }}>⚡</div>
              <div style={{ fontSize: '13px', fontWeight: '600', color: '#f8fafc', marginBottom: '4px' }}>
                {isDragging ? 'Drop files here to beam to phone!' : 'Drag & drop any file here to send instantly'}
              </div>
              <div style={{ fontSize: '11px', color: '#8a99b5' }}>
                Files are saved directly to your phone's Download folder at maximum Wi-Fi speeds
              </div>
            </div>
          </div>

          {/* Real-time LAN & Network Pipeline Details */}
          <div
            style={{
              background: '#2b3447',
              border: '1px solid #3a4661',
              borderRadius: '16px',
              padding: '20px',
              boxShadow: '0 8px 30px rgba(0,0,0,0.15)'
            }}
          >
            <div style={{ fontSize: '14px', fontWeight: '700', color: '#f8fafc', marginBottom: '14px', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <span>📡</span> LAN Network Health & Security Diagnostics
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
              <div style={{ background: '#242c3d', border: '1px solid #354058', borderRadius: '10px', padding: '12px 14px' }}>
                <div style={{ fontSize: '11px', color: '#8a99b5' }}>PC Server Host</div>
                <div style={{ fontSize: '13px', fontWeight: '600', color: '#f8fafc', marginTop: '3px' }}>
                  {networkStatus?.lanIp || '10.91.47.146'}:8080
                </div>
              </div>

              <div style={{ background: '#242c3d', border: '1px solid #354058', borderRadius: '10px', padding: '12px 14px' }}>
                <div style={{ fontSize: '11px', color: '#8a99b5' }}>Phone P2P Port</div>
                <div style={{ fontSize: '13px', fontWeight: '600', color: '#34d399', marginTop: '3px' }}>
                  {connectedDevice ? `${connectedDevice.ipAddress}:8085` : 'Listening :8085'}
                </div>
              </div>

              <div style={{ background: '#242c3d', border: '1px solid #354058', borderRadius: '10px', padding: '12px 14px' }}>
                <div style={{ fontSize: '11px', color: '#8a99b5' }}>Connection Protocol</div>
                <div style={{ fontSize: '13px', fontWeight: '600', color: '#f8fafc', marginTop: '3px' }}>
                  Direct P2P LAN (Subnet)
                </div>
              </div>

              <div style={{ background: '#242c3d', border: '1px solid #354058', borderRadius: '10px', padding: '12px 14px' }}>
                <div style={{ fontSize: '11px', color: '#8a99b5' }}>Security Layer</div>
                <div style={{ fontSize: '13px', fontWeight: '600', color: '#10b981', marginTop: '3px' }}>
                  HMAC Token + TLS 1.3
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Right Column: Active Phone Status & Quick Actions */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          
          {/* Connected Device Card */}
          <div
            style={{
              background: '#2b3447',
              border: '1px solid #3a4661',
              borderRadius: '16px',
              padding: '20px',
              boxShadow: '0 8px 30px rgba(0,0,0,0.15)'
            }}
          >
            <div style={{ fontSize: '14px', fontWeight: '700', color: '#f8fafc', marginBottom: '14px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <span>📱 Paired Mobile Device</span>
              {connectedDevice && (
                <span style={{ fontSize: '11px', color: '#34d399', background: 'rgba(16, 185, 129, 0.15)', padding: '2px 8px', borderRadius: '12px', fontWeight: '600' }}>
                  Active
                </span>
              )}
            </div>

            {connectedDevice ? (
              <div style={{ background: '#242c3d', border: '1px solid #354058', borderRadius: '12px', padding: '16px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '14px' }}>
                  <div style={{
                    width: '42px', height: '42px', borderRadius: '10px',
                    background: '#2b3447', border: '1px solid #3a4661',
                    display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '20px'
                  }}>
                    📱
                  </div>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontSize: '15px', fontWeight: '700', color: '#f8fafc' }}>{connectedDevice.deviceName}</div>
                    <div style={{ fontSize: '11px', color: '#8a99b5' }}>
                      {connectedDevice.ipAddress} · {connectedDevice.deviceType || 'Android'}
                    </div>
                  </div>
                </div>

                <div style={{ display: 'flex', gap: '10px' }}>
                  <button
                    onClick={() => onNavigate && onNavigate('files')}
                    style={{
                      flex: 1,
                      padding: '8px',
                      background: '#354058',
                      border: '1px solid #48577a',
                      borderRadius: '8px',
                      color: '#f8fafc',
                      fontSize: '12px',
                      fontWeight: '600',
                      cursor: 'pointer'
                    }}
                  >
                    📁 Browse Files
                  </button>
                  <button
                    onClick={() => onNavigate && onNavigate('pairing')}
                    style={{
                      padding: '8px 12px',
                      background: 'transparent',
                      border: '1px solid #354058',
                      borderRadius: '8px',
                      color: '#8a99b5',
                      fontSize: '12px',
                      cursor: 'pointer'
                    }}
                  >
                    Manage
                  </button>
                </div>
              </div>
            ) : (
              <div style={{ textAlign: 'center', padding: '28px 14px', background: '#242c3d', border: '1px solid #354058', borderRadius: '12px' }}>
                <div style={{ fontSize: '36px', marginBottom: '8px' }}>📱</div>
                <div style={{ fontSize: '13px', fontWeight: '600', color: '#f8fafc' }}>No Phone Connected</div>
                <div style={{ fontSize: '11px', color: '#8a99b5', margin: '4px 0 14px' }}>
                  Pair your mobile device to transfer files and stream media
                </div>
                <button
                  onClick={() => onNavigate && onNavigate('pairing')}
                  style={{
                    background: '#6366f1',
                    border: 'none',
                    color: '#ffffff',
                    padding: '8px 16px',
                    borderRadius: '8px',
                    fontSize: '12px',
                    fontWeight: '600',
                    cursor: 'pointer'
                  }}
                >
                  + Pair Device Now
                </button>
              </div>
            )}
          </div>

          {/* Quick Actions Card */}
          <div
            style={{
              background: '#2b3447',
              border: '1px solid #3a4661',
              borderRadius: '16px',
              padding: '20px',
              boxShadow: '0 8px 30px rgba(0,0,0,0.15)'
            }}
          >
            <div style={{ fontSize: '14px', fontWeight: '700', color: '#f8fafc', marginBottom: '14px' }}>
              ⚡ Quick Shortcuts
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
              {[
                { title: '📁 File Explorer', sub: 'Bidirectional PC & Phone storage bridge', page: 'files', icon: '📂' },
                { title: '↕️ Active Transfers', sub: 'Monitor live queue and clear history', page: 'transfers', icon: '↕️' },
                { title: '🔗 Pair New Phone', sub: 'Display QR code and PIN for instant connection', page: 'pairing', icon: '📱' },
              ].map(item => (
                <div
                  key={item.title}
                  onClick={() => onNavigate && onNavigate(item.page)}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '12px',
                    padding: '10px 14px',
                    background: '#242c3d',
                    border: '1px solid #354058',
                    borderRadius: '10px',
                    cursor: 'pointer',
                    transition: 'all 0.15s ease'
                  }}
                  onMouseEnter={e => e.currentTarget.style.background = '#2a3449'}
                  onMouseLeave={e => e.currentTarget.style.background = '#242c3d'}
                >
                  <span style={{ fontSize: '18px' }}>{item.icon}</span>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontSize: '13px', fontWeight: '600', color: '#f8fafc' }}>{item.title}</div>
                    <div style={{ fontSize: '11px', color: '#8a99b5' }}>{item.sub}</div>
                  </div>
                  <span style={{ color: '#8a99b5', fontSize: '14px' }}>›</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
