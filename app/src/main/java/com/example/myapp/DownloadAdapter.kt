package com.example.myapp

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.databinding.ItemDownloadBinding
import com.example.newload.DownloadStatus
import com.example.newload.DownloadTask

class DownloadAdapter(
    private val viewModel: DownloadViewModel
) : ListAdapter<DownloadTask, DownloadAdapter.DownloadViewHolder>(DownloadTaskDiffCallback()) {

    companion object {
        private const val PAYLOAD_PROGRESS = "PAYLOAD_PROGRESS"
        private const val PAYLOAD_STATUS = "PAYLOAD_STATUS"
    }

    class DownloadViewHolder(
        private val binding: ItemDownloadBinding,
        private val viewModel: DownloadViewModel
    ) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("DefaultLocale", "SetTextI18n")
        fun bind(task: DownloadTask) {
            binding.tvFileName.text = task.filePath.substringAfterLast("/")
            binding.progressBar.progress = task.progress.toInt()
            binding.tvProgress.text = String.format("%.2f%%", task.progress)
            binding.tvStatus.text = "Status: ${task.status.name}"

            when (task.status) {
                DownloadStatus.QUEUED -> {
                    binding.btnAction.text = "Start"
                    binding.btnAction.isEnabled = true
                    binding.btnAction.setOnClickListener {
                        viewModel.addTask(
                            task.url,
                            task.filePath
                        )
                    }
                }

                DownloadStatus.DOWNLOADING -> {
                    binding.btnAction.text = "Pause"
                    binding.btnAction.isEnabled = true
                    binding.btnAction.setOnClickListener { viewModel.pauseTask(task.taskId) }
                }

                DownloadStatus.PAUSED -> {
                    binding.btnAction.text = "Resume"
                    binding.btnAction.isEnabled = true
                    binding.btnAction.setOnClickListener { viewModel.resumeTask(task.taskId) }
                }

                DownloadStatus.COMPLETED -> {
                    binding.btnAction.text = "Completed"
                    binding.btnAction.isEnabled = false
                }

                DownloadStatus.FAILED -> {
                    binding.btnAction.text = "Retry"
                    binding.btnAction.isEnabled = true
                    binding.btnAction.setOnClickListener {
                        viewModel.addTask(
                            task.url,
                            task.filePath
                        )
                    }
                }

                DownloadStatus.CANCELLED -> {
                    binding.btnAction.text = "Restart"
                    binding.btnAction.isEnabled = true
                    binding.btnAction.setOnClickListener {
                        viewModel.addTask(
                            task.url,
                            task.filePath
                        )
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DownloadViewHolder {
        val binding =
            ItemDownloadBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DownloadViewHolder(binding, viewModel)
    }

    override fun onBindViewHolder(holder: DownloadViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DownloadTaskDiffCallback : DiffUtil.ItemCallback<DownloadTask>() {
        override fun areItemsTheSame(oldItem: DownloadTask, newItem: DownloadTask): Boolean {
            return oldItem.taskId == newItem.taskId
        }

        override fun areContentsTheSame(oldItem: DownloadTask, newItem: DownloadTask): Boolean {

            return oldItem == newItem
//            return oldItem.status == newItem.status &&
//                    oldItem.progress == newItem.progress &&
//                    oldItem.downloadedBytes == newItem.downloadedBytes &&
//                    oldItem.totalBytes == newItem.totalBytes


        }

//        override fun getChangePayload(oldItem: DownloadTask, newItem: DownloadTask): Any? {
//            val payloads = mutableListOf<String>()
//            if (oldItem.progress != newItem.progress || oldItem.downloadedBytes != newItem.downloadedBytes || oldItem.totalBytes != newItem.totalBytes) {
//                payloads.add(PAYLOAD_PROGRESS)
//            }
//            if (oldItem.status != newItem.status) {
//                payloads.add(PAYLOAD_STATUS)
//            }
//            return if (payloads.isNotEmpty()) payloads else null
//        }
    }
}