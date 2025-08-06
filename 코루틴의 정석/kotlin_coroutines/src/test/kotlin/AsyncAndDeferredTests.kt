import kotlinx.coroutines.*
import kotlin.test.Test
import kotlin.test.assertEquals


class AsyncAndDeferredTests {

    /**
     * `launch` 코루틴 빌더를 통해 생성되는 코루틴은 기본적으로 작업 실행 후 결과를 반환하지 않는다.
     *
     * 코루틴 라이브러리는 비동기 작업으로부터 결과를 수신해야 하는 경우를 위해 `async` 코루틴 빌더를 통해 생성되는 **코루틴으로부터 결과값을 수신**받을 수 있도록 한다.
     *
     * `async` 함수를 사용하면 결과값이 있는 코루틴 객체인 `Deferred`가 반환되며, `Deferred` 객체를 통해 코루틴으로부터 결과값을 수신할 수 있다.
     *
     * ``` kotlin
     * public fun <T> CoroutineScope.async(
     *  context: CoroutineContext = EmptyCoroutineContext,
     *  start: CoroutineStart = CoroutineStart.DEFAULT,
     *  block: suspend CoroutineScope.() -> T
     * ): Deferred<T>
     * ```
     */
    @Test
    fun async_코루틴_빌더() = runBlocking<Unit> {
        val networkDeferred: Deferred<String> = async(Dispatchers.IO) {
            delay(1000L)
            return@async "Response"
        }
    }

    /**
     * Deferred 객체는 결과값 수신의 대기를 위해 await 함수를 제공한다.
     *
     * await 함수는 await의 대상이 된 Deferred 코루틴이 실행 완료될 때까지 await 함수를 호출한 코루틴을 일시 중단하며, Deferred 코루틴이 실행 완료되면 결과값을 반환하고 호출부의 코루틴을 재개한다.
     *
     * Deferred 객체의 await 함수는 코루틴이 실행 완료될 때까지 호출부의 코루틴을 일시 중단한다는 점에서 Job 객체의 join 함수와 매우 유사하게 동작한다.
     */
    @Test
    fun await를_사용한_결과값_수신() = runBlocking<Unit> {
        val networkDeferred: Deferred<String> = async(Dispatchers.IO) {
            delay(1000L)
            return@async "Response"
        }

        // networkDeferred로부터 결과값이 반환될 때까지 runBlocking 일시 중단
        val result = networkDeferred.await()

        assertEquals("Response", result)
    }

    /**
     * Deferred 인터페이스는 Job 인터페이스의 서브타입으로 선언된 인터페이스이다.
     *
     * Deferred 객체는 코루틴으로부터 결과값 수신을 위해 Job 객체에서 몇 가지 기능이 추가된 Job 객체의 일종이다.
     *
     * ``` kotlin
     * public interface Deferred<out T> : Job {
     *  public suspend fun await(): T
     *  ...
     * }
     * ```
     *
     * 이런 특성 때문에 Deferred 객체는 Job 객체의 모든 함수와 프로퍼티를 사용할 수 있다.
     * - join을 사용해 Deferred 객체가 완료될 때까지 호출부의 코루틴을 일시 중단
     * - Deferred 객체가 취소돼야 할 경우 cancel 함수를 호출해 취소
     * - 상태 조회를 위해 isActive, isCancelled, isCompleted 같은 프로퍼티들을 사용
     */
    @Test
    fun Deferred는_특수한_형태의_Job이다() = runBlocking<Unit> {
        val networkDeferred: Deferred<String> = async(Dispatchers.IO) {
            delay(1000L)
            "Response"
        }
        networkDeferred.join()
        printJobState(networkDeferred)
    }

    @Test
    fun await를_사용해_복수의_코루틴으로부터_결과값_수신하기() = runBlocking<Unit> {
        val startTime = System.currentTimeMillis()
        val participantDeferred1: Deferred<Array<String>> = async(Dispatchers.IO) {
            delay(1000L)
            return@async arrayOf("James", "Jason")
        }

        val participantDeferred2: Deferred<Array<String>> = async(Dispatchers.IO) {
            delay(1000L)
            return@async arrayOf("Jenny")
        }

        val participants1 = participantDeferred1.await()
        val participants2 = participantDeferred2.await()

        println("[${getElapsedTime(startTime)}] 참여자 목록: ${listOf(*participants1, *participants2)}")
    }

    /**
     * 코루틴 라이브러리는 복수의 Deferred 객체로부터 결과값을 수신하기 위한 awaitAll 함수를 제공한다.
     *
     * ``` kotlin
     * public suspend fun <T> awaitAll(vararg deferreds: Deferred<T>): List<T>
     * ```
     *
     * 가변 인자로 Deferred 타입의 객체를 받아 인자로 받은 모든 Deferred 코루틴으로부터 결과가 수신될 때까지 호출부의 코루틴을 일시 중단한 후 결과가 모두 수신되면 Deferred 코루틴들로부터 수신한 결과값들을 List로 만들어 반환하고 호출부의 코루틴을 재개한다.
     */
    @Test
    fun awaitAll을_사용한_결과값_수신() = runBlocking<Unit> {
        val startTime = System.currentTimeMillis()
        val participantDeferred1: Deferred<Array<String>> = async(Dispatchers.IO) {
            delay(1000L)
            return@async arrayOf("James", "Jason")
        }

        val participantDeferred2: Deferred<Array<String>> = async(Dispatchers.IO) {
            delay(1000L)
            return@async arrayOf("Jenny")
        }

        val results: List<Array<String>> = awaitAll(participantDeferred1, participantDeferred2)

        println("[${getElapsedTime(startTime)}] 참여자 목록: ${listOf(*results[0], *results[1])}")
    }

