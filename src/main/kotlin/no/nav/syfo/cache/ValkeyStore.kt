package no.nav.syfo.cache

import no.nav.syfo.util.configuredJacksonMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import redis.clients.jedis.JedisPool
import redis.clients.jedis.exceptions.JedisConnectionException

class ValkeyStore(private val jedisPool: JedisPool) {

    private val log: Logger = LoggerFactory.getLogger("no.nav.syfo.cache")
    private val mapper = configuredJacksonMapper()

    fun <T> getObject(key: String, clazz: Class<T>): T? {
        val value = get(key)
        return if (value != null) {
            mapper.readValue(value, clazz)
        } else {
            null
        }
    }

    fun <T> getListObject(key: String, clazz: Class<T>): List<T>? {
        val value = get(key)
        return if (value != null) {
            mapper.readValue(value, mapper.typeFactory.constructCollectionType(ArrayList::class.java, clazz))
        } else {
            null
        }
    }

    fun get(key: String): String? {
        try {
            jedisPool.resource.use { jedis -> return jedis.get(key) }
        } catch (e: JedisConnectionException) {
            log.warn("Got connection error when fetching from redis! Continuing without cached value", e)
            return null
        }
    }

    fun <T> setObject(key: String, value: T, expireSeconds: Long) {
        set(key, mapper.writeValueAsString(value), expireSeconds)
    }

    fun set(key: String, value: String, expireSeconds: Long) {
        try {
            jedisPool.resource.use { jedis ->
                jedis.setex(
                    key,
                    expireSeconds,
                    value,
                )
            }
        } catch (e: JedisConnectionException) {
            log.warn("Got connection error when storing in redis! Continue without caching", e)
        }
    }
}
