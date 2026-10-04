export function Logo() {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
      <svg viewBox="0 0 24 24" width="26" height="26" aria-hidden="true" style={{ color: 'var(--ac)' }}>
        <circle cx="6" cy="17" r="3.2" fill="none" stroke="currentColor" strokeWidth="2" />
        <circle cx="18" cy="7" r="3.2" fill="currentColor" />
        <path d="M8.6 15.2 15.4 8.8" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
      </svg>
      <span style={{ fontWeight: 700, fontSize: 16, letterSpacing: '-0.01em' }}>V&amp;V Monitor</span>
    </div>
  )
}
