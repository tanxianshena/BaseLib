package com.tzh.baselib.view.file

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tzh.baselib.R
import com.tzh.baselib.adapter.XRvBindingHolder
import com.tzh.baselib.adapter.XRvBindingPureDataAdapter
import com.tzh.baselib.databinding.ItemFileBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FilePickerAdapter(val context: Context, val onFileSelected: (FileItem) -> Unit) : XRvBindingPureDataAdapter<FileItem>(R.layout.item_file){

    private var files: List<FileItem> = emptyList()

    var selectedFiles = mutableListOf<FileItem>()
    override fun onBindViewHolder(holder: XRvBindingHolder, position: Int, data: FileItem) {
        val binding = holder.getBinding<ItemFileBinding>()
        binding.tvName.text = data.name
        binding.ivIcon.setImageResource(data.iconRes)

        if (data.isDirectory) {
            binding.tvInfo.text = "文件夹"
            binding.ivSelect.visibility = View.GONE
        } else {
            binding.tvInfo.text = "${formatSize(data.size)} • ${formatDate(data.lastModified)}"
            binding.ivSelect.visibility = View.VISIBLE
        }

        if(selectedFiles.contains(data)){
            binding.ivSelect.setImageResource(R.drawable.icon_select_yes)
        }else{
            binding.ivSelect.setImageResource(R.drawable.icon_select_no)
        }

        binding.root.setOnClickListener {
            if(data.isDirectory){
                onFileSelected(data)
            }else{
                if(selectedFiles.contains(data)){
                    selectedFiles.remove(data)
                }else{
                    selectedFiles.add(data)
                }

                notifyItemChanged(position)
            }
        }
    }

    fun updateFiles(newFiles: MutableList<FileItem>) {
        setDatas(newFiles)
        files = newFiles
        notifyDataSetChanged()
    }

    private fun formatSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            else -> "${size / (1024 * 1024)} MB"
        }
    }

    private fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .format(Date(timestamp))
    }
}