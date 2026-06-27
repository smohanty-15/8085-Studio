/**
 * RegisterPanel Component
 *
 * Displays all 8085 registers and their current values.
 * Updates after every instruction execution.
 *
 * WHAT EACH REGISTER DOES (shown as tooltip descriptions):
 * - A (Accumulator): Most arithmetic operations put their result here
 * - B,C,D,E,H,L: General purpose; can be used as 16-bit pairs BC, DE, HL
 * - PC: Program Counter — address of the NEXT instruction to run
 * - SP: Stack Pointer — top of the stack
 */

import type { Registers } from '../types/emulator'

interface RegisterPanelProps {
  registers: Registers
}

export function RegisterPanel({ registers }: RegisterPanelProps) {
  return (
    <div className="flex flex-col h-full">
      <div className="text-[10px] text-[#8B949E] font-semibold uppercase tracking-widest mb-2 px-1">
        Registers
      </div>

      <div className="space-y-1 flex-1 overflow-y-auto">
        {/* 8-bit general purpose registers */}
        <RegRow name="A" value={registers.A} bits={8} note="Accumulator" highlight />
        <div className="h-px bg-[#21262D] my-1" />
        <RegRow name="B" value={registers.B} bits={8} note="General" />
        <RegRow name="C" value={registers.C} bits={8} note="General" />
        <RegRow name="D" value={registers.D} bits={8} note="General" />
        <RegRow name="E" value={registers.E} bits={8} note="General" />
        <RegRow name="H" value={registers.H} bits={8} note="General" />
        <RegRow name="L" value={registers.L} bits={8} note="General" />
        <div className="h-px bg-[#21262D] my-1" />
        {/* 16-bit registers */}
        <RegRow name="PC" value={registers.PC} bits={16} note="Prog Counter" highlight />
        <RegRow name="SP" value={registers.SP} bits={16} note="Stack Ptr" />
        <div className="h-px bg-[#21262D] my-1" />
        {/* HL pair computed value */}
        <div className="flex items-center justify-between px-2 py-1 rounded bg-[#21262D]/50">
          <div>
            <span className="text-[#8B949E] text-[11px]">HL</span>
            <span className="text-[#484F58] text-[9px] ml-1">pair</span>
          </div>
          <div className="text-right">
            <span className="text-[#58A6FF] text-xs font-mono">
              {((registers.H << 8) | registers.L).toString(16).toUpperCase().padStart(4, '0')}H
            </span>
          </div>
        </div>
      </div>
    </div>
  )
}

interface RegRowProps {
  name: string
  value: number
  bits: 8 | 16
  note?: string
  highlight?: boolean
}

function RegRow({ name, value, bits, note, highlight }: RegRowProps) {
  const hexWidth = bits === 8 ? 2 : 4
  const hexStr = value.toString(16).toUpperCase().padStart(hexWidth, '0')
  const decStr = value.toString(10)

  return (
    <div className={`
      flex items-center justify-between px-2 py-1 rounded
      ${highlight ? 'bg-[#21262D]' : 'hover:bg-[#21262D]/50'}
      transition-colors
    `}>
      <div className="flex items-center gap-2 min-w-[48px]">
        <span className={`text-xs font-semibold w-6 ${highlight ? 'text-[#F0C674]' : 'text-[#8B949E]'}`}>
          {name}
        </span>
        {note && <span className="text-[9px] text-[#484F58]">{note}</span>}
      </div>

      <div className="text-right">
        <span className="text-[#39D353] text-xs font-mono">{hexStr}H</span>
        <span className="text-[#484F58] text-[10px] ml-2">{decStr}</span>
      </div>
    </div>
  )
}
