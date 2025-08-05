import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test

class CoroutineBuilderAndJobTests {

    /**
     * 코루틴을 생성하는 두 가지 함수는 `runBlocking`, `launch`는 코루틴 빌더 함수라고 불린다.
     *
     * 코루틴 빌더 함수가 호출되면 새로운 코루틴이 생성되며 코루틴을 추상화한 `Job` 객체를 생성한다.
     *
     * 코루틴은 일시 중단할 수 있는 작업으로 실행 도중 일시 중단된 후 나중에 이어서 실행될 수 있다.
     *
     * 코루틴을 추상화한 `Job` 객체는 이에 대응해 **코루틴을 제어할 수 있는 함수**와 코루틴의 상태를 나타내는 **상태 값들을 외부에 노출**한다.
     */
    @Test
    fun 코루틴_빌더() {
    }

    /**
     * 인증 토큰이 업데이트가 된 이후 네트워크 요청이 실행돼야 문제 없이 처리될 수 있는 상황
     *
     * ### delay와 sleep
     * `delay` 함수는 `Thread.sleep` 함수와 비슷하게 작업의 실행을 일정 시간 동안 지연시키는 역할을 한다.
     *
     * `Thread.sleep` 함수는 스레드가 블로킹돼 사용할 수 없는 상태가 되지만, `delay` 함수를 사용하면 함수가 실행되는 동안 스레드는 다른 코루틴이 사용할 수 있다.
     */
    @Test
    fun 순차_처리가_안_될_경우의_문제() = runBlocking<Unit> {
        val updateTokenJob = launch(Dispatchers.IO) {
            println("[${Thread.currentThread().name}] 토큰 업데이트 시작")
            delay(100L) // 토큰 업데이트 지연 시간
            println("[${Thread.currentThread().name}] 토큰 업데이트 완료")
        }

        val networkCallJob = launch(Dispatchers.IO) {
            println("[${Thread.currentThread().name}] 네트워크 요청")
        }
    }

    /**
     * `Job` 객체의 `join` 함수를 사용하면 코루틴 간에 순차 처리가 가능하다.
     *
     * 만약 JobA 코루틴이 완료된 후에 JobB 코루틴이 실행돼야 한다면 JobB 코루틴이 실행되기 전에 JobA 코루틴에 `join` 함수를 호출하면 된다.
     */
    @Test
    fun join_함수_사용해_순차_처리하기() = runBlocking<Unit> {
        val updateTokenJob = launch(Dispatchers.IO) {
            println("[${Thread.currentThread().name}] 토큰 업데이트 시작")
            delay(100L) // 토큰 업데이트 지연 시간
            println("[${Thread.currentThread().name}] 토큰 업데이트 완료")
        }

        updateTokenJob.join() // updateTokenJob이 완료될 때 까지 runBlocking 코루틴 일시 중단

        val networkCallJob = launch(Dispatchers.IO) {
            println("[${Thread.currentThread().name}] 네트워크 요청")
        }
    }

    /**
     * `join` 함수는 `join` 함수를 호출한 코루틴을 제외하고 이미 실행 중인 다른 코루틴을 일시 중단하지 않는다.
     */
    @Test
    fun join_함수는_join을_호출한_코루틴만_일시_중단한다() = runBlocking<Unit> {
        val updateTokenJob = launch(Dispatchers.IO) {
            println("[${Thread.currentThread().name}] 토큰 업데이트 시작")
            delay(100L)
            println("[${Thread.currentThread().name}] 토큰 업데이트 완료")
        }

        val independentJob = launch(Dispatchers.IO) {
            println("[${Thread.currentThread().name}] 독립적인 작업 실행")
            delay(300L)
            println("[${Thread.currentThread().name}] 독립적인 작업 완료")
        }

        updateTokenJob.join()
        val networkCallJob = launch(Dispatchers.IO) {
            println("[${Thread.currentThread().name}] 네트워크 요청")
        }
    }

    /**
     * 코루틴 라이브러리는 복수의 코루틴의 실행이 모두 끝날 때까지 호출부의 코루틴을 일시 중단시키는 `joinAll` 함수를 제공한다.
     *
     * ``` kotlin
     * public suspend fun joinAll(vararg jobs: Job): Unit = jobs.forEach {
     *  it.join()
     * }
     * ```
     */
    @Test
    fun joinAll_함수() = runBlocking<Unit> {
        val convertImageJob1: Job = launch(Dispatchers.Default) {
            Thread.sleep(1000L)
            println("[${Thread.currentThread().name}] 이미지 1 변환 완료")
        }

        val convertImageJob2: Job = launch(Dispatchers.Default) {
            Thread.sleep(1000L)
            println("[${Thread.currentThread().name}] 이미지 2 변환 완료")
        }

        joinAll(convertImageJob1, convertImageJob2)

        val uploadImageJob: Job = launch(Dispatchers.IO) {
            println("[${Thread.currentThread().name}] 이미지 1,2 업로드")
        }
    }

