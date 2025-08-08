import kotlinx.coroutines.*
import kotlin.test.Test


class UnderstandingCoroutineTests {

    /**
     * 루틴에서 서브루틴이 호출되면 서브루틴이 완료될 때까지 루틴이 아무런 작업을 할 수 없는 것과 다르게
     * 코루틴은 함께 실행되는 루틴으로 서로 간에 스레드 사용을 양보하며 함께 실행된다.
     */
    @Test
    fun 서브루틴과_코루틴의_차이() = runBlocking<Unit> {
        launch {
            while (true) {
                println("자식 코루틴에서 작업 실행 중")
                yield()
            }
        }

        while (true) {
            println("부모 코루틴에서 작업 실행 중")
            yield()
        }
    }

    @Test
    fun delay_일시_중단_함수를_통해_알아보는_스레드_양보() = runBlocking<Unit> {
        val startTime = System.currentTimeMillis()
        repeat(10) { repeatTime ->
            launch {
                delay(1000L)
                println("[${getElapsedTime(startTime)}] 코루틴${repeatTime} 실행 완료")
            }
        }
    }

    /**
     * runBlocking 코루틴과 launch 코루틴은 단일 스레드인 메인 스레드에서 실행되기 때문에
     * 하나의 코루틴이 스레드를 양보하지 않으면 다른 코루틴이 실행되지 못한다.
     */
    @Test
    fun join과_await의_동작_방식_자세히_알아보기() = runBlocking<Unit> {
        val job = launch {
            println("1. launch 코루틴 작업이 시작됐습니다")
            delay(1000L)
            println("2. launch 코루틴 작업이 완료됐습니다")
        }
        println("3. runBlocking 코루틴이 곧 일시 중단 되고 메인 스레드가 양보됩니다")
        job.join()
        println("4. runBlocking이 메인 스레드에 분배돼 작업이 다시 재개됩니다")
    }

    /**
     * 코루틴 라이브러리에서 제공하는 많은 함수는 delay나 join 같이 내부적으로 스레드 양보를 일으키며, 스레드 양보를 개발자가 직접 세세하게 조정할 필요가 없게 한다.
     *
     * 하지만 몇 가지 특수한 상황에서는 스레드 양보를 직접 호출해야 할 필요가 있다.
     */
    @Test
    fun yield_함수_호출해_스레드_양보하기() = runBlocking<Unit> {
        val job = launch {
            while (this.isActive) {
                println("작업 중")
                yield()
            }
        }
        delay(100L)
        job.cancel()
    }

    /**
     * 코루틴이 일시 중단 후 재개되면 CoroutineDispatcher 객체는 재개된 코루틴을 다시 스레드에 할당한다.
     *
     * 이때 CoroutineDispatcher 객체는 코루틴을 자신이 사용할 수 있는 스레드 중 하나에 할당하는데
     * 이 스레드는 코루틴이 일시 중단 전에 실행되던 스레드와 다를 수 있다.
     *
     * 일시 중단된 코루틴이 재개되면 다시 CoroutineDispatcher 객체의 작업 대기열로 이동하며,
     * CoroutineDispatcher 객체에 의해 스레드로 보내져 실행된다.
     */
    @Test
    fun 코루틴의_실행_스레드는_고정이_아니다() = runBlocking<Unit> {
        val dispatcher = newFixedThreadPoolContext(2, "MyThread")
        launch(dispatcher) {
            repeat(5) {
                println("[${Thread.currentThread().name}] 코루틴 실행이 일시 중단 됩니다")
                delay(100L)
                println("[${Thread.currentThread().name}] 코루틴 실행이 재개 됩니다")
            }
        }
    }

    /**
     * 코루틴의 실행 스레드가 바뀌는 시점은 코루틴이 재개될 때이다.
     *
     * 즉, 코루틴이 스레드 양보를 하지 않아 일시 중단될 일이 없다면 실행 스레드가 바뀌지 않는다.
     */
    @Test
    fun 스레드를_양보하지_않으면_실행_스레드가_바뀌지_않는다() = runBlocking<Unit> {
        val dispatcher = newFixedThreadPoolContext(2, "MyThread")
        launch(dispatcher) {
            repeat(5) {
                println("[${Thread.currentThread().name}] 스레드를 점유한채로 100밀리초간 대기합니다")
                Thread.sleep(100L)
                println("[${Thread.currentThread().name}] 점유한 스레드에서 마저 실행됩니다")
            }
        }
    }
}