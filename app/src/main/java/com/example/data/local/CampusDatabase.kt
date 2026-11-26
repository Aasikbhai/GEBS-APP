package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        StudentRecordEntity::class,
        SmsAlertEntity::class,
        FeeItemEntity::class,
        NoticeEntity::class,
        ComplaintEntity::class,
        ExamScheduleEntity::class,
        TeacherLeaveEntity::class,
        SalaryInquiryEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class CampusDatabase : RoomDatabase() {
    abstract fun campusDao(): CampusDao

    companion object {
        @Volatile
        private var INSTANCE: CampusDatabase? = null

        fun getInstance(context: Context): CampusDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CampusDatabase::class.java,
                    "campuspulse_db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
