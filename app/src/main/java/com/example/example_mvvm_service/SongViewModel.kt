package com.example.example_mvvm_service

import android.app.Application
import android.media.MediaMetadataRetriever
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SongViewModel(application: Application) : AndroidViewModel(application) {

    private var _song = MutableLiveData<List<Song>>()
    val song : LiveData<List<Song>> get() = _song

    init {
        _song.value = arrayListOf(
            Song("Bao nhiêu loài hoa", getMusicDuration(R.raw.baonhieuloaihoa) ,R.raw.baonhieuloaihoa),
            Song("Em xinh", getMusicDuration(R.raw.emxinh),R.raw.emxinh),
            Song("Đã từng vô giá", getMusicDuration(R.raw.datungvogia),R.raw.datungvogia),
            )
    }

    private fun getMusicDuration(resourceId: Int): Int {
        val retriever = MediaMetadataRetriever()
        val fd = getApplication<Application>().resources.openRawResourceFd(resourceId)
        retriever.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
        val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        retriever.release()
        return durationStr?.toIntOrNull() ?: 0
    }
}