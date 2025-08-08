import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.coroutines.resume
import kotlin.test.Test

class AdvancedCoroutineTests {

    /**
     * 메모리 가시성 문제
     * - 스레드가 변수를 읽는 메모리 공간에 관한 문제로 CPU 캐시와 메인 메모리 등으로 이뤄지는 하드웨어의 메모리 구조와 연관돼 있다.
     * - 스레드가 변수를 변경시킬때 메인 메모리가 아닌 CPU 캐시를 사용할 경우
     *   CPU 캐시의 값이 메인 메모리에 전파되는 데 약간의 시간이 걸려 CPU 캐시와 메인 메모리 간에 데이터 불일치 문제가 발생한다.
     * - 따라서 다른 스레드에서 해당 변수를 읽을 때 변수가 변경된 것을 확인하지 못할 수 있다.
     *
     * 경쟁 상태 문제
     * - 2개의 스레드가 동시에 값을 읽고 업데이트 시키면 같은 연산이 두 번 일어난다.
     */
    @Test
    fun 가변_변수를_사용할_때의_문제점() = runBlocking<Unit> {
        var count = 0

        withContext(Dispatchers.Default) {
            repeat(10_000) {
                launch {
                    count += 1
                }
            }
        }

        println("count = ${count}")
    }


    /**
     * 코틀린에서 메모리 가시성 문제를 해결하기 위해서는 @Volatile 어노테이션을 사용하면 된다.
     *
     * @Volatile 어노테이션이 설정된 변수를 읽고 쓸 때는 CPU 캐시 메모리를 사용하지 않는다.
     *
     * 즉, 각 스레드는 count 변수의 값을 변경시키는 데 CPU 캐시 메모리를 사용하지 않고 메인 메모리를 사용한다.
     */
    @Volatile
    var volatileCount = 0

    @Test
    fun Volatile_사용해_공유_상태에_대한_메모리_가시성_문제_해결하기() = runBlocking<Unit> {


        withContext(Dispatchers.Default) {
            repeat(10_000) {
                launch {
                    volatileCount += 1
                }
            }
        }

        println("count = ${volatileCount}")
    }

    /**
     * 동시 접근을 제한하는 간단한 방법은 공유 변수의 변경 가능 지점을 임계 영역(Critical Section)으로 만들어 동시 접근을 제한하는 것이다.
     *
     * 코루틴에서는 코루틴에 대한 임계 영역을 만들기 위한 Mutex 객체를 제공한다.
     */
    @Test
    fun Mutex_사용해_동시_접근_제한하기() = runBlocking<Unit> {
        var count = 0
        val mutex = Mutex()

        withContext(Dispatchers.Default) {
            repeat(10_000) {
                launch {
                    mutex.withLock {
                        count += 1
                    }
                }
            }
        }

        println("count = ${count}")
    }

    /**
     *  Mutext 객체에 락이 걸려 있으면 코루틴은 기존의 락이 해제될 때까지 스레드를 양보하고 일시 중단한다.
     *
     *  반면 ReentrantLock 객체에 대해 lock을 호출했을 때 이미 다른 스레드에서 락을 획득했다면
     *  코루틴은 락이 해제될 때까지 lock을 호출한 스레드를 블로킹하고 기다린다.
     *
     *  이런 특성 때문에 코루틴에서는 ReentrantLock 객체 대신 Mutex 객체를 사용하는 것이 권장된다.
     */
    @Test
    fun ReetrantLock_사용해_동시_접근_제한하기() = runBlocking<Unit> {
        var count = 0
        val reentrantLock = ReentrantLock()

        withContext(Dispatchers.Default) {
            repeat(10_000) {
                launch {
                    reentrantLock.lock()
                    count += 1
                    reentrantLock.unlock()
                }
            }
        }

        println("count = ${count}")
    }

    /**
     * 스레드 간에 공유 상태를 사용해 생기는 문제점은 복수의 스레드가 공유 상태에 동시에 접근할 수 있기 때문에 일어난다.
     *
     * 따라서 공유 상태에 접근할 때 하나의 전용 스레드만 사용하도록 강제하면 공유 상태에 동시에 접근하는 문제를 해결할 수 있다.
     */
    @Test
    fun 공유_상태_변경을_위해_전용_스레드_사용하기() = runBlocking<Unit> {
        var count = 0
        val countChangeDispatcher = newSingleThreadContext("CountChangeThread")

        withContext(Dispatchers.Default) {
            repeat(10_000) {
                launch {
                    withContext(countChangeDispatcher) {
                        count += 1
                    }
                }
            }
        }

        println("count = ${count}")
    }

