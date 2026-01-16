package com.harsh.myapplication

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform