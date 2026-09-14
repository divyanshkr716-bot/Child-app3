package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope

@Database(
  entities = [
    DeviceEntity::class,
    NotificationEntity::class,
    SmsEntity::class,
    ContactEntity::class,
    CallLogEntity::class,
    TransferEntity::class,
    LocationEntity::class,
    GeofenceEntity::class,
    ManagedAppEntity::class,
    TimelineEventEntity::class,
    ChildRequestEntity::class,
    DeviceAlertEntity::class,
    WebHistoryEntity::class,
    DrivingTripEntity::class,
    DowntimePolicyEntity::class,
    InstantBlockEntity::class,
    DetectionEventEntity::class,
    ImageDetectionEntity::class,
    FamilyMemberEntity::class,
    FamilyChatMessageEntity::class,
    OfflineQueueEntity::class
  ],
  version = 4,
  exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun deviceDao(): DeviceDao
  abstract fun notificationDao(): NotificationDao
  abstract fun smsDao(): SmsDao
  abstract fun contactDao(): ContactDao
  abstract fun callLogDao(): CallLogDao
  abstract fun transferDao(): TransferDao
  abstract fun locationDao(): LocationDao
  abstract fun geofenceDao(): GeofenceDao
  abstract fun managedAppDao(): ManagedAppDao
  abstract fun timelineEventDao(): TimelineEventDao
  abstract fun childRequestDao(): ChildRequestDao
  abstract fun deviceAlertDao(): DeviceAlertDao
  abstract fun webHistoryDao(): WebHistoryDao
  abstract fun drivingTripDao(): DrivingTripDao
  abstract fun downtimePolicyDao(): DowntimePolicyDao
  abstract fun instantBlockDao(): InstantBlockDao
  abstract fun detectionEventDao(): DetectionEventDao
  abstract fun imageDetectionDao(): ImageDetectionDao
  abstract fun familyMemberDao(): FamilyMemberDao
  abstract fun familyChatMessageDao(): FamilyChatMessageDao
  abstract fun offlineQueueDao(): OfflineQueueDao

  companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "airdroid_parent.db"
        )
          .addMigrations(MIGRATION_3_4)
          .build()
        INSTANCE = instance
        instance
      }
    }

    private val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE devices ADD COLUMN authToken TEXT NOT NULL DEFAULT ''")
      }
    }

  }
}
