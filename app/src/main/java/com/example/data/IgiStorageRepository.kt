package com.example.data

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import com.example.model.ContentLayer
import com.example.model.RendererBackend
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.Locale

class IgiStorageRepository(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "projectigi_prefs"
        private const val PREF_RENDERER_BACKEND = "renderer_backend"
        private const val PREF_TARGET_FPS = "target_fps"
        private const val PREF_GYRO_ENABLED = "gyro_enabled"
        private const val PREF_RAIN_AUDIO = "rain_audio_enabled"
        private const val PREF_AIM_ASSIST_MODE = "aim_assist_mode"
        private const val PREF_AIM_SNAP_SPEED = "aim_snap_speed"
        private const val PREF_AIM_BREAKAWAY = "aim_breakaway"

        private const val GAME_DIR_NAME = "Project IGI"
        private const val CONTENT_DIR_NAME = "content"
        private const val CONTENT_PROFILE_NAME = "content-profile.json"
        private const val LOCAL_LAYER_ID = "local"
        private const val LOCAL_LAYER_NAME = "Local overrides (game folder)"

        private val SIGNATURE_ITEMS = setOf(
            "common", "missions", "weapons", "menusystem",
            "sounds", "humanplayer", "config.qvm", "igi.exe"
        )
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getRendererBackend(): RendererBackend =
        RendererBackend.fromId(prefs.getString(PREF_RENDERER_BACKEND, RendererBackend.VULKAN.id))

    fun setRendererBackend(backend: RendererBackend) {
        prefs.edit().putString(PREF_RENDERER_BACKEND, backend.id).apply()
    }

    fun getTargetFps(): Int = prefs.getInt(PREF_TARGET_FPS, 30)

    fun setTargetFps(fps: Int) {
        prefs.edit().putInt(PREF_TARGET_FPS, fps).apply()
    }

    fun isGyroEnabled(): Boolean = prefs.getBoolean(PREF_GYRO_ENABLED, true)

    fun setGyroEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_GYRO_ENABLED, enabled).apply()
    }

    fun isRainAudioEnabled(): Boolean = prefs.getBoolean(PREF_RAIN_AUDIO, true)

    fun setRainAudioEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_RAIN_AUDIO, enabled).apply()
    }

    fun getAimAssistConfig(): com.example.combat.AimAssistConfig {
        val mode = com.example.combat.AimAssistMode.fromId(
            prefs.getString(PREF_AIM_ASSIST_MODE, com.example.combat.AimAssistMode.SOFT.id)
        )
        val snap = prefs.getFloat(PREF_AIM_SNAP_SPEED, 13.5f)
        val breakaway = prefs.getFloat(PREF_AIM_BREAKAWAY, 0.085f)
        return com.example.combat.AimAssistConfig(
            mode = mode,
            snapSpeed = snap,
            breakawayDistance = breakaway
        )
    }

    fun setAimAssistConfig(config: com.example.combat.AimAssistConfig) {
        prefs.edit()
            .putString(PREF_AIM_ASSIST_MODE, config.mode.id)
            .putFloat(PREF_AIM_SNAP_SPEED, config.snapSpeed)
            .putFloat(PREF_AIM_BREAKAWAY, config.breakawayDistance)
            .apply()
    }

    fun getGameDataDir(): File = File(context.filesDir, GAME_DIR_NAME)

    fun getContentProfileFile(): File = File(context.filesDir, CONTENT_PROFILE_NAME)

    fun checkExistingGameFolder(): Boolean {
        val dataDir = getGameDataDir()
        if (!dataDir.exists() || !dataDir.isDirectory) return false
        val files = dataDir.listFiles() ?: return false
        var matches = 0
        for (f in files) {
            if (f.name.lowercase(Locale.US) in SIGNATURE_ITEMS) {
                matches++
            }
        }
        return matches >= 3
    }

    sealed class CopyResult {
        data class Success(val message: String, val fileCount: Int = 0) : CopyResult()
        data class Failure(val reason: String) : CopyResult()
    }

    fun installGameFolderFromTree(
        treeUri: Uri,
        onStage: (String) -> Unit,
        onProgress: (percent: Int, doneBytes: Long, totalBytes: Long) -> Unit
    ): CopyResult {
        tryTakePersistablePermission(treeUri)
        val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
        if (rootDoc == null || !rootDoc.exists() || !rootDoc.isDirectory) {
            return CopyResult.Failure("INVALID FOLDER SELECTED")
        }

        val children = rootDoc.listFiles()
        var matches = 0
        for (child in children) {
            val name = child.getName()?.lowercase(Locale.US) ?: continue
            if (name in SIGNATURE_ITEMS) {
                matches++
            }
        }
        if (matches < 2) {
            return CopyResult.Failure("INVALID GAME FOLDER: Missing Project I.G.I. files")
        }

        onStage("COUNTING GAME FILES...")
        val totalBytes = countTreeBytes(rootDoc)
        val targetDataDir = getGameDataDir()
        if (!targetDataDir.exists()) {
            targetDataDir.mkdirs()
        }

        val tracker = ByteProgressTracker(totalBytes, onProgress)
        copyDocumentTree(rootDoc, targetDataDir, tracker)
        onProgress(100, totalBytes, totalBytes)
        return CopyResult.Success("GAME FILES VERIFIED & READY!", countFiles(targetDataDir))
    }

    fun installContentPackFromTree(
        treeUri: Uri,
        onStage: (String) -> Unit,
        onProgress: (percent: Int, doneBytes: Long, totalBytes: Long) -> Unit
    ): CopyResult {
        tryTakePersistablePermission(treeUri)
        val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
        if (rootDoc == null || !rootDoc.exists() || !rootDoc.isDirectory) {
            return CopyResult.Failure("INVALID FOLDER SELECTED")
        }

        val displayName = rootDoc.getName() ?: "pack"
        val packId = sanitizePackId(displayName)
        val targetDir = File(File(context.filesDir, CONTENT_DIR_NAME), packId)

        onStage("COUNTING CONTENT PACK FILES...")
        val totalBytes = countTreeBytes(rootDoc)
        if (targetDir.exists()) {
            targetDir.deleteRecursively()
        }
        targetDir.mkdirs()

        val tracker = ByteProgressTracker(totalBytes, onProgress)
        copyDocumentTree(rootDoc, targetDir, tracker)
        onProgress(100, totalBytes, totalBytes)

        val fileCount = countFiles(targetDir)
        if (fileCount == 0) {
            targetDir.deleteRecursively()
            return CopyResult.Failure("EMPTY CONTENT PACK")
        }

        val recorded = addPackToProfile(packId, displayName, targetDir)
        return if (recorded) {
            CopyResult.Success("CONTENT PACK INSTALLED ($fileCount files)", fileCount)
        } else {
            CopyResult.Failure("PACK INSTALL ERROR: Profile write failed")
        }
    }

    fun ensureContentProfile(): File? {
        val profile = getContentProfileFile()
        return try {
            var changed = false
            val root = if (!profile.exists()) {
                changed = true
                JSONObject()
            } else {
                try {
                    JSONObject(profile.readText(StandardCharsets.UTF_8))
                } catch (_: JSONException) {
                    return profile
                }
            }

            var titles = root.optJSONObject("titles")
            if (titles == null) {
                titles = JSONObject()
                root.put("titles", titles)
                changed = true
            }
            var igi1 = titles.optJSONArray("igi1")
            if (igi1 == null) {
                igi1 = JSONArray()
                titles.put("igi1", igi1)
                changed = true
            }
            var hasLocal = false
            for (i in 0 until igi1.length()) {
                val layer = igi1.optJSONObject(i)
                if (layer != null && LOCAL_LAYER_ID == layer.optString("id")) {
                    hasLocal = true
                    break
                }
            }
            if (!hasLocal) {
                val local = JSONObject().apply {
                    put("id", LOCAL_LAYER_ID)
                    put("name", LOCAL_LAYER_NAME)
                    put("kind", "local")
                    put("enabled", true)
                }
                igi1.put(local)
                changed = true
            }

            if (changed) {
                root.put("schema", 1)
                writeProfileJsonAtomic(profile, root)
            }
            profile
        } catch (_: Exception) {
            null
        }
    }

    fun loadContentLayers(): List<ContentLayer> {
        val profile = ensureContentProfile() ?: return emptyList()
        return try {
            val root = JSONObject(profile.readText(StandardCharsets.UTF_8))
            val igi1 = root.optJSONObject("titles")?.optJSONArray("igi1") ?: return emptyList()
            val list = mutableListOf<ContentLayer>()
            for (i in 0 until igi1.length()) {
                val obj = igi1.optJSONObject(i) ?: continue
                val id = obj.optString("id")
                val kind = obj.optString("kind", "pack")
                val path = if (obj.has("path")) obj.optString("path") else null
                val dir = if (kind == "local") {
                    File(getGameDataDir(), "overrides")
                } else if (!path.isNullOrBlank()) {
                    File(path)
                } else {
                    null
                }
                val fileCount = if (dir != null && dir.exists()) countFiles(dir) else 0
                list.add(
                    ContentLayer(
                        id = id,
                        name = obj.optString("name", id),
                        kind = kind,
                        path = path,
                        enabled = obj.optBoolean("enabled", true),
                        fileCount = fileCount
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun toggleLayerEnabled(layerId: String, enabled: Boolean): List<ContentLayer> {
        updateIgi1Layers { array ->
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                if (obj.optString("id") == layerId) {
                    obj.put("enabled", enabled)
                }
            }
            array
        }
        return loadContentLayers()
    }

    fun moveLayer(layerId: String, delta: Int): List<ContentLayer> {
        updateIgi1Layers { array ->
            val items = (0 until array.length()).mapNotNull { array.optJSONObject(it) }.toMutableList()
            val idx = items.indexOfFirst { it.optString("id") == layerId }
            val targetIdx = idx + delta
            if (idx in items.indices && targetIdx in items.indices) {
                val moved = items.removeAt(idx)
                items.add(targetIdx, moved)
            }
            JSONArray().apply { items.forEach { put(it) } }
        }
        return loadContentLayers()
    }

    fun disableAllContent(): Boolean {
        return try {
            val profile = getContentProfileFile()
            val root = if (profile.exists()) {
                try {
                    JSONObject(profile.readText(StandardCharsets.UTF_8))
                } catch (_: JSONException) {
                    JSONObject()
                }
            } else {
                JSONObject()
            }

            root.put("schema", 1)
            var titles = root.optJSONObject("titles")
            if (titles == null) {
                titles = JSONObject()
                root.put("titles", titles)
            }

            val hasIgi1 = titles.has("igi1")
            val keys = titles.keys()
            while (keys.hasNext()) {
                val layers = titles.optJSONArray(keys.next()) ?: continue
                for (i in 0 until layers.length()) {
                    layers.optJSONObject(i)?.put("enabled", false)
                }
            }
            if (!hasIgi1) {
                val igi1 = JSONArray()
                val local = JSONObject().apply {
                    put("id", LOCAL_LAYER_ID)
                    put("name", LOCAL_LAYER_NAME)
                    put("kind", "local")
                    put("enabled", false)
                }
                igi1.put(local)
                titles.put("igi1", igi1)
            }
            writeProfileJsonAtomic(profile, root)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun exportLogcatToDownloads(): Result<String> {
        return try {
            val process = Runtime.getRuntime().exec("logcat -d -v time")
            val logData = BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                reader.readText()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "projectigi_logs.txt")
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val externalUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
                try {
                    context.contentResolver.delete(
                        externalUri,
                        "${MediaStore.MediaColumns.DISPLAY_NAME}=?",
                        arrayOf("projectigi_logs.txt")
                    )
                } catch (_: Exception) {
                }
                val uri = context.contentResolver.insert(externalUri, values)
                    ?: return Result.failure(IOException("Unable to create Downloads entry"))
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(logData.toByteArray(StandardCharsets.UTF_8))
                }
                Result.success("Downloads/projectigi_logs.txt")
            } else {
                val fallbackFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "projectigi_logs.txt")
                fallbackFile.parentFile?.mkdirs()
                fallbackFile.writeText(logData, StandardCharsets.UTF_8)
                Result.success(fallbackFile.absolutePath)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun updateIgi1Layers(transform: (JSONArray) -> JSONArray) {
        val profile = ensureContentProfile() ?: return
        try {
            val root = JSONObject(profile.readText(StandardCharsets.UTF_8))
            val titles = root.optJSONObject("titles") ?: JSONObject().also { root.put("titles", it) }
            val igi1 = titles.optJSONArray("igi1") ?: JSONArray()
            titles.put("igi1", transform(igi1))
            root.put("schema", 1)
            writeProfileJsonAtomic(profile, root)
        } catch (_: Exception) {
        }
    }

    private fun addPackToProfile(packId: String, packName: String, packDir: File): Boolean {
        val profile = ensureContentProfile() ?: return false
        return try {
            val root = JSONObject(profile.readText(StandardCharsets.UTF_8))
            val titles = root.optJSONObject("titles") ?: JSONObject().also { root.put("titles", it) }
            val igi1 = titles.optJSONArray("igi1") ?: JSONArray().also { titles.put("igi1", it) }
            for (i in igi1.length() - 1 downTo 0) {
                val layer = igi1.optJSONObject(i)
                if (layer != null && packId == layer.optString("id")) {
                    igi1.remove(i)
                }
            }
            val spec = JSONObject().apply {
                put("id", packId)
                put("name", packName)
                put("kind", "pack")
                put("path", packDir.absolutePath)
                put("enabled", true)
            }
            val reordered = JSONArray()
            reordered.put(spec)
            for (i in 0 until igi1.length()) {
                reordered.put(igi1.get(i))
            }
            titles.put("igi1", reordered)
            root.put("schema", 1)
            writeProfileJsonAtomic(profile, root)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun writeProfileJsonAtomic(profile: File, root: JSONObject) {
        val tmp = File(profile.parentFile, "${profile.name}.tmp")
        FileOutputStream(tmp).use { out ->
            out.write(root.toString(2).toByteArray(StandardCharsets.UTF_8))
        }
        if (!tmp.renameTo(profile)) {
            if (profile.exists()) profile.delete()
            if (!tmp.renameTo(profile)) {
                throw IOException("Could not write ${profile.name}")
            }
        }
    }

    private fun tryTakePersistablePermission(treeUri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {
        }
    }

    private fun countTreeBytes(doc: DocumentFile): Long {
        var total = 0L
        for (file in doc.listFiles()) {
            if (file.isDirectory) {
                total += countTreeBytes(file)
            } else if (file.isFile) {
                total += maxOf(0L, file.length())
            }
        }
        return total
    }

    private fun copyDocumentTree(
        sourceDoc: DocumentFile,
        targetDir: File,
        tracker: ByteProgressTracker
    ) {
        if (!targetDir.exists()) targetDir.mkdirs()
        for (file in sourceDoc.listFiles()) {
            val fileName = file.getName() ?: continue
            if (file.isDirectory) {
                copyDocumentTree(file, File(targetDir, fileName), tracker)
            } else if (file.isFile) {
                val targetFile = File(targetDir, fileName)
                if (targetFile.exists() && targetFile.length() == file.length()) {
                    tracker.advance(maxOf(0L, file.length()))
                    continue
                }
                context.contentResolver.openInputStream(file.uri)?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        val buffer = ByteArray(262144)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            tracker.advance(read.toLong())
                        }
                    }
                }
            }
        }
    }

    private fun countFiles(dir: File): Int {
        val children = dir.listFiles() ?: return 0
        var count = 0
        for (child in children) {
            count += if (child.isDirectory) countFiles(child) else 1
        }
        return count
    }

    private fun sanitizePackId(name: String): String {
        val id = name.lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
        return id.ifEmpty { "pack" }
    }

    private class ByteProgressTracker(
        private val totalBytes: Long,
        private val onProgress: (Int, Long, Long) -> Unit
    ) {
        private var doneBytes = 0L
        private var lastUiMs = 0L

        fun advance(bytes: Long) {
            doneBytes += bytes
            if (totalBytes <= 0L) return
            val now = SystemClock.uptimeMillis()
            val pct = minOf(100, ((doneBytes * 100L) / totalBytes).toInt())
            if (pct < 100 && now - lastUiMs < 150L) return
            lastUiMs = now
            onProgress(pct, doneBytes, totalBytes)
        }
    }
}
