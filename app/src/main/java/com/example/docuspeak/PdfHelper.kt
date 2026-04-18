package com.example.docuspeak

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

data class WordInfo(
    val text: String,
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float,
    val startIdx: Int,
    val endIdx: Int
)

data class DocumentPage(
    val bitmap: Bitmap?,
    val text: String = "",
    val words: List<WordInfo> = emptyList(),
    val pdfWidth: Float = 612f,
    val pdfHeight: Float = 792f
)

class PdfHelper(private val context: Context) {

    init {
        PDFBoxResourceLoader.init(context.applicationContext)
    }

    /**
     * Stripper that captures word boundaries and builds synchronized text in a single pass.
     */
    private class WordPositionStripper(val words: MutableList<WordInfo>) : PDFTextStripper() {
        private val sb = StringBuilder()

        fun getBuiltText() = sb.toString()

        override fun writeString(string: String?, textPositions: MutableList<TextPosition>?) {
            if (string == null || textPositions == null) return
            
            if (textPositions.isNotEmpty()) {
                val first = textPositions.first()
                val last = textPositions.last()
                
                val x = first.xDirAdj
                val y = first.yDirAdj
                val w = last.xDirAdj + last.widthDirAdj - x
                val h = textPositions.maxOf { it.heightDir }
                
                val startIdx = sb.length
                sb.append(string)
                val endIdx = sb.length
                
                words.add(WordInfo(
                    text = string,
                    x = x,
                    y = y,
                    w = w,
                    h = h,
                    startIdx = startIdx,
                    endIdx = endIdx
                ))
            }
            // super.writeString here adds its own line separators to the stripper's internal buffer,
            // but we use our 'sb' as the source of truth for TTS.
            super.writeString(string, textPositions)
        }

        // We override this to ensure our 'sb' matches whatever separators the stripper adds.
        override fun writeLineSeparator() {
            sb.append("\n")
            super.writeLineSeparator()
        }

        override fun writeWordSeparator() {
            sb.append(" ")
            super.writeWordSeparator()
        }
    }

    /**
     * Renders PDF pages with high visual fidelity.
     */
    fun streamPages(uri: Uri, onPage: (DocumentPage) -> Unit) {
        val tempFile = File(context.cacheDir, "temp_render_${System.currentTimeMillis()}.pdf")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output -> input.copyTo(output) }
            }

            val pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val displayWidth = context.resources.displayMetrics.widthPixels

            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                val scale = (displayWidth.toFloat() / page.width.toFloat()).coerceAtMost(3.0f)
                val width = (page.width * scale).toInt()
                val height = (page.height * scale).toInt()

                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE)
                
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                onPage(DocumentPage(bitmap))
            }

            renderer.close()
            pfd.close()
        } catch (e: Exception) {
            Log.e("PdfHelper", "Error rendering PDF", e)
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }
    }

    /**
     * Extracts text page-by-page with word coordinates.
     */
    fun extractTextStreamed(uri: Uri, onProgress: (Int, Int) -> Unit, onPageData: (String, List<WordInfo>, Float, Float) -> Unit) {
        var document: PDDocument? = null
        try {
            val inputStream: InputStream = context.contentResolver.openInputStream(uri) ?: return
            document = PDDocument.load(inputStream)
            val totalPages = document.numberOfPages

            for (i in 1..totalPages) {
                val words = mutableListOf<WordInfo>()
                val stripper = WordPositionStripper(words)
                stripper.startPage = i
                stripper.endPage = i
                // We call getText to trigger the extraction process
                stripper.getText(document)
                // BUT we use the text built inside our stripper for 100% index accuracy
                val text = stripper.getBuiltText()
                
                // Get page dimensions for scaling
                val pageObj = document.getPage(i-1)
                val pw = pageObj.mediaBox.width
                val ph = pageObj.mediaBox.height
                
                onProgress(i, totalPages)
                onPageData(text, words, pw, ph)
            }
        } catch (e: Exception) {
            Log.e("PdfHelper", "Error extraction", e)
        } finally {
            document?.close()
        }
    }

    fun extractAllText(uri: Uri): String {
        var document: PDDocument? = null
        return try {
            val inputStream: InputStream = context.contentResolver.openInputStream(uri) ?: return ""
            document = PDDocument.load(inputStream)
            PDFTextStripper().getText(document)
        } catch (e: Exception) { "" } finally { document?.close() }
    }
}
