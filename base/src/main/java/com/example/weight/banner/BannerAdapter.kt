package com.example.weight.banner

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.base.R
import com.example.image.ImageLoader

class BannerAdapter(
    private val imageList: List<String>,
    private val clickListener: BannerView.BannerClickListener?
) : RecyclerView.Adapter<BannerAdapter.BannerViewHolder>() {

    private var infiniteScrollEnabled = true

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_banner_image, parent, false)
        return BannerViewHolder(view)
    }

    override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
        val realPos = position % imageList.size
        ImageLoader.load(imageList[realPos], holder.imageView)
        holder.itemView.setOnClickListener {
            clickListener?.onBannerClick(realPos)
        }
    }

    override fun getItemCount(): Int {
        val realSize = imageList.size
        return if (realSize <= 1) realSize else if (infiniteScrollEnabled) Int.MAX_VALUE else realSize
    }

    fun getStartPosition(): Int {
        return if (imageList.isEmpty()) 0 else Int.MAX_VALUE / 2 - (Int.MAX_VALUE / 2) % imageList.size
    }

    fun getImageListSize() = imageList.size

    fun setInfiniteScrollEnabled(enabled: Boolean) {
        infiniteScrollEnabled = enabled
        notifyDataSetChanged()
    }

    class BannerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
    }
}
