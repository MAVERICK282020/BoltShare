import { useState, useEffect, useRef } from 'react'
import { api, formatBytes, formatDate } from '../App'

const FILE_ICONS = {
  directory: '📁', jpg: '🖼️', jpeg: '🖼️', png: '🖼️', gif: '🖼️', webp: '🖼️',
  mp4: '🎬', mkv: '🎬', avi: '🎬', mov: '🎬', webm: '🎬',
  mp3: '🎵', flac: '🎵', wav: '🎵', ogg: '🎵', m4a: '🎵',
  pdf: '📄', doc: '📝', docx: '📝', txt: '📝', xls: '📊', xlsx: '📊',
  zip: '📦', rar: '📦', '7z': '📦', tar: '📦', gz: '📦',
  apk: '📱', exe: '⚙️', default: '📄'
}

function getFileIcon(file) {
  if (file.isDirectory) return '📁'
  const ext = file.extension?.toLowerCase() || ''
  return FILE_ICONS[ext] || FILE_ICONS.default
}

function isMediaFile(file) {
  if (file.isDirectory) return false
  const ext = file.extension?.toLowerCase() || ''
  const mime = file.mimeType?.toLowerCase() || ''
  return (
    mime.startsWith('image/') ||
    mime.startsWith('video/') ||
    mime.startsWith('audio/') ||
    mime === 'application/pdf' ||
    ['jpg', 'jpeg', 'png', 'gif', 'webp', 'mp4', 'mkv', 'avi', 'mov', 'webm', 'mp3', 'flac', 'wav', 'ogg', 'm4a', 'pdf'].includes(ext)
  )
}

