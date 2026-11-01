package com.example

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AgriculturalParcel
import com.example.data.model.MapInteractionTool
import com.example.data.model.MapLayerMode
import com.example.data.offline.FellahOfflineDatabase
import com.example.data.offline.LocalPlotRepository
import com.example.ui.components.OsmdroidPlotBoundaryLayout
import com.example.ui.theme.MyApplicationTheme
import com.example.util.GeoCoordinateMapper.GeoLatLng
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LocalPlotRoomDatabaseTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var context: Context
  private lateinit var inMemoryDb: FellahOfflineDatabase
  private lateinit var localPlotRepo: LocalPlotRepository

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    inMemoryDb =
      Room.inMemoryDatabaseBuilder(context, FellahOfflineDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    localPlotRepo = LocalPlotRepository(inMemoryDb.savedPlotDao())
  }

  @After
  fun tearDown() {
    inMemoryDb.close()
  }

  @Test
  fun `room schema stores ordered plot coordinates area measurements and custom plot names with cascade delete`() {
    runBlocking {
      val vertices4 =
        listOf(
          GeoLatLng(35.2500, 0.1580),
          GeoLatLng(35.2500, 0.1615),
          GeoLatLng(35.2475, 0.1615),
          GeoLatLng(35.2475, 0.1580),
        )

      val plotId =
        localPlotRepo.saveCustomPlot(
          plotName = "حقل الزيتون الشرقي - معسكر",
          vertices = vertices4,
          wilaya = "29 - معسكر (Mascara)",
          commune = "غريس",
          ilotReference = "Ilot N 44 / P-09",
          mainCrop = "olivier",
        )
      assertTrue(plotId > 0L)

      val allSaved = localPlotRepo.savedPlots.first()
      assertEquals(1, allSaved.size)
      val saved = allSaved.first()
      assertEquals("حقل الزيتون الشرقي - معسكر", saved.plot.plotName)
      assertEquals(4, saved.plot.vertexCount)
      assertEquals(4, saved.orderedCoordinates.size)
      assertEquals(35.2500, saved.orderedCoordinates[0].latitude, 0.00001)
      assertEquals(0.1580, saved.orderedCoordinates[0].longitude, 0.00001)
      assertEquals(35.2475, saved.orderedCoordinates[3].latitude, 0.00001)
      assertTrue("Expected positive area in Ha", saved.plot.areaHectares > 5.0)
      assertEquals(saved.plot.areaHectares * 10_000.0, saved.plot.areaSquareMeters, 1.0)
      assertTrue("Expected positive geodesic perimeter in m", saved.plot.perimeterMeters > 900.0)

      // Custom plot rename & search query
      localPlotRepo.renameSavedPlot(plotId, "واحة النخيل النموذجية - طولقة")
      val searchHits = localPlotRepo.searchSavedPlots("طولقة").first()
      assertEquals(1, searchHits.size)
      assertEquals("واحة النخيل النموذجية - طولقة", searchHits.first().plot.plotName)

      // Update coordinates in-place on the same local plot (5 vertices)
      val vertices5 = vertices4 + GeoLatLng(35.2488, 0.1570)
      val updatedId =
        localPlotRepo.saveCustomPlot(
          plotName = "واحة النخيل النموذجية - طولقة",
          vertices = vertices5,
          existingLocalId = plotId,
        )
      assertEquals(plotId, updatedId)
      val reloaded = localPlotRepo.getSavedPlotById(plotId)
      assertNotNull(reloaded)
      assertEquals(5, reloaded!!.orderedCoordinates.size)

      // Delete plot -> ForeignKey.CASCADE must automatically delete child rows in saved_plot_coordinates
      localPlotRepo.deleteSavedPlot(plotId)
      assertTrue(localPlotRepo.savedPlots.first().isEmpty())

      inMemoryDb.openHelper.readableDatabase
        .query("SELECT COUNT(*) FROM saved_plot_coordinates")
        .use { cursor ->
          assertTrue(cursor.moveToFirst())
          assertEquals(0, cursor.getInt(0))
        }
    }
  }

  @Test
  fun `migration 2 to 3 preserves existing offline map regions and cached news while creating saved plot tables`() {
    runBlocking {
      val dbName = "test_migration_v2_to_v3_${System.currentTimeMillis()}.db"
      val dbFile: File = context.getDatabasePath(dbName)
      dbFile.parentFile?.mkdirs()
      if (dbFile.exists()) dbFile.delete()

      // Seed an authentic Version 2 SQLite database directly
      SQLiteDatabase.openOrCreateDatabase(dbFile, null).use { rawDb ->
        rawDb.execSQL(
          """
          CREATE TABLE IF NOT EXISTS `offline_map_regions` (
            `id` TEXT NOT NULL,
            `regionName` TEXT NOT NULL,
            `wilaya` TEXT NOT NULL,
            `parcelReference` TEXT NOT NULL,
            `centerLat` REAL NOT NULL,
            `centerLng` REAL NOT NULL,
            `zoomLevel` REAL NOT NULL,
            `radiusKm` REAL NOT NULL,
            `mapLayerCode` TEXT NOT NULL,
            `polygonSnapshotEncoded` TEXT NOT NULL,
            `sizeMb` REAL NOT NULL,
            `downloadedAtMillis` INTEGER NOT NULL,
            PRIMARY KEY(`id`)
          )
          """.trimIndent()
        )
        rawDb.execSQL(
          """
          INSERT INTO `offline_map_regions` VALUES (
            'reg_souf', 'وادي سوف', '39 - الوادي', 'Ilot 12', 33.36, 6.86, 15.0, 10.0,
            'SATELLITE', '0.2,0.2|0.8,0.2|0.5,0.8', 18.2, 1700000000000
          )
          """.trimIndent()
        )
        rawDb.execSQL(
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
        rawDb.execSQL(
          "CREATE INDEX IF NOT EXISTS `index_cached_news_articles_publishedAtEpochMs` ON `cached_news_articles` (`publishedAtEpochMs`)"
        )
        rawDb.execSQL(
          "CREATE UNIQUE INDEX IF NOT EXISTS `index_cached_news_articles_canonicalUrl` ON `cached_news_articles` (`canonicalUrl`)"
        )
        rawDb.version = 2
      }

      val migratedDb =
        Room.databaseBuilder(context, FellahOfflineDatabase::class.java, dbName)
          .addMigrations(FellahOfflineDatabase.MIGRATION_1_2, FellahOfflineDatabase.MIGRATION_2_3)
          .allowMainThreadQueries()
          .build()

      try {
        // Trigger Room OpenHelper validation & verify v2 data survived
        val regions = migratedDb.offlineMapRegionDao().observeAllRegions().first()
        assertEquals(1, regions.size)
        assertEquals("reg_souf", regions.first().id)

        val migratedPlotRepo = LocalPlotRepository(migratedDb.savedPlotDao())
        val savedId =
          migratedPlotRepo.saveCustomPlot(
            plotName = "محيط البطاطا - الوادي",
            vertices =
              listOf(
                GeoLatLng(33.3700, 6.8650),
                GeoLatLng(33.3700, 6.8690),
                GeoLatLng(33.3670, 6.8670),
              ),
          )
        assertTrue(savedId > 0L)
        val savedPlots = migratedPlotRepo.savedPlots.first()
        assertEquals(1, savedPlots.size)
        assertEquals("محيط البطاطا - الوادي", savedPlots.first().plot.plotName)
      } finally {
        migratedDb.close()
        context.deleteDatabase(dbName)
      }
    }
  }

  @Test
  fun `localSavedPlotsSection displays custom plot name area measurements and triggers load rename and delete`() {
    runBlocking {
      val savedId =
        localPlotRepo.saveCustomPlot(
          plotName = "بستان البرتقال - بوفاريك",
          vertices =
            listOf(
              GeoLatLng(36.5760, 2.9100),
              GeoLatLng(36.5760, 2.9140),
              GeoLatLng(36.5730, 2.9120),
            ),
          wilaya = "09 - البليدة (Blida)",
        )

      var loadedParcelName = ""

      composeTestRule.setContent {
        val scope = rememberCoroutineScope()
        val plots by localPlotRepo.savedPlots.collectAsState(initial = emptyList())
        var activeParcel by remember { mutableStateOf(AgriculturalParcel()) }

        MyApplicationTheme {
          Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            OsmdroidPlotBoundaryLayout(
              parcel = activeParcel,
              mapLayerMode = MapLayerMode.SATELLITE,
              mapTool = MapInteractionTool.VIEW,
              isEffectiveOffline = true,
              isSavingParcel = false,
              onSelectMapLayer = {},
              onToggleMapTool = {},
              onMapLatLngTap = {},
              onMapVertexDrag = { _, _ -> },
              onUndoVertex = {},
              onClearPolygon = {},
              onResetDefaultPolygon = {},
              onRemoveVertexAt = {},
              onPlotNameChanged = { activeParcel = activeParcel.copy(name = it) },
              onSavePlotBoundary = {},
              localSavedPlots = plots,
              onLoadLocalPlot = { item ->
                activeParcel = item.toAgriculturalParcel(activeParcel)
                loadedParcelName = activeParcel.name
              },
              onRenameLocalPlot = { id, newName ->
                scope.launch { localPlotRepo.renameSavedPlot(id, newName) }
              },
              onDeleteLocalPlot = { id ->
                scope.launch { localPlotRepo.deleteSavedPlot(id) }
              },
            )
          }
        }
      }

      composeTestRule.waitForIdle()
      composeTestRule
        .onNodeWithTag("local_saved_plot_card_$savedId")
        .performScrollTo()
        .assertIsDisplayed()
      composeTestRule.onNodeWithText("بستان البرتقال - بوفاريك").assertIsDisplayed()

      // Load plot onto map
      composeTestRule
        .onNodeWithTag("load_local_plot_button_$savedId")
        .performScrollTo()
        .performClick()
      composeTestRule.waitForIdle()
      assertEquals("بستان البرتقال - بوفاريك", loadedParcelName)

      // Rename custom plot via dialog
      composeTestRule
        .onNodeWithTag("rename_local_plot_button_$savedId")
        .performScrollTo()
        .performClick()
      composeTestRule.waitForIdle()
      composeTestRule
        .onNodeWithTag("local_plot_rename_input")
        .performTextReplacement("بستان الحوامض الممتاز - بوفاريك")
      composeTestRule.onNodeWithTag("local_plot_rename_confirm_button").performClick()
      composeTestRule.waitUntil(timeoutMillis = 5_000) {
        composeTestRule
          .onAllNodesWithText("بستان الحوامض الممتاز - بوفاريك")
          .fetchSemanticsNodes()
          .isNotEmpty()
      }
      composeTestRule.onNodeWithText("بستان الحوامض الممتاز - بوفاريك").assertIsDisplayed()

      // Delete plot locally
      composeTestRule
        .onNodeWithTag("delete_local_plot_button_$savedId")
        .performScrollTo()
        .performClick()
      composeTestRule.waitUntil(timeoutMillis = 5_000) {
        composeTestRule
          .onAllNodesWithTag("local_saved_plots_empty_state")
          .fetchSemanticsNodes()
          .isNotEmpty()
      }
      composeTestRule
        .onNodeWithTag("local_saved_plots_empty_state")
        .performScrollTo()
        .assertIsDisplayed()
    }
  }
}
