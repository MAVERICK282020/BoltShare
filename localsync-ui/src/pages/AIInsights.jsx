import { useState } from 'react'
import { api, formatBytes } from '../App'

const BAR_COLORS = ['#3b82f6', '#06b6d4', '#8b5cf6', '#10b981', '#f59e0b', '#ec4899']

export default function AIInsights({ addToast }) {
  const [report, setReport] = useState(null)
  const [insights, setInsights] = useState([])
  const [duplicates, setDuplicates] = useState([])
  const [loading, setLoading] = useState(false)
  const [analysisPath, setAnalysisPath] = useState(() =>
    // Default to user's home directory
    'C:/Users/' + (window.location.hostname === 'localhost' ? 'aksha' : 'user')
  )
  const [analyzed, setAnalyzed] = useState(false)

  const runAnalysis = async () => {
    if (!analysisPath.trim()) return
    setLoading(true)
    setAnalyzed(false)
    try {
      const [r, ins, dups] = await Promise.all([
        api.get(`/api/ai/analyze?path=${encodeURIComponent(analysisPath)}`),
        api.get(`/api/ai/insights?path=${encodeURIComponent(analysisPath)}`),
        api.get(`/api/ai/duplicates?path=${encodeURIComponent(analysisPath)}`),
      ])
      setReport(r)
      setInsights(Array.isArray(ins) ? ins : [])
      setDuplicates(Array.isArray(dups) ? dups : [])
      setAnalyzed(true)
      addToast('🤖 AI analysis complete!', 'success')
    } catch (e) {
      addToast('Analysis failed — is the server running?', 'error')
    }
    setLoading(false)
  }

  const categories = report ? Object.entries(report.categorySizes || {}) : []
  const maxVal = Math.max(...categories.map(([, v]) => v), 1)
  const totalWasted = duplicates.reduce((a, d) => a + (d.wastedBytes || 0), 0)

  return (
    <div>
      <div className="page-header">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end' }}>
          <div>
            <h1 className="page-title">🤖 AI Insights</h1>
            <p className="page-sub">Smart storage analysis — duplicate detection & recommendations</p>
          </div>
          <div style={{ display: 'flex', gap: 10 }}>
            <input
              value={analysisPath}
              onChange={e => setAnalysisPath(e.target.value)}
              style={{
                background: 'var(--bg-card)', border: '1px solid var(--border)',
                borderRadius: 8, padding: '8px 12px',
                color: 'var(--text-primary)', fontSize: 13, width: 280,
              }}
              placeholder="Folder path to analyze..."
            />
            <button className="btn btn-primary" onClick={runAnalysis} disabled={loading}>
              {loading ? <span className="spinner" style={{ width: 16, height: 16 }} /> : '🔍 Analyze'}
            </button>
          </div>
        </div>
      </div>

      <div className="ai-page">
        {!analyzed && !loading && (
          <div className="card empty-state" style={{ padding: 60, margin: '0 0 20px' }}>
            <div className="empty-icon">🤖</div>
            <div style={{ fontSize: 16, fontWeight: 600, marginBottom: 8 }}>AI Storage Analysis</div>
            <div style={{ fontSize: 13, color: 'var(--text-secondary)', maxWidth: 400, textAlign: 'center', marginBottom: 24 }}>
              Enter a folder path above and click Analyze to detect duplicate files, categorize storage usage, and get smart recommendations.
            </div>
            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', justifyContent: 'center' }}>
              {[
                `C:/Users/${analysisPath.split('/')[2] || 'aksha'}/Documents`,
                `C:/Users/${analysisPath.split('/')[2] || 'aksha'}/Downloads`,
                `C:/Users/${analysisPath.split('/')[2] || 'aksha'}/Pictures`,
              ].map(p => (
                <button key={p} className="btn btn-secondary btn-sm"
                  onClick={() => { setAnalysisPath(p) }}>
                  {p.split('/').slice(-2).join('/')}
                </button>
              ))}
            </div>
          </div>
        )}

        {loading && (
          <div className="card empty-state" style={{ padding: 60, margin: '0 0 20px' }}>
            <div className="spinner" style={{ width: 40, height: 40, borderWidth: 3, margin: '0 auto 16px' }} />
            <div style={{ fontSize: 14, color: 'var(--text-secondary)' }}>Analyzing storage... This may take a moment for large folders.</div>
          </div>
        )}

        {analyzed && report && (
          <>
            <div className="storage-breakdown">
              {/* Storage Bars */}
              <div className="card storage-chart-card">
                <div className="chart-title">📊 Storage Breakdown</div>
                <div style={{ fontSize: 28, fontWeight: 800, color: 'var(--text-primary)', marginBottom: 4 }}>
                  {formatBytes(report.totalSizeBytes)}
                </div>
                <div style={{ fontSize: 12, color: 'var(--text-muted)', marginBottom: 20 }}>
                  {(report.fileCount || 0).toLocaleString()} files in {report.rootPath}
                </div>
                <div className="storage-bars">
                  {categories.map(([cat, size], i) => (
                    <div key={cat} className="storage-bar-item">
                      <div className="storage-bar-label">
                        <span>{cat}</span>
                        <span className="bar-size">{formatBytes(size)}</span>
                      </div>
                      <div className="storage-bar-track">
                        <div className="storage-bar-fill" style={{
                          width: `${(size / maxVal) * 100}%`,
                          background: BAR_COLORS[i % BAR_COLORS.length]
                        }} />
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {/* AI Insights */}
              <div className="card insights-card">
                <div className="chart-title">💡 Recommendations</div>
                {insights.length === 0 ? (
                  <div className="empty-state" style={{ padding: 20 }}>
                    <div className="empty-text">✅ No issues detected! Storage looks clean.</div>
                  </div>
                ) : (
                  insights.map((ins, i) => (
                    <div key={i} className="insight-item">
                      <div className={`insight-dot ${ins.type}`} />
                      <div>
                        <div className="insight-title">{ins.title}</div>
                        <div className="insight-desc">{ins.description}</div>
                        <div className="insight-action">{ins.actionLabel} →</div>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>

            {/* Duplicates */}
            <div className="card" style={{ padding: 24 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
                <div className="chart-title" style={{ margin: 0 }}>🔍 Duplicate Files (SHA-256)</div>
                {totalWasted > 0 && (
                  <div className="tag tag-amber">
                    {formatBytes(totalWasted)} wasted space
                  </div>
                )}
              </div>
              {duplicates.length === 0 ? (
                <div className="empty-state" style={{ padding: 24 }}>
                  <div style={{ fontSize: 28, marginBottom: 8 }}>✅</div>
                  <div className="empty-text">No duplicate files found!</div>
                </div>
              ) : (
                <div className="duplicates-list">
                  {duplicates.slice(0, 10).map((d, i) => (
                    <div key={i} className="dup-group">
                      <div className="dup-header">
                        <span className="dup-count">⚠️ {d.fileCount} copies · {formatBytes(d.sizePerFile)} each</span>
                        <span className="dup-wasted">Wasting {formatBytes(d.wastedBytes)}</span>
                      </div>
                      <div className="dup-files">
                        {d.files.map((f, j) => (
                          <div key={j} className="dup-file">{f}</div>
                        ))}
                      </div>
                    </div>
                  ))}
                  {duplicates.length > 10 && (
                    <div style={{ textAlign: 'center', color: 'var(--text-muted)', fontSize: 12, padding: 8 }}>
                      ... and {duplicates.length - 10} more duplicate groups
                    </div>
                  )}
                </div>
              )}
            </div>
          </>
        )}
      </div>
    </div>
  )
}
