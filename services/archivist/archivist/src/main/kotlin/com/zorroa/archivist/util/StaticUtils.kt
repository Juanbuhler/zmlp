package com.zorroa.archivist.util

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.Calendar
import java.util.Date
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom
import java.util.stream.IntStream

object StaticUtils {

    val mapper = jacksonObjectMapper()

    fun init() {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
        mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false)
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        mapper.configure(SerializationFeature.WRITE_ENUMS_USING_INDEX, true)
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
        mapper.configure(MapperFeature.USE_GETTERS_AS_SETTERS, false)
        mapper.dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z")
    }

    val UUID_REGEXP = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$", RegexOption.IGNORE_CASE) }

private const val SYMBOLS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0987654321"

fun randomString(length: Int = 16): String {
    val random = ThreadLocalRandom.current()
    val buf = CharArray(length)
    for (i in 0 until length) {
        buf[i] = SYMBOLS[random.nextInt(SYMBOLS.length)]
    }
    return String(buf)
}

/**
 * Return the given Date with minutes and seconds zeroed out.
 */
fun toHourlyDate(date: Date?): Date {
    val time: Calendar = Calendar.getInstance()
    time.timeInMillis = date?.time ?: Date().time
    time.set(Calendar.MINUTE, 0)
    time.set(Calendar.SECOND, 0)
    time.set(Calendar.MILLISECOND, 0)
    return Date(time.toInstant().toEpochMilli())
}

/**
 * Extension function for printing UUID chars
 */
fun UUID.prefix(size: Int = 8): String {
    return this.toString().substring(0, size)
}

/**
 * Extension function to check if a string is a UUID
 */
fun String.isUUID(): Boolean = StaticUtils.UUID_REGEXP.matches(this)
