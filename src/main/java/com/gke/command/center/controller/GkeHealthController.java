package com.gke.command.center.controller;

import com.gke.command.center.service.GkeHealthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/gke")
public class GkeHealthController {

    private final GkeHealthService service;

    public GkeHealthController(GkeHealthService service) {
        this.service = service;
    }

    @GetMapping("/health")
    public ResponseEntity<?> getHealth() throws Exception {
        return ResponseEntity.ok(service.getFinalFrontendResponse());
    }
}
