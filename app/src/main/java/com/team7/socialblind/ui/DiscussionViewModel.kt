package com.team7.socialblind.ui

import androidx.lifecycle.viewModelScope
import com.team7.elbess.base.State
import com.team7.socialblind.base.BaseViewModel
import com.team7.socialblind.models.Discussion
import com.team7.socialblind.repo.DiscussionFailure
import com.team7.socialblind.repo.DiscussionRepository
import com.team7.socialblind.repo.None
import com.team7.socialblind.util.*
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import timber.log.Timber

data class DiscussionState(val discusion : Async<Discussion> = Uninitialized,
                           val timeFinishedEvent: Event<None>? = null ,
                           val timeLeft :Event<Long>? = null ,
                           val onNewMassageSent : Event<None>?  = null): State
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
        getCreatedAt()
    }
    fun setDiscussionState(state:Async<Discussion>){
        setState {
            copy(discusion = state)
        }
    }
    private fun getCreatedAt(){
        viewModelScope.launch(Dispatchers.Main) {
            val either  = async(Dispatchers.IO){
                repository.getLeftSeconds()
            }.await()
            either.either({
                Timber.e("Failure")
            }, {
                setState {
                    if(it<0){
                        repository.deleteTime()
                        copy(timeFinishedEvent = Event(None()))
                    }else{
                        copy(timeLeft =  Event(it))
                    }
                }
            })
        }
    }

    fun sendMessage(text:String){
        viewModelScope.launch(Dispatchers.Main){
            val either  = async(Dispatchers.IO) {
                repository.sendMessage(text)
            }.await()
            either.either({
              Timber.e("Failure")
            }, {
                setState {
                    copy(onNewMassageSent = Event(None()))
                }
            })

        }
    }
    fun setTimeFinished(){
        repository.deleteTime()
        setState {
            copy(timeFinishedEvent =  Event(None()))
        }
    }
    fun changeSubject(){

    }



}