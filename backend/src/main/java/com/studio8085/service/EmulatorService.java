package com.studio8085.service;

import com.studio8085.emulator.*;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * EmulatorService contains all the application logic.
 * The Controller calls these methods and gets back DTOs (plain Maps)
 * which are serialized to JSON and sent to the React frontend.
 */
@Service
public class EmulatorService {

    // Single global session - in Version 1 there's only one "user"
    private final EmulatorSession session = new EmulatorSession();
    private final Assembler assembler = new Assembler();

    // Track the program's start address after assembling
    private int programStartAddress = 0;

    /**
     * Assemble source code and load it into memory.
     * Also resets the CPU so PC points to the start of the program.
     */
    public Map<String, Object> assemble(String source) {
        // Assemble source to bytes
        Assembler.AssemblyResult result = assembler.assemble(source);

        Map<String, Object> response = new HashMap<>();

        if (!result.errors.isEmpty()) {
            response.put("success", false);
            response.put("errors", result.errors);
            return response;
        }

        // Reset CPU state but keep memory clean for new program
        session.reset();

        // Load program bytes into memory at the assembled start address
        session.memory.load(result.startAddress, result.bytes);

        // Point PC to start of program
        session.registers.setPC(result.startAddress);
        programStartAddress = result.startAddress;

        response.put("success", true);
        response.put("errors", List.of());
        response.put("startAddress", result.startAddress);
        response.put("byteCount", result.bytes.length);

        // Include current CPU state
        response.put("cpuState", getCpuState());

        return response;
    }

    /**
     * Execute one instruction (Step mode).
     */
    public Map<String, Object> step() {
        Map<String, Object> response = new HashMap<>();

        if (session.cpu.isHalted()) {
            response.put("success", false);
            response.put("message", "CPU is halted. Reset to run again.");
            response.put("halted", true);
            response.put("cpuState", getCpuState());
            return response;
        }

        boolean canContinue = session.cpu.step();
        String error = session.cpu.getLastError();

        response.put("success", error == null);
        response.put("halted", session.cpu.isHalted());
        response.put("cpuState", getCpuState());

        if (error != null) {
            response.put("errors", List.of(error));
        }

        return response;
    }

    /**
     * Run the program until HLT or error.
     */
    public Map<String, Object> run() {
        Map<String, Object> response = new HashMap<>();

        if (session.cpu.isHalted()) {
            response.put("success", false);
            response.put("message", "CPU is halted. Reset to run again.");
            response.put("halted", true);
            response.put("cpuState", getCpuState());
            return response;
        }

        session.cpu.run();

        String error = session.cpu.getLastError();
        response.put("success", error == null);
        response.put("halted", session.cpu.isHalted());
        response.put("cpuState", getCpuState());

        if (error != null) {
            response.put("errors", List.of(error));
        }

        return response;
    }

    /**
     * Reset the CPU and memory entirely.
     */
    public Map<String, Object> reset() {
        session.reset();
        programStartAddress = 0;
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("cpuState", getCpuState());
        return response;
    }

    /**
     * Get current CPU state (registers + flags).
     * This is sent to the frontend after every operation.
     */
    public Map<String, Object> getCpuState() {
        Map<String, Object> state = new HashMap<>();

        // Registers
        Map<String, Object> regs = new HashMap<>();
        regs.put("A", session.registers.getA());
        regs.put("B", session.registers.getB());
        regs.put("C", session.registers.getC());
        regs.put("D", session.registers.getD());
        regs.put("E", session.registers.getE());
        regs.put("H", session.registers.getH());
        regs.put("L", session.registers.getL());
        regs.put("PC", session.registers.getPC());
        regs.put("SP", session.registers.getSP());
        state.put("registers", regs);

        // Flags
        Map<String, Object> flgs = new HashMap<>();
        flgs.put("S",  session.flags.isS());
        flgs.put("Z",  session.flags.isZ());
        flgs.put("AC", session.flags.isAC());
        flgs.put("P",  session.flags.isP());
        flgs.put("CY", session.flags.isCY());
        state.put("flags", flgs);

        state.put("halted", session.cpu.isHalted());

        return state;
    }

    /**
     * Get a chunk of memory for the Memory Viewer.
     * Returns 'length' bytes starting from 'startAddress'.
     */
    public Map<String, Object> getMemory(int startAddress, int length) {
        int[] bytes = session.memory.dump(startAddress, Math.min(length, 256));

        Map<String, Object> response = new HashMap<>();
        response.put("startAddress", startAddress);

        // Build list of {address, value} objects for the table
        Map<String, Object>[] cells = new Map[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            Map<String, Object> cell = new HashMap<>();
            cell.put("address", startAddress + i);
            cell.put("addressHex", String.format("%04X", startAddress + i));
            cell.put("value", bytes[i]);
            cell.put("valueHex", String.format("%02X", bytes[i]));
            cells[i] = cell;
        }

        response.put("cells", cells);
        return response;
    }
}
