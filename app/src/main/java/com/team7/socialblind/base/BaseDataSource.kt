package com.team7.elbess.base

import androidx.paging.PositionalDataSource
import timber.log.Timber

abstract class BaseDataSource<T> : PositionalDataSource<T>() {

    protected lateinit var pageCallback: PageCallback<T>

    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<T>) {
        Timber.v("load range [${params.startPosition},${params.startPosition + params.loadSize}")
        pageCallback = object : PageCallback<T> {
            override fun onResult(items: List<T>) {
                callback.onResult(items)
            }
        }
        loadPage(LoadParam(params.startPosition, params.loadSize))
    }

    override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<T>) {
        Timber.v("loadInitial size : ${params.pageSize}, reqested loaded size ${params.requestedLoadSize},start position ${params.requestedStartPosition}")
        pageCallback = object : PageCallback<T> {
            override fun onResult(items: List<T>) {
                callback.onResult(items, items.size)
            }
        }
        startInteractor()
        loadPage(LoadParam(params.requestedStartPosition, params.pageSize))
    }

    abstract fun startInteractor()

    abstract fun loadPage(loadParam: LoadParam)

    abstract fun dispose()

    data class LoadParam(val start: Int, val size: Int)

    interface PageCallback<T> {
        fun onResult(items: List<T>)
    }
}