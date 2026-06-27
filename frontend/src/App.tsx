/**
 * App.tsx - Main Application Layout
 *
 * This is the root component. It puts together:
 *   - Toolbar (buttons)
 *   - Code Editor (Monaco)
 *   - Register Panel (right side)
 *   - Flags Panel (right side, below registers)
 *   - Memory Viewer (bottom)
 *   - Message/Error Panel (below editor)
 *
 * Layout:
 * ┌─────────────────────────────────────────┐
 * │            TOOLBAR                       │
 * ├──────────────────────┬──────────────────┤
 * │                      │  Registers       │
 * │   Code Editor        │  Flags           │
 * │                      │  PC info         │
 * ├──────────────────────┴──────────────────┤
 * │  Messages / Errors                       │
 * ├─────────────────────────────────────────┤
 * │  Memory Viewer                          │
 * └─────────────────────────────────────────┘
 */

import { useState, useCallback } from 'react'
import { Toolbar }       from './components/Toolbar'
import { CodeEditor, DEFAULT_CODE } from './components/CodeEditor'
import { RegisterPanel } from './components/RegisterPanel'
import { FlagsPanel }    from './components/FlagsPanel'
import { MemoryViewer }  from './components/MemoryViewer'
import { MessagePanel }  from './components/MessagePanel'
import { SamplePrograms } from './components/SamplePrograms'
import { useEmulator }   from './hooks/useEmulator'

export default function App() {
  const [code, setCode] = useState(DEFAULT_CODE)

  const {
    cpuState, memory, errors, messages,
    isAssembled, isLoading, memStart,
    assemble, step, run, reset, navigateMemory,
  } = useEmulator()

  const handleAssemble = useCallback(() => {
    assemble(code)
  }, [assemble, code])

  const handleClear = useCallback(() => {
    setCode('')
  }, [])

  const handleLoadSample = useCallback((sampleCode: string) => {
    setCode(sampleCode)
  }, [])

  return (
    <div className="flex flex-col h-screen bg-[#0D1117] text-[#E6EDF3] overflow-hidden">

      {/* ── TOOLBAR ── */}
      <Toolbar
        onAssemble={handleAssemble}
        onRun={run}
        onStep={step}
        onReset={reset}
        onClear={handleClear}
        isLoading={isLoading}
        isHalted={cpuState.halted}
        isAssembled={isAssembled}
      />

      {/* ── MAIN AREA (editor + panels) ── */}
      <div className="flex flex-1 min-h-0">

        {/* LEFT: Code Editor */}
        <div className="flex flex-col flex-1 min-w-0">

          {/* Editor toolbar (samples, file buttons) */}
          <div className="flex items-center gap-2 px-4 py-1.5 bg-[#161B22] border-b border-[#30363D]">
            <span className="text-[10px] text-[#484F58] uppercase tracking-widest">Editor</span>
            <div className="ml-2">
              <SamplePrograms onLoad={handleLoadSample} />
            </div>
            {/* File save/load buttons */}
            <SaveLoadButtons code={code} onLoad={setCode} />
            <span className="ml-auto text-[10px] text-[#484F58]">Ctrl+Enter to assemble</span>
          </div>

          {/* Monaco editor */}
          <div className="flex-1 min-h-0">
            <CodeEditor
              value={code}
              onChange={setCode}
              onAssemble={handleAssemble}
            />
          </div>

          {/* Error/success messages */}
          <MessagePanel errors={errors} messages={messages} />
        </div>

        {/* RIGHT: Registers + Flags + PC info */}
        <div className="w-56 flex flex-col border-l border-[#30363D] bg-[#161B22] overflow-hidden">

          {/* PC / Status section */}
          <div className="px-3 pt-2 pb-2 border-b border-[#30363D]">
            <div className="text-[10px] text-[#8B949E] uppercase tracking-widest mb-1">Status</div>
            <div className="flex items-center gap-2">
              <span className="text-[10px] text-[#484F58]">PC:</span>
              <span className="text-xs text-[#F0C674] font-mono">
                {cpuState.registers.PC.toString(16).toUpperCase().padStart(4, '0')}H
              </span>
              <span className={`ml-auto text-[9px] px-1.5 py-0.5 rounded-full font-medium ${
                cpuState.halted
                  ? 'bg-[#FF6B6B]/20 text-[#FF6B6B]'
                  : 'bg-[#39D353]/20 text-[#39D353]'
              }`}>
                {cpuState.halted ? 'HALTED' : 'ACTIVE'}
              </span>
            </div>
          </div>

          {/* Registers */}
          <div className="flex-1 overflow-y-auto px-3 pt-3 min-h-0">
            <RegisterPanel registers={cpuState.registers} />
          </div>

          {/* Flags */}
          <div className="px-3 pb-3 border-t border-[#30363D] pt-3">
            <FlagsPanel flags={cpuState.flags} />
          </div>
        </div>
      </div>

      {/* ── BOTTOM: Memory Viewer ── */}
      <div className="h-52 border-t border-[#30363D] bg-[#0D1117] px-4 py-2 flex flex-col">
        <MemoryViewer
          cells={memory}
          currentPC={cpuState.registers.PC}
          memStart={memStart}
          onNavigate={navigateMemory}
        />
      </div>
    </div>
  )
}

// ─── File Save / Load Buttons ───────────────────────────────────────────────

function SaveLoadButtons({ code, onLoad }: { code: string; onLoad: (c: string) => void }) {
  const handleSave = () => {
    const blob = new Blob([code], { type: 'text/plain' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'program.asm'
    a.click()
    URL.revokeObjectURL(url)
  }

  const handleOpen = () => {
    const input = document.createElement('input')
    input.type = 'file'
    input.accept = '.asm,.txt'
    input.onchange = (e) => {
      const file = (e.target as HTMLInputElement).files?.[0]
      if (!file) return
      const reader = new FileReader()
      reader.onload = (ev) => onLoad(ev.target?.result as string)
      reader.readAsText(file)
    }
    input.click()
  }

  return (
    <div className="flex items-center gap-1">
      <button
        onClick={handleOpen}
        className="px-2 py-0.5 text-[10px] text-[#8B949E] hover:text-[#E6EDF3] border border-transparent hover:border-[#30363D] rounded transition-colors"
        title="Open .asm file"
      >
        📁 Open
      </button>
      <button
        onClick={handleSave}
        className="px-2 py-0.5 text-[10px] text-[#8B949E] hover:text-[#E6EDF3] border border-transparent hover:border-[#30363D] rounded transition-colors"
        title="Save as .asm file"
      >
        💾 Save
      </button>
    </div>
  )
}
