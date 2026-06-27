/**
 * Toolbar Component
 *
 * Contains the main action buttons: Assemble, Run, Step, Reset
 * Also shows a status indicator (halted / running / idle)
 */

import React from 'react'

interface ToolbarProps {
  onAssemble: () => void
  onRun: () => void
  onStep: () => void
  onReset: () => void
  onClear: () => void
  isLoading: boolean
  isHalted: boolean
  isAssembled: boolean
}

export function Toolbar({
  onAssemble, onRun, onStep, onReset, onClear,
  isLoading, isHalted, isAssembled
}: ToolbarProps) {

  return (
    <div className="flex items-center gap-2 px-4 py-2 bg-[#161B22] border-b border-[#30363D]">
      {/* Logo */}
      <div className="flex items-center gap-2 mr-4">
        <div className="w-2 h-2 rounded-full bg-[#39D353] animate-pulse" />
        <span className="text-[#39D353] font-semibold text-sm tracking-widest">8085 STUDIO</span>
      </div>

      <div className="w-px h-5 bg-[#30363D]" />

      {/* Assemble button - primary action */}
      <Button
        onClick={onAssemble}
        disabled={isLoading}
        color="green"
        title="Assemble (Ctrl+Enter)"
      >
        ⚙ Assemble
      </Button>

      {/* Run button */}
      <Button
        onClick={onRun}
        disabled={isLoading || !isAssembled || isHalted}
        color="blue"
        title="Run until HLT"
      >
        ▶ Run
      </Button>

      {/* Step button */}
      <Button
        onClick={onStep}
        disabled={isLoading || !isAssembled || isHalted}
        color="yellow"
        title="Execute one instruction"
      >
        ⏭ Step
      </Button>

      <div className="w-px h-5 bg-[#30363D]" />

      {/* Reset button */}
      <Button onClick={onReset} disabled={isLoading} color="red" title="Reset CPU & Memory">
        ↺ Reset
      </Button>

      {/* Clear editor button */}
      <Button onClick={onClear} disabled={isLoading} color="gray" title="Clear editor">
        ✕ Clear
      </Button>

      {/* Status indicator */}
      <div className="ml-auto flex items-center gap-2">
        {isLoading && (
          <span className="text-[#8B949E] text-xs">Processing...</span>
        )}
        {isHalted && !isLoading && (
          <span className="text-[#FF6B6B] text-xs font-medium">● HALTED</span>
        )}
        {isAssembled && !isHalted && !isLoading && (
          <span className="text-[#39D353] text-xs font-medium">● READY</span>
        )}
        {!isAssembled && !isLoading && (
          <span className="text-[#8B949E] text-xs">Not assembled</span>
        )}
      </div>
    </div>
  )
}

// Small reusable button inside toolbar
interface ButtonProps {
  onClick: () => void
  disabled?: boolean
  color: 'green' | 'blue' | 'yellow' | 'red' | 'gray'
  title?: string
  children: React.ReactNode
}

function Button({ onClick, disabled, color, title, children }: ButtonProps) {
  const colorMap = {
    green:  'hover:bg-[#39D353]/20 hover:text-[#39D353] border-[#39D353]/30',
    blue:   'hover:bg-[#58A6FF]/20 hover:text-[#58A6FF] border-[#58A6FF]/30',
    yellow: 'hover:bg-[#F0C674]/20 hover:text-[#F0C674] border-[#F0C674]/30',
    red:    'hover:bg-[#FF6B6B]/20 hover:text-[#FF6B6B] border-[#FF6B6B]/30',
    gray:   'hover:bg-[#8B949E]/20 hover:text-[#8B949E] border-[#8B949E]/30',
  }

  return (
    <button
      onClick={onClick}
      disabled={disabled}
      title={title}
      className={`
        px-3 py-1 rounded text-xs font-medium
        text-[#8B949E] border border-transparent
        transition-all duration-150
        disabled:opacity-30 disabled:cursor-not-allowed
        ${!disabled ? colorMap[color] : ''}
      `}
    >
      {children}
    </button>
  )
}
