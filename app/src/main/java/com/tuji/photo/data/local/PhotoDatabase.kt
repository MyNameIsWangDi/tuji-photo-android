package com.tuji.photo.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/**
 * 一个「编辑项目」= 一张正在/曾经编辑的照片及其参数快照。
 */
@Entity(tableName = "edit_projects")
data class EditProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val imageUri: String,
    val adjustmentsJson: String = "{}",
    val createdAt: Long,
    val lastEditedAt: Long,
    /** 套用模板时记录的边框样式。 */
    val borderStyle: Int = 1,
    /** 套用模板时记录的水印位置（3×3 索引）。 */
    val watermarkPosition: Int = 8,
)

@Dao
interface EditProjectDao {

    @Query("SELECT * FROM edit_projects ORDER BY lastEditedAt DESC LIMIT 1")
    fun observeLatest(): Flow<EditProjectEntity?>

    @Query("SELECT * FROM edit_projects ORDER BY lastEditedAt DESC")
    fun observeAll(): Flow<List<EditProjectEntity>>

    @Query("SELECT * FROM edit_projects WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): EditProjectEntity?

    @Insert
    suspend fun insert(project: EditProjectEntity): Long

    @Query("UPDATE edit_projects SET adjustmentsJson = :json, lastEditedAt = :editedAt WHERE id = :id")
    suspend fun updateAdjustments(id: Long, json: String, editedAt: Long)
}

@Database(entities = [EditProjectEntity::class], version = 2, exportSchema = false)
abstract class PhotoDatabase : RoomDatabase() {
    abstract fun editProjectDao(): EditProjectDao
}
