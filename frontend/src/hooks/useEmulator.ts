/**
 * useEmulator - Custom React Hook
 *
 * Manages all emulator state in one place.
 * Components just call this hook to get state and actions.
 *
 * This is a common React pattern: keep logic in hooks, keep components simple.
 */

import { useState, useCallback } from 'react'
import type { CpuState, MemoryCell } from '../types/emulator'
import * as api from '../services/api'

// Default CPU state (all zeros, not halted)
const defaultCpuState: CpuState = {
  registers: { A: 0, B: 0, C: 0, D: 0, E: 0, H: 0, L: 0, PC: 0, SP: 0xFFFF },
  flags: { S: false, Z: false, AC: false, P: false, CY: false },
  halted: false,
}

export function useEmulator() {
  // === State ===
  const [cpuState, setCpuState]       = useState<CpuState>(defaultCpuState)
  const [memory, setMemory]           = useState<MemoryCell[]>([])
  const [errors, setErrors]           = useState<string[]>([])
  const [messages, setMessages]       = useState<string[]>([])
  const [isAssembled, setIsAssembled] = useState(false)
  const [isLoading, setIsLoading]     = useState(false)
  const [memStart, setMemStart]       = useState(0)

  // === Helpers ===
  const clearMessages = () => { setErrors([]); setMessages([]) }

  const refreshMemory = useCallback(async (start: number = memStart) => {
    try {
      const memData = await api.getMemory(start, 64)
      setMemory(memData.cells)
    } catch {
      // memory fetch is non-critical
    }
  }, [memStart])

  // === Actions ===

  /**
   * Assemble the source code.
   * On success: loads program into backend memory, updates CPU state.
   * On failure: shows errors.
   */
  const assemble = useCallback(async (source: string) => {
    clearMessages()
    setIsLoading(true)
    try {
      const result = await api.assembleCode(source)
      if (result.success) {
        setIsAssembled(true)
        if (result.cpuState) setCpuState(result.cpuState)
        setMessages([
          `✓ Assembled successfully. ${result.byteCount} bytes at 0x${(result.startAddress ?? 0).toString(16).toUpperCase().padStart(4, '0')}`
        ])
        // Show memory starting at program address
        const startAddr = result.startAddress ?? 0
        setMemStart(startAddr)
        await refreshMemory(startAddr)
      } else {
        setIsAssembled(false)
        setErrors(result.errors)
      }
    } catch (e) {
      setErrors(['Failed to connect to backend. Is Spring Boot running on port 9090?'])
    } finally {
      setIsLoading(false)
    }
  }, [refreshMemory])

  /**
   * Step: execute one instruction.
   */
  const step = useCallback(async () => {
    if (!isAssembled) {
      setErrors(['Assemble the code first before stepping.'])
      return
    }
    clearMessages()
    setIsLoading(true)
    try {
      const result = await api.stepCpu()
      setCpuState(result.cpuState)
      if (result.halted) {
        setMessages(['CPU halted. Reset to run again.'])
      }
      if (result.errors?.length) {
        setErrors(result.errors)
      }
      await refreshMemory()
    } catch (e) {
      setErrors(['Connection error'])
    } finally {
      setIsLoading(false)
    }
  }, [isAssembled, refreshMemory])

  /**
   * Run: execute until HLT.
   */
  const run = useCallback(async () => {
    if (!isAssembled) {
      setErrors(['Assemble the code first before running.'])
      return
    }
    clearMessages()
    setIsLoading(true)
    try {
      const result = await api.runCpu()
      setCpuState(result.cpuState)
      if (result.halted) {
        setMessages(['Program finished (HLT reached).'])
      }
      if (result.errors?.length) {
        setErrors(result.errors)
      }
      await refreshMemory()
    } catch (e) {
      setErrors(['Connection error'])
    } finally {
      setIsLoading(false)
    }
  }, [isAssembled, refreshMemory])

  /**
   * Reset: clear CPU, memory, and all UI state.
   */
  const reset = useCallback(async () => {
    clearMessages()
    setIsLoading(true)
    try {
      const result = await api.resetCpu()
      setCpuState(result.cpuState)
      setIsAssembled(false)
      setMemStart(0)
      await refreshMemory(0)
      setMessages(['CPU and memory reset.'])
    } catch (e) {
      setErrors(['Connection error'])
    } finally {
      setIsLoading(false)
    }
  }, [refreshMemory])

  /**
   * Navigate memory viewer to a different starting address.
   */
  const navigateMemory = useCallback(async (start: number) => {
    setMemStart(start)
    await refreshMemory(start)
  }, [refreshMemory])

  return {
    // State
    cpuState,
    memory,
    errors,
    messages,
    isAssembled,
    isLoading,
    memStart,
    // Actions
    assemble,
    step,
    run,
    reset,
    navigateMemory,
  }
}
