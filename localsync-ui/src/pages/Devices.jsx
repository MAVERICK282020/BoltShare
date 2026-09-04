import { useState, useEffect } from 'react'
import { api } from '../App'

const PERMISSION_FOLDERS = ['Documents', 'Downloads', 'Pictures', 'Videos', 'Desktop', 'Music']

export default function Devices({ addToast }) {
  const [devices, setDevices] = useState([])
  const [selected, setSelected] = useState(null)
  const [networkStatus, setNetworkStatus] = useState(null)
  const [loadingStatus, setLoadingStatus] = useState(false)

  const loadDevices = () => {
    api.get('/api/auth/devices')
      .then(data => {
        if (Array.isArray(data)) {
          setDevices(data)
          if (data.length > 0 && !selected) setSelected(data[0])
        }
      })
      .catch(() => {})
  }

  const loadNetworkStatus = () => {
    fetch('http://localhost:8080/api/auth/network-status')
      .then(r => r.json())
      .then(d => setNetworkStatus(d))
      .catch(() => {})
  }

  useEffect(() => {
    loadDevices()
    loadNetworkStatus()
    const interval = setInterval(() => {
      loadDevices()
    }, 4000)
    return () => clearInterval(interval)
  }, [])

  const toggleRemote = async () => {
    setLoadingStatus(true)
    try {
      const resp = await fetch('http://localhost:8080/api/auth/toggle-remote', { method: 'POST' })
      const data = await resp.json()
      setNetworkStatus(data)
      addToast(data.remoteEnabled ? '🌐 Anywhere Access enabled (Mobile Data & Internet)' : '🔒 Remote Access disabled (LAN only)', 'info')
    } catch {
      addToast('Failed to toggle remote access', 'error')
    } finally {
      setLoadingStatus(false)
    }
  }

  const unpair = async (deviceId) => {
    if (!confirm('Remove this device? It will lose access immediately.')) return
    try {
      await api.delete(`/api/auth/devices/${deviceId}`)
      setDevices(d => d.filter(x => x.deviceId !== deviceId))
      if (selected?.deviceId === deviceId) setSelected(null)
      addToast('Device removed', 'success')
    } catch { addToast('Failed to remove device', 'error') }
  }

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Devices</h1>
        <p className="page-sub">Manage paired devices, folder permissions, and anywhere access settings</p>
      </div>

      {/* Anywhere Access Banner */}
      <div style={{ padding: '0 28px 20px' }}>
        <div className="card" style={{ padding: '16px 20px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', borderLeft: '4px solid var(--accent)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
            <div style={{ width: 40, height: 40, borderRadius: 10, background: 'var(--accent-bg)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 20 }}>
              🌐
            </div>
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <span style={{ fontSize: 14, fontWeight: 700, color: 'var(--text-primary)' }}>Anywhere Access (Mobile Data & Internet)</span>
                <span className={`tag ${networkStatus?.remoteEnabled ? 'tag-green' : 'tag-grey'}`}>
                  {networkStatus?.remoteEnabled ? 'Active' : 'Disabled'}
                </span>
              </div>
              <div style={{ fontSize: 12, color: 'var(--text-secondary)', marginTop: 2 }}>
                {networkStatus?.remoteEnabled
                  ? `Paired devices can access authorized storage even when outside on 4G/5G mobile data.`
                  : `Devices can only connect when on the same Wi-Fi network.`}
              </div>
            </div>
          </div>
          <button 
            className={`btn ${networkStatus?.remoteEnabled ? 'btn-secondary' : 'btn-primary'} btn-sm`}
            onClick={toggleRemote}
            disabled={loadingStatus}
          >
            {networkStatus?.remoteEnabled ? 'Disable Remote Access' : 'Enable Anywhere Access'}
          </button>
        </div>
      </div>

      <div style={{ padding: '0 28px 28px', display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 20 }}>
        {/* Device List */}
        <div>
          <div style={{ fontSize: 12, fontWeight: 700, color: 'var(--text-muted)', marginBottom: 12, textTransform: 'uppercase', letterSpacing: 0.8 }}>TRUSTED DEVICES</div>
          <div className="devices-list">
            {devices.length === 0 && (
              <div className="empty-state card" style={{ padding: 40 }}>
                <div className="empty-icon">📱</div>
                <div style={{ fontSize: 14, fontWeight: 600, color: 'var(--text-primary)', marginBottom: 4 }}>No paired devices yet</div>
                <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>Scan the QR code from the "Pair Device" page to connect your phone.</div>
              </div>
            )}
            {devices.map(d => (
              <div
                key={d.deviceId}
                className="card device-card"
                style={{ cursor: 'pointer', borderColor: selected?.deviceId === d.deviceId ? 'var(--accent)' : undefined }}
                onClick={() => setSelected(d)}
              >
                <div className="device-avatar">📱</div>
                <div className="device-info">
                  <div className="device-name">{d.deviceName}</div>
                  <div className="device-meta">
                    {d.lastSeenIp ? `IP: ${d.lastSeenIp}` : 'IP unknown'} · {d.deviceType || 'android'}
                  </div>
                  <div style={{ display: 'flex', gap: 6, marginTop: 4 }}>
                    <span className="tag tag-green">✓ Permanent</span>
                    {networkStatus?.remoteEnabled && (
                      <span className="tag tag-blue">🌐 Anywhere</span>
                    )}
                  </div>
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 8, alignItems: 'flex-end' }}>
                  <div className="device-status">
                    <div className="status-dot online"></div>
                    <span style={{ color: 'var(--green)', fontSize: 11, fontWeight: 600 }}>Connected</span>
                  </div>
                  <button className="btn btn-danger btn-xs" onClick={e => { e.stopPropagation(); unpair(d.deviceId) }}>Remove</button>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Permission Panel */}
        <div>
          <div style={{ fontSize: 12, fontWeight: 700, color: 'var(--text-muted)', marginBottom: 12, textTransform: 'uppercase', letterSpacing: 0.8 }}>PERMISSIONS & STORAGE ACCESS</div>
          {selected ? (
            <div className="card" style={{ padding: 20 }}>
              <div style={{ fontSize: 15, fontWeight: 700, marginBottom: 2 }}>{selected.deviceName}</div>
              <div style={{ fontSize: 12, color: 'var(--text-muted)', marginBottom: 18 }}>
                Select which folders this device can browse, stream, and manage:
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                {PERMISSION_FOLDERS.map(folder => (
                  <PermissionRow key={folder} folder={folder} />
                ))}
              </div>

              <div style={{ marginTop: 20, paddingTop: 16, borderTop: '1px solid var(--border)' }}>
                <div style={{ fontSize: 12, fontWeight: 700, color: 'var(--text-muted)', marginBottom: 8, textTransform: 'uppercase', letterSpacing: 0.8 }}>Access Mode</div>
                <div style={{ padding: '10px 12px', background: 'var(--bg-elevated)', borderRadius: 8, fontSize: 12, color: 'var(--text-secondary)' }}>
                  ✅ <strong>Persistent Virtual Drive:</strong> Device can stream videos and open files directly without downloading or re-scanning QR codes.
                </div>
              </div>
            </div>
          ) : (
            <div className="card empty-state" style={{ minHeight: 240 }}>
              <div className="empty-icon">👆</div>
              <div className="empty-text">Select a device to view and edit authorized storage permissions</div>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

function PermissionRow({ folder }) {
  const [read, setRead] = useState(true)
  const [write, setWrite] = useState(false)
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '8px 10px', borderRadius: 8, background: 'var(--bg-elevated)', border: '1px solid var(--border)' }}>
      <span style={{ fontSize: 16 }}>📁</span>
      <span style={{ flex: 1, fontSize: 13, fontWeight: 600 }}>{folder}</span>
      <label style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 12, color: 'var(--text-secondary)', cursor: 'pointer' }}>
        <input type="checkbox" checked={read} onChange={e => setRead(e.target.checked)} />
        Read / Stream
      </label>
      <label style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 12, color: 'var(--text-secondary)', cursor: 'pointer' }}>
        <input type="checkbox" checked={write} onChange={e => setWrite(e.target.checked)} />
        Upload / Edit
      </label>
    </div>
  )
}
