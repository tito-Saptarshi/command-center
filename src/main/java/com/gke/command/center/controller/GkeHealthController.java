package com.gke.command.center.controller;

import com.gke.command.center.dto.DeploymentInfo;
import com.gke.command.center.response.GkeHealthResponse;
import com.gke.command.center.dto.PodInfo;
import com.gke.command.center.service.GkeHealthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/gke")
public class GkeHealthController {

    private final GkeHealthService service;

    public GkeHealthController(GkeHealthService service) {
        this.service = service;
    }

    @GetMapping("/health")
    public GkeHealthResponse getHealth() throws Exception {

        CompletableFuture<List<DeploymentInfo>> deploymentsFuture = service.getDeployments();
        CompletableFuture<List<PodInfo>> podsFuture = service.getPods();

        CompletableFuture.allOf(deploymentsFuture, podsFuture).join();

        return new GkeHealthResponse(
                deploymentsFuture.get(),
                podsFuture.get()
        );
    }
}
