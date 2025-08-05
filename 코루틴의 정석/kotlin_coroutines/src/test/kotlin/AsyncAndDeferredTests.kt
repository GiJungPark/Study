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


}