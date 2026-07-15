package com.crm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class CrmApplication extends SpringBootServletInitializer {  // ← EXTEND this class

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        // Tell Tomcat where to find the main Spring Boot configuration
        return application.sources(CrmApplication.class);
    }

    public static void main(String[] args) {
        // Drop the old table before Spring Boot starts if the id column is missing
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (java.sql.Connection conn = java.sql.DriverManager.getConnection(
                    "jdbc:mysql://127.0.0.1:3306/crm_01_apr_2026?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&zeroDateTimeBehavior=convertToNull",
                    "root",
                    ""
            ); java.sql.Statement stmt = conn.createStatement()) {
                boolean needsDrop = false;
                try {
                    stmt.executeQuery("SELECT id FROM crm_integration_config LIMIT 1");
                } catch (java.sql.SQLException e) {
                    needsDrop = true;
                }
                if (needsDrop) {
                    stmt.executeUpdate("DROP TABLE IF EXISTS crm_integration_config");
                    System.out.println("Pre-startup: Dropped old crm_integration_config table to trigger recreation.");
                }
            }
        } catch (Exception e) {
            System.out.println("Pre-startup db check skipped/failed: " + e.getMessage());
        }

        SpringApplication.run(CrmApplication.class, args);
    }
}