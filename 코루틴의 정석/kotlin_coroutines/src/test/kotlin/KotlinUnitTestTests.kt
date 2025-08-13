import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KotlinUnitTestTests {

    @Test
    fun `100번 더하면 100이 반환된다`() = runBlocking {
        // Given
        val repeatAddUseCase = RepeatAddUseCase()

        // When
        val result = repeatAddUseCase.add(100)

        // Then
        assertEquals(100, result)
    }

    /**
     * runBlocking 함수를 사용한 테스트에서 실행에 오랜 시간이 걸리는 일시 중단 함수를 실행하면 문제가 나타난다.
     * -> 테스트 소요 시간 증가
     */
    @Test
    fun `runBlocking을 사용한 테스트의 한계`() = runBlocking {
        // Given
        val repeatAddUseCase = RepeatAddWithDelayUseCase()

        // When
        val result = repeatAddUseCase.add(100)

        // Then
        assertEquals(100, result)
    }

    /**
     * 코루틴 테스트 시 코루틴에 오랜 시간이 걸리는 작업이 포함돼 있으면
     * 가상 시간을 사용해 코루틴의 동작이 자신이 원하는 시간까지 단번에 진행될 수 있도록 만들면 테스트를 빠르게 완료할 수 있다.
     */
    @Test
    fun `TestCoroutineScheduler 사용해 가상 시간에서 테스트 진행하기`() {
    }

    @Test
    fun `가상 시간 조절 테스트`() {
        val testCoroutineScheduler = TestCoroutineScheduler()

        // 가상 시간에서 5초를 흐르게 만듦: 현재 5초
        testCoroutineScheduler.advanceTimeBy(5000L)
        assertEquals(5000L, testCoroutineScheduler.currentTime)

        // 가상 시간에서 6초를 흐르게 만듦: 현재 11초
        testCoroutineScheduler.advanceTimeBy(6000L)
        assertEquals(11000L, testCoroutineScheduler.currentTime)

        // 가상 시간에서 10초를 흐르게 만듦: 현재 21초
        testCoroutineScheduler.advanceTimeBy(10000L)
        assertEquals(21000L, testCoroutineScheduler.currentTime)
    }

    /**
     * TestCoroutineScheduler 객체는
     * 테스트용 CoroutineDispatcher 객체인 TestDispatcher를 만드는 StandardTestDispatcher 함수와 함께 사용할 수 있다.
     *
     * testCoroutineScope는 testCoroutineScheduler에 의해 시간이 관리되기 때문에
     * 이 범위에서 실행되는 코루틴들은 가상 시간이 흐르지 않으면 실행되지 않는다.
     */
    @Test
    fun `TestCoroutineScheduler와 StandardTestDispatcher 사용해 가상 시간 위에서 테스트 진행하기`() {
        // 테스트 환경 설정
        val testCoroutineScheduler: TestCoroutineScheduler = TestCoroutineScheduler()
        val testDispatcher: TestDispatcher = StandardTestDispatcher(scheduler = testCoroutineScheduler)
        val testCoroutineScope: CoroutineScope = CoroutineScope(context = testDispatcher)

        // Given
        var result = 0

        // When
        testCoroutineScope.launch {
            delay(10000L) // 10초 대기
            result = 1
            delay(10000L) // 10초 대기
            result = 2
            println(Thread.currentThread().name)
        }

        // Then
        assertEquals(0, result)
        testCoroutineScheduler.advanceTimeBy(5000L) // 가상 시간에서 5초를 흐르게 만듦: 현재 5초
        assertEquals(0, result)

        testCoroutineScheduler.advanceTimeBy(6000L) // 가상 시간에서 6초를 흐르게 만듦: 현재 11초
        assertEquals(1, result)

        testCoroutineScheduler.advanceTimeBy(10000L) // 가상 시간에서 10초를 흐르게 만듦: 현재 21초
        assertEquals(2, result)
    }

    /**
     * 테스트가 제대로 실행되기 위해서는 테스트 대상 코드가 모두 실행되고 나서 검증이 실행돼야 한다.
     * 이를 위해 TestCoroutineScheduler 객체는
     * 이 객체를 사용하는 모든 디스패처와 연결된 작업이 모두 완료될 때까지 가상 시간을 흐르게 만드는 advanceUntilIdel 함수를 제공한다.
     */
    @Test
    fun `advanceUntilIdle 사용해 모든 코루틴 실행시키기`() {
        // 테스트 환경 설정
        val testCoroutineScheduler: TestCoroutineScheduler = TestCoroutineScheduler()
        val testDispatcher: TestDispatcher = StandardTestDispatcher(scheduler = testCoroutineScheduler)
        val testCoroutineScope: CoroutineScope = CoroutineScope(context = testDispatcher)

        // Given
        var result = 0

        // When
        testCoroutineScope.launch {
            delay(10_000L) // 10초 대기
            result = 1
            delay(10_000L) // 10초 대기
            result = 2
        }
        testCoroutineScheduler.advanceUntilIdle()

        // Then
        assertEquals(2, result)
    }

    /**
     * StandardTestDispatcher 함수에는 기본적으로 TestCoroutineScheduler 객체를 생성하는 부분이 포함돼 있다.
     * ``` kotlin
     * public fun StandardTestDispatcher(
     *  scheduler: TestCoroutineScheduler? = null,
     *  name: String? = null
     * ): TestDispatcher = StandardTestDispatcherImpl(
     *  scheduler ?: TestMainDispatcher.currentTestScheduler ?: TestCoroutineScheduler(),
     *  name
     * )
     * ```
     */
    @Test
    fun `TestCoroutineScheduler를 포함하는 StandardTestDispatcher`() {
        // 테스트 환경 설정
        val testDispatcher: TestDispatcher = StandardTestDispatcher()
        val testCoroutineScope: CoroutineScope = CoroutineScope(context = testDispatcher)

        // Given
        var result = 0

        // When
        testCoroutineScope.launch {
            delay(10_000L) // 10초 대기
            result = 1
            delay(10_000L) // 10초 대기
            result = 2
        }
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(2, result)
    }

    /**
     * 매번 TestDispatcher 객체를 CoroutineScope 함수로 감싸는 것이 불편하기 때문에 TestScope 함수를 제공한다.
     *
     * TestScope 함수를 호출하면 내부에 TestDispatcher 객체를 가진 TestScope 객체가 반환된다.
     *
     * TestScope 함수는 기본적으로 StandardTestDispatcher 함수로 생성되는 TestDispatcher 객체를 포함한다.
     */
    @Test
    fun `TestScope 사용해 가상 시간에서 테스트 진행하기`() {
        // 테스트 환경 설정
        val testCoroutineScope: TestScope = TestScope()

        // Given
        var result = 0

        // When
        testCoroutineScope.launch {
            delay(10_000L)
            result = 1
            delay(10_000L)
            result = 2
        }

        testCoroutineScope.advanceUntilIdle()

        // Then
        assertEquals(2, result)
    }

    /**
     * runTest 함수는 TestScope 객체를 사용해 코루틴을 실행시키고, 그 코루틴 내부에서 일시 중단 함수가 실행되더라도
     * 작업이 곧바로 실행 완료될 수 있도록 가상 시간을 흐르게 만드는 기능을 가진 코루틴 빌더이다.
     *
     * runTest 함수는 TestScope 함수를 포함하고,
     * TestScope 함수는 StandardTestDispatcher 함수를 포함하고,
     * StandardTestDispatcher 함수는 TestCoroutineScheduler를 포함한다.
     */
    @Test
    fun `runTest 사용해 테스트 만들기`() {
        // Given
        var result = 0

        // When
        runTest {
            delay(10_000L)
            result = 1
            delay(10_000L)
            result = 2
        }

        // Then
        assertEquals(2, result)
    }

    @Test
    fun `runTest로 테스트 감싸기`() = runTest {
        // Given
        var result = 0

        // When
        delay(10_000L)
        result = 1
        delay(10_000L)
        result = 2

        // Then
        assertEquals(2, result)
    }

    /**
     * runTest 함수는 람다식에서 TestScope 객체를 수신 객체로 갖기 때문에 this를 통해 TestScope 객체에 접근할 수 있다.
     */
    @Test
    fun `runTest 함수의 람다식에서 TestScope 사용하기`() = runTest {
        delay(10_000L)
        println("가상 시간: ${this.currentTime}ms")
        delay(10_000L)
        println("가상 시간: ${this.currentTime}ms")
    }

    /**
     * advanceTimeBy 함수나 advanceUntilIdle 함수는 runTest의 TestScope 내부에서 새로운 코루틴이 실행될 때
     * 해당 코루틴이 모두 실행 완료될 때까지 가상 시간을 흐르게 하는 데 사용할 수 있다.
     *
     * runTest 함수는 runTest 함수로 생성된 내부에서 실행된 일시 중단 함수에 대해서만 가상 시간이 흐르게 만들며,
     * TestScope 상에서 새로 실행된 launch 코루틴에 대해서는 자동으로 시간을 흐르게 하지 않는다.
     */
    @Test
    fun `runTest 내부에서 advanceUntilIdle 사용하기`() = runTest {
        var result = 0
        launch {
            delay(1000L)
            result = 1
        }

        println("가상 시간: ${currentTime}ms, result: ${result}")
        advanceUntilIdle()
        println("가상 시간: ${currentTime}ms, result: ${result}")
    }

    /**
     * runTest 코루틴의 자식 코루틴을 생성하고 해당 코루틴에 대해 join을 호출하면 runTest 코루틴의 가상 시간이 흐른다.
     * - join 함수의 호출이 runTest 코루틴을 일시 중단시키기 때문
     */
    @Test
    fun `runTest 내부에서 join 사용하기`() = runTest {
        var result = 0
        launch {
            delay(1000L)
            result = 1
        }.join()

        println("가상 시간: ${currentTime}ms, result: ${result}")
    }

    @Test
    fun `updateStringWithDelay("ABC")가 호출되면 문자열이 ABC로 변경된다`() = runTest {
        // Given
        val testDispatcher = StandardTestDispatcher()
        val stringStateHolder = StringStateHolder(testDispatcher)

        // When
        stringStateHolder.updateStringWithDelay("ABC")

        // Then
        testDispatcher.scheduler.advanceUntilIdle()
        Assertions.assertEquals("ABC", stringStateHolder.stringState)
    }

    @Test
    fun `backgroundScope를 사용하는 테스트`() = runTest {
        var result = 0

        backgroundScope.launch {
            while(true) {
                delay(1000L)
                result += 1
            }
        }

        advanceTimeBy(1500L)
        Assertions.assertEquals(1, result)
        advanceTimeBy(1000L)
        Assertions.assertEquals(2, result)
    }
}
