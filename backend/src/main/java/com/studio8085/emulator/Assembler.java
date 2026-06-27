package com.studio8085.emulator;

import java.util.*;

/**
 * 8085 Assembler
 *
 * Converts human-readable assembly code (like "MVI A, 10H") into
 * machine code bytes that the CPU can execute.
 *
 * TWO-PASS ASSEMBLER:
 *   Pass 1: Scan all lines, record label addresses (so JMP can resolve forward refs)
 *   Pass 2: Actually generate the bytes, now that all labels are known
 *
 * Example:
 *   Input:  MVI A, 10H
 *   Output: [0x3E, 0x10]   (opcode 0x3E means "load immediate into A")
 */
public class Assembler {

    // Register name → register code used in MOV opcode calculation
    private static final Map<String, Integer> REG_CODE = new HashMap<>();
    static {
        REG_CODE.put("B", 0);
        REG_CODE.put("C", 1);
        REG_CODE.put("D", 2);
        REG_CODE.put("E", 3);
        REG_CODE.put("H", 4);
        REG_CODE.put("L", 5);
        // M (memory via HL) would be 6, but we skip it for simplicity
        REG_CODE.put("A", 7);
    }

    /** Result returned to the caller */
    public static class AssemblyResult {
        public final int[] bytes;         // the machine code
        public final int startAddress;    // where in memory it starts
        public final List<String> errors; // any error messages
        public final Map<Integer, Integer> lineToAddress; // line number → memory address

        public AssemblyResult(int[] bytes, int startAddress,
                              List<String> errors, Map<Integer, Integer> lineToAddress) {
            this.bytes = bytes;
            this.startAddress = startAddress;
            this.errors = errors;
            this.lineToAddress = lineToAddress;
        }
    }

    /**
     * Assemble the given source code text.
     *
     * @param source  The full assembly source code as a string
     * @return        AssemblyResult with bytes and any errors
     */
    public AssemblyResult assemble(String source) {
        List<String> errors = new ArrayList<>();
        Map<String, Integer> labels = new HashMap<>();    // label → address
        Map<Integer, Integer> lineToAddress = new HashMap<>();

        String[] lines = source.split("\n");
        List<String[]> tokenLines = new ArrayList<>();    // parsed tokens per line
        List<Integer> originalLineNums = new ArrayList<>(); // track line numbers for errors

        int currentAddress = 0x0000;  // default load address

        // ==================
        // PASS 1: Find labels and calculate addresses
        // ==================
        for (int lineNum = 0; lineNum < lines.length; lineNum++) {
            String line = lines[lineNum].trim();

            // Remove comments (; is the comment character in 8085 assembly)
            int commentIdx = line.indexOf(';');
            if (commentIdx >= 0) line = line.substring(0, commentIdx).trim();

            if (line.isEmpty()) continue;

            // Check for ORG directive (e.g. "ORG 2000H" sets the load address)
            if (line.toUpperCase().startsWith("ORG")) {
                String[] parts = line.split("\\s+", 2);
                if (parts.length == 2) {
                    try {
                        currentAddress = parseNumber(parts[1].trim());
                    } catch (Exception e) {
                        errors.add("Line " + (lineNum + 1) + ": Invalid ORG address: " + parts[1]);
                    }
                }
                continue;
            }

            // Check for label (ends with colon, e.g. "LOOP:")
            if (line.contains(":")) {
                String labelPart = line.substring(0, line.indexOf(':')).trim();
                labels.put(labelPart.toUpperCase(), currentAddress);
                line = line.substring(line.indexOf(':') + 1).trim();
                if (line.isEmpty()) continue;
            }

            // Tokenize: split mnemonic from operands
            String[] parts = line.split("\\s+", 2);
            String mnemonic = parts[0].toUpperCase();
            String operands = parts.length > 1 ? parts[1].trim() : "";

            // Calculate how many bytes this instruction takes (for address tracking)
            int size = getInstructionSize(mnemonic, operands, lineNum + 1, errors);
            currentAddress += size;

            tokenLines.add(new String[]{mnemonic, operands, String.valueOf(lineNum + 1)});
            originalLineNums.add(lineNum + 1);
        }

        // ==================
        // PASS 2: Generate machine code bytes
        // ==================
        List<Integer> byteList = new ArrayList<>();
        currentAddress = 0x0000;
        int startAddress = 0x0000;
        boolean firstInstruction = true;

        // Re-scan to find ORG and then generate bytes
        for (int lineNum = 0; lineNum < lines.length; lineNum++) {
            String line = lines[lineNum].trim();

            int commentIdx = line.indexOf(';');
            if (commentIdx >= 0) line = line.substring(0, commentIdx).trim();
            if (line.isEmpty()) continue;

            if (line.toUpperCase().startsWith("ORG")) {
                String[] parts = line.split("\\s+", 2);
                if (parts.length == 2) {
                    try {
                        currentAddress = parseNumber(parts[1].trim());
                        if (firstInstruction) startAddress = currentAddress;
                    } catch (Exception ignored) {}
                }
                continue;
            }

            if (line.contains(":")) {
                line = line.substring(line.indexOf(':') + 1).trim();
                if (line.isEmpty()) continue;
            }

            String[] parts = line.split("\\s+", 2);
            String mnemonic = parts[0].toUpperCase();
            String operands = parts.length > 1 ? parts[1].trim() : "";

            if (firstInstruction) {
                startAddress = currentAddress;
                firstInstruction = false;
            }

            lineToAddress.put(lineNum + 1, currentAddress);

            try {
                List<Integer> instrBytes = encodeInstruction(mnemonic, operands, labels, lineNum + 1);
                byteList.addAll(instrBytes);
                currentAddress += instrBytes.size();
            } catch (Exception e) {
                errors.add("Line " + (lineNum + 1) + ": " + e.getMessage());
                // Skip this instruction size approximation
                currentAddress += getInstructionSize(mnemonic, operands, lineNum + 1, errors);
            }
        }

        int[] bytes = byteList.stream().mapToInt(Integer::intValue).toArray();
        return new AssemblyResult(bytes, startAddress, errors, lineToAddress);
    }

