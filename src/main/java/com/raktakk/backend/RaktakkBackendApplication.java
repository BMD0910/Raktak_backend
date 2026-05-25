package com.raktakk.backend;

import com.raktakk.backend.config.RailwayDataSourceConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(RailwayDataSourceConfig.class)
public class RaktakkBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(RaktakkBackendApplication.class, args);
    }
}
