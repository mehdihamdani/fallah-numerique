package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.AgriculturalParcel
import com.example.data.model.MapLayerMode
import com.example.data.model.NormalizedPoint
import com.example.util.GeoCoordinateMapper
import com.example.util.GeoCoordinateMapper.GeoLatLng
import java.io.File
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline

/**
 * High-resolution Esri World Imagery satellite tile source (`z/y/x` URL format).
 * Works immediately in keyless demo mode with automatic SQLite disk caching for offline field use.
 */
private val EsriWorldImageryTileSource: ITileSource =
  object :
    OnlineTileSourceBase(
      "EsriWorldImagery",
      0,
      19,
      256,
      ".jpg",
      arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/"),
    ) {
    override fun getTileURLString(pMapTileIndex: Long): String {
      val zoom = MapTileIndex.getZoom(pMapTileIndex)
      val y = MapTileIndex.getY(pMapTileIndex)
      val x = MapTileIndex.getX(pMapTileIndex)
      return baseUrl + zoom + "/" + y + "/" + x
    }
  }

internal fun isRunningInUnitTestEnvironment(): Boolean {
  return Build.FINGERPRINT.equals("robolectric", ignoreCase = true)
}

private fun ensureOsmDroidConfigured(context: Context) {
  val appCtx = context.applicationContext
  val config = Configuration.getInstance()
  config.userAgentValue = "${appCtx.packageName}/1.0 (FellahNumerique-DemoGIS)"
  val baseDir = File(appCtx.cacheDir, "fellah_osm_base").apply { mkdirs() }
  val tileCacheDir = File(appCtx.cacheDir, "fellah_osm_tiles").apply { mkdirs() }
  config.osmdroidBasePath = baseDir
  config.osmdroidTileCache = tileCacheDir
  config.tileFileSystemCacheMaxBytes = 150L * 1024L * 1024L
  config.tileFileSystemCacheTrimBytes = 120L * 1024L * 1024L
}

private fun createVertexNodeDrawable(context: Context, label: String): Drawable {
  val density = context.resources.displayMetrics.density
  val sizePx = (30f * density).toInt().coerceAtLeast(28)
  val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
  val canvas = Canvas(bitmap)
  val cx = sizePx / 2f
  val cy = sizePx / 2f
  val radius = sizePx * 0.40f

  val fillPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.parseColor("#1E7E34")
      style = Paint.Style.FILL
    }
  val strokePaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.WHITE
      style = Paint.Style.STROKE
      strokeWidth = 2.8f * density
    }
  val textPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.WHITE
      textSize = 9.5f * density
      textAlign = Paint.Align.CENTER
      isFakeBoldText = true
    }

  canvas.drawCircle(cx, cy, radius, fillPaint)
  canvas.drawCircle(cx, cy, radius, strokePaint)
  val textY = cy - (textPaint.descent() + textPaint.ascent()) / 2f
  canvas.drawText(label, cx, textY, textPaint)
  return BitmapDrawable(context.resources, bitmap)
}

private fun createForageBoreholeDrawable(context: Context): Drawable {
  val density = context.resources.displayMetrics.density
  val sizePx = (34f * density).toInt().coerceAtLeast(32)
  val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
  val canvas = Canvas(bitmap)
  val cx = sizePx / 2f
  val cy = sizePx / 2f
  val radius = sizePx * 0.42f

  val fillPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.parseColor("#0277BD")
      style = Paint.Style.FILL
    }
  val strokePaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.WHITE
      style = Paint.Style.STROKE
      strokeWidth = 3f * density
    }
  val innerDotPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.parseColor("#B3E5FC")
      style = Paint.Style.FILL
    }

  canvas.drawCircle(cx, cy, radius, fillPaint)
  canvas.drawCircle(cx, cy, radius, strokePaint)
  canvas.drawCircle(cx, cy, radius * 0.38f, innerDotPaint)
  return BitmapDrawable(context.resources, bitmap)
}

