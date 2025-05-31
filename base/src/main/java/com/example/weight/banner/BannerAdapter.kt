package com.example.weight.banner

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.elvishew.xlog.XLog
import com.example.base.R
import com.example.image.ImageLoader

class BannerAdapter(
    private var imageList: List<String> = listOf(),
) : RecyclerView.Adapter<BannerAdapter.BannerViewHolder>() {


    private var onItemClickListener: ((Int) -> Unit)? = null
    private var enableInfiniteScroll = true // 是否启用无限滚动
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_banner_image, parent, false)
        return BannerViewHolder(view)
    }

    override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
        val realPos = position % imageList.size
        ImageLoader.load(imageList[realPos], holder.imageView)
        holder.itemView.setOnClickListener {
            onItemClickListener?.invoke(realPos)
        }
    }

    override fun getItemCount(): Int {
        val realSize = imageList.size
        return when {
            realSize == 0 -> 0
            realSize == 1 -> 1
            enableInfiniteScroll -> Int.MAX_VALUE
            else -> realSize
        }
    }

    fun setOnItemClickListener(listener: (Int) -> Unit) {
        onItemClickListener = listener
    }

    fun setInfiniteScrollEnabled(enabled: Boolean) {
        enableInfiniteScroll = enabled
        notifyDataSetChanged()
    }


    fun setImages(images: List<String>) {
        imageList = images
        notifyDataSetChanged()
    }

    class BannerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
    }
}