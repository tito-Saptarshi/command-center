package com.gke.command.center.dto;

public class PodInfo {

    private String name;
    private int restartCount;

    public PodInfo(String name, int restartCount) {
        this.name = name;
        this.restartCount = restartCount;
    }

    public String getName() { return name; }
    public int getRestartCount() { return restartCount; }
}
