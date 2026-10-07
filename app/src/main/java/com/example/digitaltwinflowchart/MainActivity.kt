package com.example.digitaltwinflowchart

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private lateinit var canvas: FrameLayout

    // Expertise level, set from the menu: how much of each node description is read
    // 0 = Principiante: type, text and edges
    // 1 = Intermedio: text and edges
    // 2 = Esperto: text only
    private var expertiseLevel = 0

    // null when the device has no vibration motor, as on many tablets
    private val vibrator: Vibrator? by lazy {
        val v = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (v.hasVibrator()) v else null
    }

    // Short pulse on every touch; needs android.permission.VIBRATE in AndroidManifest.xml
    private fun vibrate() {
        val v = vibrator ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(40)
            }
        } catch (e: SecurityException) {
            println("Vibration needs android.permission.VIBRATE in AndroidManifest.xml")
        }
    }

    // File picker to load a new diagram
    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val newJsonString = inputStream?.bufferedReader().use { it?.readText() }

                if (newJsonString != null) {
                    drawFlowchart(newJsonString)
                    tts.speak("Diagramma caricato con successo", TextToSpeech.QUEUE_FLUSH, null, "")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                tts.speak("Errore durante il caricamento del file", TextToSpeech.QUEUE_FLUSH, null, "")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup Screen & UI
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemBars()
        setContentView(R.layout.activity_main)
        canvas = findViewById(R.id.canvas_layout)

        // Setup Text-To-Speech
        tts = TextToSpeech(this, this)

        // Set up the hidden File Picker trigger (Long Press on background)
        canvas.setOnLongClickListener {
            vibrate()
            tts.speak("Seleziona un diagramma", TextToSpeech.QUEUE_FLUSH, null, "")
            pickFileLauncher.launch("*/*")
            true
        }

        // Load Default Diagram on Startup
        try {
            val inputStream: InputStream = assets.open("output_coordinates.json")
            val defaultJsonString = inputStream.bufferedReader().use { it.readText() }
            drawFlowchart(defaultJsonString)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun drawFlowchart(jsonString: String) {
        canvas.removeAllViews()

        val trimmedString = jsonString.trim()
        val jsonObject = if (trimmedString.startsWith("[")) {
            val jsonArray = JSONArray(trimmedString)
            if (jsonArray.length() > 0) jsonArray.getJSONObject(0) else JSONObject()
        } else {
            JSONObject(trimmedString)
        }

        val childrenArray = jsonObject.getJSONArray("children")
        val edgesArray = jsonObject.optJSONArray("edges")

        // Files written by pipeline.py (app_layout.json) carry the position of every
        // hole on the screen in mm, computed by the same code that builds the printed
        // board. When present they are used as they are; older files without them
        // fall back to the layout computation below, exactly as before.
        val usePhysicalLayout = jsonObject.has("screen_width_mm") &&
            (0 until childrenArray.length()).all { childrenArray.getJSONObject(it).has("screen_x_mm") }

        // Screen and node sizes in mm
        val displayMetrics = resources.displayMetrics
        fun mmToPixelsX(mm: Float): Int = ((mm / 25.4f) * displayMetrics.xdpi).toInt()
        fun mmToPixelsY(mm: Float): Int = ((mm / 25.4f) * displayMetrics.ydpi).toInt()

        // Screen size: from the JSON when it provides it, otherwise the old fixed values
        val baseWidthMM = if (usePhysicalLayout) jsonObject.getDouble("screen_width_mm").toFloat() else 135f
        val baseHeightMM = if (usePhysicalLayout) jsonObject.getDouble("screen_height_mm").toFloat() else 217f

        val blockSizeMM = 17f
        val marginXMM = 5f
        val marginYMM = 5f
        val safeMarginX = marginXMM + (blockSizeMM / 2f)
        val safeMarginY = marginYMM + (blockSizeMM / 2f)

        // Layout for files without hole positions: the ELK coordinates scaled to the screen.
        // Bounding box of the node centres and of the bend points of the edges
        var minX = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE; var maxY = -Float.MAX_VALUE

        for (i in 0 until childrenArray.length()) {
            val node = childrenArray.getJSONObject(i)
            val centerX = node.getDouble("x").toFloat() + (node.getDouble("width").toFloat() / 2f)
            val centerY = -(node.getDouble("y").toFloat() + (node.getDouble("height").toFloat() / 2f))

            minX = min(minX, centerX); maxX = max(maxX, centerX)
            minY = min(minY, centerY); maxY = max(maxY, centerY)
        }

        if (edgesArray != null) {
            for (i in 0 until edgesArray.length()) {
                val sections = edgesArray.getJSONObject(i).optJSONArray("sections") ?: continue
                for (j in 0 until sections.length()) {
                    val bendPoints = sections.getJSONObject(j).optJSONArray("bendPoints") ?: continue
                    for (k in 0 until bendPoints.length()) {
                        val bpX = bendPoints.getJSONObject(k).getDouble("x").toFloat()
                        val bpY = -bendPoints.getJSONObject(k).getDouble("y").toFloat()
                        minX = min(minX, bpX); maxX = max(maxX, bpX)
                        minY = min(minY, bpY); maxY = max(maxY, bpY)
                    }
                }
            }
        }

        // Centre the layout horizontally on the column of the first node
        val firstNode = childrenArray.getJSONObject(0)
        val spineX = firstNode.getDouble("x").toFloat() + (firstNode.getDouble("width").toFloat() / 2f)
        val maxReachX = max(abs(maxX - spineX), abs(minX - spineX))

        minX = spineX - maxReachX
        maxX = spineX + maxReachX

        // Scale and offsets
        val gridSpacing = min(
            (baseWidthMM - (safeMarginX * 2)) / max(1f, maxX - minX),
            (baseHeightMM - (safeMarginY * 2)) / max(1f, maxY - minY)
        )

        val xOffsetMM = (baseWidthMM / 2f) - (((minX + maxX) / 2f) * gridSpacing)
        val yOffsetMM = (baseHeightMM / 2f) - (((minY + maxY) / 2f) * gridSpacing)

        // One touch region per node
        val playMM = 8.0f // Safety margin for button overlap
        val drawnSizeMM = blockSizeMM + playMM

        for (i in 0 until childrenArray.length()) {
            val node = childrenArray.getJSONObject(i)
            val nodeId = node.optString("id")
            val nX = node.getDouble("x").toFloat()
            val nY = node.getDouble("y").toFloat()
            val nWidth = node.getDouble("width").toFloat()
            val nHeight = node.getDouble("height").toFloat()

            val centerX = nX + (nWidth / 2f)
            val centerY = -(nY + (nHeight / 2f))
            // Node centre in mm from the top-left corner of the screen
            val physicalX_MM = if (usePhysicalLayout) node.getDouble("screen_x_mm").toFloat() else (centerX * gridSpacing) + xOffsetMM
            val physicalY_MM = if (usePhysicalLayout) node.getDouble("screen_y_mm").toFloat() else baseHeightMM - ((centerY * gridSpacing) + yOffsetMM) // Android Y-Flip

            val pixelX = mmToPixelsX(physicalX_MM - (drawnSizeMM / 2f))
            val pixelY = mmToPixelsY(physicalY_MM - (drawnSizeMM / 2f))
            val pixelWidth = mmToPixelsX(drawnSizeMM)
            val pixelHeight = mmToPixelsY(drawnSizeMM)

            val baseText = node.optJSONArray("labels")?.getJSONObject(0)?.optString("text") ?: "Node"
            val nodeType = node.optString("type")
            val shapeType = node.optString("myCustomShape", "square")

            val button = Button(this@MainActivity).apply {
                text = baseText
                isAllCaps = false
                androidx.core.widget.TextViewCompat.setAutoSizeTextTypeWithDefaults(
                    this, androidx.core.widget.TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM
                )

                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(android.graphics.Color.parseColor("#FFFF00"))
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                }

                // The description is built at each touch, so it follows the current expertise level
                setOnClickListener {
                    vibrate()
                    val spokenText = buildNodeSpokenText(nodeId, baseText, nodeType, shapeType, edgesArray, expertiseLevel)
                    tts.speak(spokenText, TextToSpeech.QUEUE_FLUSH, null, "")
                }
            }

            val params = FrameLayout.LayoutParams(pixelWidth, pixelHeight).apply {
                leftMargin = pixelX
                topMargin = pixelY
            }
            canvas.addView(button, params)
        }

        // Menu button, under the menu opening in the top-right corner of the board
        val menuRadiusMM = 8f
        val menuOffsetMM = 5f
        val menuSizeMM = menuRadiusMM * 2f

        // Center relative to Android's top-left origin (0,0)
        val menuCenterX_MM = baseWidthMM - menuRadiusMM - menuOffsetMM
        val menuCenterY_MM = menuRadiusMM + menuOffsetMM

        val menuPixelX = mmToPixelsX(menuCenterX_MM - (menuSizeMM / 2f))
        val menuPixelY = mmToPixelsY(menuCenterY_MM - (menuSizeMM / 2f))

        val menuButton = Button(this@MainActivity).apply {
            text = "Menu"
            isAllCaps = false
            androidx.core.widget.TextViewCompat.setAutoSizeTextTypeWithDefaults(
                this, androidx.core.widget.TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM
            )
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.parseColor("#FF9800"))
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            }

            // A tap moves to the next expertise level
            setOnClickListener {
                vibrate()
                expertiseLevel = (expertiseLevel + 1) % 3
                val levelName = when(expertiseLevel) {
                    0 -> "Principiante"
                    1 -> "Intermedio"
                    2 -> "Esperto"
                    else -> ""
                }
                tts.speak("Livello $levelName", TextToSpeech.QUEUE_FLUSH, null, "")
            }

            // A long press opens the file picker
            setOnLongClickListener {
                vibrate()
                tts.speak("Seleziona un diagramma", TextToSpeech.QUEUE_FLUSH, null, "")
                pickFileLauncher.launch("*/*")
                true
            }
        }

        val menuParams = FrameLayout.LayoutParams(mmToPixelsX(menuSizeMM), mmToPixelsY(menuSizeMM)).apply {
            leftMargin = menuPixelX
            topMargin = menuPixelY
        }
        canvas.addView(menuButton, menuParams)
    }

    /**
     * Text read aloud when a node is touched, as detailed as the expertise level asks.
     */
    private fun buildNodeSpokenText(nodeId: String, baseText: String, nodeType: String, shapeType: String,
                                    edgesArray: JSONArray?, level: Int): String {

        // Operators read as words, at every level
        val cleanText = baseText.replace("<=", " minore o uguale di ")
            .replace(">=", " maggiore o uguale di ")
            .replace("==", " uguale a ")
            .replace("!=", " diverso da ")
            .replace("<", " minore di ")
            .replace(">", " maggiore di ")
            .replace("++", " più uno ")
            .replace("--", " meno uno ")
            .replace("+", " più ")
            .replace("-", " meno ")
            .replace("=", " uguale a ")

        // Esperto: text only
        if (level == 2) {
            return cleanText
        }

        // Node type: from the layout file, or guessed from the shape for older files without it
        val logicalMeaning = when (nodeType) {
            "start" -> "Nodo Inizio"
            "end" -> "Nodo Fine"
            "decision" -> "Nodo Decisione"
            "io" -> "Nodo Input/Output"
            "process" -> "Nodo Processo"
            else -> when (shapeType) {
                "circle" -> if (baseText.contains("START", ignoreCase = true)) "Nodo Inizio" else "Nodo Fine"
                "diamond" -> "Nodo Decisione"
                "trapezoid" -> "Nodo Input/Output"
                else -> "Nodo Processo"
            }
        }

        // Every edge leaving the node, with its direction and Yes/No label
        var routingText = " "
        if (edgesArray != null) {
            for (j in 0 until edgesArray.length()) {
                val edge = edgesArray.getJSONObject(j)
                if (edge.optString("source") == nodeId) {
                    val edgeLabel = edge.optJSONArray("labels")?.getJSONObject(0)?.optString("text") ?: ""
                    // the model writes the labels in English
                    val spokenLabel = when (edgeLabel.lowercase()) {
                        "yes" -> "Sì"
                        "no" -> "No"
                        else -> edgeLabel
                    }
                    var direction = ""

                    val sections = edge.optJSONArray("sections")
                    if (sections != null && sections.length() > 0) {
                        val section = sections.getJSONObject(0)
                        val startX = section.getJSONObject("startPoint").getDouble("x")
                        val startY = section.getJSONObject("startPoint").getDouble("y")

                        val bendPoints = section.optJSONArray("bendPoints")
                        val nextPoint = if (bendPoints != null && bendPoints.length() > 0) {
                            bendPoints.getJSONObject(0)
                        } else {
                            section.getJSONObject("endPoint")
                        }

                        val dx = nextPoint.getDouble("x") - startX
                        val dy = nextPoint.getDouble("y") - startY

                        direction = if (abs(dx) > abs(dy)) {
                            if (dx > 0) "a destra" else "a sinistra"
                        } else {
                            if (dy > 0) "giù" else "su"
                        }
                    }

                    routingText += if (spokenLabel.isNotEmpty()) " Arco $spokenLabel va $direction. " else " Arco va $direction. "
                }
            }
        }

        // Intermedio: text and edges
        if (level == 1) {
            return "$cleanText. $routingText."
        }

        return if (routingText.trim().isNotEmpty()) "$logicalMeaning. $cleanText... ${routingText.trim()}" else "$logicalMeaning. $cleanText."
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.ITALY)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                println("TTS Error: Italian language pack missing.")
            } else {
                tts.setSpeechRate(1.5f)
                tts.setPitch(0.9f)
            }
        }
    }

    override fun onDestroy() {
        if (::tts.isInitialized) { tts.stop(); tts.shutdown() }
        super.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        windowInsetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}
