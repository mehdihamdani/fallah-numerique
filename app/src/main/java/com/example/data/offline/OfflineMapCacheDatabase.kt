package com.example.data.offline

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/**
 * Local SQLite cache table storing downloaded offline agricultural map regions
 * (WGS84 bounding coordinates, zoom level, cached polygon geometry, and disk footprint)
 * so farmers can inspect and delineate parcels in Saharan/rural zones with zero connectivity.
 */
@Entity(tableName = "offline_map_regions")
data class OfflineMapRegionEntity(
  @PrimaryKey val id: String,
  val regionName: String,
  val wilaya: String,
  val parcelReference: String,
  val centerLat: Double,
  val centerLng: Double,
  val zoomLevel: Float,
  val radiusKm: Double,
  val mapLayerCode: String,
  val polygonSnapshotEncoded: String,
  val sizeMb: Double,
  val downloadedAtMillis: Long = System.currentTimeMillis(),
)

@Dao
interface OfflineMapRegionDao {
  @Query("SELECT * FROM offline_map_regions ORDER BY downloadedAtMillis DESC")
  fun observeAllRegions(): Flow<List<OfflineMapRegionEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertRegion(region: OfflineMapRegionEntity)

  @Query("DELETE FROM offline_map_regions WHERE id = :regionId")
  suspend fun deleteRegionById(regionId: String)

  @Query("DELETE FROM offline_map_regions")
  suspend fun clearAllRegions()
}

/**
 * Local SQLite cache table storing sanitized, approved/verified public agricultural news articles
 * retrieved from permitted official RSS feeds or approved editorial publications so both
 * unauthenticated Visitors and offline operators can browse **Fellah Matin** safely.
 */
@Entity(
  tableName = "cached_news_articles",
  indices = [
    androidx.room.Index(value = ["publishedAtEpochMs"]),
    androidx.room.Index(value = ["canonicalUrl"], unique = true),
  ],
)
data class CachedNewsArticleEntity(
  @PrimaryKey val id: String,
  val canonicalUrl: String,
  val title: String,
  val summary: String,
  val sourceName: String,
  val originalUrl: String,
  val publishedAtIso: String,
  val publishedAtEpochMs: Long,
  val retrievedAtEpochMs: Long,
  val categoryCode: String,
  val language: String,
  val wilaya: String,
  val imageUrl: String,
  val verificationStatusCode: String,
)

@Dao
interface CachedNewsArticleDao {
  @Query("SELECT * FROM cached_news_articles ORDER BY publishedAtEpochMs DESC")
  fun observeAllCachedArticles(): Flow<List<CachedNewsArticleEntity>>

  @Query("SELECT * FROM cached_news_articles ORDER BY publishedAtEpochMs DESC")
  suspend fun getAllCachedArticles(): List<CachedNewsArticleEntity>

  @Query("SELECT * FROM cached_news_articles WHERE id = :articleId LIMIT 1")
  suspend fun getCachedArticleById(articleId: String): CachedNewsArticleEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertArticles(articles: List<CachedNewsArticleEntity>)

  @Query("DELETE FROM cached_news_articles WHERE id = :articleId")
  suspend fun deleteArticleById(articleId: String)

  @Query("DELETE FROM cached_news_articles")
  suspend fun clearAllCachedArticles()
}

@Database(
  entities = [
    OfflineMapRegionEntity::class,
    CachedNewsArticleEntity::class,
  ],
  version = 2,
  exportSchema = false,
)
abstract class FellahOfflineDatabase : RoomDatabase() {
  abstract fun offlineMapRegionDao(): OfflineMapRegionDao

  abstract fun cachedNewsArticleDao(): CachedNewsArticleDao

  companion object {
    @Volatile private var instance: FellahOfflineDatabase? = null

    /**
     * Non-destructive SQLite schema migration preserving all existing `offline_map_regions` rows.
     */
    val MIGRATION_1_2: androidx.room.migration.Migration =
      object : androidx.room.migration.Migration(1, 2) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `cached_news_articles` (
              `id` TEXT NOT NULL,
              `canonicalUrl` TEXT NOT NULL,
              `title` TEXT NOT NULL,
              `summary` TEXT NOT NULL,
              `sourceName` TEXT NOT NULL,
              `originalUrl` TEXT NOT NULL,
              `publishedAtIso` TEXT NOT NULL,
              `publishedAtEpochMs` INTEGER NOT NULL,
              `retrievedAtEpochMs` INTEGER NOT NULL,
              `categoryCode` TEXT NOT NULL,
              `language` TEXT NOT NULL,
              `wilaya` TEXT NOT NULL,
              `imageUrl` TEXT NOT NULL,
              `verificationStatusCode` TEXT NOT NULL,
              PRIMARY KEY(`id`)
            )
            """.trimIndent()
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_cached_news_articles_publishedAtEpochMs` ON `cached_news_articles` (`publishedAtEpochMs`)"
          )
          db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_cached_news_articles_canonicalUrl` ON `cached_news_articles` (`canonicalUrl`)"
          )
        }
      }

    fun getInstance(context: Context): FellahOfflineDatabase {
      return instance
        ?: synchronized(this) {
          instance
            ?: Room.databaseBuilder(
                context.applicationContext,
                FellahOfflineDatabase::class.java,
                "fellah_offline_maps_cache.db",
              )
              .addMigrations(MIGRATION_1_2)
              .build()
              .also { instance = it }
        }
    }
  }
}

class OfflineMapCacheRepository(private val dao: OfflineMapRegionDao) {
  val downloadedRegions: Flow<List<OfflineMapRegionEntity>> = dao.observeAllRegions()

  suspend fun saveDownloadedRegion(region: OfflineMapRegionEntity) {
    dao.upsertRegion(region)
  }

  suspend fun deleteRegion(regionId: String) {
    dao.deleteRegionById(regionId)
  }

  suspend fun clearCache() {
    dao.clearAllRegions()
  }
}

