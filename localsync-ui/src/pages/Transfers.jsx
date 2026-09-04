import { useState, useEffect } from 'react'
import { api, formatBytes } from '../App'

export default function Transfers({ addToast }) {
  const [transfers, setTransfers] = useState([])
  const [loading, setLoading] = useState(true)

  const loadTransfers = () => {
    api.get('/api/transfer/all')
      .then(data => { setTransfers(Array.isArray(data) ? data : []); setLoading(false) })
      .catch(() => { setTransfers([]); setLoading(false) })
  }

  // Auto-refresh transfer queue every 2.5 seconds
  useEffect(() => { 
    loadTransfers()
    const id = setInterval(loadTransfers, 2500)
    return () => clearInterval(id)
  }, [])

  const cancel = async (jobId) => {
    try {
      await api.delete(`/api/transfer/${jobId}`)
      addToast('Transfer cancelled', 'info')
      loadTransfers()
    } catch { addToast('Cancel failed', 'error') }
  }

  const clearHistory = async () => {
    try {
      const res = await api.delete('/api/transfer/history')
      addToast(res.message || 'Transfer history cleared', 'success')
      loadTransfers()
    } catch {
      addToast('Failed to clear history', 'error')
    }
  }

  const active = transfers.filter(t => ['PENDING', 'IN_PROGRESS', 'PAUSED'].includes(t.status))
  const done = transfers.filter(t => ['COMPLETED', 'FAILED', 'CANCELLED'].includes(t.status))

  if (loading) return (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '60vh' }}>
      <div className="spinner" />
    </div>
  )

  return (
    <div style={{ padding: '24px 32px' }}>
      {/* Header */}
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
            ↕️
          </div>
          <div>
            <h1 style={{ fontSize: '22px', fontWeight: '800', color: '#111827', margin: 0, letterSpacing: '-0.02em' }}>
              Transfers & Pipeline Hub
            </h1>
            <p style={{ fontSize: '12px', color: '#475569', margin: '3px 0 0 0' }}>
              Real-time high-speed chunked P2P pipeline over local Wi-Fi
            </p>
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          {active.length > 0 && (
            <div style={{
              padding: '6px 14px', borderRadius: '20px',
              background: 'rgba(99, 102, 241, 0.15)', border: '1px solid rgba(99, 102, 241, 0.35)',
              color: '#818cf8', fontSize: '12px', fontWeight: '700', display: 'flex', alignItems: 'center', gap: '6px'
            }}>
              <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: '#818cf8', animation: 'pulse 1.5s infinite' }} />
              {active.length} Transfer{active.length > 1 ? 's' : ''} Active
            </div>
          )}

          {done.length > 0 && (
            <button
              id="clear-history-btn"
              onClick={clearHistory}
              style={{
                padding: '7px 16px',
                borderRadius: '10px',
                background: 'rgba(239, 68, 68, 0.12)',
                border: '1px solid rgba(239, 68, 68, 0.3)',
                color: '#f87171',
                fontSize: '12px',
                fontWeight: '700',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
                transition: 'all 0.2s ease'
              }}
              onMouseEnter={e => { e.currentTarget.style.background = 'rgba(239, 68, 68, 0.25)'; e.currentTarget.style.color = '#ef4444' }}
              onMouseLeave={e => { e.currentTarget.style.background = 'rgba(239, 68, 68, 0.12)'; e.currentTarget.style.color = '#f87171' }}
            >
              <span>🗑️</span> Clear History
            </button>
          )}
        </div>
      </div>

      {transfers.length === 0 ? (
        <div style={{
          background: '#2b3447', borderRadius: '16px', border: '1px solid #3a4661',
          padding: '60px 20px', textAlign: 'center', boxShadow: '0 8px 30px rgba(0,0,0,0.15)'
        }}>
          <div style={{ fontSize: '48px', marginBottom: '16px' }}>↕️</div>
          <div style={{ fontSize: '18px', fontWeight: '700', color: '#f8fafc', marginBottom: '8px' }}>No Active Transfers</div>
          <div style={{ fontSize: '13px', color: '#8a99b5', maxWidth: '420px', margin: '0 auto 20px', lineHeight: 1.6 }}>
            Head over to <b>File Browser</b> to drag and beam files between your PC and Phone at gigabit local network speeds.
          </div>
        </div>
      ) : (
        <>
          {active.length > 0 && (
            <div style={{ marginBottom: '32px' }}>
              <div style={{ fontSize: '12px', fontWeight: '700', color: '#475569', letterSpacing: '0.06em', marginBottom: '14px' }}>
                ACTIVE QUEUE ({active.length})
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                {active.map(t => <TransferCard key={t.jobId} t={t} onCancel={cancel} />)}
              </div>
            </div>
          )}
          {done.length > 0 && (
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
                <div style={{ fontSize: '12px', fontWeight: '700', color: '#475569', letterSpacing: '0.06em' }}>
                  RECENT COMPLETED & HISTORY ({done.length})
                </div>
                <button
                  onClick={clearHistory}
                  style={{
                    background: 'transparent',
                    border: 'none',
                    color: '#f87171',
                    fontSize: '12px',
                    fontWeight: '600',
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '4px'
                  }}
                  onMouseEnter={e => e.currentTarget.style.textDecoration = 'underline'}
                  onMouseLeave={e => e.currentTarget.style.textDecoration = 'none'}
                >
                  Clear all history ({done.length})
                </button>
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                {done.map(t => <TransferCard key={t.jobId} t={t} onCancel={cancel} />)}
              </div>
            </div>
          )}
        </>
      )}
    </div>
  )
}

