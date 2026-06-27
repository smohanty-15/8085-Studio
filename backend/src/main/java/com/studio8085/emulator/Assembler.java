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
 *
 * FULL 8085 INSTRUCTION SET - 74 basic types / 246 total opcodes:
 *   Data Transfer:  MOV, MVI, LDA, STA, LHLD, SHLD, LDAX, STAX, XCHG
 *                   LXI, PUSH, POP, XTHL, SPHL, PCHL, OUT, IN
 *   Arithmetic:     ADD, ADC, ADI, ACI, SUB, SBB, SUI, SBI
 *                   INR, DCR, INX, DCX, DAD, DAA
 *   Logical:        ANA, ANI, ORA, ORI, XRA, XRI, CMP, CPI
 *                   RLC, RRC, RAL, RAR, CMA, CMC, STC
 *   Branch:         JMP, JC, JNC, JZ, JNZ, JM, JP, JPE, JPO
 *                   CALL, CC, CNC, CZ, CNZ, CM, CP, CPE, CPO
 *                   RET, RC, RNC, RZ, RNZ, RM, RP, RPE, RPO
 *                   RST, PCHL
 *   Control:        NOP, HLT, DI, EI, RIM, SIM
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
        REG_CODE.put("M", 6); // Memory via HL pair
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

            // Check for DB directive (Define Byte - embed raw bytes)
            if (line.toUpperCase().startsWith("DB")) {
                String[] parts = line.split("\\s+", 2);
                if (parts.length == 2) {
                    String[] values = parts[1].split(",");
                    currentAddress += values.length;
                }
                continue;
            }

            // Check for DW directive (Define Word - embed 16-bit words)
            if (line.toUpperCase().startsWith("DW")) {
                String[] parts = line.split("\\s+", 2);
                if (parts.length == 2) {
                    String[] values = parts[1].split(",");
                    currentAddress += values.length * 2;
                }
                continue;
            }

            // Check for EQU directive (define a constant, no bytes emitted)
            if (line.toUpperCase().contains(" EQU ") || line.toUpperCase().contains("\tEQU\t")) {
                String[] parts = line.split("(?i)\\s+EQU\\s+", 2);
                if (parts.length == 2) {
                    try {
                        labels.put(parts[0].trim().toUpperCase(), parseNumber(parts[1].trim()));
                    } catch (Exception e) {
                        errors.add("Line " + (lineNum + 1) + ": Invalid EQU value: " + parts[1]);
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

            // Handle DB directive in pass 2
            if (line.toUpperCase().startsWith("DB")) {
                String[] parts = line.split("\\s+", 2);
                if (parts.length == 2) {
                    String[] values = parts[1].split(",");
                    for (String v : values) {
                        try {
                            int val = parseNumber(v.trim());
                            byteList.add(val & 0xFF);
                            if (firstInstruction) { startAddress = currentAddress; firstInstruction = false; }
                            lineToAddress.put(lineNum + 1, currentAddress);
                            currentAddress++;
                        } catch (Exception e) {
                            errors.add("Line " + (lineNum + 1) + ": Invalid DB value: " + v.trim());
                        }
                    }
                }
                continue;
            }

            // Handle DW directive in pass 2
            if (line.toUpperCase().startsWith("DW")) {
                String[] parts = line.split("\\s+", 2);
                if (parts.length == 2) {
                    String[] values = parts[1].split(",");
                    for (String v : values) {
                        try {
                            int val = parseNumber(v.trim());
                            byteList.add(val & 0xFF);
                            byteList.add((val >> 8) & 0xFF);
                            if (firstInstruction) { startAddress = currentAddress; firstInstruction = false; }
                            lineToAddress.put(lineNum + 1, currentAddress);
                            currentAddress += 2;
                        } catch (Exception e) {
                            errors.add("Line " + (lineNum + 1) + ": Invalid DW value: " + v.trim());
                        }
                    }
                }
                continue;
            }

            // Skip EQU directives in pass 2
            if (line.toUpperCase().contains(" EQU ") || line.toUpperCase().contains("\tEQU\t")) {
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
     *
     * 1-byte instructions: NOP, HLT, MOV, ADD, ADC, SUB, SBB, INR, DCR,
     *                      ANA, ANI(2), ORA, XRA, CMP, RLC, RRC, RAL, RAR,
     *                      CMA, CMC, STC, XCHG, XTHL, SPHL, PCHL, DI, EI,
     *                      RIM, SIM, RET, RC, RNC, RZ, RNZ, RM, RP, RPE, RPO,
     *                      RST, INX, DCX, DAD, DAA, LDAX, STAX, PUSH, POP
     * 2-byte instructions: MVI, ADI, ACI, SUI, SBI, ANI, ORI, XRI, CPI, IN, OUT
     * 3-byte instructions: LDA, STA, LHLD, SHLD, LXI,
     *                      JMP, JC, JNC, JZ, JNZ, JM, JP, JPE, JPO,
     *                      CALL, CC, CNC, CZ, CNZ, CM, CP, CPE, CPO
     */
    private int getInstructionSize(String mnemonic, String operands, int lineNum, List<String> errors) {
        return switch (mnemonic) {
            // 1-byte
            case "NOP", "HLT" -> 1;
            case "MOV", "ADD", "ADC", "SUB", "SBB" -> 1;
            case "INR", "DCR" -> 1;
            case "ANA", "ORA", "XRA", "CMP" -> 1;
            case "RLC", "RRC", "RAL", "RAR" -> 1;
            case "CMA", "CMC", "STC" -> 1;
            case "XCHG", "XTHL", "SPHL", "PCHL" -> 1;
            case "DI", "EI", "RIM", "SIM" -> 1;
            case "RET", "RC", "RNC", "RZ", "RNZ", "RM", "RP", "RPE", "RPO" -> 1;
            case "RST" -> 1;
            case "INX", "DCX", "DAD" -> 1;
            case "DAA" -> 1;
            case "LDAX", "STAX" -> 1;
            case "PUSH", "POP" -> 1;
            // 2-byte
            case "MVI", "ADI", "ACI", "SUI", "SBI" -> 2;
            case "ANI", "ORI", "XRI", "CPI" -> 2;
            case "IN", "OUT" -> 2;
            // 3-byte
            case "LDA", "STA", "LHLD", "SHLD" -> 3;
            case "LXI" -> 3;
            case "JMP", "JC", "JNC", "JZ", "JNZ", "JM", "JP", "JPE", "JPO" -> 3;
            case "CALL", "CC", "CNC", "CZ", "CNZ", "CM", "CP", "CPE", "CPO" -> 3;
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
     *
     * HOW RST WORKS:
     *   RST n  →  opcode = 0xC7 + (n * 8),  n = 0..7
     *   RST 0 = 0xC7, RST 1 = 0xCF, RST 2 = 0xD7 ... RST 7 = 0xFF
     */
    private List<Integer> encodeInstruction(String mnemonic, String operands,
                                             Map<String, Integer> labels, int lineNum) {
        List<Integer> bytes = new ArrayList<>();

        switch (mnemonic) {

            // ==================
            // NOP - No Operation
            // ==================
            case "NOP" -> bytes.add(0x00);

            // ==================
            // HLT - Halt
            // ==================
            case "HLT" -> bytes.add(0x76);

            // ==================
            // MOV - Move Register to Register (or Memory via M)
            // MOV dst, src  →  0x40 + (dst * 8) + src
            // ==================
            case "MOV" -> {
                String[] ops = splitOperands(operands, 2, "MOV", lineNum);
                String dst = ops[0].toUpperCase();
                String src = ops[1].toUpperCase();
                int dstCode = getRegCode(dst, lineNum);
                int srcCode = getRegCode(src, lineNum);
                // 0x76 is HLT, not MOV M,M — handled above
                if (dstCode == 6 && srcCode == 6)
                    throw new RuntimeException("MOV M,M is not a valid instruction (opcode 0x76 = HLT)");
                bytes.add(0x40 + (dstCode * 8) + srcCode);
            }

            // ==================
            // MVI - Move Immediate into register (or memory M)
            // MVI reg, imm8
            // Opcodes: B=0x06, C=0x0E, D=0x16, E=0x1E, H=0x26, L=0x2E, M=0x36, A=0x3E
            // ==================
            case "MVI" -> {
                String[] ops = splitOperands(operands, 2, "MVI", lineNum);
                String reg = ops[0].toUpperCase();
                int imm = parseNumber(ops[1].trim());
                int[] mviOpcodes = {0x06, 0x0E, 0x16, 0x1E, 0x26, 0x2E, 0x36, 0x3E};
                bytes.add(mviOpcodes[getRegCode(reg, lineNum)]);
                bytes.add(imm & 0xFF);
            }

            // ==================
            // LXI - Load Register Pair Immediate (16-bit load)
            // LXI rp, imm16   rp = B(BC), D(DE), H(HL), SP
            // Opcodes: B=0x01, D=0x11, H=0x21, SP=0x31
            // ==================
            case "LXI" -> {
                String[] ops = splitOperands(operands, 2, "LXI", lineNum);
                String rp = ops[0].toUpperCase().trim();
                int imm16 = resolveAddress(ops[1].trim(), labels, lineNum);
                int opcode = switch (rp) {
                    case "B"  -> 0x01;
                    case "D"  -> 0x11;
                    case "H"  -> 0x21;
                    case "SP" -> 0x31;
                    default -> throw new RuntimeException("LXI: invalid register pair: " + rp);
                };
                bytes.add(opcode);
                bytes.add(imm16 & 0xFF);        // low byte
                bytes.add((imm16 >> 8) & 0xFF); // high byte
            }

            // ==================
            // LDA - Load Accumulator Direct
            // A = memory[address]
            // ==================
            case "LDA" -> {
                int addr = resolveAddress(operands.trim(), labels, lineNum);
                bytes.add(0x3A);
                bytes.add(addr & 0xFF);
                bytes.add((addr >> 8) & 0xFF);
            }

            // ==================
            // STA - Store Accumulator Direct
            // memory[address] = A
            // ==================
            case "STA" -> {
                int addr = resolveAddress(operands.trim(), labels, lineNum);
                bytes.add(0x32);
                bytes.add(addr & 0xFF);
                bytes.add((addr >> 8) & 0xFF);
            }

            // ==================
            // LHLD - Load HL Direct
            // L = memory[address], H = memory[address+1]
            // ==================
            case "LHLD" -> {
                int addr = resolveAddress(operands.trim(), labels, lineNum);
                bytes.add(0x2A);
                bytes.add(addr & 0xFF);
                bytes.add((addr >> 8) & 0xFF);
            }

            // ==================
            // SHLD - Store HL Direct
            // memory[address] = L, memory[address+1] = H
            // ==================
            case "SHLD" -> {
                int addr = resolveAddress(operands.trim(), labels, lineNum);
                bytes.add(0x22);
                bytes.add(addr & 0xFF);
                bytes.add((addr >> 8) & 0xFF);
            }

            // ==================
            // LDAX - Load Accumulator Indirect
            // A = memory[rp]   rp = B(BC) or D(DE)
            // Opcodes: B=0x0A, D=0x1A
            // ==================
            case "LDAX" -> {
                String rp = operands.trim().toUpperCase();
                int opcode = switch (rp) {
                    case "B" -> 0x0A;
                    case "D" -> 0x1A;
                    default -> throw new RuntimeException("LDAX: only B or D pair allowed: " + rp);
                };
                bytes.add(opcode);
            }

            // ==================
            // STAX - Store Accumulator Indirect
            // memory[rp] = A   rp = B(BC) or D(DE)
            // Opcodes: B=0x02, D=0x12
            // ==================
            case "STAX" -> {
                String rp = operands.trim().toUpperCase();
                int opcode = switch (rp) {
                    case "B" -> 0x02;
                    case "D" -> 0x12;
                    default -> throw new RuntimeException("STAX: only B or D pair allowed: " + rp);
                };
                bytes.add(opcode);
            }

            // ==================
            // XCHG - Exchange HL with DE
            // H↔D, L↔E
            // ==================
            case "XCHG" -> bytes.add(0xEB);

            // ==================
            // ADD - Add register (or M) to Accumulator
            // A = A + reg;  opcode = 0x80 + reg_code
            // ==================
            case "ADD" -> {
                String reg = operands.trim().toUpperCase();
                bytes.add(0x80 + getRegCode(reg, lineNum));
            }

            // ==================
            // ADC - Add register with Carry to Accumulator
            // A = A + reg + CY;  opcode = 0x88 + reg_code
            // ==================
            case "ADC" -> {
                String reg = operands.trim().toUpperCase();
                bytes.add(0x88 + getRegCode(reg, lineNum));
            }

            // ==================
            // ADI - Add Immediate to Accumulator
            // A = A + imm8
            // ==================
            case "ADI" -> {
                int imm = parseNumber(operands.trim());
                bytes.add(0xC6);
                bytes.add(imm & 0xFF);
            }

            // ==================
            // ACI - Add Immediate with Carry
            // A = A + imm8 + CY
            // ==================
            case "ACI" -> {
                int imm = parseNumber(operands.trim());
                bytes.add(0xCE);
                bytes.add(imm & 0xFF);
            }

            // ==================
            // SUB - Subtract register from Accumulator
            // A = A - reg;  opcode = 0x90 + reg_code
            // ==================
            case "SUB" -> {
                String reg = operands.trim().toUpperCase();
                bytes.add(0x90 + getRegCode(reg, lineNum));
            }

            // ==================
            // SBB - Subtract register with Borrow
            // A = A - reg - CY;  opcode = 0x98 + reg_code
            // ==================
            case "SBB" -> {
                String reg = operands.trim().toUpperCase();
                bytes.add(0x98 + getRegCode(reg, lineNum));
            }

            // ==================
            // SUI - Subtract Immediate from Accumulator
            // A = A - imm8
            // ==================
            case "SUI" -> {
                int imm = parseNumber(operands.trim());
                bytes.add(0xD6);
                bytes.add(imm & 0xFF);
            }

            // ==================
            // SBI - Subtract Immediate with Borrow
            // A = A - imm8 - CY
            // ==================
            case "SBI" -> {
                int imm = parseNumber(operands.trim());
                bytes.add(0xDE);
                bytes.add(imm & 0xFF);
            }

            // ==================
            // INR - Increment Register (or M)
            // reg = reg + 1;  does NOT affect CY
            // Opcodes: B=0x04, C=0x0C, D=0x14, E=0x1C, H=0x24, L=0x2C, M=0x34, A=0x3C
            // ==================
            case "INR" -> {
                String reg = operands.trim().toUpperCase();
                int[] inrOpcodes = {0x04, 0x0C, 0x14, 0x1C, 0x24, 0x2C, 0x34, 0x3C};
                bytes.add(inrOpcodes[getRegCode(reg, lineNum)]);
            }

            // ==================
            // DCR - Decrement Register (or M)
            // reg = reg - 1;  does NOT affect CY
            // Opcodes: B=0x05, C=0x0D, D=0x15, E=0x1D, H=0x25, L=0x2D, M=0x35, A=0x3D
            // ==================
            case "DCR" -> {
                String reg = operands.trim().toUpperCase();
                int[] dcrOpcodes = {0x05, 0x0D, 0x15, 0x1D, 0x25, 0x2D, 0x35, 0x3D};
                bytes.add(dcrOpcodes[getRegCode(reg, lineNum)]);
            }

            // ==================
            // INX - Increment Register Pair
            // rp = rp + 1  (16-bit increment)
            // Opcodes: B=0x03, D=0x13, H=0x23, SP=0x33
            // ==================
            case "INX" -> {
                String rp = operands.trim().toUpperCase();
                int opcode = switch (rp) {
                    case "B"  -> 0x03;
                    case "D"  -> 0x13;
                    case "H"  -> 0x23;
                    case "SP" -> 0x33;
                    default -> throw new RuntimeException("INX: invalid register pair: " + rp);
                };
                bytes.add(opcode);
            }

            // ==================
            // DCX - Decrement Register Pair
            // rp = rp - 1  (16-bit decrement)
            // Opcodes: B=0x0B, D=0x1B, H=0x2B, SP=0x3B
            // ==================
            case "DCX" -> {
                String rp = operands.trim().toUpperCase();
                int opcode = switch (rp) {
                    case "B"  -> 0x0B;
                    case "D"  -> 0x1B;
                    case "H"  -> 0x2B;
                    case "SP" -> 0x3B;
                    default -> throw new RuntimeException("DCX: invalid register pair: " + rp);
                };
                bytes.add(opcode);
            }

            // ==================
            // DAD - Double Add (add register pair to HL)
            // HL = HL + rp;  only CY is affected
            // Opcodes: B=0x09, D=0x19, H=0x29, SP=0x39
            // ==================
            case "DAD" -> {
                String rp = operands.trim().toUpperCase();
                int opcode = switch (rp) {
                    case "B"  -> 0x09;
                    case "D"  -> 0x19;
                    case "H"  -> 0x29;
                    case "SP" -> 0x39;
                    default -> throw new RuntimeException("DAD: invalid register pair: " + rp);
                };
                bytes.add(opcode);
            }

            // ==================
            // DAA - Decimal Adjust Accumulator
            // Converts A to BCD after ADD/ADC; adjusts for BCD carry
            // ==================
            case "DAA" -> bytes.add(0x27);

            // ==================
            // ANA - AND Accumulator with register (or M)
            // A = A AND reg;  opcode = 0xA0 + reg_code
            // ==================
            case "ANA" -> {
                String reg = operands.trim().toUpperCase();
                bytes.add(0xA0 + getRegCode(reg, lineNum));
            }

            // ==================
            // ANI - AND Immediate with Accumulator
            // A = A AND imm8
            // ==================
            case "ANI" -> {
                int imm = parseNumber(operands.trim());
                bytes.add(0xE6);
                bytes.add(imm & 0xFF);
            }

            // ==================
            // ORA - OR Accumulator with register (or M)
            // A = A OR reg;  opcode = 0xB0 + reg_code
            // ==================
            case "ORA" -> {
                String reg = operands.trim().toUpperCase();
                bytes.add(0xB0 + getRegCode(reg, lineNum));
            }

            // ==================
            // ORI - OR Immediate with Accumulator
            // A = A OR imm8
            // ==================
            case "ORI" -> {
                int imm = parseNumber(operands.trim());
                bytes.add(0xF6);
                bytes.add(imm & 0xFF);
            }

            // ==================
            // XRA - XOR Accumulator with register (or M)
            // A = A XOR reg;  opcode = 0xA8 + reg_code
            // ==================
            case "XRA" -> {
                String reg = operands.trim().toUpperCase();
                bytes.add(0xA8 + getRegCode(reg, lineNum));
            }

            // ==================
            // XRI - XOR Immediate with Accumulator
            // A = A XOR imm8
            // ==================
            case "XRI" -> {
                int imm = parseNumber(operands.trim());
                bytes.add(0xEE);
                bytes.add(imm & 0xFF);
            }

            // ==================
            // CMP - Compare Accumulator with register (or M)
            // Flags set as if A - reg; A unchanged;  opcode = 0xB8 + reg_code
            // ==================
            case "CMP" -> {
                String reg = operands.trim().toUpperCase();
                bytes.add(0xB8 + getRegCode(reg, lineNum));
            }

            // ==================
            // CPI - Compare Immediate with Accumulator
            // Flags set as if A - imm8; A unchanged
            // ==================
            case "CPI" -> {
                int imm = parseNumber(operands.trim());
                bytes.add(0xFE);
                bytes.add(imm & 0xFF);
            }

            // ==================
            // RLC - Rotate Accumulator Left through Carry
            // bit7 → CY, A rotated left; old CY not used
            // ==================
            case "RLC" -> bytes.add(0x07);

            // ==================
            // RRC - Rotate Accumulator Right (Circular)
            // bit0 → CY, A rotated right; old CY not used
            // ==================
            case "RRC" -> bytes.add(0x0F);

            // ==================
            // RAL - Rotate Accumulator Left Through Carry
            // bit7 → CY, CY → bit0
            // ==================
            case "RAL" -> bytes.add(0x17);

            // ==================
            // RAR - Rotate Accumulator Right Through Carry
            // bit0 → CY, CY → bit7
            // ==================
            case "RAR" -> bytes.add(0x1F);

            // ==================
            // CMA - Complement Accumulator
            // A = ~A (bitwise NOT)
            // ==================
            case "CMA" -> bytes.add(0x2F);

            // ==================
            // CMC - Complement Carry Flag
            // CY = !CY
            // ==================
            case "CMC" -> bytes.add(0x3F);

            // ==================
            // STC - Set Carry Flag
            // CY = 1
            // ==================
            case "STC" -> bytes.add(0x37);

            // ==================
            // PUSH - Push Register Pair onto Stack
            // SP--, memory[SP]=high; SP--, memory[SP]=low
            // Opcodes: B=0xC5, D=0xD5, H=0xE5, PSW=0xF5
            // PSW = A + Flags register
            // ==================
            case "PUSH" -> {
                String rp = operands.trim().toUpperCase();
                int opcode = switch (rp) {
                    case "B"   -> 0xC5;
                    case "D"   -> 0xD5;
                    case "H"   -> 0xE5;
                    case "PSW" -> 0xF5;
                    default -> throw new RuntimeException("PUSH: invalid register pair: " + rp);
                };
                bytes.add(opcode);
            }

            // ==================
            // POP - Pop Register Pair from Stack
            // low=memory[SP], SP++; high=memory[SP], SP++
            // Opcodes: B=0xC1, D=0xD1, H=0xE1, PSW=0xF1
            // ==================
            case "POP" -> {
                String rp = operands.trim().toUpperCase();
                int opcode = switch (rp) {
                    case "B"   -> 0xC1;
                    case "D"   -> 0xD1;
                    case "H"   -> 0xE1;
                    case "PSW" -> 0xF1;
                    default -> throw new RuntimeException("POP: invalid register pair: " + rp);
                };
                bytes.add(opcode);
            }

            // ==================
            // XTHL - Exchange HL with top of Stack
            // H↔memory[SP+1], L↔memory[SP]
            // ==================
            case "XTHL" -> bytes.add(0xE3);

            // ==================
            // SPHL - Move HL to Stack Pointer
            // SP = HL
            // ==================
            case "SPHL" -> bytes.add(0xF9);

            // ==================
            // PCHL - Move HL to Program Counter (indirect jump)
            // PC = HL
            // ==================
            case "PCHL" -> bytes.add(0xE9);

            // ==================
            // IN - Input from port
            // A = port[imm8]
            // ==================
            case "IN" -> {
                int port = parseNumber(operands.trim());
                bytes.add(0xDB);
                bytes.add(port & 0xFF);
            }

            // ==================
            // OUT - Output to port
            // port[imm8] = A
            // ==================
            case "OUT" -> {
                int port = parseNumber(operands.trim());
                bytes.add(0xD3);
                bytes.add(port & 0xFF);
            }

            // ==================
            // DI - Disable Interrupts
            // ==================
            case "DI" -> bytes.add(0xF3);

            // ==================
            // EI - Enable Interrupts
            // ==================
            case "EI" -> bytes.add(0xFB);

            // ==================
            // RIM - Read Interrupt Mask
            // Reads interrupt mask/status into A
            // ==================
            case "RIM" -> bytes.add(0x20);

            // ==================
            // SIM - Set Interrupt Mask
            // Sets interrupt mask from A
            // ==================
            case "SIM" -> bytes.add(0x30);

            // ==================
            // JMP - Unconditional Jump
            // ==================
            case "JMP" -> {
                int addr = resolveAddress(operands.trim(), labels, lineNum);
                bytes.add(0xC3);
                bytes.add(addr & 0xFF);
                bytes.add((addr >> 8) & 0xFF);
            }

            // ==================
            // Conditional Jumps - all 3-byte (opcode + 16-bit address)
            // JC   = Jump if Carry      (CY=1)  0xDA
            // JNC  = Jump if No Carry   (CY=0)  0xD2
            // JZ   = Jump if Zero       (Z=1)   0xCA
            // JNZ  = Jump if Not Zero   (Z=0)   0xC2
            // JM   = Jump if Minus      (S=1)   0xFA
            // JP   = Jump if Plus       (S=0)   0xF2
            // JPE  = Jump if Parity Even(P=1)   0xEA
            // JPO  = Jump if Parity Odd (P=0)   0xE2
            // ==================
            case "JC" -> {
                int addr = resolveAddress(operands.trim(), labels, lineNum);
                bytes.add(0xDA); bytes.add(addr & 0xFF); bytes.add((addr >> 8) & 0xFF);
            }
            case "JNC" -> {
                int addr = resolveAddress(operands.trim(), labels, lineNum);
                bytes.add(0xD2); bytes.add(addr & 0xFF); bytes.add((addr >> 8) & 0xFF);
            }
            case "JZ" -> {
                int addr = resolveAddress(operands.trim(), labels, lineNum);
                bytes.add(0xCA); bytes.add(addr & 0xFF); bytes.add((addr >> 8) & 0xFF);
            }
            case "JNZ" -> {
                int addr = resolveAddress(operands.trim(), labels, lineNum);
                bytes.add(0xC2); bytes.add(addr & 0xFF); bytes.add((addr >> 8) & 0xFF);
            }
            case "JM" -> {
                int addr = resolveAddress(operands.trim(), labels, lineNum);
                bytes.add(0xFA); bytes.add(addr & 0xFF); bytes.add((addr >> 8) & 0xFF);
            }
            case "JP" -> {
                int addr = resolveAddress(operands.trim(), labels, lineNum);
                bytes.add(0xF2); bytes.add(addr & 0xFF); bytes.add((addr >> 8) & 0xFF);
            }
            case "JPE" -> {
                int addr = resolveAddress(operands.trim(), labels, lineNum);
                bytes.add(0xEA); bytes.add(addr & 0xFF); bytes.add((addr >> 8) & 0xFF);
            }
            case "JPO" -> {
                int addr = resolveAddress(operands.trim(), labels, lineNum);
                bytes.add(0xE2); bytes.add(addr & 0xFF); bytes.add((addr >> 8) & 0xFF);
            }

            // ==================
            // CALL - Unconditional Subroutine Call
            // Pushes PC+3 onto stack, then jumps to address
            // ==================
            case "CALL" -> {
                int addr = resolveAddress(operands.trim(), labels, lineNum);
                bytes.add(0xCD);
                bytes.add(addr & 0xFF);
                bytes.add((addr >> 8) & 0xFF);
            }

            // ==================
            // Conditional CALLs - all 3-byte
            // CC  = Call if Carry       (CY=1)  0xDC
            // CNC = Call if No Carry    (CY=0)  0xD4
            // CZ  = Call if Zero        (Z=1)   0xCC
            // CNZ = Call if Not Zero    (Z=0)   0xC4
            // CM  = Call if Minus       (S=1)   0xFC
            // CP  = Call if Plus        (S=0)   0xF4
            // CPE = Call if Parity Even (P=1)   0xEC
            // CPO = Call if Parity Odd  (P=0)   0xE4
            // ==================
            case "CC" -> {
                int addr = resolveAddress(operands.trim(), labels, lineNum);
                bytes.add(0xDC); bytes.add(addr & 0xFF); bytes.add((addr >> 8) & 0xFF);
            }
            case "CNC" -> {
                int addr = resolveAddress(operands.trim(), labels, lineNum);
                bytes.add(0xD4); bytes.add(addr & 0xFF); bytes.add((addr >> 8) & 0xFF);
            }
            case "CZ" -> {
                int addr = resolveAddress(operands.trim(), labels, lineNum);
                bytes.add(0xCC); bytes.add(addr & 0xFF); bytes.add((addr >> 8) & 0xFF);
            }
            case "CNZ" -> {
                int addr = resolveAddress(operands.trim(), labels, lineNum);
                bytes.add(0xC4); bytes.add(addr & 0xFF); bytes.add((addr >> 8) & 0xFF);
            }
            case "CM" -> {
                int addr = resolveAddress(operands.trim(), labels, lineNum);
                bytes.add(0xFC); bytes.add(addr & 0xFF); bytes.add((addr >> 8) & 0xFF);
            }
            case "CP" -> {
                int addr = resolveAddress(operands.trim(), labels, lineNum);
                bytes.add(0xF4); bytes.add(addr & 0xFF); bytes.add((addr >> 8) & 0xFF);
            }
            case "CPE" -> {
                int addr = resolveAddress(operands.trim(), labels, lineNum);
                bytes.add(0xEC); bytes.add(addr & 0xFF); bytes.add((addr >> 8) & 0xFF);
            }
            case "CPO" -> {
                int addr = resolveAddress(operands.trim(), labels, lineNum);
                bytes.add(0xE4); bytes.add(addr & 0xFF); bytes.add((addr >> 8) & 0xFF);
            }

            // ==================
            // RET - Unconditional Return from Subroutine
            // PC = memory[SP], SP += 2
            // ==================
            case "RET" -> bytes.add(0xC9);

            // ==================
            // Conditional RETURNs - all 1-byte
            // RC  = Return if Carry       (CY=1)  0xD8
            // RNC = Return if No Carry    (CY=0)  0xD0
            // RZ  = Return if Zero        (Z=1)   0xC8
            // RNZ = Return if Not Zero    (Z=0)   0xC0
            // RM  = Return if Minus       (S=1)   0xF8
            // RP  = Return if Plus        (S=0)   0xF0
            // RPE = Return if Parity Even (P=1)   0xE8
            // RPO = Return if Parity Odd  (P=0)   0xE0
            // ==================
            case "RC"  -> bytes.add(0xD8);
            case "RNC" -> bytes.add(0xD0);
            case "RZ"  -> bytes.add(0xC8);
            case "RNZ" -> bytes.add(0xC0);
            case "RM"  -> bytes.add(0xF8);
            case "RP"  -> bytes.add(0xF0);
            case "RPE" -> bytes.add(0xE8);
            case "RPO" -> bytes.add(0xE0);

            // ==================
            // RST - Restart (software interrupt)
            // RST n  →  opcode = 0xC7 + (n * 8),  n = 0..7
            // Pushes PC, then jumps to n*8 (0x00, 0x08, 0x10...0x38)
            // RST 0=0xC7, RST 1=0xCF, RST 2=0xD7, RST 3=0xDF
            // RST 4=0xE7, RST 5=0xEF, RST 6=0xF7, RST 7=0xFF
            // ==================
            case "RST" -> {
                int n = parseNumber(operands.trim());
                if (n < 0 || n > 7)
                    throw new RuntimeException("RST: n must be 0-7, got: " + n);
                bytes.add(0xC7 + (n * 8));
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

    /** Get register code (0-7) for MOV/ADD encoding, including M=6 */
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