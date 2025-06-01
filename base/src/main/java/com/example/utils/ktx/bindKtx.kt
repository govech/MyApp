package com.example.utils.ktx

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.viewbinding.ViewBinding
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

fun <VB : ViewBinding> Activity.binding(inflater: (LayoutInflater) -> VB) = lazy {
    inflater(layoutInflater).apply { setContentView(root) }
}


/**
 * 使用方式：不覆写 onCreateView()，使用 Fragment(R.layout.xxx) 构造函数 + 扩展委托： private val mBinding by binding(FragmentHomeBinding::bind)
 *
 * 只能在 onViewCreated 或之后使用 binding，否则 requireView() 会抛异常。
 * 不能在 onCreateView() 使用它，因为此时还没 attach View
 *
 *
 */
fun <VB : ViewBinding> Fragment.binding(bind: (View) -> VB) = FragmentBindingDelegate(bind)


fun <VB : ViewBinding> View.getBinding(bind: (View) -> VB): VB = bind(this)

class FragmentBindingDelegate<VB : ViewBinding>(private val bind: (View) -> VB) :
    ReadOnlyProperty<Fragment, VB> {

    private var binding: VB? = null
    private var isObserverAdded = false

    override fun getValue(thisRef: Fragment, property: KProperty<*>): VB {
        val view = try {
            thisRef.requireView()
        } catch (e: IllegalStateException) {
            throw IllegalStateException("The property of ${property.name} has been destroyed.")
        }

        if (binding == null) {
            binding = view.getBinding(bind)
        }

        if (!isObserverAdded) {
            thisRef.viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    binding = null
                    isObserverAdded = false
                }
            })
            isObserverAdded = true
        }

        return binding!!
    }
}
