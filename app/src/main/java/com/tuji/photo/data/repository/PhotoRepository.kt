package com.tuji.photo.data.repository

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import androidx.room.withTransaction
import com.tuji.photo.data.local.EditProjectDao
import com.tuji.photo.data.local.EditProjectEntity
import com.tuji.photo.data.local.PhotoDatabase
import com.tuji.photo.data.model.PhotoTemplate
import com.tuji.photo.ui.editor.Adjustments
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** 图库中的一张照片（来自 MediaStore）。 */
data class GalleryPhoto(val uri: String, val dateTaken: Long)

/**
 * Repository 层：统一收口图库读取（MediaStore）与编辑项目持久化（Room）。
 */
@Singleton
class PhotoRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: PhotoDatabase,
) {
    private val dao: EditProjectDao = database.editProjectDao()

    /** 最近的照片，最多取 60 张用于 3×N 网格。 */
    suspend fun loadRecentPhotos(limit: Int = 60): List<GalleryPhoto> =
        withContext(Dispatchers.IO) {
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATE_ADDED,
            )
            val results = mutableListOf<GalleryPhoto>()
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC",
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                while (cursor.moveToNext() && results.size < limit) {
                    val id = cursor.getLong(idCol)
                    val uri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
                    )
                    results += GalleryPhoto(uri.toString(), cursor.getLong(dateCol))
                }
            }
            results
        }

    fun observeLatestProject(): Flow<EditProjectEntity?> = dao.observeLatest()

    fun observeAllProjects(): Flow<List<EditProjectEntity>> = dao.observeAll()

    suspend fun getProject(id: Long): EditProjectEntity? = dao.getById(id)

    suspend fun createProject(imageUri: String, template: PhotoTemplate? = null): Long {
        val now = System.currentTimeMillis()
        return dao.insert(
            EditProjectEntity(
                title = template?.name ?: "未命名编辑",
                imageUri = imageUri,
                adjustmentsJson = template?.let {
                    Adjustments(it.brightness, it.contrast, it.saturation, it.temperature).toJson()
                } ?: "{}",
                createdAt = now,
                lastEditedAt = now,
                borderStyle = template?.borderStyle ?: 1,
                watermarkPosition = template?.watermarkPosition ?: 8,
            )
        )
    }

    suspend fun saveAdjustments(projectId: Long, adjustmentsJson: String) =
        dao.updateAdjustments(projectId, adjustmentsJson, System.currentTimeMillis())

    suspend fun <R> inTransaction(block: suspend () -> R): R =
        database.withTransaction { block() }
}
