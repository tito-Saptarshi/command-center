package com.gke.command.center.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gke.command.center.dto.DeploymentInfo;
import com.gke.command.center.dto.PodInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
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


    @Async
    public CompletableFuture<List<DeploymentInfo>> getDeployments() throws Exception {

        String token = authService.getAccessToken();
        String url = "https://" + endpoint + "/apis/apps/v1/namespaces/dev/deployments";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        String json = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class
        ).getBody();

        JsonNode root = mapper.readTree(json);
        List<DeploymentInfo> list = new ArrayList<>();

        for (JsonNode item : root.path("items")) {

            String name = item.path("metadata").path("name").asText("Unknown");

            int replicas = item.path("spec").path("replicas").asInt(0);

            JsonNode containers = item.path("spec")
                    .path("template")
                    .path("spec")
                    .path("containers");

            JsonNode container = (containers.isArray() && containers.size() > 0)
                    ? containers.get(0)
                    : mapper.createObjectNode();

            JsonNode limits = container.path("resources").path("limits");
            JsonNode requests = container.path("resources").path("requests");

            String cpuLimit = limits.path("cpu").asText("N/A");
            String memoryLimit = limits.path("memory").asText("N/A");

            String cpuRequest = requests.path("cpu").asText("N/A");
            String memoryRequest = requests.path("memory").asText("N/A");

            String status = "Unknown";

            for (JsonNode condition : item.path("status").path("conditions")) {
                if ("Available".equals(condition.path("type").asText())) {
                    status = condition.path("type").asText();
                    break;
                }
            }

            list.add(new DeploymentInfo(
                    name,
                    replicas,
                    status,
                    cpuLimit,
                    memoryLimit,
                    cpuRequest,
                    memoryRequest
            ));
        }

        return CompletableFuture.completedFuture(list);
    }



    @Async
    public CompletableFuture<List<PodInfo>> getPods() throws Exception {

        String token = authService.getAccessToken();
        String url = "https://" + endpoint + "/api/v1/namespaces/dev/pods";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        String json = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class
        ).getBody();

        JsonNode root = mapper.readTree(json);
        List<PodInfo> list = new ArrayList<>();

        for (JsonNode item : root.path("items")) {

            String name = item.path("metadata").path("name").asText("Unknown");

            JsonNode statuses = item.path("status").path("containerStatuses");

            int restartCount = 0;

            if (statuses.isArray() && statuses.size() > 0) {
                restartCount = statuses.get(0).path("restartCount").asInt(0);
            }

            list.add(new PodInfo(name, restartCount));
        }

        return CompletableFuture.completedFuture(list);
    }
}
