package com.tzh.baselib.view.file

import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tzh.baselib.R
import java.io.File
import java.util.Locale

class FilePickerFragment : Fragment() {
    private lateinit var adapter: FilePickerAdapter
    private var currentPath = Environment.getExternalStorageDirectory().path

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_file_picker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.rv_files)
        val btnConfirm = view.findViewById<Button>(R.id.btn_confirm)
        val tvCurrentPath = view.findViewById<TextView>(R.id.tv_current_path)
        val btnUp = view.findViewById<ImageButton>(R.id.btn_up)

        adapter = FilePickerAdapter(requireContext()) { file ->
            if (file.isDirectory) {
                currentPath = file.path
                loadFiles(currentPath)
                tvCurrentPath.text = currentPath
            }
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        btnUp.setOnClickListener {
            if(currentPath==Environment.getExternalStorageDirectory().path){
                activity?.finish()
            }else{
                val parent = File(currentPath).parent
                if (parent != null) {
                    currentPath = parent
                    loadFiles(currentPath)
                    tvCurrentPath.text = currentPath
                }
            }
        }

        btnConfirm.setOnClickListener {
            (activity as? FileSelectListener)?.onFilesSelected(adapter.selectedFiles)
//            dismiss()
        }

        loadFiles(currentPath)
        tvCurrentPath.text = currentPath
    }

    private fun loadFiles(path: String) {
        val dir = File(path)
        if (!dir.exists() || !dir.isDirectory) return

        val fileList = dir.listFiles()?.map { file ->
            FileItem(
                name = file.name,
                path = file.absolutePath,
                isDirectory = file.isDirectory,
                size = if (file.isDirectory) 0 else file.length(),
                lastModified = file.lastModified(),
                iconRes = getIconForFile(file)
            )
        }?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase(Locale.ROOT) }))

        fileList?.let { adapter.updateFiles(it.toMutableList()) }
    }

    private fun getIconForFile(file: File): Int {
        return when {
            file.isDirectory -> R.drawable.icon_lib_folder
            file.extension.lowercase(Locale.ROOT) in listOf("jpg", "png", "gif") -> R.drawable.icon_lib_image
            file.extension.lowercase(Locale.ROOT) in listOf("mp3", "wav", "flac") -> R.drawable.icon_lib_audio
            file.extension.lowercase(Locale.ROOT) in listOf("mp4", "avi", "mkv") -> R.drawable.icon_lib_video
            file.extension.lowercase(Locale.ROOT) in listOf("pdf", "doc", "docx") -> R.drawable.icon_lib_document
            file.extension.lowercase(Locale.ROOT) in listOf("zip", "rar", "7z") -> R.drawable.icon_lib_archive
            else -> R.drawable.icon_lib_file
        }
    }

    interface FileSelectListener {
        fun onFilesSelected(files: List<FileItem>)
    }
}