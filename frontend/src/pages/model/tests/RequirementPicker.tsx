import { useState } from 'react'
import type { Element } from '../../../api/types'
import { SearchIcon } from '../../../components/icons'
import { kindInfo } from '../../../utils/labels'
import './Tests.css'

interface RequirementPickerProps {
  /** Elementos do modelo; so os requisitos (RF e RNF) aparecem, regras de negocio nao sao cobertas. */
  elements: Element[]
  value: string[]
  onChange: (ids: string[]) => void
  invalid?: boolean
}

/** Lista de requisitos com busca e caixas de selecao, para escolher o que um teste cobre (RF9). */
export function RequirementPicker({ elements, value, onChange, invalid = false }: RequirementPickerProps) {
  const [search, setSearch] = useState('')
  const term = search.trim().toLowerCase()
  const requirements = elements.filter((e) => e.kind !== 'BUSINESS_RULE')
  const shown = requirements.filter((e) => !term || `${e.code} ${e.description}`.toLowerCase().includes(term))
  const toggle = (id: string) => onChange(value.includes(id) ? value.filter((v) => v !== id) : [...value, id])

  return (
    <div className={`rp${invalid ? ' err' : ''}`}>
      <div className="rp-search">
        <SearchIcon />
        <input
          className="inp sm with-icon"
          type="search"
          placeholder="Buscar requisito"
          aria-label="Buscar requisito"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <span className="hint rp-count">{value.length} {value.length === 1 ? 'selecionado' : 'selecionados'}</span>
      </div>
      <ul className="rp-list" role="group" aria-label="Requisitos cobertos">
        {shown.length === 0 && <li className="hint rp-empty">Nenhum requisito encontrado.</li>}
        {shown.map((e) => (
          <li key={e.id}>
            <label className="rp-item">
              <input type="checkbox" checked={value.includes(e.id)} onChange={() => toggle(e.id)} />
              <span className={`chip ${kindInfo(e.kind).chip}`}>{e.code}</span>
              <span className="rp-desc" title={e.description}>{e.description}</span>
            </label>
          </li>
        ))}
      </ul>
    </div>
  )
}
