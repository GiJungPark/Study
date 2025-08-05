import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test

class CoroutineCancelBenchmarkTest {

    private val duration = 100L // 각 벤치마크 테스트 시간 (ms)
    private val repeatCount = 10 // 반복 횟수

    @Test
    fun 세_가지_취소_방법_벤치마크_순차_실행_및_평균_출력() = runBlocking {
        println("=== delay 방식 벤치마크 ===")
        val delayResults = runBenchmarkRepeatedly(::runDelayBenchmark)

        println("\n=== yield 방식 벤치마크 ===")
        val yieldResults = runBenchmarkRepeatedly(::runYieldBenchmark)

        println("\n=== isActive 방식 벤치마크 ===")
        val isActiveResults = runBenchmarkRepeatedly(::runIsActiveBenchmark)

        println("\n=== 최종 벤치마크 평균 결과 (duration = ${duration}ms, repeat = $repeatCount) ===")
        println("delay 방식 평균:    ${delayResults.average().toInt()}회")
        println("yield 방식 평균:    ${yieldResults.average().toInt()}회")
        println("isActive 방식 평균: ${isActiveResults.average().toInt()}회")
    }

    private suspend fun runBenchmarkRepeatedly(benchmark: suspend () -> Int): List<Int> {
        return (1..repeatCount).map { i ->
            val result = benchmark()
            println("  [${i}회차] 반복 횟수: $result")
            result
        }
    }

    private suspend fun runDelayBenchmark(): Int = coroutineScope {
        val i = AtomicInteger(0)
        val job = launch(Dispatchers.Default) {
            while (true) {
                i.incrementAndGet()
                delay(1L)
            }
        }
        delay(duration)
        job.cancelAndJoin()
        i.get()
    }

    private suspend fun runYieldBenchmark(): Int = coroutineScope {
        val i = AtomicInteger(0)
        val job = launch(Dispatchers.Default) {
            while (true) {
                i.incrementAndGet()
                yield()
            }
        }
        delay(duration)
        job.cancelAndJoin()
        i.get()
    }

    private suspend fun runIsActiveBenchmark(): Int = coroutineScope {
        val i = AtomicInteger(0)
        val job = launch(Dispatchers.Default) {
            while (isActive) {
                i.incrementAndGet()
            }
        }
        delay(duration)
        job.cancelAndJoin()
        i.get()
    }
}