import { useEffect, useRef, useState } from 'react'
import { useAuth } from '../auth/AuthContext'
import { initials } from '../utils/format'
import { LogoutIcon } from './icons'
import './AccountMenu.css'

/** Avatar do usuario que abre um menu com nome, e-mail e "Sair". */
export function AccountMenu() {
  const { user, signOut } = useAuth()
  const [open, setOpen] = useState(false)
  const ref = useRef<HTMLDivElement>(null)

  // fecha ao clicar fora ou apertar Esc
  useEffect(() => {
    if (!open) return
    const onClick = (e: MouseEvent) => !ref.current?.contains(e.target as Node) && setOpen(false)
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && setOpen(false)
    document.addEventListener('mousedown', onClick)
    document.addEventListener('keydown', onKey)
    return () => {
      document.removeEventListener('mousedown', onClick)
      document.removeEventListener('keydown', onKey)
    }
  }, [open])

  return (
    <div className="account" ref={ref}>
      <button
        type="button"
        className="account-trigger"
        aria-label={`Conta de ${user?.name ?? ''}`}
        aria-expanded={open}
        onClick={() => setOpen((o) => !o)}
      >
        <span className="av" style={{ width: 32, height: 32 }}>{initials(user?.name)}</span>
      </button>
      {open && (
        <div className="account-menu" role="menu">
          <div className="account-who">
            <strong>{user?.name}</strong>
            <span>{user?.email}</span>
          </div>
          <button type="button" role="menuitem" className="account-item" onClick={signOut}>
            <LogoutIcon />
            Sair
          </button>
        </div>
      )}
    </div>
  )
}
