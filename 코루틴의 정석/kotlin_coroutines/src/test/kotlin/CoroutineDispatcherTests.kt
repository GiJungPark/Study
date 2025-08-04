import kotlinx.coroutines.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoroutineDispatcherTests {

    @Test
    fun singleThreadDispatcher() {
        val singleThreadDispatcher: CoroutineDispatcher = newSingleThreadContext(name = "SingleThread")
    }

    /**
     * `newFixedThreadPoolContext`는 스레드의 개수(nThreads)와 스레드의 이름(name)을 매개변수로 받는다.
     *
     * 만들어지는 스레드들은 인자로 받은 name 값 뒤에 '-1'부터 시작해 숫자가 하나씩 증가하는 형식으로 이름을 붙인다.
     *
     * ex) MultiThread-1, MultiThread-2 ...
     */
    @Test
    fun multiThreadDispatcher() {
        val multiThreadDispatcher: CoroutineDispatcher = newFixedThreadPoolContext(nThreads = 2, name = "MultiThread")

    }

    /**
     * `newFixedThreadPoolContext` 함수로 만들어진 CoroutineDispatcher의 모습은 `newSingleThreadPoolContext` 함수를 호출해 만들어진 CoroutineDispatcher와 매우 비슷하다.
     *
     * 이는, `newSingleThreadContext`가 내부적으로 `newFixedThreadPoolContext`를 사용하도록 구현돼 있기 때문이다.
     *
     * ```kotlin
     * public fun newSingleThreadContext(name: String): CloseableCoroutineDispatcher = newFixedThreadPoolContext(1, name)
     * ```
     */
    @Test
    fun singleAndMultiDispatcher() {
        val singleThreadDispatcher = newFixedThreadPoolContext(nThreads = 1, name = "SingleThread")
    }

    @Test
    fun 단일_스레드_디스패처_사용해_코루틴_실행하기() = runBlocking<Unit> {
        val dispatcher = newSingleThreadContext(name = "SingleThread")
        launch(context = dispatcher) {
            assertEquals(Thread.currentThread().name, "SingleThread @coroutine#2")
        }
    }

    @Test
    fun 멀티_스레드_디스패처_사용해_코루틴_실행하기() = runBlocking<Unit> {
        val multiThreadDispatcher = newFixedThreadPoolContext(nThreads = 2, name = "MultiThread")
        launch(context = multiThreadDispatcher) {
            assertEquals(Thread.currentThread().name, "MultiThread-1 @coroutine#2")
        }
        launch(context = multiThreadDispatcher) {
            assertEquals(Thread.currentThread().name, "MultiThread-2 @coroutine#3")
        }
    }

    /**
     * 코루틴은 구조화를 제공해 코루틴 내부에서 새로운 코루틴을 실행할 수 있다.
     *
     * 이때, 바깥 코루틴을 부모 코루틴이라고 하고, 내부에서 생성되는 새로운 코루틴을 자식 코루틴이라고 한다.
     *
     * 자식 코루틴들은 기본적으로 부모 코루틴의 CoroutineDispatcher 객체를 사용한다.
     */
    @Test
    fun 부모_코루틴의_CoroutineDispatcher_사용해_자식_코루틴_실행하기() = runBlocking<Unit> {
        val multiThreadDispatcher = newFixedThreadPoolContext(nThreads = 2, name = "MultiThread")
        launch(multiThreadDispatcher) {
            assertEquals(Thread.currentThread().name, "MultiThread-1 @coroutine#2")
            launch {
                assertEquals(Thread.currentThread().name, "MultiThread-2 @coroutine#3")
                launch {
                    assertEquals(Thread.currentThread().name, "MultiThread-1 @coroutine#4")
                }
            }
        }
    }

    /**
     * 사용자가 CoroutineDispatcher를 만드는 것은 비효율적일 가능성이 높다.
     * - CoroutineDispatcher 객체를 만들게 되면 특정 CoroutineDispatcher 객체에서만 사용되는 스레드풀이 생성되며, 스레드풀에 속한 스레드의 수가 너무 적거나 많이 생성돼 비효율적으로 동작할 수 있다.
     * - 여러 개발자가 함께 개발할 경우 특정 용도를 위해 만들어진 CoroutineDispatcher 객체가 이미 메모리상에 있음에도 객체를 만들어 리소스를 낭비하게 될 수도 있다.
     *
     * 따라서, 코루틴 라이브러리는 미리 정의된 CoroutineDispatcher 목록을 제공한다.
     * - `Dispatchers.IO`: 네트워크 요청이나 파일 입출력 등의 입출력(I/O) 작업을 위한 CoroutineDispatcher
     * - `Dispatchers.Default`: CPU를 많이 사용하는 연산 작업을 위한 CoroutineDispatcher
     * - `Dispathcers.Main`: 메인 스레드를 사용하기 위한 CoroutineDispatcher
     */
    @Test
    fun 미리_정의된_CoroutineDispatcher() {
        val IO_Dispatcher = Dispatchers.IO
        val Default_Dispatcher = Dispatchers.Default
        val Main_Dispatcher = Dispatchers.Main
    }

    /**
     * 애플리케이션에서는 네트워크 통신을 위해 HTTP 요청을 하거나 DB 작업 같은 **입출력 작업 여러 개를 동시에 수행**한다.
     *
     * 이런 요청을 동시에 수행하기 위해 많은 스레드가 필요하며, 이를 위해 입출력 작업을 위해 미리 정의된 `Dispatchers.IO`를 제공한다.
     *
     * 1.7.2 버전을 기준으로 `Dispatchers.IO`가 최대로 사용할 수 있는 스레드의 수는 **JVM에서 사용이 가능한 프로세서의 수**와 **64** 중 큰 값으로 설정돼 있다.
     *
     * **DefaultDispatcher-worker**가 붙은 스레드는 코루틴 라이브러리에서 제공하는 **공유 스레드풀**에 속한 스레드이다.
     *
     * 따라서, `Dispatchers.IO`는 **공유 스레드풀의 스레드를 사용**할 수 있도록 구현되어 있다.
     */
    @Test
    fun Dispatchers_IO() = runBlocking<Unit> {
        launch(Dispatchers.IO) {
            assertEquals(Thread.currentThread().name, "DefaultDispatcher-worker-1 @coroutine#2")
        }
    }

    /**
     * **대용량 데이터를 처리**해야 하는 작업처럼 CPU 연산이 필요한 작업을 **CPU 바운드 작업**이라고 한다.
     *
     * `Dispatchers.Default`는 CPU 바운드 작업이 필요할 때 사용하는 CoroutineDispatcher이다.
     */
    @Test
    fun Dispatchers_Default() = runBlocking<Unit> {
        launch(Dispatchers.Default) {
            assertEquals(Thread.currentThread().name, "DefaultDispatcher-worker-1 @coroutine#2")
        }
    }

    /**
     * ### 입출력 작업과 CPU 바운드 작업
     * - 입출력 작업: 네트워크 요청, DB 조회 요청 등을 실행한 후 결과를 반환받을 때까지 스레드를 사용하지 않는다.
     * - CPU 바운드 작업: 작업을 하는 동안 스레드를 지속적으로 사용한다.
     *
     * 입출력 작업을 코루틴을 사용해 실행하면 입출력 작업 실행 후 스레드가 대기하는 동안 해당 스레드에서 다른 입출력 작업을 동시에 실행할 수 있어서 효율적이다.
     *
     * 반면 CPU 바운드 작업은 코루틴을 사용해 실행하더라도 스레드가 지속적으로 사용되기 때문에 스레드 기반 작업을 사용해 실행됐을 때와 처리 속도에 큰 차이가 없다.
     */
    @Test
    fun Dispatchers_IO_and_Default() {
    }

    /**
     * `Dispatchers.Default`를 사용해 무겁고 오래 걸리는 연산을 처리하면 특정 연산을 위해 `Dispatchers.Default`의 모든 스레드가 사용될 수 있다.
     *
     * 이를 방지하기 위해 코루틴 라이브러리는 `Dispatchers.Default`의 **일부 스레드만 사용해 특정 연산을 실행**할 수 있도록 하는 `limitedParallelism` 함수를 지원한다.
     */
    @Test
    fun limitedParallelism_사용해_Dispatchers_Default_스레드_사용_제한하기() = runBlocking<Unit> {
        launch(Dispatchers.Default.limitedParallelism(2)) {
            repeat(10) {
                launch {
                    assertTrue(
                        Thread.currentThread().name.startsWith("DefaultDispatcher-worker-1 @coroutine#")
                                || Thread.currentThread().name.startsWith("DefaultDispatcher-worker-2 @coroutine#")
                    )
                }
            }
        }
    }

    /**
     * `Dispatchers.IO`와 `Dispatchers.Default`를 사용하면 코루틴을 실행시킨 스레드 이름은 **DefaultDispatcher-worker-{숫자}** 이다.
     *
     * 이는, `Dispatchers.IO`와 `Dispatchers.Default`가 코루틴 라이브러리의 **공유 스레드 풀을 사용**하기 때문이다.
     *
     * ### 공유 스레드 풀
     * 스레드의 생성과 관리를 효율적으로 할 수 있도록 애플리케이션 레벨의 공유 스레드풀을 제공한다.
     *
     * 이 공유 스레드풀에서는 스레드를 무제한으로 생성할 수 있으며, 코루틴 라이브러리는 공유 스레드풀에 스레드를 생성하고 사용할 수 있도록 하는 API를 제공한다.
     *
     * - `Dispatchers.IO`와 `Dispatchers.Default`가 사용하는 스레드는 **구분**된다.
     * - `Dispatchers.Default.limitedParallelism(2)`는 `Dispatchers.Default`의 **여러 스레드 중 2개의 스레드만 사용**한다.
     *
     * **newFixedThreadPoolContext 함수로 만들어지는 디스패처가 자신만 사용할 수 있는 전용 스레드풀을 생성하는 것과 다르게 Dispatchers.IO와 Dispatchers.Default는 공유 스레드풀의 스레드를 사용한다.**
     */
    @Test
    fun 공유_스레드풀을_사용하는_Dispatchers_IO와_Dispatchers_Default() {
    }

    /**
     *
     */
    @Test
    fun DisPatchers_IO의_limitedParallelism() = runBlocking<Unit> {}

    /**
     *
     */
    @Test
    fun Dispatchers_Main() = runBlocking<Unit> {

    }

}