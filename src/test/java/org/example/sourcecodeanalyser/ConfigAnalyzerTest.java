
package org.example.sourcecodeanalyser;

import org.example.sourcecodeanalyser.analyzer.configuration.ConfigurationAnalyzer;
import org.example.sourcecodeanalyser.model.configuration.ConfigurationFileAnalysis;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConfigAnalyzerTest {

    @TempDir
    Path tempDir;

    private final ConfigurationAnalyzer analyzer =
            new ConfigurationAnalyzer();

    @Test
    void shouldExtractNestedYamlKeys() throws Exception {

        Path file = tempDir.resolve("application.yml");

        Files.writeString(file, """
                server:
                  port: 8080
                  servlet:
                    context-path: /api
                spring:
                  datasource:
                    url: jdbc:postgresql://localhost/test
                    username: admin
                    password: secret123
                """);

        ConfigurationFileAnalysis result = analyzer.analyze(file);

        assertEquals("YAML", result.getFileType());

        assertEquals(
                List.of(
                        "server",
                        "server.port",
                        "server.servlet",
                        "server.servlet.context-path",
                        "spring",
                        "spring.datasource",
                        "spring.datasource.url",
                        "spring.datasource.username",
                        "spring.datasource.password"
                ),
                result.getKeys()
        );

        assertFalse(result.getKeys().toString().contains("secret123"));
        assertTrue(result.getDiagnostics().isEmpty());
    }

    @Test
    void shouldExtractYamlKeysInsideLists() throws Exception {

        Path file = tempDir.resolve("services.yml");

        Files.writeString(file, """
                services:
                  - name: api
                    port: 8080
                  - name: worker
                    port: 9090
                """);

        ConfigurationFileAnalysis result = analyzer.analyze(file);

        assertEquals(
                List.of(
                        "services",
                        "services[0].name",
                        "services[0].port",
                        "services[1].name",
                        "services[1].port"
                ),
                result.getKeys()
        );
    }

    @Test
    void shouldExtractPropertiesKeys() throws Exception {

        Path file = tempDir.resolve("application.properties");

        Files.writeString(file, """
                spring.datasource.url=jdbc:postgresql://localhost/test
                spring.datasource.username=admin
                spring.datasource.password=secret123
                server.port=8080
                """);

        ConfigurationFileAnalysis result = analyzer.analyze(file);

        assertEquals("PROPERTIES", result.getFileType());

        assertEquals(
                List.of(
                        "server.port",
                        "spring.datasource.password",
                        "spring.datasource.url",
                        "spring.datasource.username"
                ),
                result.getKeys()
        );

        assertFalse(result.getKeys().toString().contains("secret123"));
    }

    @Test
    void shouldExtractEnvironmentVariableNames() throws Exception {

        Path file = tempDir.resolve(".env");

        Files.writeString(file, """
                # Database configuration
                DATABASE_URL=postgres://localhost/test
                DATABASE_PASSWORD=secret123
                REDIS_URL=redis://localhost:6379
                export APP_ENV=development
                """);

        ConfigurationFileAnalysis result = analyzer.analyze(file);

        assertEquals("ENV", result.getFileType());

        assertEquals(
                List.of(
                        "DATABASE_URL",
                        "DATABASE_PASSWORD",
                        "REDIS_URL",
                        "APP_ENV"
                ),
                result.getKeys()
        );

        assertFalse(result.getKeys().toString().contains("secret123"));
    }

    @Test
    void shouldExtractDockerArgAndEnvNames() throws Exception {

        Path file = tempDir.resolve("Dockerfile");

        Files.writeString(file, """
                FROM eclipse-temurin:21
                ARG APP_VERSION=1.0
                ARG BUILD_ENV=production
                ENV APP_ENV=production
                ENV SERVER_PORT 8080
                RUN echo "Building application"
                """);

        ConfigurationFileAnalysis result = analyzer.analyze(file);

        assertEquals("DOCKER", result.getFileType());

        assertEquals(
                List.of(
                        "APP_VERSION",
                        "BUILD_ENV",
                        "APP_ENV",
                        "SERVER_PORT"
                ),
                result.getKeys()
        );
    }

    @Test
    void shouldExtractMultipleDockerEnvDeclarations()
            throws Exception {

        Path file = tempDir.resolve("Dockerfile.production");

        Files.writeString(file, """
                FROM eclipse-temurin:21
                ENV APP_ENV=production SERVER_PORT=8080 LOG_LEVEL=INFO
                """);

        ConfigurationFileAnalysis result = analyzer.analyze(file);

        assertEquals(
                List.of("APP_ENV", "SERVER_PORT", "LOG_LEVEL"),
                result.getKeys()
        );
    }

    @Test
    void shouldIgnoreCommentsAndEmptyLinesInEnvFile()
            throws Exception {

        Path file = tempDir.resolve(".env.example");

        Files.writeString(file, """
                # Comment

                DATABASE_URL=
                # Another comment
                APP_NAME=TaskFlow
                """);

        ConfigurationFileAnalysis result = analyzer.analyze(file);

        assertEquals("ENV", result.getFileType());

        assertEquals(
                List.of("DATABASE_URL", "APP_NAME"),
                result.getKeys()
        );
    }

    @Test
    void shouldReturnEmptyKeysForEmptyYamlFile()
            throws Exception {

        Path file = tempDir.resolve("empty.yml");

        Files.writeString(file, "");

        ConfigurationFileAnalysis result = analyzer.analyze(file);

        assertEquals("YAML", result.getFileType());
        assertTrue(result.getKeys().isEmpty());
        assertTrue(result.getDiagnostics().isEmpty());
    }

    @Test
    void shouldReturnDiagnosticForMalformedYaml()
            throws Exception {

        Path file = tempDir.resolve("invalid.yml");

        Files.writeString(file, """
                application:
                  name: TaskFlow
                  invalid: [unclosed
                """);

        ConfigurationFileAnalysis result = analyzer.analyze(file);

        assertEquals("YAML", result.getFileType());
        assertTrue(result.getKeys().isEmpty());
        assertFalse(result.getDiagnostics().isEmpty());
    }

    @Test
    void shouldUseRepositoryRelativeFilePath() throws Exception {

        Path repository = tempDir.resolve("repository");
        Path configDirectory = repository.resolve("config");

        Files.createDirectories(configDirectory);

        Path file = configDirectory.resolve("application.yml");

        Files.writeString(file, """
                server:
                  port: 8080
                """);

        ConfigurationFileAnalysis result =
                analyzer.analyze(file, repository);

        assertEquals(
                "config/application.yml",
                result.getFilePath()
        );
    }
}
