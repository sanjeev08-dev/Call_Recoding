package com.sanjeevdev.callrecodingdemo2

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.recorded_item.view.*
import java.io.IOException

class RecordingAdaptar(var recoredItem: List<RecordingModal>, context: Context, recordingListener: RecordingListener) :
    RecyclerView.Adapter<RecordingAdaptar.ViewHolder>() {

    val player = MediaPlayer()

    val mContext = context
    val mRecordingListener = recordingListener

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val playButton = view.audioPlayButton
        val fileName = view.fileName
        val fileDate = view.fileDate
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.recorded_item, parent, false)
        return ViewHolder(view)
    }


    override fun getItemCount() = recoredItem.size
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val link = recoredItem[position].downloadLink
        holder.fileName.text = recoredItem[position].name
        holder.fileDate.text = recoredItem[position].date
        holder.playButton.setOnClickListener {
            mRecordingListener.initiateMediaPlayer(recoredItem[position],holder)
        }
    }
}