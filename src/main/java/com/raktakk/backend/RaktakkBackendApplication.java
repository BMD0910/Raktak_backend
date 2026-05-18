package com.raktakk.backend;

import com.raktakk.backend.config.RailwayDatasourceBootstrap;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RaktakkBackendApplication {

    public static void main(String[] args) {
        RailwayDatasourceBootstrap.apply();
        SpringApplication.run(RaktakkBackendApplication.class, args);
    }
}
