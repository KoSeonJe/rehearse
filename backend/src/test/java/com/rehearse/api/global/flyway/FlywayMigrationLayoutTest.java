package com.rehearse.api.global.flyway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Flyway migration resource layout")
class FlywayMigrationLayoutTest {

    private static final Path MIGRATION_ROOT = Path.of("src/main/resources/db/migration");
    private static final Pattern VERSIONED_MIGRATION = Pattern.compile("V(\\d+)__.+\\.sql");

    @Test
    @DisplayName("rollback SQL files stay outside Flyway migration locations")
    void rollbackFilesAreOutsideMigrationLocations() throws IOException {
        List<Path> rollbackFiles = Files.walk(MIGRATION_ROOT)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().contains("/rollback/"))
                .toList();

        assertThat(rollbackFiles).isEmpty();
    }

    @Test
    @DisplayName("versioned migrations under db/migration do not reuse a version")
    void versionedMigrationsDoNotReuseVersion() throws IOException {
        Map<String, List<Path>> byVersion = Files.walk(MIGRATION_ROOT)
                .filter(Files::isRegularFile)
                .filter(path -> VERSIONED_MIGRATION.matcher(path.getFileName().toString()).matches())
                .collect(Collectors.groupingBy(this::extractVersion));

        Map<String, List<Path>> duplicates = byVersion.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        assertThat(duplicates).isEmpty();
    }

    @Test
    @DisplayName("plan-13 V34 drops Lambda content columns and keeps rollback outside migration path")
    void plan13DropLambdaContentColumnsMigrationExists() throws IOException {
        Path migration = Path.of("src/main/resources/db/migration/V34__drop_lambda_content_columns.sql");
        Path rollback = Path.of("src/main/resources/db/rollback/V34__rollback.sql");

        assertThat(migration).exists();
        assertThat(rollback).exists();

        String migrationSql = Files.readString(migration);
        assertThat(migrationSql)
                .contains("DROP COLUMN verbal_comment")
                .contains("DROP COLUMN accuracy_issues")
                .contains("DROP COLUMN coaching_structure")
                .contains("DROP COLUMN coaching_improvement");

        String rollbackSql = Files.readString(rollback);
        assertThat(rollbackSql)
                .contains("ADD COLUMN verbal_comment")
                .contains("ADD COLUMN accuracy_issues")
                .contains("ADD COLUMN coaching_structure")
                .contains("ADD COLUMN coaching_improvement");
    }

    private String extractVersion(Path path) {
        Matcher matcher = VERSIONED_MIGRATION.matcher(path.getFileName().toString());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Not a Flyway versioned migration: " + path);
        }
        return matcher.group(1);
    }
}
