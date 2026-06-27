/**
 * MemoryViewer Component
 *
 * Displays the contents of the 8085's 64KB memory as a table.
 * Each row shows: Address (hex) | Value (hex) | Value (decimal) | Visual bar
 *
 * You can navigate to any memory address to inspect it.
 * The current PC (Program Counter) address is highlighted.
 */

import { useState } from 'react'
import type { MemoryCell } from '../types/emulator'

interface MemoryViewerProps {
  cells: MemoryCell[]
  currentPC: number
  memStart: number
  onNavigate: (address: number) => void
}

export function MemoryViewer({ cells, currentPC, memStart, onNavigate }: MemoryViewerProps) {
  const [inputAddr, setInputAddr] = useState('')

  const handleNavigate = () => {
    const raw = inputAddr.trim().toUpperCase().replace(/H$/, '')
    const addr = parseInt(raw, 16)
    if (!isNaN(addr) && addr >= 0 && addr <= 0xFFFF) {
      onNavigate(addr)
      setInputAddr('')
    }
  }

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="flex items-center justify-between mb-2 px-1">
        <span
          className="text-[10px] font-semibold uppercase tracking-widest"
          style={{ color: 'var(--text-secondary)' }}
        >
          Memory Viewer
        </span>

        <div className="flex items-center gap-2">
          <span
            className="text-[10px]"
            style={{ color: 'var(--text-muted)' }}
          >
            0x{memStart.toString(16).toUpperCase().padStart(4, '0')} — 0x{(memStart + cells.length - 1).toString(16).toUpperCase().padStart(4, '0')}
          </span>

          {/* Navigation buttons */}
          <button
            onClick={() => onNavigate(Math.max(0, memStart - 64))}
            className="px-2 py-0.5 text-[10px] border rounded transition-colors"
            style={{
              color: 'var(--text-secondary)',
              borderColor: 'var(--border-color)',
            }}
            onMouseEnter={e => {
              e.currentTarget.style.color = 'var(--text-primary)'
              e.currentTarget.style.borderColor = 'var(--text-muted)'
            }}
            onMouseLeave={e => {
              e.currentTarget.style.color = 'var(--text-secondary)'
              e.currentTarget.style.borderColor = 'var(--border-color)'
            }}
          >
            ‹ Prev
          </button>

          <button
            onClick={() => onNavigate(Math.min(0xFFC0, memStart + 64))}
            className="px-2 py-0.5 text-[10px] border rounded transition-colors"
            style={{
              color: 'var(--text-secondary)',
              borderColor: 'var(--border-color)',
            }}
            onMouseEnter={e => {
              e.currentTarget.style.color = 'var(--text-primary)'
              e.currentTarget.style.borderColor = 'var(--text-muted)'
            }}
            onMouseLeave={e => {
              e.currentTarget.style.color = 'var(--text-secondary)'
              e.currentTarget.style.borderColor = 'var(--border-color)'
            }}
          >
            Next ›
          </button>

          {/* Jump to address input */}
          <input
            type="text"
            value={inputAddr}
            onChange={e => setInputAddr(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && handleNavigate()}
            placeholder="Go to addr (e.g. 2000H)"
            className="text-[10px] border rounded px-2 py-0.5 focus:outline-none w-36"
            style={{
              background: 'var(--bg-tertiary)',
              borderColor: 'var(--border-color)',
              color: 'var(--text-primary)',
            }}
          />

          <button
            onClick={handleNavigate}
            className="px-2 py-0.5 text-[10px] border rounded transition-colors"
            style={{
              color: 'var(--accent-blue)',
              borderColor: 'var(--accent-blue)',
            }}
            onMouseEnter={e => {
              e.currentTarget.style.background = 'rgba(88,166,255,0.10)'
            }}
            onMouseLeave={e => {
              e.currentTarget.style.background = 'transparent'
            }}
          >
            Go
          </button>
        </div>
      </div>

      {/* Table */}
      <div
        className="overflow-auto flex-1 border rounded"
        style={{ borderColor: 'var(--border-color)' }}
      >
        <table className="w-full text-xs font-mono">
          <thead>
            <tr
              className="border-b sticky top-0"
              style={{
                borderColor: 'var(--border-color)',
                background: 'var(--bg-secondary)',
              }}
            >
              <th className="text-left px-3 py-1.5 font-medium w-24" style={{ color: 'var(--text-secondary)' }}>Address</th>
              <th className="text-left px-3 py-1.5 font-medium w-16" style={{ color: 'var(--text-secondary)' }}>Hex</th>
              <th className="text-left px-3 py-1.5 font-medium w-16" style={{ color: 'var(--text-secondary)' }}>Dec</th>
              <th className="text-left px-3 py-1.5 font-medium w-16" style={{ color: 'var(--text-secondary)' }}>Binary</th>
              <th className="px-3 py-1.5"></th>
            </tr>
          </thead>

          <tbody>
            {cells.map(cell => {
              const isPC = cell.address === currentPC
              const isEmpty = cell.value === 0

              return (
                <tr
                  key={cell.address}
                  className="border-b transition-colors"
                  style={{
                    borderColor: 'var(--border-subtle)',
                    background: isPC
                      ? 'rgba(240,198,116,0.10)'
                      : 'transparent',
                    opacity: isPC
                      ? 1
                      : isEmpty
                      ? 0.3
                      : 1,
                  }}
                >
                  <td
                    className="px-3 py-1"
                    style={{
                      color: isPC ? 'var(--accent-yellow)' : 'var(--accent-blue)',
                      fontWeight: isPC ? 'bold' : 'normal',
                      borderLeft: isPC ? '2px solid var(--accent-yellow)' : undefined,
                    }}
                  >
                    {isPC && (
                      <span
                        className="mr-1"
                        style={{ color: 'var(--accent-yellow)' }}
                      >
                        ▶
                      </span>
                    )}
                    {cell.addressHex}H
                  </td>

                  <td
                    className="px-3 py-1"
                    style={{
                      color: isEmpty
                        ? 'var(--text-muted)'
                        : 'var(--accent-green)',
                    }}
                  >
                    {cell.valueHex}
                  </td>

                  <td
                    className="px-3 py-1"
                    style={{ color: 'var(--text-secondary)' }}
                  >
                    {cell.value}
                  </td>

                  <td
                    className="px-3 py-1 text-[9px]"
                    style={{ color: 'var(--text-muted)' }}
                  >
                    {cell.value.toString(2).padStart(8, '0')}
                  </td>

                  <td className="px-3 py-1">
                    {/* Visual bar showing relative value */}
                    {cell.value > 0 && (
                      <div
                        className="h-1.5 rounded-full"
                        style={{
                          width: `${(cell.value / 255) * 60}px`,
                          background: 'var(--accent-green)',
                          opacity: 0.4,
                        }}
                      />
                    )}
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>

        {cells.length === 0 && (
          <div
            className="flex items-center justify-center h-16 text-xs"
            style={{ color: 'var(--text-muted)' }}
          >
            No memory data. Assemble a program first.
          </div>
        )}
      </div>
    </div>
  )
}