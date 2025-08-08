import kotlinx.coroutines.*

fun getElapsedTime(startTime: Long): String =
    "지난 시간: ${System.currentTimeMillis() - startTime}ms"

fun printJobState(job: Job) {
    println(
        "Job State\n" +
                "isActive >> ${job.isActive}\n" +
                "isCancelled >> ${job.isCancelled}\n" +
                "isCompleted >> ${job.isCompleted}\n"
    )
}

fun executeAfterJobCancelled() {}

suspend fun delayAndPrint(keyword: String) {
    delay(1000L)
    println(keyword)
}

suspend fun delayAndPrintHelloWorld() {
    delay(1000L)
    println("Hello World")
}

suspend fun searchByKeyword(keyword: String): Array<String> = coroutineScope {
    val dbResultsDeferred = async {
        searchFromDB(keyword)
    }
    val serverResultsDeferred = async {
        searchFromServer(keyword)
    }
    return@coroutineScope arrayOf(*dbResultsDeferred.await(), *serverResultsDeferred.await())
}

suspend fun searchByKeyword2(keyword: String): Array<String> = supervisorScope {
    val dbResultsDeferred = async {
        throw Exception("dbResultsDeferred에서 예외가 발생했습니다.")
        searchFromDB(keyword)
    }
    val serverResultsDeferred = async {
        searchFromServer(keyword)
    }

    val dbResults = try {
        dbResultsDeferred.await()
    } catch (e: Exception) {
        arrayOf()
    }

    val serverResults = try {
        serverResultsDeferred.await()
    } catch (e: Exception) {
        arrayOf()
    }

    return@supervisorScope arrayOf(*dbResults, *serverResults)
}

suspend fun searchFromDB(keyword: String): Array<String> {
    delay(1000L)
    return arrayOf("[DB]${keyword}1", "[DB]${keyword}2")
}

suspend fun searchFromServer(keyword: String): Array<String> {
    delay(1000L)
    return arrayOf("[Server]${keyword}1", "[Server]${keyword}2")
}