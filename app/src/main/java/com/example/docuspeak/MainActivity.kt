package com.example.docuspeak

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.*
import java.util.*

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var btnOpenFile: FloatingActionButton
    private lateinit var btnPlayTts: FloatingActionButton
    private lateinit var btnStopTts: FloatingActionButton
    private lateinit var tvStatus: TextView
    private lateinit var progressBar: LinearProgressIndicator
    private lateinit var readerPanel: MaterialCardView
    private lateinit var tvHighlighter: TextView
    private lateinit var adapter: PdfPageAdapter

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private val extractedPagesOrder = Collections.synchronizedList(mutableListOf<DocumentPage>())
    private var isExtracting = false
    private var extractionJob: Job? = null

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { processFile(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        loadSavedTheme()
        super.onCreate(savedInstanceState)
        
        // Proper Edge-to-Edge: Transparent bars, content under status bar
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        
        // Allow content to draw in notch area (for true floating look)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = 
                android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        
        setContentView(R.layout.activity_main)

        recyclerView  = findViewById(R.id.recyclerView)
        btnOpenFile   = findViewById(R.id.btnOpenFile)
        btnPlayTts    = findViewById(R.id.btnPlayTts)
        btnStopTts    = findViewById(R.id.btnStopTts)
        val btnPauseTts: FloatingActionButton = findViewById(R.id.btnPauseTts)
        tvStatus      = findViewById(R.id.tvStatus)
        progressBar   = findViewById(R.id.progressBar)
        readerPanel   = findViewById(R.id.readerPanel)
        tvHighlighter = findViewById(R.id.tvHighlighter)
        val btnOptions: View = findViewById(R.id.btnOptions)

        adapter = PdfPageAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        btnOptions.setOnClickListener { showDevMenu(it) }

        tts = TextToSpeech(this, this)

        btnOpenFile.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/pdf"
            }
            filePickerLauncher.launch(intent)
        }

        btnPlayTts.setOnClickListener {
            syncButtons(true)
            if (extractedPagesOrder.isEmpty()) {
                if (isExtracting) {
                    Toast.makeText(this, "Wait a second, extracting first page...", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "No text found", Toast.LENGTH_SHORT).show()
                }
                syncButtons(false)
            } else {
                startChunkedTts()
            }
        }
        btnPauseTts.setOnClickListener { tts?.stop(); readerPanel.visibility = View.GONE }
        btnStopTts.setOnClickListener { 
            tts?.stop()
            tvStatus.text = "Stopped"
            readerPanel.visibility = View.GONE
            syncButtons(false)
        }
    }

    private fun showDevMenu(view: View) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.menu_dev, popup.menu)

        try {
            val fields = popup.javaClass.declaredFields
            for (field in fields) {
                if ("mPopup" == field.name) {
                    field.isAccessible = true
                    val menuPopupHelper = field.get(popup)
                    val classPopupHelper = Class.forName(menuPopupHelper.javaClass.name)
                    val setForceIcons = classPopupHelper.getMethod("setForceShowIcon", Boolean::class.javaPrimitiveType)
                    setForceIcons.invoke(menuPopupHelper, true)
                    break
                }
            }
        } catch (e: Exception) { e.printStackTrace() }

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_github -> {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Hirojonn")))
                    true
                }
                R.id.theme_light -> { updateTheme(AppCompatDelegate.MODE_NIGHT_NO); true }
                R.id.theme_dark -> { updateTheme(AppCompatDelegate.MODE_NIGHT_YES); true }
                R.id.theme_system -> { updateTheme(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM); true }
                R.id.menu_voice -> { showVoiceDialog(); true }
                else -> false
            }
        }
        popup.show()
    }

    private fun showVoiceDialog() {
        if (!ttsReady) {
            Toast.makeText(this, "TTS not ready", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Fetch all available voices and sort them alphabetically by language
        val allVoices = tts?.voices?.toList()?.sortedBy { it.locale.displayLanguage } ?: emptyList()
        
        if (allVoices.isEmpty()) {
            Toast.makeText(this, "No voices found on your device", Toast.LENGTH_SHORT).show()
            return
        }

        val displayNames = allVoices.mapIndexed { index, voice -> 
            getFriendlyVoiceName(voice, index)
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Select Narrator")
            .setItems(displayNames) { _, which ->
                val selectedVoice = allVoices[which]
                tts?.voice = selectedVoice
                tts?.language = selectedVoice.locale // Important: Also set language
                getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().putString("tts_voice", selectedVoice.name).apply()
                Toast.makeText(this, "Selected: ${displayNames[which]}", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun getFriendlyVoiceName(voice: Voice, index: Int): String {
        val lang = voice.locale.displayLanguage
        val country = voice.locale.displayCountry
        val nameLower = voice.name.lowercase()
        
        // Removed emojis for compatibility as requested
        val persona = when {
            nameLower.contains("male") || nameLower.contains("-m-") || nameLower.contains("guy") -> "Boy/Male"
            nameLower.contains("female") || nameLower.contains("-f-") || nameLower.contains("girl") -> "Girl/Female"
            else -> "Voice ${index + 1}"
        }

        return if (country.isNotBlank()) {
            "$lang ($country) — $persona"
        } else {
            "$lang — $persona"
        }
    }

    private fun updateTheme(mode: Int) {
        AppCompatDelegate.setDefaultNightMode(mode)
        getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().putInt("theme_mode", mode).apply()
    }

    private fun loadSavedTheme() {
        val mode = getSharedPreferences("prefs", Context.MODE_PRIVATE).getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    private fun processFile(uri: Uri) {
        extractionJob?.cancel()
        extractedPagesOrder.clear()
        adapter.clear()
        isExtracting = true

        progressBar.visibility = View.VISIBLE
        progressBar.isIndeterminate = true
        tvStatus.text = "Opening PDF..."

        recyclerView.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            PdfHelper(this@MainActivity).streamPages(uri) { page ->
                runOnUiThread { adapter.addPage(page) }
            }
        }

        extractionJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                PdfHelper(this@MainActivity).extractTextStreamed(uri, { current, total ->
                    runOnUiThread {
                        progressBar.isIndeterminate = false
                        progressBar.max = total
                        progressBar.progress = current
                        tvStatus.text = "Extracting Page $current/$total"
                    }
                }, { pageText, pageWords, pw, ph ->
                    val page = DocumentPage(null, pageText, pageWords, pw, ph)
                    extractedPagesOrder.add(page)
                })
            } finally {
                withContext(Dispatchers.Main) {
                    isExtracting = false
                    progressBar.visibility = View.GONE
                    if (extractedPagesOrder.isNotEmpty()) {
                        val wordCount = extractedPagesOrder.sumOf { it.text.split("\\s+".toRegex()).size }
                        tvStatus.text = "Ready to Read ($wordCount total words)"
                    }
                }
            }
        }
    }

    private fun startChunkedTts() {
        tts?.stop()
        readerPanel.visibility = View.VISIBLE
        
        val pages = ArrayList(extractedPagesOrder)
        
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?) { 
                if (id == "CHUNK_LAST") runOnUiThread { 
                    tvStatus.text = "Finished reading."
                    readerPanel.visibility = View.GONE
                    syncButtons(false)
                }
            }
            override fun onError(id: String?) {}
            
            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                val pageIdx = utteranceId?.substringAfter("CHUNK_")?.toIntOrNull() ?: 0
                val page = extractedPagesOrder.getOrNull(pageIdx) ?: return
                val pageText = page.text
                
                runOnUiThread {
                    // 1. Update Floating Highlighter Snippet
                    val windowSize = 40
                    val startIdx = (start - windowSize).coerceAtLeast(0)
                    val endIdx = (end + windowSize).coerceAtMost(pageText.length)
                    var actualStart = startIdx
                    while (actualStart > 0 && pageText[actualStart-1] !in " \n") actualStart--
                    var actualEnd = endIdx
                    while (actualEnd < pageText.length && pageText[actualEnd] !in " \n") actualEnd++
                    
                    val snippet = pageText.substring(actualStart, actualEnd)
                    val relativeStart = (start - actualStart).coerceAtLeast(0)
                    val relativeEnd = (end - actualStart).coerceAtMost(snippet.length)
                    val spannable = SpannableString(snippet)
                    val highlightColor = if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) 
                        Color.parseColor("#447B61FF") else Color.parseColor("#227B61FF")
                    
                    if (relativeStart < relativeEnd) {
                        try { spannable.setSpan(BackgroundColorSpan(highlightColor), relativeStart, relativeEnd, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE) } catch (e: Exception) {}
                    }
                    tvHighlighter.text = spannable

                    // 2. Direct PDF Word Tracking
                    val word = page.words.find { it.startIdx <= start && it.endIdx >= end }
                    if (word != null) {
                        // Scroll to page if needed
                        recyclerView.smoothScrollToPosition(pageIdx)
                        
                        val vh = recyclerView.findViewHolderForAdapterPosition(pageIdx) as? PdfPageAdapter.VH
                        if (vh != null) {
                            // Calculate scale dynamically using actual PDF dimensions
                            val scaleX = vh.img.width.toFloat() / page.pdfWidth
                            val scaleY = vh.img.height.toFloat() / page.pdfHeight
                            
                            val highlightRect = RectF(
                                word.x * scaleX, 
                                word.y * scaleX, // PDFBox uses consistent scaling for points
                                (word.x + word.w) * scaleX, 
                                (word.y + word.h) * scaleX
                            )
                            vh.overlay.setHighlight(highlightRect)
                        }
                    }
                }
            }
        })

        // Apply saved voice
        val savedVoiceName = getSharedPreferences("prefs", Context.MODE_PRIVATE).getString("tts_voice", null)
        if (savedVoiceName != null) {
            tts?.voices?.find { it.name == savedVoiceName }?.let { 
                tts?.voice = it 
                tts?.language = it.locale
            }
        }

        // Apply Speed and Pitch settings
        val prefs_ = getSharedPreferences("prefs", Context.MODE_PRIVATE)
        val speed = prefs_.getFloat("tts_speed", 1.0f)
        val pitch = prefs_.getFloat("tts_pitch", 1.0f)
        tts?.setSpeechRate(speed)
        tts?.setPitch(pitch)

        for (i in pages.indices) {
            val mode = if (i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            val utteranceId = if (i == pages.size - 1) "CHUNK_LAST" else "CHUNK_$i"
            tts?.speak(pages[i].text, mode, null, utteranceId)
        }
        tvStatus.text = " Reading aloud…"
    }

    private fun syncButtons(playing: Boolean) {
        btnPlayTts.alpha = if (playing) 0.5f else 1.0f
        btnPlayTts.isClickable = !playing
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) { tts?.language = Locale.US; ttsReady = true }
    }

    override fun onDestroy() { tts?.stop(); tts?.shutdown(); super.onDestroy() }

    inner class PdfPageAdapter : RecyclerView.Adapter<PdfPageAdapter.VH>() {
        private val pages = mutableListOf<DocumentPage>()
        fun addPage(page: DocumentPage) { pages.add(page); notifyItemInserted(pages.size - 1) }
        fun getPage(idx: Int) = pages.getOrNull(idx)
        fun clear() { pages.clear(); notifyDataSetChanged() }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_page, parent, false)
            return VH(v)
        }
        override fun onBindViewHolder(holder: VH, position: Int) {
            pages[position].bitmap?.let { 
                holder.img.setImageBitmap(it)
                holder.img.visibility = View.VISIBLE 
            }
            holder.overlay.clear()
        }
        override fun getItemCount() = pages.size
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val img: ImageView = v.findViewById(R.id.pageImage)
            val overlay: HighlightOverlay = v.findViewById(R.id.highlightOverlay)
        }
    }
}
