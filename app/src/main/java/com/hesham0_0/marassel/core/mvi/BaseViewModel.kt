package com.hesham0_0.marassel.core.mvi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class BaseViewModel<S : UiState, E : UiEvent, F : UiEffect>(
    initialState: S
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)

    val state: StateFlow<S> = _state.asStateFlow()

    protected val currentState: S
        get() = _state.value

    private val _effects = Channel<F>(capacity = Channel.BUFFERED)

    val effects = _effects.receiveAsFlow()

    abstract fun onEvent(event: E)

    protected fun setState(reducer: S.() -> S) {
        _state.update(reducer)
    }

    protected fun setEffect(effect: F) {
        viewModelScope.launch {
            _effects.send(effect)
        }
    }

    protected fun launch(
        onError: ((Throwable) -> Unit)? = null,
        block: suspend () -> Unit
    ) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                onError?.invoke(e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        _effects.close()
    }
}