package com.example.data.offline

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.AgriculturalParcel
import com.example.util.GeoCoordinateMapper
import com.example.util.GeoCoordinateMapper.GeoLatLng
import kotlin.math.round
import kotlinx.coroutines.flow.Flow

/**
 * Room entity storing locally saved agricultural plots on the device, including custom plot names,
 * calibrated geodesic area measurements (`areaHectares`, `areaSquareMeters`), perimeter (`perimeterMeters`),
 * centroid/borehole WGS84 coordinates, and agronomic metadata.
 */
@Entity(
  tableName = "saved_plots",
  indices = [
    Index(value = ["updatedAtMillis"]),
    Index(value = ["plotName"]),
    Index(value = ["cloudParcelId"]),
  ],
)
data class SavedPlotEntity(
  @PrimaryKey(autoGenerate = true) val localId: Long = 0L,
  val cloudParcelId: String = "",
  val plotName: String,
  val ilotReference: String = "",
  val wilaya: String = "",
  val commune: String = "",
  val mainCrop: String = "",
  val areaHectares: Double,
  val areaSquareMeters: Double,
  val perimeterMeters: Double,
  val vertexCount: Int,
  val centroidLatitude: Double,
  val centroidLongitude: Double,
  val boreholeLatitude: Double,
  val boreholeLongitude: Double,
  val coordinatesLabel: String,
  val createdAtMillis: Long = System.currentTimeMillis(),
  val updatedAtMillis: Long = System.currentTimeMillis(),
)

/**
 * Relational Room child entity storing each ordered WGS84 boundary vertex (`R1..Rn`) of a saved plot.
 * Cascades automatically when the parent [SavedPlotEntity] is deleted.
 */
@Entity(
  tableName = "saved_plot_coordinates",
  foreignKeys = [
    ForeignKey(
      entity = SavedPlotEntity::class,
      parentColumns = ["localId"],
      childColumns = ["plotLocalId"],
      onDelete = ForeignKey.CASCADE,
    )
  ],
  indices = [
    Index(value = ["plotLocalId"]),
  ],
)
data class SavedPlotCoordinateEntity(
  @PrimaryKey(autoGenerate = true) val coordinateId: Long = 0L,
  val plotLocalId: Long,
  val vertexOrder: Int,
  val latitude: Double,
  val longitude: Double,
)

/**
 * Relational projection joining a [SavedPlotEntity] with all of its ordered boundary coordinates.
 */
data class SavedPlotWithCoordinates(
  @Embedded val plot: SavedPlotEntity,
  @Relation(
    parentColumn = "localId",
    entityColumn = "plotLocalId",
  )
  val coordinates: List<SavedPlotCoordinateEntity>,
) {
  val orderedCoordinates: List<SavedPlotCoordinateEntity>
    get() = coordinates.sortedBy { it.vertexOrder }

  fun toGeoLatLngList(): List<GeoLatLng> =
    orderedCoordinates.map { GeoLatLng(it.latitude, it.longitude) }

  fun toAgriculturalParcel(
    baseTemplate: AgriculturalParcel = AgriculturalParcel(),
  ): AgriculturalParcel {
    val encodedVertices =
      orderedCoordinates.map {
        GeoCoordinateMapper.encodeGeoPoint(GeoLatLng(it.latitude, it.longitude))
      }
    val encodedBorehole =
      GeoCoordinateMapper.encodeGeoPoint(GeoLatLng(plot.boreholeLatitude, plot.boreholeLongitude))
    return baseTemplate.copy(
      id = plot.cloudParcelId,
      name = plot.plotName,
      wilaya = plot.wilaya.ifBlank { baseTemplate.wilaya },
      commune = plot.commune.ifBlank { baseTemplate.commune },
      ilotNumber = plot.ilotReference.ifBlank { baseTemplate.ilotNumber },
      mainCrop = plot.mainCrop,
      areaHectares = plot.areaHectares,
      coordinatesLabel = plot.coordinatesLabel,
      polygonPoints =
        if (encodedVertices.size >= 3) encodedVertices else baseTemplate.polygonPoints,
      wellPoint = encodedBorehole,
    )
  }
}

