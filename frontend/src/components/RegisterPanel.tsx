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
      <div
        className="text-[10px] font-semibold uppercase tracking-widest mb-2 px-1"
        style={{ color: 'var(--text-secondary)' }}
      >
        Registers
      </div>

      <div className="space-y-1 flex-1 overflow-y-auto">
        {/* 8-bit general purpose registers */}
        <RegRow name="A" value={registers.A} bits={8} note="Accumulator" highlight />

        <div
          className="h-px my-1"
          style={{ background: 'var(--border-subtle)' }}
        />

        <RegRow name="B" value={registers.B} bits={8} note="General" />
        <RegRow name="C" value={registers.C} bits={8} note="General" />
        <RegRow name="D" value={registers.D} bits={8} note="General" />
        <RegRow name="E" value={registers.E} bits={8} note="General" />
        <RegRow name="H" value={registers.H} bits={8} note="General" />
        <RegRow name="L" value={registers.L} bits={8} note="General" />

        <div
          className="h-px my-1"
          style={{ background: 'var(--border-subtle)' }}
        />

        {/* 16-bit registers */}
        <RegRow name="PC" value={registers.PC} bits={16} note="Prog Counter" highlight />
        <RegRow name="SP" value={registers.SP} bits={16} note="Stack Ptr" />

        <div
          className="h-px my-1"
          style={{ background: 'var(--border-subtle)' }}
        />

        {/* HL pair computed value */}
        <div
          className="flex items-center justify-between px-2 py-1 rounded"
          style={{ background: 'color-mix(in srgb, var(--bg-tertiary) 50%, transparent)' }}
        >
          <div>
            <span
              className="text-[11px]"
              style={{ color: 'var(--text-secondary)' }}
            >
              HL
            </span>

            <span
              className="text-[9px] ml-1"
              style={{ color: 'var(--text-muted)' }}
            >
              pair
            </span>
          </div>

          <div className="text-right">
            <span
              className="text-xs font-mono"
              style={{ color: 'var(--accent-blue)' }}
            >
              {((registers.H << 8) | registers.L)
                .toString(16)
                .toUpperCase()
                .padStart(4, '0')}
              H
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
    <div
      className="flex items-center justify-between px-2 py-1 rounded transition-colors"
      style={{
        background: highlight
          ? 'var(--bg-tertiary)'
          : 'transparent',
      }}
      onMouseEnter={e => {
        if (!highlight) {
          e.currentTarget.style.background =
            'color-mix(in srgb, var(--bg-tertiary) 50%, transparent)'
        }
      }}
      onMouseLeave={e => {
        if (!highlight) {
          e.currentTarget.style.background = 'transparent'
        }
      }}
    >
      <div className="flex items-center gap-2 min-w-[48px]">
        <span
          className="text-xs font-semibold w-6"
          style={{
            color: highlight
              ? 'var(--accent-yellow)'
              : 'var(--text-secondary)',
          }}
        >
          {name}
        </span>

        {note && (
          <span
            className="text-[9px]"
            style={{ color: 'var(--text-muted)' }}
          >
            {note}
          </span>
        )}
      </div>

      <div className="text-right">
        <span
          className="text-xs font-mono"
          style={{ color: 'var(--accent-green)' }}
        >
          {hexStr}H
        </span>

        <span
          className="text-[10px] ml-2"
          style={{ color: 'var(--text-muted)' }}
        >
          {decStr}
        </span>
      </div>
    </div>
  )
}