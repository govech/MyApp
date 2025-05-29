package com.example.weight

import android.content.Context
import android.graphics.Outline
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.base.R
import com.example.image.ImageLoader
import com.example.utils.ktx.dp2px

class BannerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val viewPager: ViewPager2
    private val indicatorLayout: LinearLayout
    private var imageList: List<String> = emptyList()
    private var interval: Long = 3000L
    private var autoPlay = true
    private var handler = Handler(Looper.getMainLooper())
    private var onItemClickListener: ((Int) -> Unit)? = null
    private var cornerRadius = context.dp2px(16f) // 默认 16dp
    private lateinit var autoScrollRunnable: Runnable

    init {
        val root = LayoutInflater.from(context).inflate(R.layout.view_banner, this, true)
        viewPager = root.findViewById(R.id.bannerViewPager)
        indicatorLayout = root.findViewById(R.id.bannerIndicatorLayout)
        autoScrollRunnable = object : Runnable {
            override fun run() {
                if (autoPlay && imageList.isNotEmpty()) {
                    val next = viewPager.currentItem + 1
                    viewPager.setCurrentItem(next, true)
                    handler.postDelayed(this, interval)
                }
            }
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateIndicator(position % imageList.size)
            }

            override fun onPageScrollStateChanged(state: Int) {
                when (state) {
                    ViewPager2.SCROLL_STATE_DRAGGING -> stopAutoPlay()
                    ViewPager2.SCROLL_STATE_IDLE -> startAutoPlay()
                }
            }
        })


        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.BannerView,
            0, 0
        ).apply {
            try {
                cornerRadius = getDimension(R.styleable.BannerView_cornerRadius, cornerRadius)
            } finally {
                recycle()
            }
        }

        clipToOutline = true
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, cornerRadius)
            }
        }
    }


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        invalidateOutline()
    }

    fun setCornerRadius(radiusDp: Float) {
        cornerRadius = context.dp2px(radiusDp)
        invalidateOutline()
    }

    fun setImages(images: List<String>) {
        imageList = images
        viewPager.adapter = BannerAdapter()
        val startIndex = Int.MAX_VALUE / 2 - (Int.MAX_VALUE / 2) % images.size
        viewPager.setCurrentItem(startIndex, false)
        setupIndicator(images.size)
        startAutoPlay()
    }

    fun setInterval(millis: Long) {
        interval = millis
        startAutoPlay()
    }

    fun setAutoPlay(enable: Boolean) {
        autoPlay = enable
        if (enable) startAutoPlay() else stopAutoPlay()
    }

    fun setOnItemClickListener(listener: (Int) -> Unit) {
        onItemClickListener = listener
    }

    private fun startAutoPlay() {
        handler.removeCallbacks(autoScrollRunnable)
        if (autoPlay && imageList.size > 1) {
            handler.postDelayed(autoScrollRunnable, interval)
        }
    }

    private fun stopAutoPlay() {
        handler.removeCallbacks(autoScrollRunnable)
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

    inner class BannerAdapter : RecyclerView.Adapter<BannerAdapter.BannerViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_banner_image, parent, false)
            return BannerViewHolder(view)
        }

        override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
            val realPos = position % imageList.size
            val imageView = holder.itemView.findViewById<ImageView>(R.id.imageView)
            ImageLoader.load(imageList[realPos], imageView)
            holder.itemView.setOnClickListener {
                onItemClickListener?.invoke(realPos)
            }
        }

        override fun getItemCount(): Int = Int.MAX_VALUE

        inner class BannerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    }
}
