package com.example.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import java.lang.reflect.ParameterizedType


/**
 * 一个使用 ViewBinding 的基础 Fragment 类
 * 提供了在 Fragment 中使用 ViewBinding 的基本功能，包括绑定布局和释放资源
 *
 * @param VB ViewBinding 的具体实现类，用于绑定布局
 */
abstract class BaseBindingFragment<VB : ViewBinding> : Fragment() {

    private var _binding: VB? = null
    protected val binding: VB
        get() = _binding!!

    /**
     * 在绑定创建后调用，用于初始化 UI 和数据
     * 子类可以覆盖此方法以执行额外的创建逻辑
     */
    abstract fun onBindingCreated()

    /**
     * 创建 Fragment 视图
     * 使用反射来实例化 ViewBinding 类，并绑定布局
     *
     * @param inflater 布局 inflater，用于充气布局
     * @param container Fragment 将被添加到的父视图组
     * @param savedInstanceState 保存的实例状态，如果有的话
     * @return 返回绑定的根视图
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // 通过反射获取 ViewBinding 类
        val clazz =
            (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0] as Class<VB>
        // 获取并调用 ViewBinding 类的 inflate 方法来创建视图绑定实例
        val method = clazz.getMethod(
            "inflate",
            LayoutInflater::class.java,
            ViewGroup::class.java,
            Boolean::class.javaPrimitiveType
        )
        @Suppress("UNCHECKED_CAST")
        _binding = method.invoke(null, inflater, container, false) as VB
        // 返回绑定的根视图
        return _binding!!.root
    }

    /**
     * 在 Fragment 的视图被创建后调用
     * 调用子类实现的 onBindingCreated 方法来执行 UI 初始化
     *
     * @param view 绑定的视图
     * @param savedInstanceState 保存的实例状态，如果有的话
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onBindingCreated()
    }

    /**
     * 在 Fragment 的视图被销毁时调用
     * 释放 ViewBinding 实例以避免内存泄漏
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
