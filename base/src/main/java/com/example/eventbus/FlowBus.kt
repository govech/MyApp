package com.example.eventbus

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


/**
 * 扩展函数：订阅 FlowBus 中的普通事件
 */
inline fun <reified T : Any> LifecycleOwner.observeFlowBusEvent(
    state: Lifecycle.State = Lifecycle.State.STARTED,
    crossinline onEvent: (T) -> Unit
) {
    lifecycleScope.launch {
        lifecycle.repeatOnLifecycle(state) {
            FlowBus.subscribe<T>().collect {
                onEvent(it)
            }
        }
    }
}


/**
 * 扩展函数：订阅 FlowBus 中的粘性事件，支持立即移除或生命周期自动移除
 * @param removeAfterConsume 是否在消费后立即移除粘性事件（默认否）
 * @param autoRemoveOnDestroy 是否在生命周期销毁时自动移除（默认是）
 */
inline fun <reified T : Any> LifecycleOwner.observeFlowBusStickyEvent(
    state: Lifecycle.State = Lifecycle.State.STARTED,
    removeAfterConsume: Boolean = false,
    autoRemoveOnDestroy: Boolean = true,
    crossinline onEvent: (T) -> Unit
) {
    val clazz = T::class.java

    // 生命周期销毁时自动移除
    if (autoRemoveOnDestroy) {
        lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if (event == Lifecycle.Event.ON_DESTROY) {
                    runBlocking {
                        FlowBus.removeStickyEvent(clazz)
                    }
                    source.lifecycle.removeObserver(this)
                }
            }
        })
    }

    // 订阅事件
    lifecycleScope.launch {
        lifecycle.repeatOnLifecycle(state) {
            FlowBus.subscribeSticky<T>()
                .onEach { event ->
                    onEvent(event)
                    // 消费后立即移除（根据参数控制）
                    if (removeAfterConsume) {
                        FlowBus.removeStickyEvent(clazz)
                    }
                }
                .collect()
        }
    }
}

object FlowBus {

    private val _events = MutableSharedFlow<Any>(
        replay = 0,
        extraBufferCapacity = 64
    )

    private val stickyEvents = mutableMapOf<Class<*>, MutableStateFlow<Any>>()
    private val mutex = Mutex()

    /**
     * 发送普通事件（挂起）
     */
    suspend fun post(event: Any) {
        _events.emit(event)
        logger?.invoke(event)
    }

    /**
     * 发送普通事件（非挂起，可能丢失）
     */
    fun tryPost(event: Any): Boolean {
        val success = _events.tryEmit(event)
        if (success) logger?.invoke(event)
        return success
    }

    /**
     * 发送粘性事件（同时发出普通事件）
     */
    suspend fun postSticky(event: Any) {
        mutex.withLock {
            val clazz = event.javaClass
            val flow = stickyEvents.getOrPut(clazz) { MutableStateFlow(event) }
            flow.value = event // 更新现有 StateFlow 的值
        }
        post(event)
    }

    /**
     * 订阅普通事件（通过 Class）
     */
    fun <T : Any> subscribe(clazz: Class<T>): Flow<T> {
        return _events
            .filter { clazz.isInstance(it) }
            .map { it as T } // 确保非空转换
    }

    /**
     * 订阅普通事件（inline）
     */
    inline fun <reified T : Any> subscribe(): Flow<T> = subscribe(T::class.java)

    /**
     * 订阅粘性事件（通过 Class）
     */
    fun <T : Any> subscribeSticky(clazz: Class<T>): Flow<T> = flow {
        // 安全获取粘性流并确保非空
        val stickyFlow: Flow<T> = mutex.withLock {
            stickyEvents[clazz]
                ?.filter { clazz.isInstance(it) }  // 过滤出 T 类型实例
                ?.map { it as T }                  // 安全转换为非空 T
                ?: emptyFlow()
        }

        // 合并普通流（需确保 subscribe(clazz) 返回 Flow<T>）
        val mergedFlow = merge(stickyFlow, subscribe(clazz))
        emitAll(mergedFlow)
    }

    /**
     * 订阅粘性事件（inline）
     */
    inline fun <reified T : Any> subscribeSticky(): Flow<T> = subscribeSticky(T::class.java)

    /**
     * 移除指定类型粘性事件
     */
    suspend fun <T : Any> removeStickyEvent(clazz: Class<T>) {
        mutex.withLock {
            stickyEvents.remove(clazz)
        }
    }

    /**
     * 清空所有粘性事件
     */
    suspend fun clearStickyEvents() {
        mutex.withLock {
            stickyEvents.clear()
        }
    }

    /**
     * 事件日志调试
     */
    var logger: ((Any) -> Unit)? = null
}