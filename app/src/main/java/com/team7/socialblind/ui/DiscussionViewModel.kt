package com.team7.socialblind.ui

import com.team7.elbess.base.State
import com.team7.socialblind.base.BaseViewModel
import com.team7.socialblind.models.Discussion
import com.team7.socialblind.repo.DiscussionFailure
import com.team7.socialblind.repo.DiscussionRepository
import com.team7.socialblind.util.*
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

data class DiscussionState(val discusion : Async<Discussion> = Uninitialized): State
class DiscussionViewModel() :BaseViewModel<DiscussionState>(DiscussionState()){
    private lateinit var repository: DiscussionRepository
    fun initialize(repository: DiscussionRepository){
        this.repository = repository
        setDiscussionState(Loading())
        repository.observeMessages().observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe({
                setDiscussionState(Success(it))
            }, {
                setDiscussionState(Fail(DiscussionFailure))
            })
    }
    fun getMessageDiscussion(){

    }
    fun setDiscussionState(state:Async<Discussion>){
        setState {
            copy(discusion = state)
        }
    }




}