@Dao
interface SavedPlotDao {
  @Transaction
  @Query("SELECT * FROM saved_plots ORDER BY updatedAtMillis DESC")
  fun observeAllSavedPlots(): Flow<List<SavedPlotWithCoordinates>>

  @Transaction
  @Query(
    """
    SELECT * FROM saved_plots
    WHERE plotName LIKE '%' || :query || '%'
       OR wilaya LIKE '%' || :query || '%'
       OR ilotReference LIKE '%' || :query || '%'
    ORDER BY updatedAtMillis DESC
    """
  )
  fun searchSavedPlots(query: String): Flow<List<SavedPlotWithCoordinates>>

  @Transaction
  @Query("SELECT * FROM saved_plots WHERE localId = :localId LIMIT 1")
  suspend fun getSavedPlotById(localId: Long): SavedPlotWithCoordinates?

  @Query("SELECT * FROM saved_plots WHERE cloudParcelId = :cloudParcelId AND cloudParcelId != '' LIMIT 1")
  suspend fun findByCloudParcelId(cloudParcelId: String): SavedPlotEntity?

  @Query("SELECT * FROM saved_plots WHERE plotName = :plotName LIMIT 1")
  suspend fun findByExactPlotName(plotName: String): SavedPlotEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPlot(plot: SavedPlotEntity): Long

  @Update
  suspend fun updatePlot(plot: SavedPlotEntity)

  @Query(
    "UPDATE saved_plots SET plotName = :customName, updatedAtMillis = :updatedAtMillis WHERE localId = :localId"
  )
  suspend fun renamePlot(
    localId: Long,
    customName: String,
    updatedAtMillis: Long = System.currentTimeMillis(),
  )

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCoordinates(coordinates: List<SavedPlotCoordinateEntity>)

  @Query("DELETE FROM saved_plot_coordinates WHERE plotLocalId = :plotLocalId")
  suspend fun deleteCoordinatesForPlot(plotLocalId: Long)

  @Query("DELETE FROM saved_plots WHERE localId = :localId")
  suspend fun deletePlotById(localId: Long)

  @Query("DELETE FROM saved_plots")
  suspend fun clearAllSavedPlots()

  @Transaction
  suspend fun upsertPlotWithCoordinates(
    plot: SavedPlotEntity,
    vertices: List<GeoLatLng>,
  ): Long {
    val existing: SavedPlotEntity? =
      when {
        plot.localId > 0L -> getSavedPlotById(plot.localId)?.plot
        plot.cloudParcelId.isNotBlank() -> findByCloudParcelId(plot.cloudParcelId)
        else -> findByExactPlotName(plot.plotName)
      }

    val now = System.currentTimeMillis()
    val savedPlotId =
      if (existing != null) {
        val merged =
          plot.copy(
            localId = existing.localId,
            cloudParcelId = plot.cloudParcelId.ifBlank { existing.cloudParcelId },
            createdAtMillis = existing.createdAtMillis,
            updatedAtMillis = now,
          )
        updatePlot(merged)
        deleteCoordinatesForPlot(existing.localId)
        existing.localId
      } else {
        insertPlot(plot.copy(localId = 0L, updatedAtMillis = now))
      }

    if (vertices.isNotEmpty()) {
      insertCoordinates(
        vertices.mapIndexed { idx, coord ->
          SavedPlotCoordinateEntity(
            plotLocalId = savedPlotId,
            vertexOrder = idx,
            latitude = coord.latitude,
            longitude = coord.longitude,
          )
        }
      )
    }
    return savedPlotId
  }
}

/**
 * Repository abstracting local Room persistence for saved plot boundaries, area measurements,
 * and custom plot names.
 */
class LocalPlotRepository(private val dao: SavedPlotDao) {
  val savedPlots: Flow<List<SavedPlotWithCoordinates>> = dao.observeAllSavedPlots()

  fun searchSavedPlots(query: String): Flow<List<SavedPlotWithCoordinates>> {
    val trimmed = query.trim()
    return if (trimmed.isEmpty()) dao.observeAllSavedPlots() else dao.searchSavedPlots(trimmed)
  }

  suspend fun getSavedPlotById(localId: Long): SavedPlotWithCoordinates? =
    dao.getSavedPlotById(localId)

