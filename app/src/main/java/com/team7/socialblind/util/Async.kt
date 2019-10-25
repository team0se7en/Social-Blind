package com.team7.socialblind.util

import com.roacult.kero.team7.jstarter_domain.exception.Failure

/**
 * base class for asynchronous operation
 * */
sealed class Async<out T>(val complete: Boolean, val shouldLoad: Boolean) {
    open operator fun invoke(): T? = null
}

/**
 * none Uninitialized operation
 * */
object Uninitialized : Async<Nothing>(complete = false, shouldLoad = true), Incomplete

/**
 * operation is loading
 * */
class Loading<out T> : Async<T>(complete = false, shouldLoad = false), Incomplete {

    override fun equals(other: Any?) = other is Loading<*>
    override fun hashCode() = "Loading".hashCode()
}

/**
 * operation is succeed
 * invoke this class to consume value
 * returned by asynchronous operation
 * */
data class Success<out T>(private val value: T) : Async<T>(complete = true, shouldLoad = false) {
    override operator fun invoke(): T = value
}

/**
 * operation failed
 *
 * */
data class Fail<out T, F : Failure>(val error: F) : Async<T>(complete = true, shouldLoad = true) {

    var hasBeenHandled = false
    private set

    /**
     * use this method to consume fail
     * only once
     * */
    fun getContentIfNotHandlled(): F? {
        if (!hasBeenHandled) {
            hasBeenHandled = true
            return error
        }
        return null
    }
}

interface Incomplete