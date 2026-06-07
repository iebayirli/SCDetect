package com.iebayirli.scdetect.ml

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

data class ClassificationResult(
    val label: String,
    val confidence: Float,
    val allScores: Map<String, Float>
)

class ScoliosisClassifier(private val context: Context) {

    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()

    private val IMG_SIZE = 224
    private val NUM_BYTES_PER_CHANNEL = 4 // Float32

    init {
        loadModel()
        loadLabels()
    }

    private fun loadModel() {
        val assetFileDescriptor = context.assets.openFd("scoliosis_model.tflite")
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        val modelBuffer = fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            assetFileDescriptor.startOffset,
            assetFileDescriptor.declaredLength
        )
        interpreter = Interpreter(modelBuffer)
    }

    private fun loadLabels() {
        labels = context.assets.open("labels.txt").bufferedReader().readLines()
    }

    fun classify(bitmap: Bitmap): ClassificationResult {
        val resized = Bitmap.createScaledBitmap(bitmap, IMG_SIZE, IMG_SIZE, true)
        val inputBuffer = bitmapToByteBuffer(resized)

        val outputArray = Array(1) { FloatArray(labels.size) }
        interpreter?.run(inputBuffer, outputArray)

        val scores = outputArray[0]
        val maxIdx = scores.indices.maxByOrNull { scores[it] } ?: 0

        val allScores = labels.zip(scores.toList()).toMap()

        return ClassificationResult(
            label = labels[maxIdx],
            confidence = scores[maxIdx],
            allScores = allScores
        )
    }

    private fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(
            1 * IMG_SIZE * IMG_SIZE * 3 * NUM_BYTES_PER_CHANNEL
        )
        byteBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(IMG_SIZE * IMG_SIZE)
        bitmap.getPixels(pixels, 0, IMG_SIZE, 0, 0, IMG_SIZE, IMG_SIZE)

        for (pixel in pixels) {
            val r = ((pixel shr 16) and 0xFF) / 255.0f
            val g = ((pixel shr 8) and 0xFF) / 255.0f
            val b = (pixel and 0xFF) / 255.0f
            byteBuffer.putFloat(r)
            byteBuffer.putFloat(g)
            byteBuffer.putFloat(b)
        }

        return byteBuffer
    }

    fun close() {
        interpreter?.close()
    }
}
