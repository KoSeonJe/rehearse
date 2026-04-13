package com.rehearse.api.global.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class GlobalRateLimiterFilterTest {

    @Mock
    private FilterChain filterChain;

    @Nested
    @DisplayName("비활성화 모드")
    class Disabled {

        @Test
        @DisplayName("limit=0이면 세마포어 없이 요청을 통과시킨다")
        void limit0_passthrough() throws ServletException, IOException {
            // given
            GlobalRateLimiterFilter filter = new GlobalRateLimiterFilter(0);
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            // when
            filter.doFilter(request, response, filterChain);

            // then
            then(filterChain).should().doFilter(request, response);
            assertThat(response.getStatus()).isEqualTo(200);
        }

        @Test
        @DisplayName("limit=-1이면 세마포어 없이 요청을 통과시킨다")
        void limitNegative_passthrough() throws ServletException, IOException {
            // given
            GlobalRateLimiterFilter filter = new GlobalRateLimiterFilter(-1);
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            // when
            filter.doFilter(request, response, filterChain);

            // then
            then(filterChain).should().doFilter(request, response);
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("정상 통과")
    class Permitted {

        @Test
        @DisplayName("permit이 있으면 chain.doFilter를 호출한다")
        void permitAvailable_callsChain() throws ServletException, IOException {
            // given
            GlobalRateLimiterFilter filter = new GlobalRateLimiterFilter(5);
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            // when
            filter.doFilter(request, response, filterChain);

            // then
            then(filterChain).should().doFilter(request, response);
            assertThat(response.getStatus()).isEqualTo(200);
        }

        @Test
        @DisplayName("정상 요청 후 세마포어가 반환된다 (finally 보장)")
        void afterNormalRequest_semaphoreReleased() throws ServletException, IOException {
            // given
            GlobalRateLimiterFilter filter = new GlobalRateLimiterFilter(1);
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            // when - 첫 번째 요청
            filter.doFilter(request, response, filterChain);

            // then - 두 번째 요청도 성공해야 함 (세마포어가 반환됐으므로)
            MockHttpServletResponse response2 = new MockHttpServletResponse();
            filter.doFilter(request, response2, filterChain);
            assertThat(response2.getStatus()).isEqualTo(200);
        }

        @Test
        @DisplayName("예외 발생 후에도 세마포어가 반환된다 (finally 보장)")
        void afterException_semaphoreReleasedViaFinally() throws ServletException, IOException {
            // given
            GlobalRateLimiterFilter filter = new GlobalRateLimiterFilter(1);
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            doThrow(new ServletException("테스트 예외")).when(filterChain).doFilter(request, response);

            // when - 예외가 발생해도
            try {
                filter.doFilter(request, response, filterChain);
            } catch (ServletException ignored) {
            }

            // then - 다음 요청도 처리 가능해야 함 (세마포어가 반환됐으므로)
            MockHttpServletResponse response2 = new MockHttpServletResponse();
            MockHttpServletRequest request2 = new MockHttpServletRequest();
            filter.doFilter(request2, response2, filterChain);
            assertThat(response2.getStatus()).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("한도 초과")
    class Rejected {

        @Test
        @DisplayName("permit이 없으면 503을 반환한다")
        void noPermit_returns503() throws Exception {
            // given
            GlobalRateLimiterFilter filter = new GlobalRateLimiterFilter(1);
            MockHttpServletRequest request1 = new MockHttpServletRequest();
            MockHttpServletResponse response1 = new MockHttpServletResponse();

            // limit=1인 필터에서 첫 번째 요청이 chain 안에서 블로킹 중인 상황을 시뮬레이션하기 위해
            // 세마포어를 직접 소진시킨다
            CountDownLatch blockFirst = new CountDownLatch(1);
            CountDownLatch firstAcquired = new CountDownLatch(1);

            Thread firstThread = new Thread(() -> {
                try {
                    filter.doFilter(request1, response1, (req, res) -> {
                        firstAcquired.countDown();
                        try {
                            blockFirst.await();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    });
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                }
            });
            firstThread.start();
            firstAcquired.await();

            // when - 두 번째 요청은 세마포어가 없으므로 거절된다
            MockHttpServletRequest request2 = new MockHttpServletRequest();
            MockHttpServletResponse response2 = new MockHttpServletResponse();
            filter.doFilter(request2, response2, filterChain);

            // then
            assertThat(response2.getStatus()).isEqualTo(503);
            then(filterChain).should(never()).doFilter(request2, response2);

            blockFirst.countDown();
            firstThread.join(1000);
        }

        @Test
        @DisplayName("503 응답 본문에 RATE_LIMITED JSON이 포함된다")
        void noPermit_responseBodyContainsRateLimitedJson() throws Exception {
            // given
            GlobalRateLimiterFilter filter = new GlobalRateLimiterFilter(1);
            MockHttpServletRequest request1 = new MockHttpServletRequest();
            MockHttpServletResponse response1 = new MockHttpServletResponse();

            CountDownLatch blockFirst = new CountDownLatch(1);
            CountDownLatch firstAcquired = new CountDownLatch(1);

            Thread firstThread = new Thread(() -> {
                try {
                    filter.doFilter(request1, response1, (req, res) -> {
                        firstAcquired.countDown();
                        try {
                            blockFirst.await();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    });
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                }
            });
            firstThread.start();
            firstAcquired.await();

            // when
            MockHttpServletRequest request2 = new MockHttpServletRequest();
            MockHttpServletResponse response2 = new MockHttpServletResponse();
            filter.doFilter(request2, response2, filterChain);

            // then
            assertThat(response2.getContentType()).contains("application/json");
            assertThat(response2.getContentAsString()).contains("RATE_LIMITED");
            assertThat(response2.getContentAsString()).contains("503");

            blockFirst.countDown();
            firstThread.join(1000);
        }
    }

    @Nested
    @DisplayName("동시성")
    class Concurrency {

        @Test
        @DisplayName("동시 요청이 limit을 초과하면 일부는 503을 받는다")
        void concurrentRequests_exceedLimit_some503() throws InterruptedException {
            // given
            int limit = 3;
            int totalRequests = 10;
            GlobalRateLimiterFilter filter = new GlobalRateLimiterFilter(limit);

            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch holdLatch = new CountDownLatch(1);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger rejectedCount = new AtomicInteger(0);
            CountDownLatch doneLatch = new CountDownLatch(totalRequests);

            ExecutorService executor = Executors.newFixedThreadPool(totalRequests);

            for (int i = 0; i < totalRequests; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        MockHttpServletRequest req = new MockHttpServletRequest();
                        MockHttpServletResponse res = new MockHttpServletResponse();

                        filter.doFilter(req, res, (request, response) -> {
                            try {
                                holdLatch.await();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        });

                        if (res.getStatus() == 503) {
                            rejectedCount.incrementAndGet();
                        } else {
                            successCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            // when - 모든 스레드를 동시에 시작
            startLatch.countDown();

            // limit 개수만큼 세마포어를 차지할 시간을 주고 나머지는 거절되도록 함
            Thread.sleep(100);
            holdLatch.countDown();
            doneLatch.await();

            // then
            assertThat(rejectedCount.get()).isGreaterThan(0);
            assertThat(successCount.get()).isLessThanOrEqualTo(limit);
            executor.shutdown();
        }
    }

    @Nested
    @DisplayName("필터 설정")
    class FilterConfiguration {

        @Test
        @DisplayName("필터 우선순위가 HIGHEST_PRECEDENCE이다")
        void filterOrder_isHighestPrecedence() {
            // given / when
            Order orderAnnotation = GlobalRateLimiterFilter.class.getAnnotation(Order.class);

            // then
            assertThat(orderAnnotation).isNotNull();
            assertThat(orderAnnotation.value()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
        }
    }
}
