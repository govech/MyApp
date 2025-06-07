package com.example.myapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newload.DownloadStatus
import com.example.newload.DownloadTask
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DownloadViewModel : ViewModel() {
    private val repository = DownloadRepository()
    private val _tasks = MutableStateFlow<List<DownloadTask>>(emptyList())
    val tasks: StateFlow<List<DownloadTask>> = _tasks.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getDownloadEvents()
                .collect { event ->
                updateTask(event)
            }
        }
    }

    fun addTask(url: String, filePath: String) {
        val taskId = repository.addTask(url, filePath)
        _tasks.value += DownloadTask(
            url = url,
            filePath = filePath,
            taskId = taskId
        )
    }

    fun pauseTask(taskId: String) {
        repository.pauseTask(taskId)
    }

    fun resumeTask(taskId: String) {
        repository.resumeTask(taskId)
    }

    fun cancelTask(taskId: String) {
        repository.cancelTask(taskId)
    }


    private fun updateTask(event: DownloadEvent) {
        val currentTasks = _tasks.value.toMutableList()
        val index = currentTasks.indexOfFirst { it.taskId == event.taskId }
        if (index != -1) {
            val task = currentTasks[index]
            val updatedTask = when (event.state) {
                is DownloadState.Progress -> task.copy(
                    progress = event.state.progress,
                    downloadedBytes = event.state.downloadedBytes,
                    totalBytes = event.state.totalBytes
                )

                is DownloadState.Status -> task.copy(status = event.state.status)
                is DownloadState.Error -> task.copy(status = DownloadStatus.FAILED)
            }
            currentTasks[index] = updatedTask
            _tasks.value = currentTasks
        }
    }
}