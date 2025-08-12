import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RepeatAddUseCase {

    /**
     * Dispatchers.Default -> CPU 바운드 작업을 위한 백그라운드 스레드로 전환
     * 입력된 repeatTime 만큼 result에 1을 반복해 더하고 반환
     */
    suspend fun add(repeatTime: Int): Int = withContext(Dispatchers.Default) {
        var result = 0
        repeat(repeatTime) {
            result += 1
        }
        return@withContext result
    }

}