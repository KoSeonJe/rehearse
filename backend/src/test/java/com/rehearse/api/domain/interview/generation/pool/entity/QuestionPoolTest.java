package com.rehearse.api.domain.interview.generation.pool.entity;

import com.rehearse.api.global.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuestionPoolTest {

    @Nested
    @DisplayName("create 메서드")
    class Create {

        @Test
        @DisplayName("유효한 파라미터로 생성하면 isActive=true이고 필드가 정확히 매핑된다")
        void create_validParams_returnsActivePool() {
            // given
            String cacheKey = "backend:cs:junior";
            String content = "Spring IoC 컨테이너에 대해 설명하세요.";
            String ttsContent = "Spring IoC 컨테이너에 대해 설명하세요.";
            String category = "CS";
            String modelAnswer = "IoC는 제어의 역전을 의미합니다.";
            String referenceType = "CS_FUNDAMENTAL";

            // when
            QuestionPool pool = QuestionPool.create(cacheKey, content, ttsContent, category, modelAnswer, referenceType);

            // then
            assertThat(pool.getCacheKey()).isEqualTo(cacheKey);
            assertThat(pool.getContent()).isEqualTo(content);
            assertThat(pool.getTtsContent()).isEqualTo(ttsContent);
            assertThat(pool.getCategory()).isEqualTo(category);
            assertThat(pool.getModelAnswer()).isEqualTo(modelAnswer);
            assertThat(pool.getReferenceType()).isEqualTo(referenceType);
            assertThat(pool.isActive()).isTrue();
        }

        @Test
        @DisplayName("cacheKey가 null이면 IllegalArgumentException이 발생한다")
        void create_nullCacheKey_throwsException() {
            // given
            String cacheKey = null;

            // when & then
            assertThatThrownBy(() -> QuestionPool.create(cacheKey, "content", null, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("cacheKey가 빈 문자열이면 IllegalArgumentException이 발생한다")
        void create_blankCacheKey_throwsException() {
            // given
            String cacheKey = "   ";

            // when & then
            assertThatThrownBy(() -> QuestionPool.create(cacheKey, "content", null, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("content가 null이면 IllegalArgumentException이 발생한다")
        void create_nullContent_throwsException() {
            // given
            String content = null;

            // when & then
            assertThatThrownBy(() -> QuestionPool.create("cacheKey", content, null, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("content가 빈 문자열이면 IllegalArgumentException이 발생한다")
        void create_blankContent_throwsException() {
            // given
            String content = "  ";

            // when & then
            assertThatThrownBy(() -> QuestionPool.create("cacheKey", content, null, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("nullable 필드(ttsContent, category 등)에 null을 전달하면 정상 생성된다")
        void create_nullableFields_acceptsNull() {
            // given & when
            QuestionPool pool = QuestionPool.create("backend:cs:junior", "질문 내용", null, null, null, null);

            // then
            assertThat(pool).isNotNull();
            assertThat(pool.getTtsContent()).isNull();
            assertThat(pool.getCategory()).isNull();
            assertThat(pool.getModelAnswer()).isNull();
            assertThat(pool.getReferenceType()).isNull();
        }
    }

    @Nested
    @DisplayName("deactivate 메서드")
    class Deactivate {

        @Test
        @DisplayName("활성 상태의 QuestionPool을 비활성화하면 isActive가 false가 된다")
        void deactivate_setsIsActiveFalse() {
            // given
            QuestionPool pool = TestFixtures.createQuestionPool();
            assertThat(pool.isActive()).isTrue();

            // when
            pool.deactivate();

            // then
            assertThat(pool.isActive()).isFalse();
        }
    }
}