    /**
     * Estimate instruction size (in bytes) so we can track addresses in pass 1.
     * 1-byte instructions: NOP, HLT, MOV, ADD, SUB, INR, DCR, ANA, ORA, XRA, CMP
     * 2-byte instructions: MVI, ADI
     * 3-byte instructions: LDA, STA, JMP, JZ, JNZ
     */
    private int getInstructionSize(String mnemonic, String operands, int lineNum, List<String> errors) {
        return switch (mnemonic) {
            case "NOP", "HLT" -> 1;
            case "MOV", "ADD", "SUB", "INR", "DCR",
                 "ANA", "ORA", "XRA", "CMP" -> 1;
            case "MVI", "ADI" -> 2;
            case "LDA", "STA", "JMP", "JZ", "JNZ" -> 3;
            default -> 1; // unknown, assume 1
        };
    }

    /**
     * Encode a single instruction into bytes.
     * Returns a list of bytes (1, 2, or 3 bytes depending on instruction).
     *
     * HOW MOV OPCODE WORKS:
     *   MOV dst, src  →  opcode = 0x40 + (dst_code * 8) + src_code
     *   Example: MOV A, B  →  0x40 + (7*8) + 0 = 0x40 + 56 + 0 = 0x78
     */
    private List<Integer> encodeInstruction(String mnemonic, String operands,
                                             Map<String, Integer> labels, int lineNum) {
        List<Integer> bytes = new ArrayList<>();

        switch (mnemonic) {
            case "NOP" -> bytes.add(0x00);

            case "HLT" -> bytes.add(0x76);

            case "MOV" -> {
                // MOV dst, src
                String[] ops = splitOperands(operands, 2, "MOV", lineNum);
                String dst = ops[0].toUpperCase();
                String src = ops[1].toUpperCase();
                int dstCode = getRegCode(dst, lineNum);
                int srcCode = getRegCode(src, lineNum);
                bytes.add(0x40 + (dstCode * 8) + srcCode);
            }

            case "MVI" -> {
                // MVI reg, immediate   e.g. MVI A, 10H
                String[] ops = splitOperands(operands, 2, "MVI", lineNum);
                String reg = ops[0].toUpperCase();
                int imm = parseNumber(ops[1].trim());
                // MVI opcodes: B=0x06, C=0x0E, D=0x16, E=0x1E, H=0x26, L=0x2E, A=0x3E
                int[] mviOpcodes = {0x06, 0x0E, 0x16, 0x1E, 0x26, 0x2E, -1, 0x3E};
                bytes.add(mviOpcodes[getRegCode(reg, lineNum)]);
                bytes.add(imm & 0xFF);
            }

            case "LDA" -> {
                // LDA address   - Load Accumulator from memory address
                int addr = resolveAddress(operands.trim(), labels, lineNum);
                bytes.add(0x3A);
                bytes.add(addr & 0xFF);        // low byte of address
                bytes.add((addr >> 8) & 0xFF); // high byte of address
            }

            case "STA" -> {
                // STA address   - Store Accumulator to memory address
                int addr = resolveAddress(operands.trim(), labels, lineNum);
                bytes.add(0x32);
                bytes.add(addr & 0xFF);
                bytes.add((addr >> 8) & 0xFF);
            }

            case "ADD" -> {
                // ADD reg  - A = A + reg
                String reg = operands.trim().toUpperCase();
                int regCode = getRegCode(reg, lineNum);
                bytes.add(0x80 + regCode);
            }

            case "ADI" -> {
                // ADI immediate  - A = A + immediate
                int imm = parseNumber(operands.trim());
                bytes.add(0xC6);
                bytes.add(imm & 0xFF);
            }

            case "SUB" -> {
                // SUB reg  - A = A - reg
                String reg = operands.trim().toUpperCase();
                int regCode = getRegCode(reg, lineNum);
                bytes.add(0x90 + regCode);
            }

            case "INR" -> {
                // INR reg  - reg = reg + 1 (increment)
                String reg = operands.trim().toUpperCase();
                // INR opcodes: B=0x04, C=0x0C, D=0x14, E=0x1C, H=0x24, L=0x2C, A=0x3C
                int[] inrOpcodes = {0x04, 0x0C, 0x14, 0x1C, 0x24, 0x2C, -1, 0x3C};
                bytes.add(inrOpcodes[getRegCode(reg, lineNum)]);
            }

            case "DCR" -> {
                // DCR reg  - reg = reg - 1 (decrement)
                String reg = operands.trim().toUpperCase();
                // DCR opcodes: B=0x05, C=0x0D, D=0x15, E=0x1D, H=0x25, L=0x2D, A=0x3D
                int[] dcrOpcodes = {0x05, 0x0D, 0x15, 0x1D, 0x25, 0x2D, -1, 0x3D};
                bytes.add(dcrOpcodes[getRegCode(reg, lineNum)]);
            }

            case "ANA" -> {
                // ANA reg  - A = A AND reg (logical AND)
                String reg = operands.trim().toUpperCase();
                bytes.add(0xA0 + getRegCode(reg, lineNum));
            }

            case "ORA" -> {
                // ORA reg  - A = A OR reg (logical OR)
                String reg = operands.trim().toUpperCase();
                bytes.add(0xB0 + getRegCode(reg, lineNum));
            }

            case "XRA" -> {
                // XRA reg  - A = A XOR reg (exclusive OR)
                String reg = operands.trim().toUpperCase();
                bytes.add(0xA8 + getRegCode(reg, lineNum));
            }

            case "CMP" -> {
                // CMP reg  - Compare A with reg (sets flags, doesn't change A)
                String reg = operands.trim().toUpperCase();
                bytes.add(0xB8 + getRegCode(reg, lineNum));
            }

            case "JMP" -> {
                // JMP address  - Unconditional jump
                int addr = resolveAddress(operands.trim(), labels, lineNum);
                bytes.add(0xC3);
                bytes.add(addr & 0xFF);
                bytes.add((addr >> 8) & 0xFF);
            }

            case "JZ" -> {
                // JZ address  - Jump if Zero flag is set
                int addr = resolveAddress(operands.trim(), labels, lineNum);
                bytes.add(0xCA);
                bytes.add(addr & 0xFF);
                bytes.add((addr >> 8) & 0xFF);
            }

            case "JNZ" -> {
                // JNZ address  - Jump if Zero flag is NOT set
                int addr = resolveAddress(operands.trim(), labels, lineNum);
                bytes.add(0xC2);
                bytes.add(addr & 0xFF);
                bytes.add((addr >> 8) & 0xFF);
            }

            default -> throw new RuntimeException("Unknown instruction: " + mnemonic);
        }

        return bytes;
    }