function FilePane({ 
  title, 
  icon, 
  subtitle, 
  files, 
  loading, 
  storageInfo,
  onNavigate, 
  onPreview,
  onDownload, 
  onDelete, 
  breadcrumbs, 
  onBreadcrumb, 
  onUploadExternalFiles,
  onCrossTransfer,
  isPhonePane, 
  pairedDevice 
}) {
  const fileInputRef = useRef(null)
  const [isDragging, setIsDragging] = useState(false)
  const [searchTerm, setSearchTerm] = useState('')

  const handleDragOver = (e) => {
    e.preventDefault()
    e.stopPropagation()
    setIsDragging(true)
  }

  const handleDragLeave = (e) => {
    e.preventDefault()
    e.stopPropagation()
    setIsDragging(false)
  }

  const handleDrop = (e) => {
    e.preventDefault()
    e.stopPropagation()
    setIsDragging(false)

    // Check if it's an internal cross-pane drag
    const internalData = e.dataTransfer.getData('text/plain')
    if (internalData) {
      try {
        const payload = JSON.parse(internalData)
        if (payload && payload.file && payload.source) {
          onCrossTransfer(payload.file, payload.source)
          return
        }
      } catch (ignored) {}
    }

    // Otherwise handle external OS file drop
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      onUploadExternalFiles(e.dataTransfer.files)
    }
  }

  const handleDragStart = (e, file) => {
    if (file.isDirectory) return
    const payload = { file, source: isPhonePane ? 'phone' : 'pc' }
    e.dataTransfer.setData('text/plain', JSON.stringify(payload))
    e.dataTransfer.effectAllowed = 'copy'
  }

  const filteredFiles = files.filter(f => 
    !searchTerm || f.name.toLowerCase().includes(searchTerm.toLowerCase())
  )

  return (
    <div 
      className={`file-pane-container ${isDragging ? 'pane-active-drag' : ''}`}
      onDragOver={handleDragOver}
      onDragLeave={handleDragLeave}
      onDrop={handleDrop}
      style={{
        display: 'flex',
        flexDirection: 'column',
        height: '78vh',
        background: '#2b3447',
        border: isDragging ? '2px dashed #818cf8' : '1px solid #3a4661',
        borderRadius: '16px',
        overflow: 'hidden',
        boxShadow: '0 8px 30px rgba(0,0,0,0.18)',
        transition: 'all 0.25s ease'
      }}
    >
      {/* Top Device & Storage Card matching Image 2 */}
      <div style={{ padding: '16px', borderBottom: '1px solid #354058' }}>
        <div
          style={{
            background: '#242c3d',
            border: '1px solid #354058',
            borderRadius: '14px',
            padding: '14px 16px'
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
              <div style={{
                width: '40px', height: '40px', borderRadius: '10px',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                fontSize: '20px', background: '#2b3447',
                border: '1px solid #3a4661'
              }}>
                {icon}
              </div>
              <div>
                <div style={{ fontWeight: '700', fontSize: '15px', color: '#f8fafc' }}>{title}</div>
                <div style={{ fontSize: '11px', color: isPhonePane ? '#34d399' : '#8a99b5', fontWeight: '500', display: 'flex', alignItems: 'center', gap: '5px' }}>
                  {subtitle || (isPhonePane ? '● Live LAN Direct' : 'Windows Storage')}
                </div>
              </div>
            </div>
            <div>
              <button 
                className="btn btn-outline" 
                style={{
                  padding: '6px 14px',
                  fontSize: '12px',
                  borderRadius: '10px',
                  fontWeight: '600',
                  background: '#354058',
                  border: '1px solid #48577a',
                  color: '#f8fafc',
                  cursor: 'pointer'
                }}
                onClick={() => fileInputRef.current?.click()}
              >
                + Upload
              </button>
              <input 
                type="file" 
                multiple 
                ref={fileInputRef} 
                style={{ display: 'none' }} 
                onChange={(e) => {
                  if (e.target.files && e.target.files.length > 0) {
                    onUploadExternalFiles(e.target.files)
                    e.target.value = ''
                  }
                }} 
              />
            </div>
          </div>

          {/* Storage Capacity Meter */}
          {storageInfo && storageInfo.totalBytes > 0 ? (
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '11px', color: '#8a99b5', marginBottom: '6px', fontWeight: '500' }}>
                <span>{storageInfo.drive || (isPhonePane ? 'Internal Storage' : 'C:\\')}</span>
                <span>{formatBytes(storageInfo.usedBytes)} / {formatBytes(storageInfo.totalBytes)} ({storageInfo.usedPercentage || 0}%)</span>
              </div>
              <div style={{ height: '7px', width: '100%', background: '#374158', borderRadius: '999px', overflow: 'hidden' }}>
                <div 
                  style={{ 
                    height: '100%', 
                    width: `${Math.min(100, storageInfo.usedPercentage || 0)}%`,
                    background: isPhonePane 
                      ? 'linear-gradient(90deg, #10b981 0%, #34d399 100%)' 
                      : 'linear-gradient(90deg, #94a3b8 0%, #cbd5e1 50%, #ffffff 100%)',
                    borderRadius: '999px',
                    transition: 'width 0.4s ease'
                  }} 
                />
              </div>
            </div>
          ) : isPhonePane && !pairedDevice ? (
            <div style={{ fontSize: '11px', color: '#8a99b5', fontWeight: '500' }}>
              No mobile device paired. Connect a phone to view storage.
            </div>
          ) : null}
        </div>
      </div>

      {/* Section Subheader & Search matching Image 2 */}
      <div style={{ padding: '12px 16px', background: '#242c3d', borderBottom: '1px solid #354058', display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '10px' }}>
        <div style={{ fontWeight: '700', fontSize: '14px', color: '#f8fafc' }}>
          {isPhonePane ? 'Phone Storage' : 'PC Storage'}
        </div>
        <div style={{ position: 'relative' }}>
          <input 
            type="text" 
            placeholder="🔍 Search..." 
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            style={{
              padding: '5px 12px',
              fontSize: '11px',
              borderRadius: '8px',
              border: '1px solid #354058',
              width: '130px',
              outline: 'none',
              background: '#1d2433',
              color: '#ffffff'
            }}
          />
        </div>
      </div>

      {/* Files List */}
      <div style={{ flex: 1, overflowY: 'auto', padding: '10px 14px', background: '#2b3447' }}>
        {loading ? (
          <div style={{ padding: '60px', textAlign: 'center' }}><div className="spinner" /></div>
        ) : filteredFiles.length === 0 ? (
          <div style={{ padding: '50px 20px', textAlign: 'center' }}>
            <div style={{ fontSize: '42px', marginBottom: '12px' }}>{isPhonePane ? '📱' : '📂'}</div>
            <div style={{ fontWeight: '700', color: '#f8fafc', fontSize: '15px', marginBottom: '6px' }}>
              {isPhonePane && !pairedDevice 
                ? 'No Phone Connected' 
                : searchTerm 
                  ? 'No matching files found' 
                  : (breadcrumbs.length > 1 ? 'Folder is empty' : 'No items')}
            </div>
            <div style={{ fontSize: '12px', color: '#8a99b5', maxWidth: '300px', margin: '0 auto', lineHeight: 1.5 }}>
              {isPhonePane && !pairedDevice
                ? 'Open BoltShare on your mobile device and scan the QR code or connect on Wi-Fi to browse storage.'
                : `Drag files from the ${isPhonePane ? 'PC pane' : 'Phone pane'} or your computer desktop and drop them right here to transfer!`
              }
            </div>
          </div>
        ) : (
          filteredFiles.map((f, i) => (
            <div 
              key={i} 
              draggable={!f.isDirectory}
              onDragStart={(e) => handleDragStart(e, f)}
              onDoubleClick={() => {
                if (f.isDirectory) onNavigate(f)
                else onPreview(f)
              }}
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                padding: '10px 14px',
                borderRadius: '10px',
                margin: '4px 0',
                backgroundColor: '#202738',
                border: '1px solid #2f3a52',
                cursor: f.isDirectory ? 'pointer' : 'grab',
                transition: 'all 0.15s ease'
              }}
              onMouseEnter={(e) => e.currentTarget.style.backgroundColor = '#283146'}
              onMouseLeave={(e) => e.currentTarget.style.backgroundColor = '#202738'}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '12px', minWidth: 0, flex: 1 }}>
                <span style={{ fontSize: '22px' }}>{f.isDirectory ? '📁' : getFileIcon(f)}</span>
                <div style={{ minWidth: 0, flex: 1 }}>
                  <div style={{ fontWeight: '600', fontSize: '13px', color: '#f8fafc', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                    {f.name}
                  </div>
                  <div style={{ fontSize: '11px', color: '#8a99b5' }}>
                    {f.isDirectory ? 'Folder' : formatBytes(f.size)}
                    {f.lastModified > 0 && ` · ${formatDate(f.lastModified)}`}
                  </div>
                </div>
              </div>

              <div style={{ display: 'flex', gap: '6px', alignItems: 'center' }}>
                {/* Arrow Transfer Button matching Image 2 */}
                <button
                  title={isPhonePane ? "Transfer to PC" : "Transfer to Phone"}
                  onClick={(e) => {
                    e.stopPropagation()
                    onCrossTransfer(f, isPhonePane ? 'phone' : 'pc')
                  }}
                  style={{
                    width: '32px',
                    height: '32px',
                    borderRadius: '8px',
                    border: '1px solid #48577a',
                    background: '#354058',
                    color: '#cbd5e1',
                    cursor: 'pointer',
                    fontSize: '14px',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    fontWeight: 700,
                    transition: 'all 0.15s'
                  }}
                  onMouseEnter={(e) => e.currentTarget.style.background = '#445273'}
                  onMouseLeave={(e) => e.currentTarget.style.background = '#354058'}
                >
                  {isPhonePane ? '←' : '→'}
                </button>

                {/* Delete Button matching Image 2 */}
                <button 
                  title="Delete" 
                  onClick={(e) => { e.stopPropagation(); onDelete(f) }}
                  style={{
                    width: '32px',
                    height: '32px',
                    borderRadius: '8px',
                    border: '1px solid #48577a',
                    background: '#354058',
                    color: '#cbd5e1',
                    cursor: 'pointer',
                    fontSize: '13px',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center'
                  }}
                  onMouseEnter={(e) => e.currentTarget.style.background = '#445273'}
                  onMouseLeave={(e) => e.currentTarget.style.background = '#354058'}
                >
                  🗑
                </button>
              </div>
            </div>
          ))
        )}
      </div>

      {/* Drop Zone Footer matching Image 2 */}
      <div 
        onClick={() => fileInputRef.current?.click()}
        style={{
          padding: '12px 16px',
          textAlign: 'center',
          fontSize: '11px',
          fontWeight: '500',
          color: isDragging ? '#a5b4fc' : '#8a99b5',
          background: isDragging ? 'rgba(99, 102, 241, 0.2)' : '#202738',
          borderTop: '1px solid #2f3a52',
          cursor: 'pointer',
          transition: 'all 0.2s ease',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          gap: '8px'
        }}
      >
        <span style={{ fontSize: '14px' }}>{isPhonePane ? '⬆' : '🔗'}</span>
        <span>
          {isDragging 
            ? '⚡ Drop file here to transfer instantly!' 
            : isPhonePane 
              ? 'Drag PC files here or drop OS files to upload to phone' 
              : 'Drag Phone files here or drop OS files to Save on PC'
          }
        </span>
      </div>
    </div>
  )
}

