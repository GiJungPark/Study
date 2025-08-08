import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
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


    @Test
    fun 무제한_디스패처는_코루틴이_자신을_생성한_스레드에서_즉시_실행된다() = runBlocking<Unit> {

    }
}