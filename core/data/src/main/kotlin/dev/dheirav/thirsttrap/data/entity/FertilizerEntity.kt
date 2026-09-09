package dev.dheirav.thirsttrap.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A bottle in the cupboard.
 *
 * `dilution_text` stores what the user typed off the label, not the parsed
 * result: the text is the fact and the parse is an interpretation of it. Keeping
 * the text means a bottle the parser cannot read today still reads back
 * unchanged, and a better parser later reinterprets old rows for free.
 */
@Entity(tableName = "fertilizers")
data class FertilizerEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "dilution_text") val dilutionText: String? = null,
    val npk: String? = null,
    val note: String? = null,
    val archived: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