export default function FileBrowser({ addToast }) {
  const [pcPath, setPcPath] = useState(null)
  const [pcFiles, setPcFiles] = useState([])
  const [pcLoading, setPcLoading] = useState(true)
  const [pcBreadcrumbs, setPcBreadcrumbs] = useState([{ label: 'PC Storage', path: null }])
  const [pcStorageInfo, setPcStorageInfo] = useState(null)

  const [phonePath, setPhonePath] = useState(null)
  const [phoneFiles, setPhoneFiles] = useState([])
  const [phoneLoading, setPhoneLoading] = useState(false)
  const [phoneBreadcrumbs, setPhoneBreadcrumbs] = useState([{ label: '📱 Phone Storage', path: null }])
  const [phoneStorageInfo, setPhoneStorageInfo] = useState(null)

  const [pairedDevice, setPairedDevice] = useState(null)
  const [previewMedia, setPreviewMedia] = useState(null)

  // Fetch paired devices & storage metrics without triggering reload loops
  useEffect(() => {
    let isMounted = true
    const fetchData = () => {
      api.get('/api/auth/devices')
        .then(devices => {
          if (!isMounted) return
          if (Array.isArray(devices) && devices.length > 0) {
            setPairedDevice(devices[0])
            // Fetch phone storage info for this specific active phone
            fetch(`http://localhost:8080/api/phone/storage-info?deviceId=${encodeURIComponent(devices[0].deviceId)}`)
              .then(r => r.json())
              .then(data => {
                if (isMounted) {
                  if (data && data.totalBytes > 0) {
                    setPhoneStorageInfo(data)
                  } else {
                    setPhoneStorageInfo(null)
                  }
                }
              })
              .catch(() => { if (isMounted) setPhoneStorageInfo(null) })
          } else {
            // No device is currently paired or connected!
            setPairedDevice(null)
            setPhoneStorageInfo(null)
            setPhoneFiles([])
          }
        })
        .catch(() => {
          if (!isMounted) return
          setPairedDevice(null)
          setPhoneStorageInfo(null)
          setPhoneFiles([])
        })

      // PC Storage metrics
      fetch('http://localhost:8080/api/files/storage-info')
        .then(r => r.json())
        .then(data => { if (isMounted && data.totalBytes) setPcStorageInfo(data) })
        .catch(() => {})
    }

    fetchData()
    const id = setInterval(fetchData, 4000)
    return () => { isMounted = false; clearInterval(id) }
  }, [])

  // Load PC files
  const loadPcFiles = () => {
    setPcLoading(true)
    const endpoint = pcPath ? `/api/files?path=${encodeURIComponent(pcPath)}` : '/api/files/roots'
    api.get(endpoint)
      .then(data => { setPcFiles(Array.isArray(data) ? data : []); setPcLoading(false) })
      .catch(() => { setPcLoading(false); addToast('Could not load PC files', 'error') })
  }

  useEffect(() => {
    loadPcFiles()
  }, [pcPath])

  // Load Phone files
  const loadPhoneFiles = () => {
    if (!pairedDevice) {
      setPhoneFiles([])
      setPhoneLoading(false)
      return
    }
    setPhoneLoading(true)
    const devParam = `deviceId=${encodeURIComponent(pairedDevice.deviceId)}`
    const endpoint = phonePath 
      ? `/api/phone/files?path=${encodeURIComponent(phonePath)}&${devParam}` 
      : `/api/phone/roots?${devParam}`
    
    api.get(endpoint)
      .then(data => {
        setPhoneFiles(Array.isArray(data) ? data : [])
        setPhoneLoading(false)
      })
      .catch(() => {
        setPhoneFiles([])
        setPhoneLoading(false)
      })
  }

  useEffect(() => {
    loadPhoneFiles()
  }, [phonePath, pairedDevice?.deviceId])

  // Navigation
  const navigatePC = (file) => {
    setPcPath(file.path)
    setPcBreadcrumbs(b => [...b, { label: file.name, path: file.path }])
  }

  const breadcrumbPC = (index) => {
    const crumb = pcBreadcrumbs[index]
    setPcBreadcrumbs(b => b.slice(0, index + 1))
    setPcPath(crumb.path)
  }

  const navigatePhone = (file) => {
    setPhonePath(file.path)
    setPhoneBreadcrumbs(b => [...b, { label: file.name, path: file.path }])
  }

  const breadcrumbPhone = (index) => {
    const crumb = phoneBreadcrumbs[index]
    setPhoneBreadcrumbs(b => b.slice(0, index + 1))
    setPhonePath(crumb.path)
  }

  // Previews
  const previewPCFile = (file) => {
    const url = `http://localhost:8080/api/files/download?path=${encodeURIComponent(file.path)}`
    setPreviewMedia({ file, url, isPhone: false })
  }

  const previewPhoneFile = (file) => {
    const devParam = pairedDevice?.deviceId ? `&deviceId=${encodeURIComponent(pairedDevice.deviceId)}` : ''
    const url = `http://localhost:8080/api/phone/download?path=${encodeURIComponent(file.path)}${devParam}&inline=true`
    setPreviewMedia({ file, url, isPhone: true })
  }

  // Downloads
  const downloadFile = (file) => {
    window.open(`http://localhost:8080/api/files/download?path=${encodeURIComponent(file.path)}`, '_blank')
    addToast(`⬇️ Downloading ${file.name}`, 'info')
  }

  const downloadPhoneFile = (file) => {
    const devParam = pairedDevice?.deviceId ? `&deviceId=${encodeURIComponent(pairedDevice.deviceId)}` : ''
    window.open(`http://localhost:8080/api/phone/download?path=${encodeURIComponent(file.path)}${devParam}`, '_blank')
    addToast(`⬇️ Downloading ${file.name} from phone`, 'info')
  }

  // Deletions
  const deleteFile = async (file) => {
    if (!confirm(`Delete "${file.name}"?`)) return
    try {
      await api.delete(`/api/files?path=${encodeURIComponent(file.path)}`)
      addToast(`🗑️ Deleted ${file.name}`, 'success')
      setPcPath(p => p ? `${p}` : null)
    } catch { addToast('Delete failed', 'error') }
  }

  const deletePhoneFile = async (file) => {
    if (!confirm(`Delete "${file.name}" from phone storage?`)) return
    try {
      const devParam = pairedDevice?.deviceId ? `&deviceId=${encodeURIComponent(pairedDevice.deviceId)}` : ''
      await api.delete(`/api/phone/files?path=${encodeURIComponent(file.path)}${devParam}`)
      addToast(`🗑️ Deleted ${file.name}`, 'success')
      loadPhoneFiles()
    } catch {
      addToast('Delete failed', 'error')
    }
  }

  // Cross-Pane Drag & Drop Transfer (Between PC and Phone)
  const handleCrossTransfer = async (file, source) => {
    if (source === 'pc') {
      // Transferring from PC to Phone
      const targetDir = phonePath || '/storage/emulated/0/Download'
      addToast(`📤 Transferring "${file.name}" to Phone (${targetDir})...`, 'info')
      try {
        const query = new URLSearchParams({
          sourcePath: file.path,
          targetDir: targetDir
        })
        const res = await fetch(`http://localhost:8080/api/transfer/pc-to-phone?${query.toString()}`, { method: 'POST' })
        if (res.ok) {
          addToast(`✅ Successfully transferred "${file.name}" to Phone!`, 'success')
          setTimeout(loadPhoneFiles, 1500)
        } else {
          addToast(`❌ Transfer failed for "${file.name}"`, 'error')
        }
      } catch (err) {
        addToast(`❌ Transfer error: ${err.message}`, 'error')
      }
    } else {
      // Transferring from Phone to PC
      const targetDir = pcPath || 'C:/Users/aksha/Downloads'
      addToast(`📥 Transferring "${file.name}" to PC (${targetDir})...`, 'info')
      try {
        const query = new URLSearchParams({
          sourcePath: file.path,
          targetDir: targetDir
        })
        const res = await fetch(`http://localhost:8080/api/transfer/phone-to-pc?${query.toString()}`, { method: 'POST' })
        if (res.ok) {
          addToast(`✅ Successfully transferred "${file.name}" to PC!`, 'success')
          setTimeout(loadPcFiles, 1500)
        } else {
          addToast(`❌ Transfer failed for "${file.name}"`, 'error')
        }
      } catch (err) {
        addToast(`❌ Transfer error: ${err.message}`, 'error')
      }
    }
  }

  // External file upload to PC
  const handleUploadPC = async (filesList) => {
    if (!filesList || filesList.length === 0) return
    const files = Array.from(filesList)
    const targetDir = pcPath || 'C:/Users/aksha/Documents'
    const token = localStorage.getItem('localsync_jwt')

    for (const file of files) {
      const formData = new FormData()
      formData.append('file', file)
      try {
        addToast(`Uploading ${file.name}...`, 'info')
        await fetch(`http://localhost:8080/api/files/upload?targetDir=${encodeURIComponent(targetDir)}`, {
          method: 'POST',
          headers: token ? { Authorization: `Bearer ${token}` } : {},
          body: formData
        })
        addToast(`✅ Uploaded ${file.name}`, 'success')
      } catch {
        addToast(`Upload failed for ${file.name}`, 'error')
      }
    }
    loadPcFiles()
  }

  // External file upload to Phone
  const handleUploadPhone = async (filesList) => {
    if (!filesList || filesList.length === 0) return
    const files = Array.from(filesList)

    for (const file of files) {
      const formData = new FormData()
      formData.append('file', file)

      try {
        addToast(`📤 Uploading ${file.name} to phone...`, 'info')
        const query = new URLSearchParams()
        if (pairedDevice?.deviceId) query.append('deviceId', pairedDevice.deviceId)
        if (phonePath) query.append('targetPath', phonePath)

        const res = await fetch(`http://localhost:8080/api/phone/upload?${query.toString()}`, {
          method: 'POST',
          body: formData
        })
        if (res.ok) {
          addToast(`✅ Sent ${file.name} to phone!`, 'success')
        } else {
          addToast(`❌ Upload failed for ${file.name}`, 'error')
        }
      } catch (err) {
        addToast(`❌ Error: ${err.message}`, 'error')
      }
    }
    loadPhoneFiles()
  }

  return (
    <div style={{ padding: '24px 32px' }}>
      <div className="page-header" style={{ marginBottom: '20px', padding: 0 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
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
              📁
            </div>
            <div>
              <h1 className="page-title" style={{ fontSize: '22px', fontWeight: '800', color: '#111827', margin: 0, letterSpacing: '-0.02em' }}>
                Storage Bridge & File Explorer
              </h1>
              <p className="page-sub" style={{ fontSize: '12px', color: '#475569', margin: '3px 0 0 0' }}>
                Bidirectional LAN drag & drop, live media streaming, and real-time storage sync
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Dual Pane Layout */}
      <div className="file-browser-layout" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '22px' }}>
        <FilePane
          title="💻 My Laptop"
          icon="💻"
          subtitle="Windows Storage"
          files={pcFiles}
          loading={pcLoading}
          storageInfo={pcStorageInfo}
          onNavigate={navigatePC}
          onPreview={previewPCFile}
          onDownload={downloadFile}
          onDelete={deleteFile}
          breadcrumbs={pcBreadcrumbs}
          onBreadcrumb={breadcrumbPC}
          onUploadExternalFiles={handleUploadPC}
          onCrossTransfer={handleCrossTransfer}
        />
        <FilePane
          title={pairedDevice ? `📱 ${pairedDevice.deviceName}` : '📱 Phone Storage'}
          subtitle={pairedDevice ? `● Live LAN Direct (${pairedDevice.ipAddress})` : 'Waiting for phone connection'}
          icon="📱"
          files={phoneFiles}
          loading={phoneLoading}
          storageInfo={pairedDevice ? phoneStorageInfo : null}
          onNavigate={navigatePhone}
          onPreview={previewPhoneFile}
          onDownload={downloadPhoneFile}
          onDelete={deletePhoneFile}
          breadcrumbs={phoneBreadcrumbs}
          onBreadcrumb={breadcrumbPhone}
          onUploadExternalFiles={handleUploadPhone}
          onCrossTransfer={handleCrossTransfer}
          isPhonePane={true}
          pairedDevice={pairedDevice}
        />
      </div>

      {/* Media Viewer Lightbox Modal */}
      {previewMedia && (
        <div 
          style={{
            position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
            backgroundColor: 'rgba(15, 23, 42, 0.82)', backdropFilter: 'blur(10px)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            zIndex: 99999, padding: '24px'
          }}
          onClick={() => setPreviewMedia(null)}
        >
          <div 
            style={{
              backgroundColor: '#0F172A', color: '#F8FAFC',
              borderRadius: '20px', maxWidth: '920px', width: '100%',
              maxHeight: '92vh', display: 'flex', flexDirection: 'column',
              boxShadow: '0 25px 60px -15px rgba(0,0,0,0.7)',
              border: '1px solid rgba(255,255,255,0.1)', overflow: 'hidden'
            }}
            onClick={(e) => e.stopPropagation()}
          >
            {/* Modal Header */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '16px 22px', borderBottom: '1px solid rgba(255,255,255,0.1)' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                <span style={{ fontSize: '22px' }}>{getFileIcon(previewMedia.file)}</span>
                <div>
                  <div style={{ fontWeight: '700', fontSize: '15px', color: '#F8FAFC' }}>{previewMedia.file.name}</div>
                  <div style={{ fontSize: '12px', color: '#94A3B8' }}>
                    {formatBytes(previewMedia.file.size)} · {previewMedia.isPhone ? '📱 Phone Direct Stream' : '💻 Local PC'}
                  </div>
                </div>
              </div>
              <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
                <button
                  onClick={() => {
                    if (previewMedia.isPhone) downloadPhoneFile(previewMedia.file)
                    else downloadFile(previewMedia.file)
                  }}
                  style={{
                    padding: '7px 16px', borderRadius: '10px',
                    background: '#6366F1', color: '#FFFFFF',
                    border: 'none', fontWeight: '700', fontSize: '13px', cursor: 'pointer'
                  }}
                >
                  ↓ Download
                </button>
                <button 
                  onClick={() => setPreviewMedia(null)}
                  style={{
                    background: 'rgba(255,255,255,0.1)', color: '#CBD5E1',
                    border: 'none', borderRadius: '10px',
                    padding: '7px 14px', cursor: 'pointer', fontSize: '16px'
                  }}
                >
                  ✕
                </button>
              </div>
            </div>

            {/* Modal Content */}
            <div style={{ padding: '24px', display: 'flex', alignItems: 'center', justifyContent: 'center', overflowY: 'auto', flex: 1, minHeight: '340px' }}>
              {(() => {
                const ext = previewMedia.file.extension?.toLowerCase() || ''
                if (['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp'].includes(ext)) {
                  return (
                    <img 
                      src={previewMedia.url} 
                      alt={previewMedia.file.name}
                      style={{ maxWidth: '100%', maxHeight: '72vh', objectFit: 'contain', borderRadius: '12px' }} 
                    />
                  )
                }
                if (['mp4', 'mkv', 'webm', 'mov', 'avi'].includes(ext)) {
                  return (
                    <video 
                      controls 
                      autoPlay 
                      src={previewMedia.url} 
                      style={{ maxWidth: '100%', maxHeight: '72vh', borderRadius: '12px', outline: 'none' }} 
                    />
                  )
                }
                if (['mp3', 'wav', 'flac', 'ogg', 'm4a'].includes(ext)) {
                  return (
                    <div style={{ textAlign: 'center', padding: '50px' }}>
                      <div style={{ fontSize: '72px', marginBottom: '24px' }}>🎵</div>
                      <audio controls autoPlay src={previewMedia.url} style={{ width: '380px' }} />
                    </div>
                  )
                }
                if (ext === 'pdf') {
                  return (
                    <iframe 
                      src={previewMedia.url} 
                      title={previewMedia.file.name}
                      style={{ width: '100%', height: '72vh', border: 'none', borderRadius: '12px', background: '#FFFFFF' }} 
                    />
                  )
                }
                return (
                  <div style={{ textAlign: 'center', padding: '50px' }}>
                    <div style={{ fontSize: '56px', marginBottom: '16px' }}>📄</div>
                    <div style={{ fontSize: '15px', color: '#94A3B8', marginBottom: '20px' }}>
                      No direct preview available for .{ext} files.
                    </div>
                    <button 
                      onClick={() => {
                        if (previewMedia.isPhone) downloadPhoneFile(previewMedia.file)
                        else downloadFile(previewMedia.file)
                      }}
                      style={{
                        padding: '10px 22px', background: '#6366F1', color: '#FFFFFF',
                        border: 'none', borderRadius: '10px', fontWeight: '700', cursor: 'pointer'
                      }}
                    >
                      Download to open on PC
                    </button>
                  </div>
                )
              })()}
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
