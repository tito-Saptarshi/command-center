package com.gke.command.center.service;

import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

@Service
public class GoogleAuthService {

    @Value("${gcp.credentials.path}")
    private String credentialsPath;

    public String getAccessToken() throws IOException {

        GoogleCredentials credentials = GoogleCredentials
                .fromStream(new FileInputStream(credentialsPath))
                .createScoped(List.of("https://www.googleapis.com/auth/cloud-platform"));

        credentials.refreshIfExpired();

        return credentials.getAccessToken().getTokenValue();
    }
}
