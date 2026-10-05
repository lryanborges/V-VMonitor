/** Iniciais para avatar: "Ryan Borges Lockser" -> "RL", "Ana" -> "AN". */
export function initials(name: string | null | undefined): string {
  const parts = (name ?? '').trim().split(/\s+/).filter(Boolean)
  if (parts.length === 0) return '?'
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase()
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase()
}

const MINUTE = 60_000
const HOUR = 60 * MINUTE
const DAY = 24 * HOUR

/** Tempo relativo no estilo do design: "agora", "há 5 min", "há 2 h", "ontem", "há 5 dias", "há 1 semana". */
export function timeAgo(iso: string, now: Date = new Date()): string {
  const diff = Math.max(0, now.getTime() - new Date(iso).getTime())
  if (diff < MINUTE) return 'agora'
  if (diff < HOUR) return `há ${Math.floor(diff / MINUTE)} min`
  if (diff < DAY) return `há ${Math.floor(diff / HOUR)} h`
  const days = Math.floor(diff / DAY)
  if (days === 1) return 'ontem'
  if (days < 7) return `há ${days} dias`
  if (days < 30) {
    const weeks = Math.floor(days / 7)
    return weeks === 1 ? 'há 1 semana' : `há ${weeks} semanas`
  }
  return new Date(iso).toLocaleDateString('pt-BR')
}

/** Rodape do card: "Criado ..." se nunca foi alterado, senao "Atualizado ...". */
export function activityLabel(createdAt: string, updatedAt: string): string {
  const neverUpdated = new Date(updatedAt).getTime() - new Date(createdAt).getTime() < 1000
  const when = timeAgo(updatedAt)
  const capitalized = (text: string) => text.charAt(0).toUpperCase() + text.slice(1)
  if (when === 'ontem') return neverUpdated ? 'Criado ontem' : 'Atualizado ontem'
  return capitalized(`${neverUpdated ? 'criado' : 'atualizado'} ${when}`)
}