    /**
     * 지연 코루틴은 명시적으로 실행을 요청하지 않으면 실행되지 않는다.
     *
     * 지연 코루틴을 실행하기 위해서는 `Job` 객체의 `start` 함수를 명시적으로 호출해야 한다.
     */
    @Test
    fun CoroutineStart_Lazy를_사용해_지연_코루틴_만들기() = runBlocking<Unit> {
        val startTime = System.currentTimeMillis()
        val lazyJob: Job = launch(start = CoroutineStart.LAZY) {
            println("[${getElapsedTime(startTime)}] 지연 실행")
        }
        delay(1000L)
        lazyJob.start()
    }

    /**
     * 코루틴 실행 도중 코루틴을 실행할 필요가 없어지면 즉시 취소해야 한다.
     *
     * 코루틴이 실행될 필요가 없어졌음에도 취소하지 않고 계속해서 실행되도록 두면 코루틴은 계속해서 스레드를 사용하게 된다.
     */
    @Test
    fun 코루틴_취소하기() {
    }

    @Test
    fun cancel_함수를_사용해_Job_취소하기() = runBlocking<Unit> {
        val startTime = System.currentTimeMillis()
        val longJob: Job = launch(Dispatchers.Default) {
            repeat(10) { repeatTime ->
                delay(1000L)
                println("[${getElapsedTime(startTime)}] 반복횟수 ${repeatTime}")
            }
        }
        delay(3500L)
        longJob.cancel()
    }

    /**
     * `cancel` 함수를 호출한 이후, 곧바로 다른 작업을 실행하면 해당 작업은 코루틴이 취소되기 전에 실행될 수 있다.
     *
     * `Job` 객체에 `cancel`을 호출하면 코루틴은 즉시 취소되는 것이 아니라 `Job` 객체 내부의 취소 확인용 플래그를 '취소 요청됨'으로 변경함으로써 코루틴이 취소돼야 한다는 것만 알린다.
     *
     * 취소에 대한 순차성 보장을 위해 `Job` 객체는 `cancelAndJoin` 함수를 제공한다.
     * - `cancelAndJoin` 함수를 호출하면 `cancelAndJoin`의 대상이 된 코루틴의 취소가 완료될 때까지 호출부의 코루틴이 일시 중단된다.
     */
    @Test
    fun cancelAndJoin_함수를_사용한_순차_처리() = runBlocking<Unit> {
        val longJob: Job = launch(Dispatchers.Default) {
            // 작업 실행
        }
        longJob.cancelAndJoin() // longJob이 취소될 때까지 runBlocking 코루틴 일시 중단
        executeAfterJobCancelled()
    }

    /**
     *  `cancel` 함수나 `cancelAndJoin` 함수를 사용했다고 해서 코루틴이 즉시 취소되는 것은 아니다.
     *
     *  `Job` 객체 내부에 있는 취소 확인용 플래그를 바꾸기만 하며, **코루틴이 이 플래그를 확인하는 시점에 취소**된다.
     *
     *  따라서, 코루틴이 취소를 확인할 수 있는 시점이 없다면 취소는 일어나지 않는다.
     *
     *  코루틴이 취소를 확인하는 시점은 **일반적으로 일시 중단 지점이거나 코루틴이 실행을 대기하는 시점**이다.
     */
    @Test
    fun 코루틴의_취소_확인() = runBlocking<Unit> {
        val whileJob: Job = launch(Dispatchers.Default) {
            while (true) {
                println("작업 중")
            }
        }
        delay(100L)
        whileJob.cancel() // 프로세스가 종료되지 않고, "작업 중"이 무제한 출력된다.
    }

    /**
     * `delay` 함수는 **일시 중단 함수**(suspend fun)로 선언돼 특정 시간만큼 호출부의 코루틴을 일시 중단하게 만든다.
     *
     * 따라서, 작업 중간에 `delay(1L)`을 주게 되면 `while`문이 반복될 때마다 1밀리초 만큼 일시 중단 후 취소를 확인할 수 있다.
     *
     * 하지만 이 방법은 `while`문이 반복될 때마다 작업을 강제로 1밀리초 동안 일시 중단 시킨다는 점에서 효율적이지 않다.
     */
    @Test
    fun delay를_사용한_취소_확인() = runBlocking<Unit> {
        val i: AtomicInteger = AtomicInteger(0)
        val whileJob: Job = launch(Dispatchers.Default) {
            while (true) {
                println("작업 중")
                i.getAndAdd(1);
                delay(1L)
            }
        }
        delay(100L)
        whileJob.cancel()
        println(i)
    }

    /**
     * `yield` 함수가 호출되면 코루틴은 자신이 **사용하던 스레드를 양보**한다.
     *
     * 스레드 사용을 양보한다는 것은 스레드 사용을 중단하는 뜻이므로 `yield`를 호출한 코루틴이 일시 중단되며 이 시점에 취소됐는지 체크가 일어난다.
     *
     * 하지만 `yield`를 사용하는 방법 또한 `while`문을 한 번 돌 때마다 스레드 사용이 양보되면서 일시 중단되는 문제가 있다.
     */
    @Test
    fun yield를_사용한_취소_확인() = runBlocking<Unit> {
        val i: AtomicInteger = AtomicInteger(0)
        val whileJob: Job = launch(Dispatchers.Default) {
            while (true) {
                println("작업 중")
                i.getAndAdd(1);
                yield()
            }
        }
        delay(100L)
        whileJob.cancel()
        println(i)
    }

