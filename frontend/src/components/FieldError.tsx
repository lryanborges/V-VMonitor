import type { ReactNode } from 'react'
import { AlertIcon } from './icons'

/** Mensagem de erro no padrao do design: icone de alerta e texto em var(--wr). */
export function FieldError({ id, children }: { id?: string; children: ReactNode }) {
  return (
    <div id={id} className="field-error" role="alert">
      <AlertIcon />
      <span>{children}</span>
    </div>
  )
}
