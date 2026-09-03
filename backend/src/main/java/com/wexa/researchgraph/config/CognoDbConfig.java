package com.wexa.researchgraph.config;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CognoDbConfig {

    @Value("${cognodb.uri}")
    private String uri;

    @Value("${cognodb.username}")
    private String username;

    @Value("${cognodb.password}")
    private String password;

    @Value("${cognodb.database}")
    private String database;

    @Bean(destroyMethod = "close")
    public Driver cognodbDriver() {

        Config config = Config.builder()
                .withMaxConnectionPoolSize(20)
                .withConnectionAcquisitionTimeout(10, TimeUnit.SECONDS)
                .withConnectionTimeout(30, TimeUnit.SECONDS)
                .build();

        return GraphDatabase.driver(
                uri,
                AuthTokens.basic(username, password),
                config
        );
    }

    @Bean
    public CommandLineRunner testCognoDb(Driver driver) {
        return args -> {
            try {
                driver.verifyConnectivity();

                System.out.println("====================================");
                System.out.println("COGNODB CONNECTION SUCCESSFUL");
                System.out.println("URI      : " + uri);
                System.out.println("USERNAME : " + username);
                System.out.println("DATABASE : " + database);
                System.out.println("====================================");

            } catch (Exception e) {

                System.out.println("====================================");
                System.out.println("COGNODB CONNECTION FAILED");
                System.out.println("Exception: " + e.getClass().getName());
                System.out.println("Message  : " + e.getMessage());
                System.out.println("====================================");

                throw e;
            }
        };
    }
}