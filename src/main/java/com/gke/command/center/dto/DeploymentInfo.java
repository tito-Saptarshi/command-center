package com.gke.command.center.dto;
public class DeploymentInfo {

    private String name;
    private int replicas;
    private String status;
    private String cpuLimit;
    private String memoryLimit;
    private String cpuRequest;
    private String memoryRequest;

    public DeploymentInfo(String name, int replicas, String status,
                          String cpuLimit, String memoryLimit,
                          String cpuRequest, String memoryRequest) {
        this.name = name;
        this.replicas = replicas;
        this.status = status;
        this.cpuLimit = cpuLimit;
        this.memoryLimit = memoryLimit;
        this.cpuRequest = cpuRequest;
        this.memoryRequest = memoryRequest;
    }

    public String getName() { return name; }
    public int getReplicas() { return replicas; }
    public String getStatus() { return status; }
    public String getCpuLimit() { return cpuLimit; }
    public String getMemoryLimit() { return memoryLimit; }
    public String getCpuRequest() { return cpuRequest; }
    public String getMemoryRequest() { return memoryRequest; }
}
