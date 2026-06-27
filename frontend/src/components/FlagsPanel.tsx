/**
 * FlagsPanel Component
 *
 * Displays the 5 status flags of the 8085.
 * Each flag is either SET (1) or CLEAR (0).
 * They are automatically updated after every arithmetic/logical operation.
 *
 * FLAGS EXPLAINED:
 *   S  (Sign)       - 1 if result was negative (bit 7 = 1)
 *   Z  (Zero)       - 1 if result was exactly 0
 *   AC (Aux Carry)  - 1 if carry from bit 3→4 (used for BCD arithmetic)
 *   P  (Parity)     - 1 if result has even number of 1-bits
 *   CY (Carry)      - 1 if there was a carry out / borrow
 */

import type { Flags } from '../types/emulator'

interface FlagsPanelProps {
  flags: Flags
}

export function FlagsPanel({ flags }: FlagsPanelProps) {
  return (
    <div>
      <div
        className="text-[10px] font-semibold uppercase tracking-widest mb-2 px-1"
        style={{ color: 'var(--text-secondary)' }}
      >
        Flags
      </div>

      <div className="grid grid-cols-5 gap-1">
        <FlagBit name="S" value={flags.S} title="Sign: result is negative" />
        <FlagBit name="Z" value={flags.Z} title="Zero: result is zero" />
        <FlagBit name="AC" value={flags.AC} title="Aux Carry: carry from bit 3" />
        <FlagBit name="P" value={flags.P} title="Parity: even number of 1-bits" />
        <FlagBit name="CY" value={flags.CY} title="Carry: carry out of bit 7" />
      </div>
    </div>
  )
}

interface FlagBitProps {
  name: string
  value: boolean
  title: string
}

function FlagBit({ name, value, title }: FlagBitProps) {
  return (
    <div
      title={title}
      className="flex flex-col items-center py-2 rounded cursor-help border transition-all duration-200"
      style={{
        backgroundColor: value
          ? 'color-mix(in srgb, var(--accent-green) 10%, transparent)'
          : 'var(--bg-tertiary)',
        borderColor: value
          ? 'color-mix(in srgb, var(--accent-green) 40%, transparent)'
          : 'var(--border-color)',
        color: value
          ? 'var(--accent-green)'
          : 'var(--text-muted)',
      }}
    >
      <span className="text-[10px] font-semibold">{name}</span>
      <span className="text-sm font-bold mt-0.5">{value ? '1' : '0'}</span>
    </div>
  )
}