    /**
     * 코루틴 라이브러리는 awaitAll 함수를 Collection 인터페이스에 대한 확장 함수로도 제공한다.
     *
     * ``` kotlin
     * public suspend fun <T> Collection<Deferred<T>>.awaitAll(): List<T>
     * ```
     *
     * Collection<Deferred<T>>에 대해 awaitAll 함수를 호출하면 컬렉션에 속한 Deferred들이 모두 완료돼 결과값을 반환할 때까지 대기한다.
     */
    @Test
    fun 컬렉션에_대해_awaitAll_사용하기() = runBlocking<Unit> {
        val startTime = System.currentTimeMillis()
        val participantDeferred1: Deferred<Array<String>> = async(Dispatchers.IO) {
            delay(1000L)
            return@async arrayOf("James", "Jason")
        }

        val participantDeferred2: Deferred<Array<String>> = async(Dispatchers.IO) {
            delay(1000L)
            return@async arrayOf("Jenny")
        }

        val results: List<Array<String>> = listOf(participantDeferred1, participantDeferred2).awaitAll()

        println("[${getElapsedTime(startTime)}] 참여자 목록: ${listOf(*results[0], *results[1])}")
    }

    /**
     * 코루틴 라이브러리에서 제공되는 withContext 함수를 사용하면 async-awiat 작업을 대체할 수 있다.
     *
     * ``` kotlin
     * public suspend fun <T> withContext(
     *  context: CoroutineContext,
     *  block: suspend CoroutineScope.() -> T
     * ): T
     *
     * withConext 함수가 호출되면 함수의 인자로 설정된 CoroutineContext 객체를 사용해 block 람다식을 실행하고, 완료되면 그 결과를 반환한다.
     * ```
     */
    @Test
    fun withContext로_async_await_대체하기() = runBlocking<Unit> {
        val result: String = withContext(Dispatchers.IO) {
            delay(1000L)
            return@withContext "Response"
        }

        assertEquals("Response", result)
    }

    /**
     * async-await 쌍은 새로운 코루틴을 생성해 작업을 처리하지만 withContext 함수는 실행 중이던 코루틴을 그대로 유지한 채로 코루틴의 실행 환경만 변경해 작업을 처리한다.
     *
     * withContext 함수는 새로운 코루틴을 만드는 대신 기존의 코루틴에서 CoroutineContext 객체만 바꿔서 실행된다.
     *
     * withContext 함수가 호출되면 실행 중인 코루틴의 실행 환경이 withContext 함수의 context 인자 값으로 변경돼 실행되며, 이를 컨텍스트 스위칭이라고 부른다.
     */
    @Test
    fun withContext의_동작_방식() = runBlocking<Unit> {
        println("[${Thread.currentThread().name}] runBlocking 블록 실행")
        withContext(Dispatchers.IO) {
            println("[${Thread.currentThread().name}] withContext 블록 실행")
        }
    }

    /**
     * runBlocking 함수에 의해 하나의 코루틴만 생성된다.
     *
     * 각 withContext 블록의 코드를 실행하는 데는 1초가 걸리지만 순차적으로 처리돼 2초의 시간이 걸리게 된다.
     *
     * 이는 withContext 함수가 새로운 코루틴을 생성하지 않기 때문에 생기는 문제이다.
     */
    @Test
    fun withContext_사용_시_주의점() = runBlocking<Unit> {
        val startTime = System.currentTimeMillis()
        val helloString = withContext(Dispatchers.IO) {
            delay(1000L)
            return@withContext "Hello"
        }

        val worldString = withContext(Dispatchers.IO) {
            delay(1000L)
            return@withContext "World"
        }

        println("[${getElapsedTime(startTime)}] ${helloString} ${worldString}")
    }

    /**
     * 이 문제를 해결하기 위해서는 withContext를 제거하고, 코루틴을 생성하는 async-await 쌍으로 대체해야 한다.
     */
    @Test
    fun withContext_사용_시_주의점2() = runBlocking<Unit> {
        val startTime = System.currentTimeMillis()
        val helloDeferred: Deferred<String> = async(Dispatchers.IO) {
            delay(1000L)
            return@async "Hello"
        }

        val worldDeferred: Deferred<String> = async(Dispatchers.IO) {
            delay(1000L)
            return@async "World"
        }

        val results = awaitAll(helloDeferred, worldDeferred)

        println("[${getElapsedTime(startTime)}] ${results[0]} ${results[1]}")
    }

    @Test
    fun withContext를_사용한_코루틴_스레드_전환() = runBlocking<Unit> {
        val myDispatcher1 = newSingleThreadContext("MyThread1")
        val myDispatcher2 = newSingleThreadContext("MyThread2")

        println("[${Thread.currentThread().name}] 코루틴 실행")
        withContext(myDispatcher1) {
            println("[${Thread.currentThread().name}] 코루틴 실행")
            withContext(myDispatcher2) {
                println("[${Thread.currentThread().name}] 코루틴 실행")
            }
            println("[${Thread.currentThread().name}] 코루틴 실행")
        }
        println("[${Thread.currentThread().name}] 코루틴 실행")
    }
}