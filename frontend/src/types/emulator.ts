/**
 * TypeScript type definitions for the 8085 emulator.
 * These match exactly what the Spring Boot backend sends as JSON.
 */

export interface Registers {
  A: number;
  B: number;
  C: number;
  D: number;
  E: number;
  H: number;
  L: number;
  PC: number;  // Program Counter
  SP: number;  // Stack Pointer
}

export interface Flags {
  S:  boolean;  // Sign
  Z:  boolean;  // Zero
  AC: boolean;  // Auxiliary Carry
  P:  boolean;  // Parity
  CY: boolean;  // Carry
}

export interface CpuState {
  registers: Registers;
  flags: Flags;
  halted: boolean;
}

export interface AssembleResponse {
  success: boolean;
  errors: string[];
  startAddress?: number;
  byteCount?: number;
  cpuState?: CpuState;
}

export interface StepRunResponse {
  success: boolean;
  halted: boolean;
  cpuState: CpuState;
  errors?: string[];
  message?: string;
}

export interface MemoryCell {
  address: number;
  addressHex: string;
  value: number;
  valueHex: string;
}

export interface MemoryResponse {
  startAddress: number;
  cells: MemoryCell[];
}