    /** Split operands by comma, expecting exactly 'count' parts */
    private String[] splitOperands(String operands, int count, String mnemonic, int lineNum) {
        String[] parts = operands.split(",", count);
        if (parts.length < count) {
            throw new RuntimeException(mnemonic + " requires " + count + " operands, got: " + operands);
        }
        for (int i = 0; i < parts.length; i++) parts[i] = parts[i].trim();
        return parts;
    }

    /** Get register code (0-7) for MOV/ADD encoding */
    private int getRegCode(String reg, int lineNum) {
        Integer code = REG_CODE.get(reg.toUpperCase());
        if (code == null) throw new RuntimeException("Invalid register: " + reg);
        return code;
    }

    /**
     * Resolve an address or label name to an integer address.
     * e.g. "2000H" → 0x2000, "LOOP" → whatever address LOOP was defined at
     */
    private int resolveAddress(String token, Map<String, Integer> labels, int lineNum) {
        String upper = token.toUpperCase();
        if (labels.containsKey(upper)) return labels.get(upper);
        return parseNumber(token);
    }

    /**
     * Parse a number literal in various formats:
     *   "10H" or "0AH" or "0x0A" → hex
     *   "10" or "10D" → decimal
     *   "1010B" → binary
     *
     * The 8085 assembler traditionally uses H suffix for hex: "FFH", "2000H"
     */
    public static int parseNumber(String token) {
        token = token.trim().toUpperCase();
        try {
            if (token.startsWith("0X")) {
                return Integer.parseInt(token.substring(2), 16);
            } else if (token.endsWith("H")) {
                return Integer.parseInt(token.substring(0, token.length() - 1), 16);
            } else if (token.endsWith("B")) {
                return Integer.parseInt(token.substring(0, token.length() - 1), 2);
            } else if (token.endsWith("D")) {
                return Integer.parseInt(token.substring(0, token.length() - 1), 10);
            } else {
                return Integer.parseInt(token, 10);
            }
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid number: " + token);
        }
    }
}
