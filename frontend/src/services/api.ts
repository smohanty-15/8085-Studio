/**
 * API Service
 *
 * All communication with the Spring Boot backend happens here.
 * Uses the Fetch API (built into browsers, no library needed).
 *
 * The Vite proxy (vite.config.ts) forwards /api/* → http://localhost:9090/api/*
 * so we don't need to write the full URL everywhere.
 */

import type {
  AssembleResponse,
  StepRunResponse,
  CpuState,
  MemoryResponse
} from '../types/emulator'

const BASE =
  import.meta.env.VITE_API_URL
    ? `${import.meta.env.VITE_API_URL}/api`
    : '/api'

//temp log
console.log("API BASE=",BASE);

/** Assemble source code and load it into the emulator */
export async function assembleCode(source: string): Promise<AssembleResponse> {
  const res = await fetch(`${BASE}/assemble`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ source }),
  })
  return res.json()
}

/** Execute one instruction */
export async function stepCpu(): Promise<StepRunResponse> {
  const res = await fetch(`${BASE}/step`, { method: 'POST' })
  return res.json()
}

/** Run until HLT */
export async function runCpu(): Promise<StepRunResponse> {
  const res = await fetch(`${BASE}/run`, { method: 'POST' })
  return res.json()
}

/** Reset CPU and memory */
export async function resetCpu(): Promise<{ success: boolean; cpuState: CpuState }> {
  const res = await fetch(`${BASE}/reset`, { method: 'POST' })
  return res.json()
}

/** Get current CPU state */
export async function getCpuState(): Promise<CpuState> {
  const res = await fetch(`${BASE}/state`)
  return res.json()
}

/** Get memory contents for the viewer */
export async function getMemory(start = 0, length = 64): Promise<MemoryResponse> {
  const res = await fetch(`${BASE}/memory?start=${start}&length=${length}`)
  return res.json()
}