    /**
     * CoroutineScope는 코루틴이 활성화됐는지 확인할 수 있는 Boolean 타입의 프로퍼티인 `isActive`를 제공한다.
     *
     * 코루틴에 취소가 요청되면 `isActive` 프로퍼티의 값은 false로 바뀌며, 이를 활용하여 `while`문이 취소되도록 만들 수 있다.
     *
     * 이 방법을 사용하면 코루틴이 잠시 멈추지도 않고 스레드 사용을 양보하지도 않으면서 계속해서 작업을 할 수 있어서 효율적이다.
     */
    @Test
    fun CoroutineScope_isActive를_사용한_취소_확인() = runBlocking<Unit> {
        val i: AtomicInteger = AtomicInteger(0)
        val whileJob: Job = launch(Dispatchers.Default) {
            while (this.isActive) {
                println("작업 중")
                i.getAndAdd(1);
            }
        }
        delay(100L)
        whileJob.cancel()
        println(i)
    }

    /**
     * ### 코루틴의 6가지 상태
     * - **생성**: 코루틴 빌더를 통해 코루틴을 생성하면 코루틴은 기본적으로 생성 상태에 놓이며, 자동으로 실행 중 상태로 넘어간다. 실행 중 상태로 자동으로 변경되지 않도록 만들고 싶다면 코루틴 빌더의 start 인자로 Coroutines.LAZY를 넘겨 지연 코루틴을 만들면 된다.
     * - **실행 중**: 지연 코루틴이 아닌 코루틴을 만들면 자동으로 실행 중 상태로 바뀐다. 코루틴이 실제로 실행 중일 때뿐만 아니라 실행된 후에 일시 중단된 때도 실행 중 상태로 본다.
     * - **실행 완료 중**: 부모 코루틴의 모든 코드가 실행됐지만 자식 코루틴이 실행 중인 경우, 부모 코루틴이 갖는 상태
     * - **실행 완료**: 코루틴의 모든 코드가 실행 완료된 경우 실행 완료 상태로 넘어간다.
     * - **취소 중**: Job.cancel() 등을 통해 코루틴에 취소가 요청됐을 경우 취소 중 상태로 넘어가며, 이는 아직 취소된 상태가 아니어서 코루틴은 계속해서 실행된다.
     * - **취소 완료**: 코루틴의 취소 확인 시점(일시 중단 등)에 취소가 확인된 경우 취소 완료 상태가 된다. 이 상태에서는 코루틴은 더 이상 실행되지 않는다.
     *
     * ### Job의 상태 변수
     * - **isActive**: 코루틴이 활성화돼 있는지의 여부
     * - **isCancelled**: 코루틴이 취소 요청됐는지의 여부
     * - **isCompleted**: 코루틴 실행이 완료됐는지의 여부
     */
    @Test
    fun 코루틴의_상태와_Job의_상태_변수() {
    }

    /**
     * 생성 상태의 코루틴
     * - isActive >> false
     * - isCancelled >> false
     * - isCompleted >> false
     *
     */
    @Test
    fun 생성_상태의_코루틴() = runBlocking<Unit> {
        val job: Job = launch(start = CoroutineStart.LAZY) {
            delay(1000L)
        }
        printJobState(job)
    }

    /**
     * 실행 중 상태의 코루틴
     * - isActive >> true
     * - isCancelled >> false
     * - isCompleted >> false
     */
    @Test
    fun 실행_중_상태의_코루틴() = runBlocking<Unit> {
        val job: Job = launch {
            delay(1000L)
        }
        printJobState(job)
    }

    /**
     * 실행 완료 상태의 코루틴
     * - isActive >> false
     * - isCancelled >> false
     * - isCompleted >> true
     */
    @Test
    fun 실행_완료_상태의_코루틴() = runBlocking<Unit> {
        val job: Job = launch {
            delay(1000L)
        }
        delay(2000L)
        printJobState(job)
    }

    /**
     * 취소 중인 코루틴
     * - isActive >> false
     * - isCancelled >> true
     * - isCompleted >> false
     *
     * 취소가 요청되면 실제로는 코드가 실행 중이더라도 코루틴이 활성화된 상태로 보지 않는다.
     */
    @Test
    fun 취소_중인_코루틴() = runBlocking<Unit> {
        val whileJob: Job = launch(Dispatchers.Default) {
            while(true) {
                // 작업 실행
            }
        }
        whileJob.cancel()
        printJobState(whileJob)
    }

    /**
     * 취소 완료된 코루틴
     * - isActive >> false
     * - isCancelled >> true
     * - isCompleted >> true
     */
    @Test
    fun 취소_완료된_코루틴() = runBlocking<Unit> {
        val job: Job = launch {
            delay(5000L)
        }
        job.cancelAndJoin()
        printJobState(job)
    }
}