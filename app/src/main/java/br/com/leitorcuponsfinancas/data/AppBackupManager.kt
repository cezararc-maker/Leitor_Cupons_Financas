package br.com.leitorcuponsfinancas.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class BackupSummary(val createdAt: String, val databaseVersion: Int)

class AppBackupManager(private val context: Context) {

    suspend fun createBackup(destination: Uri): Result<BackupSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val database = AppDatabase.getInstance(context)
            database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() }
            val databaseFile = context.getDatabasePath(DATABASE_NAME)
            check(databaseFile.isFile) { "O banco de dados ainda não foi criado." }
            val profile = UserProfileStore.getInstance(context).profile.value
            val createdAt = Instant.now().toString()
            val profileJson = JSONObject().put("id", profile.id).put("displayName", profile.displayName)
                .toString().toByteArray(Charsets.UTF_8)
            val databaseVersion = database.openHelper.readableDatabase.version
            val manifest = JSONObject()
                .put("format", BACKUP_FORMAT)
                .put("formatVersion", FORMAT_VERSION)
                .put("createdAt", createdAt)
                .put("databaseVersion", databaseVersion)
                .put("databaseSha256", databaseFile.sha256())
                .put("profileSha256", profileJson.sha256())
                .put("appPackage", context.packageName)
                .put("appVersion", appVersion())
                .toString(2).toByteArray(Charsets.UTF_8)
            val output = context.contentResolver.openOutputStream(destination, "w")
                ?: error("O Android não permitiu gravar no local escolhido.")
            output.use { stream ->
                ZipOutputStream(BufferedOutputStream(stream)).use { zip ->
                    zip.putNextEntry(ZipEntry(DATABASE_ENTRY))
                    FileInputStream(databaseFile).use { it.copyTo(zip) }
                    zip.closeEntry()
                    zip.writeEntry(PROFILE_ENTRY, profileJson)
                    zip.writeEntry(MANIFEST_ENTRY, manifest)
                }
            }
            BackupSummary(createdAt, databaseVersion)
        }
    }

    suspend fun restoreBackup(source: Uri): Result<BackupSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val workDir = File(context.cacheDir, "backup_restore_${System.nanoTime()}")
            check(workDir.mkdirs()) { "Não foi possível preparar a restauração." }
            try {
                extractArchive(source, workDir)
                val manifestFile = File(workDir, MANIFEST_ENTRY)
                val databaseFile = File(workDir, DATABASE_ENTRY)
                val profileFile = File(workDir, PROFILE_ENTRY)
                check(manifestFile.isFile && databaseFile.isFile && profileFile.isFile) {
                    "O arquivo não é um backup completo deste aplicativo."
                }
                val manifest = JSONObject(manifestFile.readText())
                check(manifest.optString("format") == BACKUP_FORMAT) { "Formato de backup incompatível." }
                check(manifest.optInt("formatVersion") == FORMAT_VERSION) { "Versão do backup incompatível." }
                check(manifest.optString("appPackage") == context.packageName) { "Este backup pertence a outro aplicativo." }
                check(databaseFile.sha256() == manifest.getString("databaseSha256")) { "O banco do backup está corrompido." }
                check(profileFile.readBytes().sha256() == manifest.getString("profileSha256")) { "O perfil do backup está corrompido." }
                val backupDatabaseVersion = validateDatabase(databaseFile)
                check(backupDatabaseVersion in 1..AppDatabase.VERSION) { "Versão do banco de dados incompatível." }
                val profile = JSONObject(profileFile.readText())
                val profileId = profile.getString("id").trim()
                val profileName = profile.getString("displayName").trim()
                check(profileId.isNotBlank() && profileName.isNotBlank()) { "O perfil do backup é inválido." }
                replaceCurrentData(databaseFile, profileId, profileName)
                BackupSummary(manifest.getString("createdAt"), backupDatabaseVersion)
            } finally {
                workDir.deleteRecursively()
            }
        }
    }

    fun suggestedFileName(): String {
        val stamp = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm").withZone(ZoneId.systemDefault()).format(Instant.now())
        return "Leitor_Cupons_Financas_$stamp.lcfbackup"
    }

    private fun extractArchive(source: Uri, destination: File) {
        val input = context.contentResolver.openInputStream(source) ?: error("O Android não permitiu abrir o arquivo escolhido.")
        var totalBytes = 0L
        val allowedEntries = setOf(DATABASE_ENTRY, PROFILE_ENTRY, MANIFEST_ENTRY)
        input.use { stream ->
            ZipInputStream(BufferedInputStream(stream)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    check(!entry.isDirectory && entry.name in allowedEntries) { "Conteúdo de backup inválido." }
                    val target = File(destination, entry.name)
                    check(!target.exists()) { "O backup contém entradas duplicadas." }
                    BufferedOutputStream(FileOutputStream(target)).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val read = zip.read(buffer)
                            if (read < 0) break
                            totalBytes += read
                            check(totalBytes <= MAX_BACKUP_BYTES) { "O backup excede o limite de segurança." }
                            output.write(buffer, 0, read)
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }
    }

    private fun validateDatabase(file: File): Int {
        val db = SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY)
        return db.use {
            it.rawQuery("PRAGMA quick_check", null).use { cursor ->
                check(cursor.moveToFirst() && cursor.getString(0) == "ok") { "O banco do backup falhou na verificação." }
            }
            val tables = mutableSetOf<String>()
            it.rawQuery("SELECT name FROM sqlite_master WHERE type = 'table'", null).use { cursor ->
                while (cursor.moveToNext()) tables += cursor.getString(0)
            }
            check(tables.containsAll(REQUIRED_TABLES)) { "O backup não contém todas as tabelas necessárias." }
            it.version
        }
    }

    private fun replaceCurrentData(restoredDatabase: File, profileId: String, profileName: String) {
        val current = context.getDatabasePath(DATABASE_NAME)
        val rollback = File(current.parentFile, "$DATABASE_NAME.before_restore")
        val preferences = context.getSharedPreferences("user_profile", Context.MODE_PRIVATE)
        val previousProfileId = preferences.getString("profile_id", null)
        val previousProfileName = preferences.getString("display_name", null)
        current.parentFile?.mkdirs()
        AppDatabase.closeForRestore()
        listOf(File("${current.path}-wal"), File("${current.path}-shm"), rollback).forEach { it.delete() }
        if (current.exists()) check(current.renameTo(rollback)) { "Não foi possível proteger o banco atual." }
        try {
            restoredDatabase.copyTo(current, overwrite = true)
            val saved = preferences.edit()
                .putString("profile_id", profileId).putString("display_name", profileName).commit()
            check(saved) { "Não foi possível restaurar o perfil local." }
            rollback.delete()
            UserProfileStore.resetAfterRestore()
        } catch (error: Throwable) {
            current.delete()
            if (rollback.exists()) rollback.renameTo(current)
            preferences.edit().apply {
                if (previousProfileId == null) remove("profile_id") else putString("profile_id", previousProfileId)
                if (previousProfileName == null) remove("display_name") else putString("display_name", previousProfileName)
            }.commit()
            throw error
        }
    }

    @Suppress("DEPRECATION")
    private fun appVersion(): String = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "desconhecida"
    private fun ZipOutputStream.writeEntry(name: String, bytes: ByteArray) { putNextEntry(ZipEntry(name)); write(bytes); closeEntry() }
    private fun File.sha256(): String = inputStream().use { it.sha256() }
    private fun ByteArray.sha256(): String = inputStream().use { it.sha256() }
    private fun java.io.InputStream.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) { val read = read(buffer); if (read < 0) break; digest.update(buffer, 0, read) }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val DATABASE_NAME = "leitor_cupons_financas.db"
        private const val BACKUP_FORMAT = "br.com.leitorcuponsfinancas.backup"
        private const val FORMAT_VERSION = 1
        private const val DATABASE_ENTRY = "database.db"
        private const val PROFILE_ENTRY = "profile.json"
        private const val MANIFEST_ENTRY = "manifest.json"
        private const val MAX_BACKUP_BYTES = 250L * 1024L * 1024L
        private val REQUIRED_TABLES = setOf("products", "receipts", "receipt_items", "merchant_product_links")
    }
}