    /**
     * 원자성 있는 객체는 여러 스레드가 동시에 접근해도 한 번에 하나의 스레드만 접근할 수 있도록 제한한다.
     *
     * 코루틴이 원자성 있는 객체에 접근할 때 이미 다른 스레드의 코루틴이 해당 객체에 대한 연산을 실행 중인 경우
     * 코루틴은 스레드를 블로킹하고 연산 중인 스레드가 연산을 모두 수행할 때까지 기다린다.
     */
    @Test
    fun 원자성_있는_데이터_구조를_사용한_경쟁_상태_문제_해결() = runBlocking<Unit> {
        val count = AtomicInteger(0)

        withContext(Dispatchers.Default) {
            repeat(10_000) {
                launch {
                    count.getAndUpdate { it + 1 }
                }
            }
        }

        println("count = ${count}")
    }

    /**
     * launch의 start 인자로 아무런 값이 전달되지 않으면 기본 실행 옵션인 CoroutineStart.DEFAULT가 설정된다.
     *
     * 메인 스레드에서 실행되는 runBlocking 코루틴에 의해 launch 코루틴의 실행이 즉시 예약된다.
     *
     * 하지만 runBlocking 코루틴이 메인 스레드를 양보하지 않고 계속해서 실행되므로,
     * runBlocking 코루틴에 의해 작업2가 출력되고 나서 메인 스레드가 자유로워져 launch 코루틴이 실행된다.
     */
    @Test
    fun CoroutineStart_DEFAULT() = runBlocking<Unit> {
        launch {
            println("작업1")
        }
        println("작업2")
    }

    /**
     * CoroutineStart.ATOMIC 옵션을 적용하면 해당 옵션이 적용된 코루틴은 실행 대기 상태에서 취소되지 않는다.
     */
    @Test
    fun CoroutineStart_ATOMIC() = runBlocking<Unit> {
        val job = launch(start = CoroutineStart.ATOMIC) {
            println("작업1")
        }
        job.cancel()
        println("작업2")
    }

    /**
     * CoroutineStart_UNDISPATCHED 옵션이 적용된 코루틴은
     * CoroutineDispatcher 객체의 작업 대기열을 거치지 않고 호출자의 스레드에서 즉시 실행된다.
     *
     * 처음 코루틴 빌더가 호출됐을 때만 CoroutineDispatcher 객체를 거치지 않고 실행된다.
     * - 만약 코루틴 내부에서 일시 중단 후 재개되면 CoroutineDispatcher 객체를 거쳐 실행된다.
     */
    @Test
    fun CoroutineStart_UNDISPATCHED() = runBlocking<Unit> {
        launch(start = CoroutineStart.UNDISPATCHED) {
            println("작업1")
        }
        println("작업2")
    }

    /**
     * 무제한 디스패처란 코루틴 자신을 실행시킨 스레드에서 즉시 실행하도록 만드는 디스패처이다.
     *
     * 이때 호출된 스레드가 무엇이든지 상관없기 때문에 실행 스레드가 제한되지 않으므로 무제한 디스패처라는 이름이 붙었다.
     */
    @Test
    fun 무제한_디스패처() = runBlocking<Unit> {
        launch(Dispatchers.Unconfined) {
            println("launch 코루틴 실행 스레드: ${Thread.currentThread().name}")
        }
    }

    /**
     * 무제한 디스패처를 사용하는 코루틴은 현재 자신을 실행한 스레드를 즉시 점유해 실행되며, 이는 제한된 디스패처를 사용하는 코루틴의 동작과 대조된다.
     *
     * 제한된 디스패처는 코루틴의 실행을 요청 받으면 작업 대기열에 적재한 후 해당 디스패처에서 사용할 수 있는 스레드 중 하나로 보내 실행되도록한다.
     */
    @Test
    fun 무제한_디스패처는_코루틴이_자신을_생성한_스레드에서_즉시_실행된다() = runBlocking<Unit>(Dispatchers.IO) {
        println("runBlocking 코루틴 실행 스레드: ${Thread.currentThread().name}")
        launch(Dispatchers.Unconfined) {
            println("launch 코루틴 실행 스레드: ${Thread.currentThread().name}")
        }
    }