function getFileIcon(name = '') {
  if (/\.(mp4|mkv|avi|mov|webm)$/i.test(name)) return '🎬'
  if (/\.(jpg|jpeg|png|raw|heic|gif|webp)$/i.test(name)) return '🖼️'
  if (/\.(mp3|flac|wav|aac|ogg|m4a)$/i.test(name)) return '🎵'
  if (/\.(zip|rar|7z|tar|gz)$/i.test(name)) return '📦'
  if (/\.(pdf|doc|docx|txt)$/i.test(name)) return '📄'
  return '📁'
}

function TransferCard({ t, onCancel }) {
  const progress = t.totalChunks > 0
    ? Math.min(100, Math.round(((t.lastChunkReceived + 1) / t.totalChunks) * 100))
    : (t.status === 'COMPLETED' ? 100 : 0)
  const bytes = Math.floor((t.totalSize || 0) * progress / 100)

  const isCompleted = t.status === 'COMPLETED'
  const isFailed = t.status === 'FAILED'
  const isCancelled = t.status === 'CANCELLED'

  const direction = t.targetPath?.startsWith('/') || t.targetPath?.includes('storage') 
    ? '💻 PC ➔ 📱 Phone' 
    : '📱 Phone ➔ 💻 PC'

  return (
    <div style={{
      background: '#2b3447',
      borderRadius: '16px',
      border: isCompleted ? '1px solid #3a4661' : isFailed ? '1px solid rgba(239,68,68,0.4)' : '1px solid #6366f1',
      boxShadow: '0 4px 16px rgba(0,0,0,0.12)',
      padding: '18px 20px',
      transition: 'all 0.2s ease'
    }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '12px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '14px', minWidth: 0 }}>
          <div style={{
            width: '42px', height: '42px', borderRadius: '12px',
            background: '#242c3d', border: '1px solid #354058',
            display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '20px'
          }}>
            {getFileIcon(t.fileName)}
          </div>
          <div style={{ minWidth: 0 }}>
            <div style={{ fontWeight: '700', fontSize: '14px', color: '#f8fafc', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
              {t.fileName}
            </div>
            <div style={{ fontSize: '12px', color: '#8a99b5', display: 'flex', alignItems: 'center', gap: '8px', marginTop: '2px' }}>
              <span>{formatBytes(t.totalSize)}</span>
              <span>•</span>
              <span style={{ fontWeight: '600', color: '#818cf8' }}>{direction}</span>
            </div>
          </div>
        </div>

        <div>
          <span style={{
            padding: '4px 12px', borderRadius: '999px', fontSize: '11px', fontWeight: '700',
            background: isCompleted ? 'rgba(16, 185, 129, 0.15)' : isFailed ? 'rgba(239, 68, 68, 0.15)' : isCancelled ? 'rgba(100, 116, 139, 0.15)' : 'rgba(99, 102, 241, 0.15)',
            border: `1px solid ${isCompleted ? 'rgba(16, 185, 129, 0.35)' : isFailed ? 'rgba(239, 68, 68, 0.35)' : isCancelled ? 'rgba(100, 116, 139, 0.35)' : 'rgba(99, 102, 241, 0.35)'}`,
            color: isCompleted ? '#34d399' : isFailed ? '#f87171' : isCancelled ? '#94a3b8' : '#818cf8',
          }}>
            {t.status}
          </span>
        </div>
      </div>

      {!isCompleted && !isFailed && !isCancelled && (
        <div>
          <div style={{ height: '8px', width: '100%', background: '#202738', borderRadius: '999px', overflow: 'hidden', margin: '10px 0 8px' }}>
            <div style={{
              height: '100%', width: `${progress}%`,
              background: 'linear-gradient(90deg, #6366f1 0%, #38bdf8 100%)',
              borderRadius: '999px', transition: 'width 0.3s ease'
            }} />
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '11px', color: '#8a99b5', fontWeight: '600' }}>
            <span>{formatBytes(bytes)} / {formatBytes(t.totalSize)}</span>
            <span>{progress}%</span>
            <span>Chunks {Math.max(0, t.lastChunkReceived + 1)} / {t.totalChunks}</span>
          </div>
        </div>
      )}

      {isCompleted && (
        <div style={{ fontSize: '12px', color: '#34d399', fontWeight: '600', display: 'flex', alignItems: 'center', gap: '6px' }}>
          <span>✓ Saved to:</span>
          <span style={{ color: '#8a99b5', fontWeight: '500' }}>{t.targetPath}</span>
        </div>
      )}

      {['IN_PROGRESS', 'PAUSED', 'PENDING'].includes(t.status) && (
        <div style={{ marginTop: '12px', display: 'flex', justifyContent: 'flex-end' }}>
          <button 
            onClick={() => onCancel(t.jobId)}
            style={{
              padding: '4px 12px', borderRadius: '8px', border: '1px solid rgba(239, 68, 68, 0.4)',
              background: 'rgba(239, 68, 68, 0.15)', color: '#f87171', fontSize: '12px', fontWeight: '600', cursor: 'pointer'
            }}
          >
            ✕ Cancel
          </button>
        </div>
      )}
    </div>
  )
}
