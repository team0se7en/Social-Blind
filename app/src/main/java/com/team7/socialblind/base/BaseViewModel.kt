package com.team7.socialblind.base

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.roacult.kero.team7.jstarter_domain.exception.Failure
import com.roacult.kero.team7.jstarter_domain.interactors.EitherInteractor
import com.roacult.kero.team7.jstarter_domain.interactors.ObservableEitherInteractor
import com.roacult.kero.team7.jstarter_domain.interactors.ObservableInteractor
import com.roacult.kero.team7.jstarter_domain.interactors.launchInteractor
import com.team7.elbess.base.State
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

abstract class BaseViewModel <S : State> (initialState: S) : ViewModel() {

    protected val state: MutableLiveData<S> by lazy {
        val liveData: MutableLiveData<S> = MutableLiveData()
        liveData.value = initialState
        liveData
    }

    private val job = Job()
    private val disposable: CompositeDisposable = CompositeDisposable()

    /**
     * scope to launch interactors
     * */
    protected val scope = CoroutineScope(Dispatchers.Main + job)

    /**
     * this method will change state in live date
     * */
    protected fun setState(stateChanger: S.() -> S) {
        state.value = state.value?.stateChanger()
    }

    /**
     * observe state in live date
     * */
    fun observe(lifecycleOwner: LifecycleOwner, observer: (S) -> Unit) {
        state.observe(lifecycleOwner, Observer(observer))
    }
    fun observeNotLifecycle(observer: (S) -> Unit) {
        state.observeForever(observer)
    }

    /**
     * this method will not change state
     * it's only used to handle current value of  state
     * */
    fun withState(stateHandler: (S) -> Unit) {
        stateHandler(state.value!!)
    }

    fun dispose() {
        if (job.isActive)
            job.cancel()
        disposable.dispose()
    }

    override fun onCleared() {
        super.onCleared()
        dispose()
    }

    /**
     * launch observable either interactor
     * @param interactor interactor you want launch
     * @param errorHandler callback of errors
     * @param dataHandler callback of results
     * @return Disposable to dispose stream
     *
     * Note : all interactors launched by this method
     * will automatically disposed when viewModel destroyed
     * */
    protected fun <P, R, F : Failure> launchEitherObservableInteractor(
        interactor: ObservableEitherInteractor<R, P, F>,
        param: P,
        errorHandler: (F) -> Unit,
        dataHandler: (R) -> Unit
    ): Disposable {
        val dispos = interactor.observe(param) {
            it.either(errorHandler, dataHandler)
        }
        disposable.add(dispos)
        return dispos
    }



    protected fun <P, Type> launchObservableInteractor(
        interactor: ObservableInteractor<Type, P>,
        param: P,
        errorHandler: (Throwable) -> Unit,
        dataHandler: (Type) -> Unit
    ): Disposable {
        val dispos = interactor.observe(param, errorHandler, dataHandler)
        disposable.add(dispos)
        return dispos
    }
    protected fun <P, R, F : Failure> launchEitherInteractor(
        interactor: EitherInteractor<P, R, F>,
        param: P,
        errorHandler: (F) -> Unit,
        dataHandler: (R) -> Unit
    ) {
        scope.launchInteractor(interactor, param) {
            it.either(errorHandler, dataHandler)
        }
    }

    protected fun <T> launchInteractor(observable: Observable<T>, onError: ((Throwable) -> Unit)? = null, onNext: (T) -> Unit): Disposable {

        val dispos = if (onError != null) observable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(onNext, onError)
        else observable.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(onNext)

        disposable.add(dispos)
        return dispos
    }
}
