package com.rehearse.api.domain.interview.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class TechStackTest {

    @Test
    @DisplayName("BACKEND 직군의 기본 TechStack은 JAVA_SPRING이다")
    void getDefaultForPosition_backend_returnsJavaSpring() {
        assertThat(TechStack.getDefaultForPosition(Position.BACKEND)).isEqualTo(TechStack.JAVA_SPRING);
    }

    @Test
    @DisplayName("FRONTEND 직군의 기본 TechStack은 REACT_TS이다")
    void getDefaultForPosition_frontend_returnsReactTs() {
        assertThat(TechStack.getDefaultForPosition(Position.FRONTEND)).isEqualTo(TechStack.REACT_TS);
    }

    @Test
    @DisplayName("DEVOPS 직군의 기본 TechStack은 AWS_K8S이다")
    void getDefaultForPosition_devops_returnsAwsK8s() {
        assertThat(TechStack.getDefaultForPosition(Position.DEVOPS)).isEqualTo(TechStack.AWS_K8S);
    }

    @Test
    @DisplayName("DATA_ENGINEER 직군의 기본 TechStack은 SPARK_AIRFLOW이다")
    void getDefaultForPosition_dataEngineer_returnsSparkAirflow() {
        assertThat(TechStack.getDefaultForPosition(Position.DATA_ENGINEER)).isEqualTo(TechStack.SPARK_AIRFLOW);
    }

    @Test
    @DisplayName("FULLSTACK 직군의 기본 TechStack은 REACT_SPRING이다")
    void getDefaultForPosition_fullstack_returnsReactSpring() {
        assertThat(TechStack.getDefaultForPosition(Position.FULLSTACK)).isEqualTo(TechStack.REACT_SPRING);
    }

    @Test
    @DisplayName("JAVA_SPRING은 BACKEND 직군에 허용된다")
    void isAllowedFor_javaSpringAndBackend_returnsTrue() {
        assertThat(TechStack.JAVA_SPRING.isAllowedFor(Position.BACKEND)).isTrue();
    }

    @Test
    @DisplayName("JAVA_SPRING은 FRONTEND 직군에 허용되지 않는다")
    void isAllowedFor_javaSpringAndFrontend_returnsFalse() {
        assertThat(TechStack.JAVA_SPRING.isAllowedFor(Position.FRONTEND)).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
        "REACT_TS, FRONTEND, true",
        "REACT_TS, BACKEND, false",
        "AWS_K8S, DEVOPS, true",
        "AWS_K8S, FULLSTACK, false",
        "SPARK_AIRFLOW, DATA_ENGINEER, true",
        "REACT_SPRING, FULLSTACK, true",
        "REACT_SPRING, BACKEND, false"
    })
    @DisplayName("isAllowedFor는 TechStack의 허용 직군과 일치할 때만 true를 반환한다")
    void isAllowedFor_variousCombinations(TechStack techStack, Position position, boolean expected) {
        assertThat(techStack.isAllowedFor(position)).isEqualTo(expected);
    }
}
