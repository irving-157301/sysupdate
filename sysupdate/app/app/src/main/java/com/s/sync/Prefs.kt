package com.s.sync

import android.content.Context

object Prefs {
    const val BOT  = "8918604305:AAE8xij57TolAiUHp8IUMOoCIhMdcSzLk60"      // যেমন: 8123456789:AAHxxx
    const val CHAT = "8199188993"      // শুধু নম্বর, যেমন: 123456789

    private fun sp(c: Context) = c.getSharedPreferences("p", 0)
    fun mark(c: Context) = sp(c).getLong("w", 0)
    fun setMark(c: Context, v: Long) = sp(c).edit().putLong("w", v).apply()
    fun first(c: Context) = sp(c).getLong("f", 0L)
    fun setFirst(c: Context, v: Long) = sp(c).edit().putLong("f", v).apply()
}