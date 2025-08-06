import kotlinx.coroutines.*
import kotlin.test.Test

class ExceptionHandlingTests {

    @Test
    fun 예와_전파() = runBlocking<Unit> {
        launch(CoroutineName("Coroutine1")) {
            launch(CoroutineName("Coroutine3")) {
                throw Exception("예외 발생")
            }
            delay(100L)
            println("[${Thread.currentThread().name}] 코루틴 실행")
        }

        launch(CoroutineName("Coroutine2")) {
            delay(100L)
            println("[${Thread.currentThread().name}] 코루틴 실행")
        }
        delay(1000L)
    }

    @Test
    fun Job_객체를_사용한_예외_전파_제한() = runBlocking<Unit> {
        launch(CoroutineName("Parent Coroutine")) {
            launch(CoroutineName("Coroutine1") + Job()) {
                launch(CoroutineName("Coroutine3")) {
                    throw Exception("예외 발생")
                }
                delay(100L)
                println("[${Thread.currentThread().name}] 코루틴 실행")
            }
            launch(CoroutineName("Coroutine2")) {
                delay(100L)
                println("[${Thread.currentThread().name}] 코루틴 실행")
            }
        }
        delay(1000L)
    }

    /**
     * Coroutine1 코루틴은 더 이상 Parent Coroutine 코루틴의 자식 코루틴이 아니기 때문에 취소 전파가 제한된다.
     */
    @Test
    fun Job_객체를_사용한_예외_전파_제한의_한계() = runBlocking<Unit> {
        val parentJob = launch(CoroutineName("Parent Coroutine")) {
            launch(CoroutineName("Coroutine1") + Job()) {
                launch(CoroutineName("Coroutine3")) {
                    delay(100L)
                    println("[${Thread.currentThread().name}] 코루틴 실행")
                }
                delay(100L)
                println("[${Thread.currentThread().name}] 코루틴 실행")
            }
            launch(CoroutineName("Coroutine2")) {
                delay(100L)
                println("[${Thread.currentThread().name}] 코루틴 실행")
            }
        }
        delay(20L)
        parentJob.cancel()
        delay(1000L)
    }

    /**
     * SupervisorJob 객체는 자식 코루틴으로부터 예외를 전파받지 않는 특수한 Job 객체
     *
     * 하나의 자식 코루틴에서 발생한 예외가 다른 자식 코루틴에게 영향을 미치지 못하도록 만드는데 사용된다.
     */
    @Test
    fun SupervisorJob_객체를_사용해_예외_전파_제한하기() = runBlocking<Unit> {
        val supervisorJob = SupervisorJob()
        launch(CoroutineName("Coroutine1") + supervisorJob) {
            launch(CoroutineName("Coroutine3")) {
                throw Exception("예외 발생")
            }
            delay(100L)
            println("[${Thread.currentThread().name}] 코루틴 실행")
        }

        launch(CoroutineName("Coroutine2") + supervisorJob) {
            delay(100L)
            println("[${Thread.currentThread().name}] 코루틴 실행")
        }
        delay(1000L)
    }

    @Test
    fun 코루틴의_구조화를_깨지_않고_SupervisorJob_사용하기() = runBlocking<Unit> {
        val supervisorJob = SupervisorJob(parent = this.coroutineContext[Job])
        launch(CoroutineName("Coroutine1") + supervisorJob) {
            launch(CoroutineName("Coroutine3")) {
                throw Exception("예외 발생")
            }
            delay(100L)
            println("[${Thread.currentThread().name}] 코루틴 실행")
        }

        launch(CoroutineName("Coroutine2") + supervisorJob) {
            delay(100L)
            println("[${Thread.currentThread().name}] 코루틴 실행")
        }
        supervisorJob.complete()
    }

    @Test
    fun SupervisorJob을_CoroutineScope와_함께_사용하기() = runBlocking<Unit> {
        val coroutineScope = CoroutineScope(SupervisorJob())
        coroutineScope.apply {
            launch(CoroutineName("Coroutine1")) {
                launch(CoroutineName("Coroutine3")) {
                    throw Exception("예외 발생")
                }
                delay(100L)
                println("[${Thread.currentThread().name}] 코루틴 실행")
            }
            launch(CoroutineName("Coroutine2")) {
                delay(100L)
                println("[${Thread.currentThread().name}] 코루틴 실행")
            }
        }
        delay(1000L)
    }

