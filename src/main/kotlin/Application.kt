import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.security.MessageDigest
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.streams.toList

class Application

const val appVersion = "2.1.0"
val logger: Logger = LoggerFactory.getLogger("main")
val httpLogger: Logger = LoggerFactory.getLogger("OkHttpClient")
val httpLoggingInterceptor = HttpLoggingInterceptor { httpLogger.info(it) }.apply {
    setLevel(HttpLoggingInterceptor.Level.BODY)
}
val httpClient = OkHttpClient.Builder()
    .addInterceptor(httpLoggingInterceptor)
    .build()

fun main() {
    //签到时间
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    var now = LocalDateTime.now()
    var signDateTime = now.withHour(4)
        .withMinute(0)
        .withSecond(0)
        .minusSeconds(0)
    if (!signDateTime.isAfter(now)) {
        signDateTime = signDateTime.plusDays(1)
    }
    while (true) {
        logger.info("下次签到时间:${signDateTime.format(formatter)}")
        now = LocalDateTime.now()
        val duration = Duration.between(now, signDateTime)
        Thread.sleep(duration.toMillis())
        logger.info("开始执行签到")
        val cookies = getCookies()
        //开始签到
        for (cookie in cookies) {
            val task = runCatching {
                sign(cookie)
            }
            if (task.isFailure) {
                logger.error("签到失败")
            }
        }
        logger.info("签到完成")
        signDateTime = signDateTime.plusDays(1)
    }
}

/**
 * 执行签到任务
 */
fun sign(cookie: String) {
    repeatUntilSuccess(3, 2000) {
        val role = getRoles(cookie)
        //language=JSON
        val requestBody = """
        {
          "act_id": "e202009291139501",
          "region": "${role.region}",
          "uid": ${role.game_uid}
        }
    """.trimIndent()
        val request = Request.Builder()
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .url("https://api-takumi.mihoyo.com/event/bbs_sign_reward/sign")
            .addHeader("cookie", cookie)
            .addHeader("x-rpc-device_id", md5(cookie).replace("-", "").toUpperCase())
            .addHeader("x-rpc-client_type", "5")
            .addHeader("x-rpc-app_version", appVersion)
            .addHeader("DS", getDS())
            .build()
        val response = httpClient.newCall(request).execute()
        if (response.code != 200) {
            throw IOException("签到失败")
        }
        logger.info("签到结果 :${response.body?.string()}")
    }
}

/**
 * 重复执行任务 直到任务执行成功或者执行次数达到限制
 * @return 执行成功 返回true 否则返回false
 */
fun repeatUntilSuccess(times: Int, interval: Long, action: () -> Unit): Boolean {
    val logger = LoggerFactory.getLogger("repeatUntilSuccess")
    repeat(times) {
        try {
            action()
            return true
        } catch (t: Throwable) {
            logger.error("执行失败 ${interval}毫秒后重试", t)
            Thread.sleep(interval)
        }
    }
    return false
}


/**
 * 获取角色列表
 */
fun getRoles(cookie: String): GameRole {
    val request = Request.Builder()
        .url("https://api-takumi.mihoyo.com/binding/api/getUserGameRolesByCookie?game_biz=hk4e_cn")
        .addHeader("cookie", cookie)
        .build()
    val response = httpClient.newCall(request).execute()
    if (response.code != 200) {
        throw IOException("查询角色列表失败")
    }
    val json = Json.decodeFromString<JsonObject>(response.body!!.string())
    if (json["retcode"]!!.jsonPrimitive.intOrNull != 0) {
        throw IOException("查询角色列表失败 response:$json")
    }
    val role = json["data"]!!.jsonObject["list"]!!.jsonArray.first()
    return Json.decodeFromJsonElement(role)
}

fun getDS(): String {
    val range = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    val time = System.currentTimeMillis() / 1000
    val random = (1..6).map { range.random() }.joinToString("")
    val md5 = md5("salt=${md5(appVersion)}&t=${time}&r=$random")
    return "$time,$random,$md5"
}

/**
 * 读取cookies列表
 */
fun getCookies(): List<String> {
    val inputStream = Application::class.java.classLoader.getResourceAsStream("cookies.text")!!
    return inputStream.bufferedReader().lines().toList()
}


fun md5(str: String): String {
    return MessageDigest.getInstance("MD5").digest(str.toByteArray()).joinToString("") { "%02x".format(it) }
}

@Serializable
data class GameRole(
    val game_biz: String = "",
    val region: String = "",
    val game_uid: String = "",
    val nickname: String = "",
    val level: Int = 1,
    val is_chosen: Boolean = false,
    val region_name: String = "",
    val is_official: Boolean = true
)