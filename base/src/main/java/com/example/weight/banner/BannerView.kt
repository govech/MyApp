package com.example.weight.banner

import android.content.Context
import android.graphics.Outline
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.elvishew.xlog.XLog
import com.example.base.R
import com.example.image.ImageLoader
import com.example.utils.ktx.dp2px
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class BannerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private lateinit var viewPager: ViewPager2
    private lateinit var indicatorLayout: LinearLayout
    private var imageList: List<String> = emptyList()
    private var interval: Long = 3000L
    private var enableAutoPlay = true//是否开启自动播放

    // 回调
    private var onItemClickListener: ((Int) -> Unit)? = null
    private var onPageChangeListener: ((Int) -> Unit)? = null


    private var indicatorVisible = true // 是否显示指示器
    private var infiniteScrollEnabled = true//循环滚动

    private val adapter = BannerAdapter()
    private var cornerRadius = context.dp2px(16f) // 默认 16dp
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null
    private var isAutoPlayRunning = false //用于判断协程是否已经在运行,主要用来避免协程重复启动

    init {
        initView()
        initAttributes(attrs)
        setupViewPager()
    }

    private fun initView() {
        val root = LayoutInflater.from(context).inflate(R.layout.view_banner, this, true)
        viewPager = root.findViewById(R.id.bannerViewPager)
        indicatorLayout = root.findViewById(R.id.bannerIndicatorLayout)
        clipToOutline = true
    }

    private fun initAttributes(attrs: AttributeSet?) {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.BannerView,
            0, 0
        ).apply {
            try {
                cornerRadius = getDimension(R.styleable.BannerView_cornerRadius, cornerRadius)
                interval =
                    getInt(R.styleable.BannerView_autoPlayInterval, interval.toInt()).toLong()
                enableAutoPlay = getBoolean(R.styleable.BannerView_autoPlay, enableAutoPlay)
                indicatorVisible =
                    getBoolean(R.styleable.BannerView_showIndicator, indicatorVisible)
                infiniteScrollEnabled =
                    getBoolean(R.styleable.BannerView_infiniteScroll, infiniteScrollEnabled)
            } finally {
                recycle()
            }
        }

        indicatorLayout.visibility = if (indicatorVisible) View.VISIBLE else View.GONE
    }


    private fun setupViewPager() {
        viewPager.adapter = adapter

        adapter.setInfiniteScrollEnabled(infiniteScrollEnabled)

        // 设置点击监听
        adapter.setOnItemClickListener { position ->
            onItemClickListener?.invoke(position)
        }

        // 页面变化监听
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val realPosition = position % imageList.size
                updateIndicator(realPosition)
                onPageChangeListener?.invoke(realPosition)
            }

            override fun onPageScrollStateChanged(state: Int) {
                when (state) {
                    ViewPager2.SCROLL_STATE_DRAGGING -> {
                        /** 拖动页面: 停止自动滚动 */
                        stopAutoPlay()
                    }

                    ViewPager2.SCROLL_STATE_IDLE -> {
                        /** 停止滚动: 开启自动滚动 */
                        startAutoPlay()
                    }
                }

            }
        })
    }

    /**
     * 设置自动播放间隔
     */
    fun setInterval(millis: Long) {
        if (millis <= 0) return
        interval = millis
        if (isAutoPlayRunning) {
            startAutoPlay() // 重新启动以应用新间隔
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, w, h, cornerRadius)
            }
        }
    }

    fun setCornerRadius(radiusDp: Float) {
        cornerRadius = context.dp2px(radiusDp)
        invalidateOutline()
    }

    fun setImages(images: List<String>) {
        if (images.isEmpty()) {
            stopAutoPlay()
            indicatorLayout.removeAllViews()
            viewPager.adapter = null
            return
        }
        imageList = images
        adapter.setImages(images)
        val startIndex = Int.MAX_VALUE / 2 - (Int.MAX_VALUE / 2) % images.size
        viewPager.setCurrentItem(startIndex, false)
        setupIndicator(images.size)
        startAutoPlay()
    }


    fun setAutoPlay(enable: Boolean) {
        enableAutoPlay = enable
        if (enable) startAutoPlay() else stopAutoPlay()
    }

    fun setOnItemClickListener(listener: (Int) -> Unit) {
        onItemClickListener = listener
    }
    fun setOnPageChangeListener(listener: (Int) -> Unit) {
        onPageChangeListener = listener
    }

    private fun startAutoPlay() {
        stopAutoPlay()
        if (!enableAutoPlay || imageList.isEmpty() || imageList.size < 2 || isAutoPlayRunning) return
        isAutoPlayRunning = true
        job = mainScope.launch {
            while (isActive) {
                delay(interval)
                val next = viewPager.currentItem + 1
                viewPager.setCurrentItem(next, true)
            }
        }
    }

    private fun stopAutoPlay() {
        if (!isAutoPlayRunning) return
        job?.cancel()
        job = null
        isAutoPlayRunning = false
    }

    private fun setupIndicator(count: Int) {
        indicatorLayout.removeAllViews()
        val size = context.dp2px(8f).toInt()
        for (i in 0 until count) {
            val dot = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    setMargins(8, 0, 8, 0)
                }
                setImageResource(R.drawable.banner_indicator_unselected)
            }
            indicatorLayout.addView(dot)
        }
        updateIndicator(0)
    }

    private fun updateIndicator(realPosition: Int) {
        for (i in 0 until indicatorLayout.childCount) {
            val dot = indicatorLayout.getChildAt(i) as ImageView
            dot.setImageResource(
                if (i == realPosition) R.drawable.banner_indicator_selected
                else R.drawable.banner_indicator_unselected
            )
        }
    }


    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAutoPlay()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startAutoPlay()
    }


    // 可选：添加生命周期感知
    fun attachToLifecycle(lifecycle: Lifecycle) {
        lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                when (event) {
                    Lifecycle.Event.ON_RESUME -> startAutoPlay()
                    Lifecycle.Event.ON_PAUSE -> stopAutoPlay()
                    Lifecycle.Event.ON_DESTROY -> mainScope.cancel()
                    else -> {}
                }
            }
        })
    }

}
