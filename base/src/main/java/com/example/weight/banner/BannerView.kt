package com.example.weight.banner

import android.content.Context
import android.graphics.Outline
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import androidx.viewpager2.widget.ViewPager2
import com.example.base.R
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
) : FrameLayout(context, attrs, defStyleAttr), DefaultLifecycleObserver {

    private lateinit var viewPager: ViewPager2
    private lateinit var indicatorLayout: LinearLayout

    private var interval: Long = 3000L //  自动轮播间隔时间
    private var enableAutoPlay = true //  是否开启自动轮播
    private var indicatorVisible = true //  是否显示指示器
    private var infiniteScrollEnabled = true //  是否开启无限滚动(滚动到最后一张时是否回到第一张)
    private var cornerRadius = context.dp2px(16f) //  圆角半径

    private var adapter: BannerAdapter? = null
    private var bannerClickListener: BannerClickListener? = null
    private var onPageChangeListener: ((Int) -> Unit)? = null

    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var autoPlayJob: Job? = null
    private var isAutoPlayRunning = false

    init {
        initView()
        initAttrs(attrs)
    }

    private fun initView() {
        val root = LayoutInflater.from(context).inflate(R.layout.view_banner, this, true)
        viewPager = root.findViewById(R.id.bannerViewPager)
        indicatorLayout = root.findViewById(R.id.bannerIndicatorLayout)
        clipToOutline = true

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val realPosition = position % (adapter?.getImageListSize() ?: 1)
                updateIndicator(realPosition)
                onPageChangeListener?.invoke(realPosition)
            }

            override fun onPageScrollStateChanged(state: Int) {
                when (state) {
                    ViewPager2.SCROLL_STATE_IDLE -> startAutoPlay()
                    ViewPager2.SCROLL_STATE_DRAGGING -> stopAutoPlay()
                }
            }
        })
    }

    private fun initAttrs(attrs: AttributeSet?) {
        context.theme.obtainStyledAttributes(attrs, R.styleable.BannerView, 0, 0).apply {
            try {
                cornerRadius = getDimension(R.styleable.BannerView_cornerRadius, cornerRadius)
                interval = getInt(R.styleable.BannerView_autoPlayInterval, interval.toInt()).toLong()
                enableAutoPlay = getBoolean(R.styleable.BannerView_autoPlay, enableAutoPlay)
                indicatorVisible = getBoolean(R.styleable.BannerView_showIndicator, indicatorVisible)
                infiniteScrollEnabled = getBoolean(R.styleable.BannerView_infiniteScroll, infiniteScrollEnabled)
            } finally {
                recycle()
            }
        }
        indicatorLayout.visibility = if (indicatorVisible) View.VISIBLE else View.GONE
    }

    fun setImages(images: List<String>) {
        stopAutoPlay()
        if (images.isEmpty()) {
            indicatorLayout.removeAllViews()
            viewPager.adapter = null
            return
        }
        adapter = BannerAdapter(images, bannerClickListener).also {
            it.setInfiniteScrollEnabled(infiniteScrollEnabled)
        }
        viewPager.adapter = adapter
        viewPager.setCurrentItem(adapter!!.getStartPosition(), false)
        setupIndicator(images.size)
        startAutoPlay()
    }

    fun setOnBannerClickListener(listener: BannerClickListener) {
        this.bannerClickListener = listener
    }

    fun setOnPageChangeListener(listener: (Int) -> Unit) {
        this.onPageChangeListener = listener
    }

    fun setInterval(ms: Long) {
        if (ms > 0) {
            interval = ms
            if (isAutoPlayRunning) startAutoPlay()
        }
    }

    fun setCornerRadius(dp: Float) {
        cornerRadius = context.dp2px(dp)
        invalidateOutline()
    }

    fun setAutoPlay(enable: Boolean) {
        enableAutoPlay = enable
        if (enable) startAutoPlay() else stopAutoPlay()
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

    private fun startAutoPlay() {
        stopAutoPlay()
        val size = adapter?.getImageListSize() ?: return
        if (!enableAutoPlay || size < 2) return
        isAutoPlayRunning = true
        autoPlayJob = mainScope.launch {
            while (isActive) {
                delay(interval)
                viewPager.setCurrentItem(viewPager.currentItem + 1, true)
            }
        }
    }

    private fun stopAutoPlay() {
        if (!isAutoPlayRunning) return
        autoPlayJob?.cancel()
        autoPlayJob = null
        isAutoPlayRunning = false
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAutoPlay()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startAutoPlay()
    }

    override fun onResume(owner: LifecycleOwner) = startAutoPlay()
    override fun onPause(owner: LifecycleOwner) = stopAutoPlay()
    override fun onDestroy(owner: LifecycleOwner) = mainScope.cancel()

    /**
     *  用于在Activity/Fragment中添加生命周期监听,调用这个方法会让轮播器在界面可见时自动播放,界面不可见时自动暂停
     */
    fun attachToLifecycle(lifecycle: Lifecycle) {
        lifecycle.addObserver(this)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, w, h, cornerRadius)
            }
        }
    }


    interface BannerClickListener {
        fun onBannerClick(position: Int)
    }
}
