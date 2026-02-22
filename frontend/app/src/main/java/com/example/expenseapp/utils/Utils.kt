package com.example.expenseapp.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun getCurrentDate(): String {
    return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
}

fun uriToFile(context: Context, uri: Uri): File {
    val inputStream = context.contentResolver.openInputStream(uri)
    val originalBitmap = BitmapFactory.decodeStream(inputStream)

    // Compress the image to save bandwidth and make AI processing faster
    val ratio = 512.0 / originalBitmap.width.coerceAtLeast(1)
    val height = (originalBitmap.height * ratio).toInt()
    val width = 512
    val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, true)

    val file = File(context.cacheDir, "temp_receipt_compressed.jpg")
    val outputStream = FileOutputStream(file)
    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)
    outputStream.flush()
    outputStream.close()

    return file
}