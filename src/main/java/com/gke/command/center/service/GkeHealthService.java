package com.gke.command.center.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class GkeHealthService {

    @Value("${gke.endpoint}")
    private String endpoint;

    private final RestTemplate restTemplate;
    private final GoogleAuthService authService;
    private final ObjectMapper mapper = new ObjectMapper();

    public GkeHealthService(RestTemplate restTemplate,
                            GoogleAuthService authService) {
        this.restTemplate = restTemplate;
        this.authService = authService;
    }

    // =========================
    // ASYNC CALL - DEPLOYMENTS
    // =========================

    @Async
    public CompletableFuture<JsonNode> fetchDeployments() throws Exception {

        String token = authService.getAccessToken();
        String url = "https://" + endpoint + "/apis/apps/v1/namespaces/dev/deployments";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        String response = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class
        ).getBody();

        return CompletableFuture.completedFuture(mapper.readTree(response));
    }

    // =========================
    // ASYNC CALL - PODS
    // =========================

    @Async
    public CompletableFuture<JsonNode> fetchPods() throws Exception {

        String token = authService.getAccessToken();
        String url = "https://" + endpoint + "/api/v1/namespaces/dev/pods";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        String response = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class
        ).getBody();

        return CompletableFuture.completedFuture(mapper.readTree(response));
    }

    // =========================
    // FINAL FRONTEND RESPONSE
    // =========================

    public Map<String, Object> getFinalFrontendResponse() throws Exception {

        CompletableFuture<JsonNode> deploymentsFuture = fetchDeployments();
        CompletableFuture<JsonNode> podsFuture = fetchPods();

        CompletableFuture.allOf(deploymentsFuture, podsFuture).join();

        JsonNode deploymentRoot = deploymentsFuture.get();
        JsonNode podRoot = podsFuture.get();

        return buildFrontendResponse(deploymentRoot, podRoot);
    }

    // =========================
    // TRANSFORMATION LOGIC
    // =========================

    private Map<String, Object> buildFrontendResponse(JsonNode deploymentRoot,
                                                      JsonNode podRoot) {

        List<JsonNode> deployments = new ArrayList<>();
        deploymentRoot.path("items").forEach(deployments::add);

        List<JsonNode> pods = new ArrayList<>();
        podRoot.path("items").forEach(pods::add);

        int deploymentCount = deployments.size();
        int podCount = pods.size();

        Map<String, Object> response = new LinkedHashMap<>();

        // ================= OVERVIEW =================

        List<Map<String, Object>> overview = new ArrayList<>();

        overview.add(Map.of("title", "Containers", "value", String.valueOf(deploymentCount)));
        overview.add(Map.of("title", "Services", "value", String.valueOf(deploymentCount)));
        overview.add(Map.of("title", "PODs", "value", String.valueOf(podCount)));
        overview.add(Map.of("title", "Clusters", "value", "1"));

        overview.add(Map.of("title", "Regions", "value", 0));
        overview.add(Map.of("title", "Zones", "value", 0));
        overview.add(Map.of("title", "Node Pools", "value", 0));
        overview.add(Map.of("title", "Nodes", "value", 0));

        response.put("overview", overview);

        // ================= DEPLOYMENTS =================

        List<Map<String, Object>> deploymentList = new ArrayList<>();

        for (JsonNode item : deployments) {

            String deploymentName = item.path("metadata").path("name").asText(null);

            JsonNode container = item.path("spec")
                    .path("template")
                    .path("spec")
                    .path("containers")
                    .isArray() && item.path("spec")
                    .path("template")
                    .path("spec")
                    .path("containers").size() > 0
                    ? item.path("spec")
                    .path("template")
                    .path("spec")
                    .path("containers").get(0)
                    : mapper.createObjectNode();

            String cpuLimit = container.path("resources").path("limits").path("cpu").asText(null);
            String memLimit = container.path("resources").path("limits").path("memory").asText(null);

            String cpuRequest = container.path("resources").path("requests").path("cpu").asText(null);
            String memRequest = container.path("resources").path("requests").path("memory").asText(null);

            double cpuPercent = calculateCpuPercent(cpuRequest, cpuLimit);
            double memPercent = calculateMemoryPercent(memRequest, memLimit);

            String status = "Unknown";
            for (JsonNode condition : item.path("status").path("conditions")) {
                if ("Available".equals(condition.path("type").asText())) {
                    status = condition.path("type").asText();
                }
            }

            Map<String, Object> deploymentMap = new LinkedHashMap<>();
            deploymentMap.put("deploymentName", deploymentName);

            deploymentMap.put("resources", List.of(
                    Map.of("title", "cpu", "value", cpuPercent > 0 ? String.format("%.0f%%", cpuPercent) : "-"),
                    Map.of("title", "memory", "value", memPercent > 0 ? String.format("%.0f%%", memPercent) : "-")
            ));

            deploymentMap.put("systemInfo", List.of(
                    Map.of("title", "status", "value", status)
            ));

            deploymentList.add(deploymentMap);
        }

        response.put("deployments", deploymentList);

        // ================= PODS =================

        List<Map<String, Object>> podList = new ArrayList<>();

        for (JsonNode item : pods) {

            String podName = item.path("metadata").path("name").asText(null);

            int restartCount = item.path("status")
                    .path("containerStatuses")
                    .isArray() && item.path("status")
                    .path("containerStatuses").size() > 0
                    ? item.path("status")
                    .path("containerStatuses").get(0)
                    .path("restartCount").asInt(0)
                    : 0;

            Map<String, Object> podMap = new LinkedHashMap<>();
            podMap.put("podName", podName);
            podMap.put("restartCount", restartCount);

            podList.add(podMap);
        }

        response.put("pods", podList);

        return response;
    }

    // =========================
    // HELPER METHODS
    // =========================

    private double calculateCpuPercent(String request, String limit) {
        if (request == null || limit == null) return 0;
        double req = parseCpu(request);
        double lim = parseCpu(limit);
        return lim > 0 ? (req / lim) * 100 : 0;
    }

    private double calculateMemoryPercent(String request, String limit) {
        if (request == null || limit == null) return 0;
        double req = parseMemory(request);
        double lim = parseMemory(limit);
        return lim > 0 ? (req / lim) * 100 : 0;
    }

    private double parseCpu(String cpu) {
        if (cpu == null) return 0;
        if (cpu.endsWith("m")) return Double.parseDouble(cpu.replace("m", ""));
        return Double.parseDouble(cpu) * 1000;
    }

    private double parseMemory(String memory) {
        if (memory == null) return 0;
        if (memory.endsWith("Gi")) return Double.parseDouble(memory.replace("Gi", "")) * 1024;
        if (memory.endsWith("Mi")) return Double.parseDouble(memory.replace("Mi", ""));
        return 0;
    }
}
