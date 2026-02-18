package com.gke.command.center.response;

import com.gke.command.center.dto.DeploymentInfo;
import com.gke.command.center.dto.PodInfo;

import java.util.List;

public class GkeHealthResponse {

    private List<DeploymentInfo> deployments;
    private List<PodInfo> pods;

    public GkeHealthResponse(List<DeploymentInfo> deployments, List<PodInfo> pods) {
        this.deployments = deployments;
        this.pods = pods;
    }

    public List<DeploymentInfo> getDeployments() { return deployments; }
    public List<PodInfo> getPods() { return pods; }
}
