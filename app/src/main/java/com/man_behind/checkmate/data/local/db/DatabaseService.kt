package com.man_behind.checkmate.data.local.db

import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.TypeConverters
import com.man_behind.checkmate.data.local.db.converter.LocalDateTimeConverter
import com.man_behind.checkmate.data.local.db.dao.ChecklistDao
import com.man_behind.checkmate.data.local.db.entity.ChecklistEntity
import com.man_behind.checkmate.data.local.db.entity.ChecklistItemEntity
import com.man_behind.checkmate.data.local.db.entity.ChecklistItemImageEntity
import com.man_behind.checkmate.data.local.db.entity.ChecklistItemOptionEntity
import com.man_behind.checkmate.data.local.db.entity.ChecklistSectionEntity
import javax.inject.Singleton

@Singleton
@Database(
    entities = [
        ChecklistEntity::class,
        ChecklistSectionEntity::class,
        ChecklistItemEntity::class,
        ChecklistItemOptionEntity::class,
        ChecklistItemImageEntity::class
    ],
    version = 2
)
@TypeConverters(
    LocalDateTimeConverter::class
)
abstract class DatabaseService: RoomDatabase() {
    abstract fun checklistDao(): ChecklistDao
}