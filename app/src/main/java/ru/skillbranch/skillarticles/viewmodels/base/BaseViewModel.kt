package ru.skillbranch.skillarticles.viewmodels.base

import android.os.Bundle
import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.*
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import ru.skillbranch.skillarticles.data.remote.err.ApiError
import ru.skillbranch.skillarticles.data.remote.err.NoNetworkError
import java.net.SocketTimeoutException

abstract class BaseViewModel<T: IViewModelState>(
    private val handleState: SavedStateHandle,
    initState: T
) : ViewModel() {
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    val notifications = MutableLiveData<Event<Notify>>()

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    val navigation = MutableLiveData<Event<NavigationCommand>>()

    val permissions = MutableLiveData<Event<List<String>>>()


    private val loading = MutableLiveData<Loading>(Loading.HIDE_LOADING)


    /***
     * Инициализация начального состояния аргументом конструктоа, и объявления состояния как
     * MediatorLiveData - медиатор исспользуется для того чтобы учитывать изменяемые данные модели
     * и обновлять состояние ViewModel исходя из полученных данных
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    val state: MediatorLiveData<T> = MediatorLiveData<T>().apply {
        value = initState
    }

    /***
     * getter для получения not null значения текущего состояния ViewModel
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    val currentState
        get() = state.value!!


    /***
     * лямбда выражение принимает в качестве аргумента текущее состояние и возвращает
     * модифицированное состояние, которое присваивается текущему состоянию
     */
    @UiThread
    protected inline fun updateState(update: (currentState: T) -> T) {
        val updatedState: T = update(currentState)
        state.value = updatedState
    }

    /***
     * функция для создания уведомления пользователя о событии (событие обрабатывается только один раз)
     * соответсвенно при изменении конфигурации и пересоздании Activity уведомление не будет вызвано
     * повторно
     */
    @UiThread
    protected fun notify(content: Notify) {
        notifications.value = Event(content)
    }

    open fun navigate(command: NavigationCommand){
        navigation.value = Event(command)
    }

    /**
     * отображение индикатора загрузки
     */
    protected fun showLoading(loadingType: Loading = Loading.SHOW_LOADING){
        loading.value = loadingType
    }

    protected fun hideLoading(){
        loading.value = Loading.HIDE_LOADING
    }

    /***
     * более компактная форма записи observe() метода LiveData принимает последним аргумент лямбда
     * выражение обрабатывающее изменение текущего стостояния
     */
    fun observeState(owner: LifecycleOwner, onChanged: (newState: T) -> Unit) {
        state.observe(owner, Observer { onChanged(it!!) })
    }

    /***
     * более компактная форма записи observe() метода LiveData вызывает лямбда выражение обработчик
     * только в том случае если уведомление не было уже обработанно ранее,
     * реализует данное поведение с помощью EventObserver
     */
    fun observeNotifications(owner: LifecycleOwner, onNotify: (notification: Notify) -> Unit) {
        notifications.observe(owner,
            EventObserver { onNotify(it) })
    }

    fun observeNavigation(owner: LifecycleOwner, onNavigate:(command: NavigationCommand) -> Unit){
        navigation.observe(owner,
            EventObserver{ onNavigate(it)})
    }

    fun observeLoading(owner: LifecycleOwner,onChanged: (newState: Loading) -> Unit){
        loading.observe(owner, Observer{onChanged(it)})
    }

    /***
     * функция принимает источник данных и лямбда выражение обрабатывающее поступающие данные источника
     * лямбда принимает новые данные и текущее состояние ViewModel в качестве аргументов,
     * изменяет его и возвращает модифицированное состояние, которое устанавливается как текущее
     */
    protected fun <S> subscribeOnDataSource(
        source: LiveData<S>,
        onChanged: (newValue: S, currentState: T) -> T?
    ) {
        state.addSource(source) {
            state.value = onChanged(it, currentState) ?: return@addSource
        }
    }

    fun saveState(){
        currentState.save(handleState)
    }

    fun restoreState(){
        val restoredState = currentState.restore(handleState) as T
        if(restoredState == currentState) return
        state.value = currentState.restore(handleState) as T
    }

    protected fun launchSafety(
        errHandler: ((Throwable) -> Unit)? = null,  // обработчик ошибок
        compHandler: ((Throwable?) -> Unit)? = null, // обработчик по завершении
        block: suspend CoroutineScope.() -> Unit  // основной обработчик - suspend fun ( лямбда )
    ){

        val errHand = CoroutineExceptionHandler{context, throwble ->
            errHandler?.invoke(throwble)?: when(throwble){
                is NoNetworkError -> notify(Notify.ErrorMessage("Network is not available, check internet connection",null,null))
                is SocketTimeoutException-> notify(
                    Notify.ActionMessage("Network timeout exception - please try again",
                        "Retry")
                    {launchSafety( errHandler,compHandler,block)}
                )
                is ApiError.InternalServerError-> notify(
                    Notify.ActionMessage(throwble.message,
                        "Retry")
                    {launchSafety( errHandler,compHandler,block)}
                )
                is ApiError -> notify(Notify.ErrorMessage(throwble.message,null, null))

                else -> notify(Notify.ErrorMessage(throwble.message ?:"Something goes wrong",null,null))
            }
        }

        (viewModelScope + errHand ).launch {
            // отобразить индикатор загрузки
            showLoading()
            block()
        }.invokeOnCompletion {
            // скрыть обработчик загрузки
            hideLoading()
            compHandler?.invoke(it)
        }
    }

    fun requestPermissions(requestedPermissions: List<String>){
        permissions.value = Event(requestedPermissions)
    }

    fun observerPermissions(owner: LifecycleOwner,handle:(permissions: List<String>) -> Unit){
        permissions.observe(owner,EventObserver{handle(it)})
    }
}

enum class Loading {
    SHOW_LOADING,HIDE_LOADING,SHOW_BLOCKING_LOADING
}

class Event<out E>(private val content: E) {
    var hasBeenHandled = false

    /***
     * возвращает контент который еще не был обработан иначе null
     */
    fun getContentIfNotHandled(): E? {
        return if (hasBeenHandled) null
        else {
            hasBeenHandled = true
            content
        }
    }

    fun peekContent(): E = content
}

/***
 * в качестве аргумента конструктора принимает лямбда выражение обработчик в аргумент которой передается
 * необработанное ранее событие получаемое в реализации метода Observer`a onChanged
 */
class EventObserver<E>(private val onEventUnhandledContent: (E) -> Unit) : Observer<Event<E>> {

    override fun onChanged(event: Event<E>?) {
        //если есть необработанное событие (контент) передай в качестве аргумента в лямбду
        // onEventUnhandledContent
        event?.getContentIfNotHandled()?.let {
            onEventUnhandledContent(it)
        }
    }
}

sealed class Notify(){
    abstract val message: String

    data class TextMessage(
        override val message: String
    ) : Notify()

    data class ActionMessage(
        override val message: String,
        val actionLabel: String,
        val actionHandler: (() -> Unit)
    ) : Notify()

    data class ErrorMessage(
        override val message: String,
        val errLabel: String?,
        val errHandler: (() -> Unit)?
    ) : Notify()
}

sealed class NavigationCommand(){
    data class To(
        val destination: Int,
        val args: Bundle? = null,
        val options: NavOptions? = null,
        val extras: Navigator.Extras? = null
    ): NavigationCommand()

    data class StartLogin(
        val privateDestination: Int? = null
    ): NavigationCommand()

    data class FinishLogin(
        val privateDestination: Int? = null
    ): NavigationCommand()
}