  suspend fun savePlotFromParcel(
    parcel: AgriculturalParcel,
    existingLocalId: Long = 0L,
  ): Long {
    val center = GeoCoordinateMapper.parseCenterCoordinates(parcel.coordinatesLabel)
    val vertices = GeoCoordinateMapper.decodePolygonToLatLng(parcel.polygonPoints, center)
    val borehole = GeoCoordinateMapper.decodePointToLatLng(parcel.wellPoint, center)
    val geodesicAreaHa =
      if (parcel.areaHectares >= 0.01) parcel.areaHectares
      else GeoCoordinateMapper.computeGeodesicAreaHectares(vertices)
    val areaSqMeters = round(geodesicAreaHa * 10_000.0 * 10.0) / 10.0
    val perimeterMeters = GeoCoordinateMapper.computeGeodesicPerimeterMeters(vertices)
    val centroid =
      GeoCoordinateMapper.computeCentroid(vertices, GeoLatLng(center.latitude, center.longitude))
    val cleanName =
      parcel.name.trim().ifBlank { "قطعة فلاحية - ${parcel.commune}" }.take(120)

    val entity =
      SavedPlotEntity(
        localId = existingLocalId,
        cloudParcelId = parcel.id,
        plotName = cleanName,
        ilotReference = parcel.ilotNumber,
        wilaya = parcel.wilaya,
        commune = parcel.commune,
        mainCrop = parcel.mainCrop.trim().take(80),
        areaHectares = geodesicAreaHa,
        areaSquareMeters = areaSqMeters,
        perimeterMeters = perimeterMeters,
        vertexCount = vertices.size,
        centroidLatitude = centroid.latitude,
        centroidLongitude = centroid.longitude,
        boreholeLatitude = borehole.latitude,
        boreholeLongitude = borehole.longitude,
        coordinatesLabel =
          GeoCoordinateMapper.formatDecimalLabel(centroid.latitude, centroid.longitude),
      )

    return dao.upsertPlotWithCoordinates(entity, vertices)
  }

  suspend fun saveCustomPlot(
    plotName: String,
    vertices: List<GeoLatLng>,
    wilaya: String = "",
    commune: String = "",
    ilotReference: String = "",
    mainCrop: String = "",
    boreholeLatLng: GeoLatLng? = null,
    existingLocalId: Long = 0L,
  ): Long {
    require(vertices.size >= 3) { "At least 3 boundary coordinates are required to save a plot." }
    val areaHa = GeoCoordinateMapper.computeGeodesicAreaHectares(vertices)
    val areaSqMeters = round(areaHa * 10_000.0 * 10.0) / 10.0
    val perimeterMeters = GeoCoordinateMapper.computeGeodesicPerimeterMeters(vertices)
    val centroid = GeoCoordinateMapper.computeCentroid(vertices, vertices.first())
    val borehole = boreholeLatLng ?: centroid
    val cleanName = plotName.trim().ifBlank { "قطعة محفوظة محلياً" }.take(120)

    val entity =
      SavedPlotEntity(
        localId = existingLocalId,
        plotName = cleanName,
        ilotReference = ilotReference,
        wilaya = wilaya,
        commune = commune,
        mainCrop = mainCrop.trim().take(80),
        areaHectares = areaHa,
        areaSquareMeters = areaSqMeters,
        perimeterMeters = perimeterMeters,
        vertexCount = vertices.size,
        centroidLatitude = centroid.latitude,
        centroidLongitude = centroid.longitude,
        boreholeLatitude = borehole.latitude,
        boreholeLongitude = borehole.longitude,
        coordinatesLabel =
          GeoCoordinateMapper.formatDecimalLabel(centroid.latitude, centroid.longitude),
      )

    return dao.upsertPlotWithCoordinates(entity, vertices)
  }

  suspend fun renameSavedPlot(localId: Long, customName: String) {
    val clean = customName.trim().take(120)
    if (clean.isEmpty()) return
    dao.renamePlot(localId = localId, customName = clean)
  }

  suspend fun deleteSavedPlot(localId: Long) {
    dao.deletePlotById(localId)
  }

  suspend fun clearAllSavedPlots() {
    dao.clearAllSavedPlots()
  }
}
