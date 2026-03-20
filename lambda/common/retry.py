import functools
import random
import time

import httpx


def retry_on_transient(max_retries: int = 3, base_delay: float = 1.0, max_delay: float = 10.0):
    """Retry decorator for transient HTTP errors with exponential backoff + jitter.

    Retries on: httpx.TimeoutException, httpx.ConnectError, 5xx responses.
    Fails immediately on: 4xx (client errors).
    """
    def decorator(func):
        @functools.wraps(func)
        def wrapper(*args, **kwargs):
            last_exception = None
            for attempt in range(max_retries):
                try:
                    return func(*args, **kwargs)
                except (httpx.TimeoutException, httpx.ConnectError) as e:
                    last_exception = e
                    if attempt < max_retries - 1:
                        delay = min(base_delay * (2 ** attempt) + random.uniform(0, 1), max_delay)
                        print(f"[Retry] {func.__name__} 시도 {attempt + 1}/{max_retries} 실패 ({type(e).__name__}), {delay:.1f}초 후 재시도")
                        time.sleep(delay)
                    else:
                        print(f"[Retry] {func.__name__} 모든 재시도 실패")
                except httpx.HTTPStatusError as e:
                    if e.response.status_code >= 500:
                        last_exception = e
                        if attempt < max_retries - 1:
                            delay = min(base_delay * (2 ** attempt) + random.uniform(0, 1), max_delay)
                            print(f"[Retry] {func.__name__} 시도 {attempt + 1}/{max_retries} 실패 (HTTP {e.response.status_code}), {delay:.1f}초 후 재시도")
                            time.sleep(delay)
                        else:
                            print(f"[Retry] {func.__name__} 모든 재시도 실패")
                    else:
                        raise
            raise last_exception
        return wrapper
    return decorator