    @Test
    fun 무제한_디스패처를_사용해_실행되는_코루틴은_스레드_스위칭_없이_즉시_실행된다() = runBlocking<Unit> {
        println("작업1")
        launch(Dispatchers.Unconfined) {
            println("작업2")
        }
        println("작업3")
    }

    /**
     * 무제한 디스패처를 사용해 실행되는 코루틴은 자신을 실행시킨 스레드에서 스레드 스위칭 없이 즉시 실행되지만
     * 일시 중단 전까지만 자신을 실행시킨 스레드에서 실행된다.
     *
     * DefaultExecutor 스레드는 delay 함수를 실행하는 스레드로 delay 함수가 일시 중단을 종료하고 코루틴을 재개할 때 사용하는 스레드이다.
     */
    @Test
    fun 중단_시점_이후의_재개는_코루틴을_재개하는_스레드에서_한다() = runBlocking<Unit> {
        launch(Dispatchers.Unconfined) {
            println("일시 중단 전 실행 스레드: ${Thread.currentThread().name}")
            delay(100L)
            println("일시 중단 후 실행 스레드: ${Thread.currentThread().name}")
        }
    }

    /**
     * CoroutineStart.UNDISPATCHED 옵션이 적용된 코루틴은 시작과 재개가 모두 메인 스레드에서 일어나고,
     * Dispatcher.Unconfined를 사용해 실행된 코루틴은 시작은 메인 스레드에서 됐지만
     * 재개는 delay를 실행하는 데 사용하는 DefaultExecutor 스레드에서 재개된다.
     */
    @Test
    fun CoroutineStart_UNDISPATCHER와_무제한_디스패처의_차이() = runBlocking<Unit> {
        println("runBlocking 코루틴 실행 스레드: ${Thread.currentThread().name}")
        launch(start = CoroutineStart.UNDISPATCHED) {
            println("[CoroutineStart.UNDISPATCHED] 코루틴이 시작 시 사용하는 스레드: ${Thread.currentThread().name}")
            delay(100L)
            println("[CoroutineStart.UNDISPATCHED] 코루틴이 재개 시 사용하는 스레드: ${Thread.currentThread().name}")
        }.join()

        launch(context = Dispatchers.Unconfined) {
            println("[Dispatchers.Unconfined] 코루틴이 시작 시 사용하는 스레드: ${Thread.currentThread().name}")
            delay(100L)
            println("[Dispatchers.Unconfined] 코루틴이 재개 시 사용하는 스레드: ${Thread.currentThread().name}")
        }.join()
    }

    /**
     * 코루틴은 코드를 실행하는 도중 일시 중단하고 다른 작업으로 전환한 후 필요한 시점에 다시 실행을 재개하는 기능을 지원한다.
     *
     * 코루틴이 일시 중단을 하고 재개하기 위해서는 코루틴의 실행 정보가 어딘가에 저장돼 전달해야 한다.
     *
     * 코틀린은 코루틴의 실행 정보를 저장하고 전달하는 데 CPS(Continuation Passing Style)라고 불리는 프로그래밍 방식을 채택하고 있다.
     *
     * CPS를 채택한 코틀린은 코루틴에서 이어서 실행해야 하는 작업 전달을 위해 Continuation 객체를 제공한다.
     *
     * Continuation 객체는 코루틴의 일시 중단 시점에 코루틴의 실행 상태를 저장하며, 여기에는 다음에 실행해야 할 작업에 대한 정보가 포함된다.
     */
    @Test
    fun Continuation_Passing_Style() {
    }

    /**
     * 코루틴에서 일시 중단이 일어나면 Continuation 객체에 실행 정보가 저장되며,
     * 일시 중단된 코루틴은 Continuation 객체에 대해 reseme 함수가 호출돼야 재개된다.
     */
    @Test
    fun 코루틴의_일시_중단과_재개로_알아보는_Continuation() = runBlocking<Unit> {
        println("runBlocking 코루틴 일시 중단 호출")
        suspendCancellableCoroutine<Unit> { continuation: CancellableContinuation<Unit> ->
            println("일시 중단 시점의 runBlocking 코루틴 실행 정보: ${continuation.context}")
            continuation.resume(Unit)
        }
        println("일시 중단된 코루틴이 재개되지 않아 실행되지 않는 코드")
    }
}