@Composable
internal fun LiveDemoSatelliteMapViewport(
  parcel: AgriculturalParcel,
  geoCenter: GeoCoordinateMapper.GeoCenter,
  vertices: List<NormalizedPoint>,
  wellPt: NormalizedPoint,
  mapLayerMode: MapLayerMode,
  isEffectiveOffline: Boolean,
  onMapLatLngTap: (GeoLatLng) -> Unit,
  onMapVertexDrag: (Int, GeoLatLng) -> Unit,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val latestTapCallback by rememberUpdatedState(onMapLatLngTap)
  val latestDragCallback by rememberUpdatedState(onMapVertexDrag)

  remember(context) {
    ensureOsmDroidConfigured(context)
    true
  }

  val vertexIcons =
    remember(context, vertices.size) {
      List(vertices.size.coerceAtLeast(1)) { idx ->
        createVertexNodeDrawable(context, "${idx + 1}")
      }
    }
  val forageIcon = remember(context) { createForageBoreholeDrawable(context) }

  val latLngVertices =
    remember(vertices, geoCenter) {
      vertices.map { GeoCoordinateMapper.normalizedToLatLng(it, geoCenter) }
    }
  val wellLatLng =
    remember(wellPt, geoCenter) {
      GeoCoordinateMapper.normalizedToLatLng(wellPt, geoCenter)
    }
  val centroidLatLng =
    remember(latLngVertices, geoCenter) {
      GeoCoordinateMapper.computeCentroid(
        latLngVertices,
        GeoLatLng(geoCenter.latitude, geoCenter.longitude),
      )
    }

  var mapViewRef by remember { mutableStateOf<MapView?>(null) }
  var lastCenteredCoord by remember { mutableStateOf<GeoCoordinateMapper.GeoCenter?>(null) }

  LaunchedEffect(geoCenter, mapViewRef) {
    val mapView = mapViewRef ?: return@LaunchedEffect
    if (lastCenteredCoord != geoCenter) {
      lastCenteredCoord = geoCenter
      mapView.controller.animateTo(GeoPoint(geoCenter.latitude, geoCenter.longitude), 17.2, 650L)
    }
  }

  DisposableEffect(lifecycleOwner, mapViewRef) {
    val mapView = mapViewRef
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_RESUME -> mapView?.onResume()
        Lifecycle.Event.ON_PAUSE -> mapView?.onPause()
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      mapView?.onDetach()
    }
  }

  AndroidView(
    modifier = Modifier.fillMaxSize().testTag("demo_live_satellite_map_view"),
    factory = { ctx ->
      ensureOsmDroidConfigured(ctx)
      MapView(ctx).apply {
        setMultiTouchControls(true)
        zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        isTilesScaledToDpi = true
        minZoomLevel = 5.5
        maxZoomLevel = 19.5
        controller.setZoom(17.2)
        controller.setCenter(GeoPoint(geoCenter.latitude, geoCenter.longitude))
        // Prevent parent vertical scroll container from intercepting map pan/pinch gestures
        setOnTouchListener { view, _ ->
          view.parent?.requestDisallowInterceptTouchEvent(true)
          false
        }
        mapViewRef = this
      }
    },
    update = { mapView ->
      val desiredTileSource =
        when (mapLayerMode) {
          MapLayerMode.SATELLITE -> EsriWorldImageryTileSource
          MapLayerMode.RELIEF -> TileSourceFactory.OpenTopo
          MapLayerMode.CADASTRE -> TileSourceFactory.MAPNIK
        }
      if (mapView.tileProvider.tileSource.name() != desiredTileSource.name()) {
        mapView.setTileSource(desiredTileSource)
      }

      // In Offline Mode, read strictly from the local SQLite tile cache without network requests
      mapView.setUseDataConnection(!isEffectiveOffline)

      mapView.overlays.clear()

      // 1. Tap Receiver Overlay for adding polygon vertices or placing the Forage borehole
      val eventsOverlay =
        MapEventsOverlay(
          object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
              if (p != null) {
                latestTapCallback(GeoLatLng(p.latitude, p.longitude))
                return true
              }
              return false
            }

            override fun longPressHelper(p: GeoPoint?): Boolean {
              if (p != null) {
                latestTapCallback(GeoLatLng(p.latitude, p.longitude))
                return true
              }
              return false
            }
          }
        )
      mapView.overlays.add(eventsOverlay)

      // 2. Cadastral WGS84 Polygon + Dashed Irrigation Pipeline
      if (latLngVertices.size >= 3) {
        val polygon =
          Polygon(mapView).apply {
            points = latLngVertices.map { GeoPoint(it.latitude, it.longitude) }
            fillPaint.color = Color.argb(86, 30, 126, 52)
            outlinePaint.color = Color.parseColor("#38B000")
            outlinePaint.strokeWidth = 7f
            title = "${parcel.name} (${parcel.ilotNumber})"
          }
        mapView.overlays.add(polygon)

        val irrigationLine =
          Polyline(mapView).apply {
            setPoints(
              listOf(
                GeoPoint(wellLatLng.latitude, wellLatLng.longitude),
                GeoPoint(centroidLatLng.latitude, centroidLatLng.longitude),
              )
            )
            outlinePaint.color = Color.parseColor("#F59E0B")
            outlinePaint.strokeWidth = 6f
            outlinePaint.pathEffect = DashPathEffect(floatArrayOf(18f, 10f), 0f)
          }
        mapView.overlays.add(irrigationLine)
      }

      // 3. Draggable WGS84 Polygon Vertex Markers (R1..Rn)
      latLngVertices.forEachIndexed { idx, vertex ->
        val vertexMarker =
          Marker(mapView).apply {
            position = GeoPoint(vertex.latitude, vertex.longitude)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            icon = vertexIcons.getOrElse(idx) { vertexIcons.first() }
            title = "نقطة حدود R${idx + 1}"
            snippet = GeoCoordinateMapper.formatDecimalLabel(vertex.latitude, vertex.longitude)
            isDraggable = true
            setOnMarkerDragListener(
              object : Marker.OnMarkerDragListener {
                override fun onMarkerDrag(marker: Marker) = Unit

                override fun onMarkerDragStart(marker: Marker) = Unit

                override fun onMarkerDragEnd(marker: Marker) {
                  val pos = marker.position
                  latestDragCallback(idx, GeoLatLng(pos.latitude, pos.longitude))
                }
              }
            )
          }
        mapView.overlays.add(vertexMarker)
      }

      // 4. Borehole (Forage) Marker
      val wellMarker =
        Marker(mapView).apply {
          position = GeoPoint(wellLatLng.latitude, wellLatLng.longitude)
          setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
          icon = forageIcon
          title = parcel.wellDepthLabel
          snippet = "نقطة السقي الفلاحي (Forage)"
        }
      mapView.overlays.add(wellMarker)

      mapView.invalidate()
    },
  )
}