    /**
     * launch 함수는 context 인자에 Job 객체가 입력될 경우 해당 Job 객체를 부모로 하는 새로운 Job 객체를 만들어 낸다.
     *
     * 따라서, launch 함수에 SupervisorJob()을 인자로 넘기면
     * SupervisorJob()을 통해 만들어지는 SupervisorJob 객체를 부모로 하는 새로운 Job 객체가 만들어진다.
     */
    @Test
    fun SupervisorJob을_사용할_때_흔히_하는_실수() = runBlocking<Unit> {
        launch(CoroutineName("Parent Coroutine") + SupervisorJob()) {
            launch(CoroutineName("Coroutine1")) {
                launch(CoroutineName("Coroutine3")) {
                    throw Exception("예외 발생")
                }
                delay(100L)
                println("[${Thread.currentThread().name}] 코루틴 실행")
            }
            launch(CoroutineName("Coroutine2")) {
                delay(100L)
                println("[${Thread.currentThread().name}] 코루틴 실행")
            }
        }
        delay(1000L)
    }

    /**
     * supervisorScope 함수는 SupervisorJob 객체를 가진 CoroutineScope 객체를 생성하며,
     * 이 SupervisorJob 객체는 supervisorScope 함수를 호출한 코루틴의 Job 객체를 부모로 가진다.
     *
     * supervisorScope 내부에서 실행되는 코루틴은 SupervisorJob과 부모-자식 관계로 구조화 되는데
     * supervisorScope의 SupervisorJob 객체는 코드가 모두 실행되고 자식 코루틴도 모두 실행 완료되면 자동으로 완료 처리된다.
     */
    @Test
    fun supervisorScope를_사용한_예외_전파_제한() = runBlocking<Unit> {
        supervisorScope {
            launch(CoroutineName("Coroutine1")) {
                launch(CoroutineName("Coroutine3")) {
                    throw Exception("예외 발생")
                }
                delay(100L)
                println("[${Thread.currentThread().name}] 코루틴 실행")
            }
            launch(CoroutineName("Coroutine2")) {
                delay(100L)
                println("[${Thread.currentThread().name}] 코루틴 실행")
            }
        }
    }

