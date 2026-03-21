package com.rehearse.api.domain.interview.entity;

import java.util.Arrays;

public enum TechStack {

    // Backend
    JAVA_SPRING("Java/Spring Boot", Position.BACKEND, true),
    PYTHON_DJANGO("Python/Django·FastAPI", Position.BACKEND, false),
    NODE_NESTJS("Node.js/NestJS", Position.BACKEND, false),
    GO("Go", Position.BACKEND, false),
    KOTLIN_SPRING("Kotlin/Spring Boot", Position.BACKEND, false),

    // Frontend
    REACT_TS("React/TypeScript", Position.FRONTEND, true),
    VUE_TS("Vue.js/TypeScript", Position.FRONTEND, false),
    SVELTE("Svelte/SvelteKit", Position.FRONTEND, false),
    ANGULAR("Angular", Position.FRONTEND, false),

    // DevOps
    AWS_K8S("AWS/Kubernetes", Position.DEVOPS, true),
    GCP("GCP", Position.DEVOPS, false),
    AZURE("Azure", Position.DEVOPS, false),

    // Data Engineer
    SPARK_AIRFLOW("Spark/Airflow", Position.DATA_ENGINEER, true),
    FLINK("Flink", Position.DATA_ENGINEER, false),
    DBT_SNOWFLAKE("dbt/Snowflake", Position.DATA_ENGINEER, false),

    // Fullstack
    REACT_SPRING("React + Spring Boot", Position.FULLSTACK, true),
    REACT_NODE("React + Node.js", Position.FULLSTACK, false),
    NEXTJS_FULLSTACK("Next.js Fullstack", Position.FULLSTACK, false);

    private final String displayName;
    private final Position allowedPosition;
    private final boolean isDefault;

    TechStack(String displayName, Position allowedPosition, boolean isDefault) {
        this.displayName = displayName;
        this.allowedPosition = allowedPosition;
        this.isDefault = isDefault;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Position getAllowedPosition() {
        return allowedPosition;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public static TechStack getDefaultForPosition(Position position) {
        return Arrays.stream(values())
                .filter(ts -> ts.allowedPosition == position && ts.isDefault)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("기본 TechStack 없음: " + position));
    }

    public boolean isAllowedFor(Position position) {
        return this.allowedPosition == position;
    }
}
