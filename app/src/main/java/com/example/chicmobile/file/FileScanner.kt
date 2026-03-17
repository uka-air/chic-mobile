package com.example.chicmobile.file

import android.util.Log
import java.io.File

object FileScanner {
    private const val TAG = "FileScanner"
    private const val UPLOADING_SUFFIX = ".uploading"

    fun scanEligibleFiles(folderPath: String, extensionFilter: String, minAgeSeconds: Long = 30): List<File> {
        val folder = File(folderPath)
        if (!folder.exists() || !folder.isDirectory) {
            Log.e(TAG, "Configured folder does not exist or is not a directory: $folderPath")
            return emptyList()
        }

        val now = System.currentTimeMillis()
        val extension = extensionFilter.trim().removePrefix(".").lowercase()
        val allFiles = folder.listFiles()

        if (allFiles == null) {
            Log.e(TAG, "Unable to list files from folder (permission denied or IO error): $folderPath")
            return emptyList()
        }

        Log.d(TAG, "Scanning folder=$folderPath totalEntries=${allFiles.size} extensionFilter=$extension")

        return allFiles
            .asSequence()
            ?.filter { it.isFile }
            ?.filter { !it.name.endsWith(UPLOADING_SUFFIX) }
            ?.filter {
                extension.isBlank() || it.extension.lowercase() == extension
            }
            ?.filter {
                val ageMs = now - it.lastModified()
                if (ageMs < minAgeSeconds * 1000) {
                    Log.d(TAG, "Skipping too-new file: ${it.name}, ageMs=$ageMs")
                    false
                } else {
                    true
                }
            }
            ?.toList()
            .orEmpty()
    }

}
