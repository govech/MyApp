package com.example.weight

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
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
    private var handler: Handler = Handler(Looper.getMainLooper())
    private var currentIndex = 0
    private var onItemClickListener: ((position: Int) -> Unit)? = null

    private lateinit var autoScrollRunnable: Runnable

    init {
        val root = LayoutInflater.from(context).inflate(R.layout.view_banner, this, true)
        viewPager = root.findViewById(R.id.bannerViewPager)
        indicatorLayout = root.findViewById(R.id.bannerIndicatorLayout)
        autoScrollRunnable = object : Runnable {
            override fun run() {
                if (autoPlay && imageList.isNotEmpty()) {
                    currentIndex = (currentIndex + 1) % imageList.size
                    viewPager.setCurrentItem(currentIndex, true)
                    handler.postDelayed(this, interval)
                }
            }
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentIndex = position
                updateIndicator()
            }
        })
    }

    fun setImages(images: List<String>) {
        imageList = images
        viewPager.adapter = BannerAdapter(images) { pos ->
            onItemClickListener?.invoke(pos)
        }
        setupIndicator(images.size)
        currentIndex = 0
        viewPager.setCurrentItem(Int.MAX_VALUE / 2, false)
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

    fun setOnItemClickListener(listener: (position: Int) -> Unit) {
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
        for (i in 0 until count) {
            val dot = ImageView(context).apply {
                val size = context.dp2px(8f).toInt()
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    setMargins(8, 0, 8, 0)
                }
                setImageResource(R.drawable.banner_indicator_unselected)

            }
            indicatorLayout.addView(dot)
        }
        updateIndicator()
    }

    private fun updateIndicator() {
        val realIndex = currentIndex % imageList.size
        for (i in 0 until indicatorLayout.childCount) {
            val dot = indicatorLayout.getChildAt(i) as ImageView
            dot.setImageResource(
                if (i == realIndex) R.drawable.banner_indicator_selected
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

    private class BannerAdapter(
        private val images: List<String>,
        private val clickListener: (Int) -> Unit
    ) : RecyclerView.Adapter<BannerAdapter.BannerViewHolder>() {
        override fun onCreateViewHolder(
            parent: android.view.ViewGroup,
            viewType: Int
        ): BannerViewHolder {
            val imageView = ImageView(parent.context)
            imageView.layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP
            return BannerViewHolder(imageView)
        }

        override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
            val realPos = position % images.size
            ImageLoader.load(images[realPos], holder.itemView as ImageView)
            holder.itemView.setOnClickListener { clickListener(realPos) }
        }

        override fun getItemCount(): Int = Int.MAX_VALUE

        class BannerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    }
} 