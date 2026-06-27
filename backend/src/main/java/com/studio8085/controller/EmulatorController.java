package com.studio8085.controller;

import com.studio8085.service.EmulatorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API Controller
 *
 * These are the HTTP endpoints that the React frontend calls.
 * All endpoints return JSON.
 *
 * Base URL: http://localhost:9090/api
 *
 * Endpoints:
 *   POST /api/assemble         - Assemble source code
 *   POST /api/step             - Execute one instruction
 *   POST /api/run              - Run until HLT
 *   POST /api/reset            - Reset CPU + memory
 *   GET  /api/state            - Get current CPU state
 *   GET  /api/memory           - Get memory contents
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000","https://8085-studio.vercel.app"})
public class EmulatorController {

    private final EmulatorService emulatorService;

    public EmulatorController(EmulatorService emulatorService) {
        this.emulatorService = emulatorService;
    }

    /**
     * POST /api/assemble
     * Body: { "source": "MVI A, 10H\nHLT" }
     * Returns: assembly result with errors or success
     */
    @PostMapping("/assemble")
    public ResponseEntity<Map<String, Object>> assemble(@RequestBody Map<String, String> body) {
        String source = body.getOrDefault("source", "");
        return ResponseEntity.ok(emulatorService.assemble(source));
    }

    /**
     * POST /api/step
     * Execute one instruction. Returns updated CPU state.
     */
    @PostMapping("/step")
    public ResponseEntity<Map<String, Object>> step() {
        return ResponseEntity.ok(emulatorService.step());
    }

    /**
     * POST /api/run
     * Run until HLT. Returns final CPU state.
     */
    @PostMapping("/run")
    public ResponseEntity<Map<String, Object>> run() {
        return ResponseEntity.ok(emulatorService.run());
    }

    /**
     * POST /api/reset
     * Reset CPU and memory to initial state.
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> reset() {
        return ResponseEntity.ok(emulatorService.reset());
    }

    /**
     * GET /api/state
     * Get current CPU state (registers + flags).
     */
    @GetMapping("/state")
    public ResponseEntity<Map<String, Object>> getState() {
        return ResponseEntity.ok(emulatorService.getCpuState());
    }

    /**
     * GET /api/memory?start=0&length=64
     * Get memory contents for the viewer.
     */
    @GetMapping("/memory")
    public ResponseEntity<Map<String, Object>> getMemory(
            @RequestParam(defaultValue = "0") int start,
            @RequestParam(defaultValue = "64") int length) {
        return ResponseEntity.ok(emulatorService.getMemory(start, length));
    }
}
