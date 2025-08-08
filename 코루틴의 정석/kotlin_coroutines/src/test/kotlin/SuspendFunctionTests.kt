import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class SuspendFunctionTests {

    /**
     * 일시 중단 함수는 suspend fun 키워드로 선언되는 함수로 함수 내에 일시 중단 지점을 포함할 수 있는 특별한 기능을 한다.
     *
     * 일시 중단 함수는 주로 코루틴의 비동기 작업과 관련된 복잡한 코드들을 구조화하고 재사용할 수 있는 코드의 집합으로 만드는 데 사용된다.
     */
    @Test
    fun 일시_중단_함수란_무엇인가() {
    }


    /**
     * 일시 중단 함수는 코루틴 내부에서 실행되는 코드의 집합일 뿐, 코루틴이 아니다.
     *
     * 일시 중단 함수를 코루틴처럼 사용하고 싶다면 일시 중단 함수를 코루틴 빌더로 감싸야 한다.
     */
    @Test
    fun 일시_중단_함수는_코루틴이_아니다() = runBlocking<Unit> {
        val startTime = System.currentTimeMillis()
        delayAndPrintHelloWorld()
        delayAndPrintHelloWorld()
        println(getElapsedTime(startTime))
    }

    /**
     * 일시 중단 함수를 서로 다른 코루틴에서 실행되도록 하고 싶다면 코루틴 빌더 함수로 감싸면 된다.
     */
    @Test
    fun 일시_중단_함수를_별도의_코루틴상에서_실행하기() = runBlocking<Unit> {
        val startTime = System.currentTimeMillis()
        launch {
            delayAndPrintHelloWorld()
        }
        launch {
            delayAndPrintHelloWorld()
        }
        println(getElapsedTime(startTime))
    }

    @Test
    fun 코루틴_내부에서_일시_중단_함수_호출하기() = runBlocking<Unit> {
        delayAndPrint(keyword = "I'm Parent Coroutine")
        launch {
            delayAndPrint(keyword = "I'm Child Coroutine")
        }
    }

    /**
     * 일시 중단 함수는 또 다른 일시 중단 함수에서 호출될 수 있다.
     */
    @Test
    fun 일시_중단_함수에서_다른_일시_중단_함수_호출하기() = runBlocking<Unit> {
    }

    /**
     * 일시 중단 함수에서 다른 일시 중단 함수를 호출할 경우, 순차적으로 실행된다.
     *
     * 이를 해결하기 위해서 async 코루틴 빌더 함수로 감싸 서로 다른 코루틴에서 실행되도록 해야 한다.
     *
     * 하지만 launch나 async 같은 코루틴 빌더 함수는 CoroutineScope의 확장 함수로 선언돼 있기 때문에 문제가 발생한다.
     * - 일시 중단 함수 내부에서는 일시 중단 함수를 호출한 코루틴의 CoroutineScope 객체에 접근할 수 없기 때문
     */
    @Test
    fun 일시_중단_함수에서_코루틴_빌더_호출_시_생기는_문제() = runBlocking<Unit> {
    }

    /**
     * coroutineScope 일시 중단 함수를 사용하면 일시 중단 함수 내부에 새로운 CoroutineScope 객체를 생성할 수 있다.
     *
     * coroutineScope는 구조화를 깨지 않는 CoroutineScope 객체를 생성하며,
     * 생성된 CoroutineScope 객체는 coroutineScope의 block 람다식에서 수신 객체(this)로 접근할 수 있다.
     *
     * ``` kotlin
     * public suspend fun <R> coroutineScope(block: suspend CoroutineScope.() -> R): R
     * ```
     */
    @Test
    fun coroutineScope_사용해_일시_중단_함수에서_코루틴_실행하기() = runBlocking<Unit> {
        val startTime = System.currentTimeMillis()
        val results = searchByKeyword("Keyword")
        println("[결과] ${results.toList()}")
        println(getElapsedTime(startTime))
    }

    /**
     * supervisorScope 일시 중단 함수는 Job 대신 SupervisorJob 객체를 생성한다는 점을 제외하고는 coroutineScope 일시 중단 함수와 같이 동작한다.
     * ``` kotlin
     * public suspend fun <R> supervisorScope(block: suspend CoroutineScope.() -> R): R
     * ```
     */
    @Test
    fun supervisorScope_사용해_일시_중단_함수에서_코루틴_실행하기() = runBlocking<Unit> {
        println("[결과] ${searchByKeyword2("Keyword").toList()}")
    }
}