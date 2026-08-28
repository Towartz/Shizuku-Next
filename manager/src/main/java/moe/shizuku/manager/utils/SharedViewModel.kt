package moe.shizuku.manager.utils

import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.annotation.MainThread
import androidx.collection.ArrayMap
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import java.util.concurrent.atomic.AtomicInteger

@Suppress("UNCHECKED_CAST")
@MainThread
inline fun <reified VM : ViewModel> ComponentActivity.viewModels(
    crossinline viewModelProducer: () -> VM
): Lazy<VM> = viewModels(factoryProducer = {
    object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return viewModelProducer() as T
        }
    }
})

private val referenceCounts = HashMap<String, AtomicInteger>()
private val stores = ArrayMap<String, ViewModelStore>()

@MainThread
inline fun <reified VM : ViewModel> ComponentActivity.sharedViewModels(
    noinline keyProducer: () -> String? = { null },
    noinline viewModelProducer: () -> VM
) = createSharedViewModelLazy({ this }, keyProducer, VM::class.java, viewModelProducer)

@MainThread
inline fun <reified VM : ViewModel> Fragment.activitySharedViewModels(
    noinline keyProducer: () -> String? = { null },
    noinline viewModelProducer: () -> VM
) = createSharedViewModelLazy({ requireActivity() }, keyProducer, VM::class.java, viewModelProducer)

@MainThread
fun <VM : ViewModel> createSharedViewModelLazy(
    referrerProducer: () -> ComponentActivity,
    keyProducer: () -> String?,
    clazz: Class<VM>,
    viewModelProducer: () -> VM
): Lazy<VM> = SharedViewModelLazy(referrerProducer, keyProducer, clazz, viewModelProducer)

private class SharedViewModelLazy<VM : ViewModel>(
    private val referrerProducer: () -> ComponentActivity,
    private val keyProducer: () -> String?,
    private val clazz: Class<VM>,
    private val viewModelProducer: () -> VM
) : Lazy<VM> {

    private var cached: VM? = null

    @Suppress("UNCHECKED_CAST")
    override val value: VM
        get() {
            if (cached != null) return cached!!
            val key = clazz.name + ":" + keyProducer()
            val store = stores.getOrPut(key) { ViewModelStore() }
            val factory = object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return viewModelProducer() as T
                }
            }
            val vm = ViewModelProvider(store, factory)[clazz]
            cached = vm

            referenceCounts.getOrPut(key) { AtomicInteger() }.incrementAndGet()

            val activity = referrerProducer()
            activity.lifecycle.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    if (event != Lifecycle.Event.ON_DESTROY) return
                    val count = referenceCounts[key]?.decrementAndGet() ?: 0
                    if (count == 0) {
                        referenceCounts.remove(key)
                        if (!activity.isChangingConfigurations) {
                            stores.remove(key)?.clear()
                        }
                    }
                }
            })

            return vm
        }

    override fun isInitialized(): Boolean = cached != null
}
