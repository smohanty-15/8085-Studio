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
        <span className="text-[10px] text-[#8B949E] font-semibold uppercase tracking-widest">
          Memory Viewer
        </span>

        <div className="flex items-center gap-2">
          <span className="text-[10px] text-[#484F58]">
            0x{memStart.toString(16).toUpperCase().padStart(4, '0')} — 0x{(memStart + cells.length - 1).toString(16).toUpperCase().padStart(4, '0')}
          </span>

          {/* Navigation buttons */}
          <button
            onClick={() => onNavigate(Math.max(0, memStart - 64))}
            className="px-2 py-0.5 text-[10px] text-[#8B949E] hover:text-[#E6EDF3] border border-[#30363D] rounded hover:border-[#484F58] transition-colors"
          >
            ‹ Prev
          </button>
          <button
            onClick={() => onNavigate(Math.min(0xFFC0, memStart + 64))}
            className="px-2 py-0.5 text-[10px] text-[#8B949E] hover:text-[#E6EDF3] border border-[#30363D] rounded hover:border-[#484F58] transition-colors"
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
            className="text-[10px] bg-[#21262D] border border-[#30363D] rounded px-2 py-0.5 text-[#E6EDF3] placeholder-[#484F58] focus:outline-none focus:border-[#58A6FF] w-36"
          />
          <button
            onClick={handleNavigate}
            className="px-2 py-0.5 text-[10px] text-[#58A6FF] border border-[#58A6FF]/30 rounded hover:bg-[#58A6FF]/10 transition-colors"
          >
            Go
          </button>
        </div>
      </div>

      {/* Table */}
      <div className="overflow-auto flex-1 border border-[#30363D] rounded">
        <table className="w-full text-xs font-mono">
          <thead>
            <tr className="border-b border-[#30363D] bg-[#161B22] sticky top-0">
              <th className="text-left px-3 py-1.5 text-[#8B949E] font-medium w-24">Address</th>
              <th className="text-left px-3 py-1.5 text-[#8B949E] font-medium w-16">Hex</th>
              <th className="text-left px-3 py-1.5 text-[#8B949E] font-medium w-16">Dec</th>
              <th className="text-left px-3 py-1.5 text-[#8B949E] font-medium w-16">Binary</th>
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
                  className={`
                    border-b border-[#21262D] transition-colors
                    ${isPC
                      ? 'bg-[#F0C674]/10 border-l-2 border-l-[#F0C674]'
                      : isEmpty
                        ? 'opacity-30 hover:opacity-60'
                        : 'hover:bg-[#21262D]'
                    }
                  `}
                >
                  <td className={`px-3 py-1 ${isPC ? 'text-[#F0C674] font-bold' : 'text-[#58A6FF]'}`}>
                    {isPC && <span className="mr-1 text-[#F0C674]">▶</span>}
                    {cell.addressHex}H
                  </td>
                  <td className={`px-3 py-1 ${isEmpty ? 'text-[#484F58]' : 'text-[#39D353]'}`}>
                    {cell.valueHex}
                  </td>
                  <td className="px-3 py-1 text-[#8B949E]">
                    {cell.value}
                  </td>
                  <td className="px-3 py-1 text-[#484F58] text-[9px]">
                    {cell.value.toString(2).padStart(8, '0')}
                  </td>
                  <td className="px-3 py-1">
                    {/* Visual bar showing relative value */}
                    {cell.value > 0 && (
                      <div
                        className="h-1.5 rounded-full bg-[#39D353]/40"
                        style={{ width: `${(cell.value / 255) * 60}px` }}
                      />
                    )}
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>

        {cells.length === 0 && (
          <div className="flex items-center justify-center h-16 text-[#484F58] text-xs">
            No memory data. Assemble a program first.
          </div>
        )}
      </div>
    </div>
  )
}