    /**
     * ``` kotlin
     * public inline fun CoroutineExceptionHandler(crossinline handler: (CoroutineConext, Throwable) -> Unit): CoroutineExceptionHandler
     * ```
     *
     * CoroutineExceptionHandler 함수는 예외를 처리하는 람다식인 handler를 매개변수로 가진다.
     *
     * handler는 CoroutineConext와 Throwable 타입의 매개변수를 갖는 람다식으로 이 람다식에 예외가 발생했을 때 어떤 동작을 할지 입력해 예외를 처리할 수 있다.
     */
    @Test
    fun CoroutineExceptionHandler_생성() {
        val exceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            println("[예외 발생] ${throwable}")
        }
    }

    @Test
    fun CoroutineExceptionHandler_사용() = runBlocking<Unit> {
        val exceptionHandler =
            CoroutineExceptionHandler { coroutineContext, throwable -> println("[예외 발생] ${throwable}") }
        CoroutineScope(exceptionHandler).launch(CoroutineName("Coroutine1")) {
            throw Exception("Coroutine1에 예외가 발생했습니다.")
        }
        delay(1000L)
    }

    /**
     * CoroutineExceptionHandler 객체는 처리되지 않은 예외만 처리한다.
     *
     * 자식 코루틴이 부모 코루틴으로 예외를 전파하면 자식 코루틴에서는 예외가 처리된 것으로 봐
     * 자식 코루틴에 설정된 CoroutineExceptionHandler 객체는 동작하지 않는다.
     *
     * 코루틴은 예외가 전파되면 예외를 처리한 것으로 보며, CoroutineExceptionHandler 객체는 이미 처리된 예외에 대해서는 동작하지 않는다.
     */
    @Test
    fun 처리되지_않은_예외만_처리하는_CoroutineExceptionHandler() = runBlocking<Unit> {
        val exceptionHandler =
            CoroutineExceptionHandler { coroutineContext, throwable -> println("[예외 발생] ${throwable}") }
        launch(CoroutineName("Coroutine1") + exceptionHandler) {
            throw Exception("Coroutine1에 예외가 발생했습니다")
        }
        delay(1000L)
    }

    /**
     * 예외가 마지막으로 전파되는 위치에 CoroutineExceptionHandler 객체를 설정하면 예외 처리기가 동작하도록 만들 수 있다.
     */
    @Test
    fun CoroutineExceptionHandler가_예외를_처리하도록_만들기() = runBlocking<Unit> {
        val exceptionHandler =
            CoroutineExceptionHandler { coroutineContext, throwable -> println("[예외 발생] ${throwable}") }
        CoroutineScope(exceptionHandler).launch(CoroutineName("Coroutine1")) {
            throw Exception("Coroutine1에 예외가 발생했습니다.")
        }
        delay(1000L)
    }

    /**
     * CoroutineExceptionHandler 객체가 예외를 처리하게 하는 가장 간단한 방법은
     * CoroutineExceptionHandler 객체를 루트 Job과 함께 설정하는 것이다.
     */
    @Test
    fun Job과_CoroutineExceptionHandler_함께_설정하기() = runBlocking<Unit> {
        val coroutineContext =
            Job() + CoroutineExceptionHandler { coroutineContext, throwable -> println("[예외 발생] ${throwable}") }
        launch(CoroutineName("Coroutine1") + coroutineContext) {
            throw Exception("Coroutine1에 예외가 발생했습니다")
        }
        delay(1000L)
    }

    /**
     * SupervisorJob 객체는 예외를 전파받지 않을 뿐, 어떤 예외가 발생했는지에 대한 정보를 자식 코루틴으로부터 전달받는다.
     *
     * 따라서 만약 SupervisorJob 객체와 CoroutineExceptionHandler 객체가 함께 설정되면 예외가 처리된다.
     *
     * 자식 코루틴이 부모 코루틴으로 예외를 전파하지 않고 전달만 하더라도 자식 코루틴에서 예외는 처리된 것으로 본다.
     */
    @Test
    fun SupervisorJob과_CoroutineExceptionHandler_함께_사용하기() = runBlocking<Unit> {
        val exceptionHandler =
            CoroutineExceptionHandler { coroutineContext, throwable -> println("[예외 발생] ${throwable}") }
        val supervisedScope = CoroutineScope(SupervisorJob() + exceptionHandler)
        supervisedScope.apply {
            launch(CoroutineName("Coroutine1")) {
                throw Exception("Coroutine1에 예외가 발생했습니다")
            }
            launch(CoroutineName("Coroutine2")) {
                delay(100L)
                println("[${Thread.currentThread().name}] 코루틴 실행")
            }
        }
        delay(1000L)
    }

    /**
     * CoroutineExceptionHandler는 예외가 마지막으로 처리되는 위치에서 예외를 처리할 뿐, 예외 전파를 제한하지 않는다.
     */
    @Test
    fun CoroutineExceptionHandler는_예외_전파를_제한하지_않는다() = runBlocking<Unit> {
        val exceptionHandler =
            CoroutineExceptionHandler { coroutineContext, throwable -> println("[예외 발생] ${throwable}") }
        launch(CoroutineName("Coroutine1") + exceptionHandler) {
            throw Exception("Coroutine1에 예외가 발생했습니다")
        }
    }

    @Test
    fun try_catch문을_사용해_코루틴_예외_처리하기() = runBlocking<Unit> {
        launch(CoroutineName("Coroutine1")) {
            try {
                throw Exception("Coroutine1에 예외가 발생했습니다")
            } catch (e: Exception) {
                println(e.message)
            }
        }
        launch(CoroutineName("Coroutine2")) {
            delay(100L)
            println("Coroutine2 실행 완료")
        }
    }

    /**
     * launch는 코루틴을 생성하는 데 사용되는 함수일 뿐으로
     * 람다식의 실행은 생성된 코루틴이 CoroutineDispatcher에 의해 스레드로 분배되는 시점에 일어나기 때문에
     * try catch문으로 코루틴 빌더 함수를 감싸도 예외를 잡지 못한다.
     *
     * 즉 try catch문은 launch 코루틴 빌더 함수 자체의 실행만 체크하며, 람다식은 예외 처리 대상이 아니다.
     */
    @Test
    fun 코루틴_빌더_함수에_대한_try_catch문은_코루틴의_예외를_잡지_못한다() = runBlocking<Unit> {
        try {
            launch(CoroutineName("Coroutine1")) {
                throw Exception("Coroutine1에 예외가 발생했습니다")
            }
        } catch (e: Exception) {
            println(e.message)
        }
        launch(CoroutineName("Coroutine2")) {
            delay(100L)
            println("Coroutine2 실행 완료")
        }
    }

    /**
     * async 코루틴 빌더 함수는 결괏값을 Deferred 객체로 감싸고 await 호출 시점에 결괏값을 노출한다.
     *
     * 이런 특성 때문에 코루틴 실행 도중 예외가 발생해 결괏값이 없다면 Deferred에 대한 await 호출 시 예외가 노출된다.
     *
     * async 코루틴 빌더를 호출해 만들어진 코루틴에서 예외가 발생할 경우에는 await 호출부에서 예외 처리가 될 수 있도록 해야 한다.
     */
    @Test
    fun async의_예외_노출() = runBlocking<Unit> {
        supervisorScope {
            val deferred: Deferred<String> = async(CoroutineName("Coroutine1")) {
                throw Exception("Coroutine1에 예외가 발생했습니다")
            }
            try {
                deferred.await()
            } catch (e: Exception) {
                println("[노출된 예외] ${e.message}")
            }
        }
    }

    /**
     * async 코루틴 빌더 함수 사용시, await 함수 호출부 뿐아니라 async 코루틴 빌더 함수도 예외를 처리 해야한다.
     */
    @Test
    fun async의_예외_전파() = runBlocking<Unit> {
        supervisorScope {
            async(CoroutineName("Coroutine1")) {
                throw Exception("Coroutine1에 예외가 발생했습니다")
            }
            launch(CoroutineName("Coroutine2")) {
                delay(100L)
                println("[${Thread.currentThread().name}] 코루틴 실행")
            }
        }
    }

    /**
     * 코루틴은 CancellationException 예외가 발생해도 부모 코루틴으로 전파되지 않는다.
     *
     * CancellationException은 코루틴의 취소에 사용되는 특별한 예외이다.
     */
    @Test
    fun 전파되지_않는_CancellationException() = runBlocking<Unit> {
        launch(CoroutineName("Coroutine1")) {
            launch(CoroutineName("Coroutine2")) {
                throw CancellationException()
            }
            delay(100L)
            println("[${Thread.currentThread().name}] 코루틴 실행")
        }
        delay(100L)
        println("[${Thread.currentThread().name}] 코루틴 실행")
    }

    /**
     * Job 객체에 대해 cancel 함수를 호출하면 CancellationException의 서브 클래스인 JobCancellationException을 발생시켜 코루틴을 취소시킨다.
     */
    @Test
    fun 코루틴_취소시_사용되는_JobCancellationException() = runBlocking<Unit> {
        val job = launch {
            delay(1000L)
        }
        job.invokeOnCompletion { exception ->
            println(exception)
        }
        job.cancel()
    }

    /**
     * 코루틴 라이브러리는 제한 시간을 두고 작업을 실행할 수 있도록 만드는 withTimeOut 함수를 제공한다.
     *
     * ``` kotlin
     * public suspend fun <T> withTimeout(timeMillis: Long, block: suspend CoroutineScope.() -> T): T
     * ```
     * 작업이 주어진 시간 내에 완료되지 않으면 TimeoutCancellationException을 발생시키는데 이는 CancellationException의 서브 클래스이다.
     */
    @Test
    fun withTimeOut_사용해_코루틴의_실행_시간_제한하기() = runBlocking<Unit>(CoroutineName("Parent Coroutine")) {
        launch(CoroutineName("Child Coroutine")) {
            withTimeout(1000L) {
                delay(2000L)
                println("[${Thread.currentThread().name}] 코루틴 실행")
            }
        }
        delay(2000L)
        println("[${Thread.currentThread().name}] 코루틴 실행")
    }

    /**
     * kotlinx.coroutines.TimeoutCancellationException: Timed out waiting for 1000 ms
     */
    @Test
    fun TimeoutCancellationException을_try_catch문으로_처리() = runBlocking<Unit>(CoroutineName("Parent Coroutine")) {
        try {
            withTimeout(1000L) {
                delay(2000L)
                println("[${Thread.currentThread().name}] 코루틴 실행")
            }
        } catch (e: Exception) {
            println(e)
        }
    }

    @Test
    fun withTimeOutOrNull_사용해_실행_시간_초과_시_null_반환_받기() = runBlocking<Unit>(CoroutineName("Parent Coroutine")) {
        launch(CoroutineName("Child Coroutine")) {
            val result = withTimeoutOrNull(1000L) {
                delay(2000L)
                return@withTimeoutOrNull "결과"
            }
            println(result)
        }
    }
}