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
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.model.MapInteractionTool
import com.example.data.model.MapLayerMode
import com.example.util.GeoCoordinateMapper
import com.example.util.GeoCoordinateMapper.GeoLatLng
import java.io.File
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.infowindow.InfoWindow

/**
 * High-resolution Esri World Imagery satellite tile source (`z/y/x` URL format).
 * Works immediately in keyless demo mode with automatic SQLite disk caching for offline field use.
 */
internal val EsriWorldImageryTileSource: ITileSource =
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

internal fun ensureOsmDroidConfigured(context: Context) {
  val appCtx = context.applicationContext
  val config = Configuration.getInstance()
  runCatching {
    config.load(appCtx, appCtx.getSharedPreferences("fellah_osmdroid_prefs", Context.MODE_PRIVATE))
  }
  config.userAgentValue = "${appCtx.packageName}/1.0 (FellahNumerique-OsmdroidGIS)"
  val baseDir = File(appCtx.cacheDir, "fellah_osm_base").apply { mkdirs() }
  val tileCacheDir = File(appCtx.cacheDir, "fellah_osm_tiles").apply { mkdirs() }
  config.osmdroidBasePath = baseDir
  config.osmdroidTileCache = tileCacheDir
  config.tileDownloadThreads = 2
  config.tileFileSystemThreads = 2
  config.tileFileSystemCacheMaxBytes = 48L * 1024L * 1024L
  config.tileFileSystemCacheTrimBytes = 36L * 1024L * 1024L
}

private fun createVertexNodeDrawable(
  context: Context,
  label: String,
  isDrawingActive: Boolean,
  totalVertices: Int,
): Drawable {
  val density = context.resources.displayMetrics.density
  val baseDp = if (totalVertices >= 6) 22f else 25f
  val sizePx = (baseDp * density).toInt().coerceAtLeast(22)
  val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
  val canvas = Canvas(bitmap)
  val cx = sizePx / 2f
  val cy = sizePx / 2f
  val radius = sizePx * 0.41f

  val haloPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.argb(95, 0, 0, 0)
      style = Paint.Style.FILL
    }
  val fillPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = if (isDrawingActive) Color.parseColor("#15803D") else Color.parseColor("#1E7E34")
      style = Paint.Style.FILL
    }
  val strokePaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = if (isDrawingActive) Color.parseColor("#BBF7D0") else Color.WHITE
      style = Paint.Style.STROKE
      strokeWidth = 2.2f * density
    }
  val textPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.WHITE
      textSize = (if (totalVertices >= 6) 8.2f else 9f) * density
      textAlign = Paint.Align.CENTER
      isFakeBoldText = true
    }

  canvas.drawCircle(cx, cy, radius + 1.2f * density, haloPaint)
  canvas.drawCircle(cx, cy, radius, fillPaint)
  canvas.drawCircle(cx, cy, radius, strokePaint)
  val textY = cy - (textPaint.descent() + textPaint.ascent()) / 2f
  canvas.drawText(label, cx, textY, textPaint)
  return BitmapDrawable(context.resources, bitmap)
}

private fun createForageBoreholeDrawable(context: Context): Drawable {
  val density = context.resources.displayMetrics.density
  val sizePx = (26f * density).toInt().coerceAtLeast(24)
  val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
  val canvas = Canvas(bitmap)
  val cx = sizePx / 2f
  val cy = sizePx / 2f
  val radius = sizePx * 0.41f

  val fillPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.parseColor("#0277BD")
      style = Paint.Style.FILL
    }
  val strokePaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.WHITE
      style = Paint.Style.STROKE
      strokeWidth = 2.4f * density
    }
  val innerDotPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.parseColor("#B3E5FC")
      style = Paint.Style.FILL
    }

  canvas.drawCircle(cx, cy, radius, fillPaint)
  canvas.drawCircle(cx, cy, radius, strokePaint)
  canvas.drawCircle(cx, cy, radius * 0.36f, innerDotPaint)
  return BitmapDrawable(context.resources, bitmap)
}

internal fun populateOsmdroidBoundaryOverlays(
  mapView: MapView,
  parcel: AgriculturalParcel,
  latLngVertices: List<GeoLatLng>,
  wellLatLng: GeoLatLng,
  centroidLatLng: GeoLatLng,
  vertexIcons: List<Drawable>,
  forageIcon: Drawable,
  isDrawingActive: Boolean,
  onTap: (GeoLatLng) -> Unit,
  onVertexDrag: (Int, GeoLatLng) -> Unit,
  activeTool: MapInteractionTool =
    if (isDrawingActive) MapInteractionTool.DRAW_POLYGON else MapInteractionTool.VIEW,
) {
  runCatching { InfoWindow.closeAllInfoWindowsOn(mapView) }
  mapView.overlays.clear()
  val suppressInfoBubbles = activeTool != MapInteractionTool.VIEW

  // 1. Tap Receiver Overlay for placing plot boundary vertices or the Forage borehole
  val eventsOverlay =
    MapEventsOverlay(
      object : MapEventsReceiver {
        override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
          if (p != null) {
            onTap(GeoLatLng(p.latitude, p.longitude))
            return true
          }
          return false
        }

        override fun longPressHelper(p: GeoPoint?): Boolean {
          if (p != null) {
            onTap(GeoLatLng(p.latitude, p.longitude))
            return true
          }
          return false
        }
      }
    )
  mapView.overlays.add(eventsOverlay)

  // 2a. Open Edge Preview when exactly 2 boundary vertices are placed (before polygon closure)
  if (latLngVertices.size == 2) {
    val draftEdgeLine =
      Polyline(mapView).apply {
        setPoints(latLngVertices.map { GeoPoint(it.latitude, it.longitude) })
        outlinePaint.color = Color.parseColor("#4ADE80")
        outlinePaint.strokeWidth = 6.5f
        outlinePaint.pathEffect = DashPathEffect(floatArrayOf(16f, 10f), 0f)
        title = "ضلع حدود قيد الرسم"
        infoWindow = null
      }
    mapView.overlays.add(draftEdgeLine)
  }

  // 2b. Closed Cadastral WGS84 Polygon + Dashed Irrigation Pipeline when >= 3 vertices exist
  if (latLngVertices.size >= 3) {
    val polygon =
      Polygon(mapView).apply {
        points = latLngVertices.map { GeoPoint(it.latitude, it.longitude) }
        fillPaint.color =
          if (isDrawingActive) Color.argb(92, 22, 163, 74) else Color.argb(76, 30, 126, 52)
        outlinePaint.color =
          if (isDrawingActive) Color.parseColor("#4ADE80") else Color.parseColor("#38B000")
        outlinePaint.strokeWidth = if (isDrawingActive) 7.5f else 6.5f
        title = "${parcel.name} (${parcel.ilotNumber})"
        infoWindow = null
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
        outlinePaint.strokeWidth = 5f
        outlinePaint.pathEffect = DashPathEffect(floatArrayOf(18f, 10f), 0f)
        infoWindow = null
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
        if (suppressInfoBubbles) {
          infoWindow = null
          setOnMarkerClickListener { marker, _ ->
            if (activeTool == MapInteractionTool.PLACE_FORAGE) {
              onTap(GeoLatLng(marker.position.latitude, marker.position.longitude))
            }
            true
          }
        } else {
          setOnMarkerClickListener { marker, map ->
            InfoWindow.closeAllInfoWindowsOn(map)
            marker.showInfoWindow()
            true
          }
        }
        setOnMarkerDragListener(
          object : Marker.OnMarkerDragListener {
            override fun onMarkerDrag(marker: Marker) = Unit

            override fun onMarkerDragStart(marker: Marker) {
              runCatching { InfoWindow.closeAllInfoWindowsOn(mapView) }
            }

            override fun onMarkerDragEnd(marker: Marker) {
              val pos = marker.position
              onVertexDrag(idx, GeoLatLng(pos.latitude, pos.longitude))
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
      if (suppressInfoBubbles) {
        infoWindow = null
        setOnMarkerClickListener { marker, _ ->
          onTap(GeoLatLng(marker.position.latitude, marker.position.longitude))
          true
        }
      } else {
        setOnMarkerClickListener { marker, map ->
          InfoWindow.closeAllInfoWindowsOn(map)
          marker.showInfoWindow()
          true
        }
      }
    }
  mapView.overlays.add(wellMarker)
}

@Composable
internal fun LiveDemoSatelliteMapViewport(
  parcel: AgriculturalParcel,
  geoCenter: GeoCoordinateMapper.GeoCenter,
  latLngVertices: List<GeoLatLng>,
  wellLatLng: GeoLatLng,
  mapLayerMode: MapLayerMode,
  mapTool: MapInteractionTool = MapInteractionTool.VIEW,
  isEffectiveOffline: Boolean,
  zoomInTrigger: Int = 0,
  zoomOutTrigger: Int = 0,
  fitBoundsTrigger: Int = 0,
  onMapLatLngTap: (GeoLatLng) -> Unit,
  onMapVertexDrag: (Int, GeoLatLng) -> Unit,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val latestTapCallback by rememberUpdatedState(onMapLatLngTap)
  val latestDragCallback by rememberUpdatedState(onMapVertexDrag)
  val isDrawingActive = mapTool == MapInteractionTool.DRAW_POLYGON

  remember(context) {
    ensureOsmDroidConfigured(context)
    true
  }

  val vertexIcons =
    remember(context, latLngVertices.size, isDrawingActive) {
      val count = latLngVertices.size.coerceAtLeast(1)
      List(count) { idx ->
        createVertexNodeDrawable(context, "${idx + 1}", isDrawingActive, count)
      }
    }
  val forageIcon = remember(context) { createForageBoreholeDrawable(context) }

  val centroidLatLng =
    remember(latLngVertices, geoCenter) {
      GeoCoordinateMapper.computeCentroid(
        latLngVertices,
        GeoLatLng(geoCenter.latitude, geoCenter.longitude),
      )
    }

  var mapViewRef by remember { mutableStateOf<MapView?>(null) }
  var lastCenteredCoord by remember { mutableStateOf<GeoCoordinateMapper.GeoCenter?>(null) }
  var lastParcelId by remember { mutableStateOf(parcel.id) }
  var handledZoomIn by remember { mutableIntStateOf(zoomInTrigger) }
  var handledZoomOut by remember { mutableIntStateOf(zoomOutTrigger) }
  var handledFitBounds by remember { mutableIntStateOf(fitBoundsTrigger) }

  LaunchedEffect(geoCenter, parcel.id, mapTool, mapViewRef) {
    val mapView = mapViewRef ?: return@LaunchedEffect
    val prev = lastCenteredCoord
    val movedFar =
      prev == null ||
        parcel.id != lastParcelId ||
        GeoCoordinateMapper.computeHaversineDistanceMeters(
          GeoLatLng(prev.latitude, prev.longitude),
          GeoLatLng(geoCenter.latitude, geoCenter.longitude),
        ) > 850.0
    if (movedFar && mapTool != MapInteractionTool.DRAW_POLYGON) {
      lastCenteredCoord = geoCenter
      lastParcelId = parcel.id
      mapView.controller.animateTo(GeoPoint(geoCenter.latitude, geoCenter.longitude), 17.2, 650L)
    } else if (prev == null) {
      lastCenteredCoord = geoCenter
      lastParcelId = parcel.id
    }
  }

  LaunchedEffect(zoomInTrigger, mapViewRef) {
    val mapView = mapViewRef ?: return@LaunchedEffect
    if (zoomInTrigger != handledZoomIn) {
      handledZoomIn = zoomInTrigger
      mapView.controller.zoomIn()
    }
  }

  LaunchedEffect(zoomOutTrigger, mapViewRef) {
    val mapView = mapViewRef ?: return@LaunchedEffect
    if (zoomOutTrigger != handledZoomOut) {
      handledZoomOut = zoomOutTrigger
      mapView.controller.zoomOut()
    }
  }

  LaunchedEffect(fitBoundsTrigger, mapViewRef) {
    val mapView = mapViewRef ?: return@LaunchedEffect
    if (fitBoundsTrigger != handledFitBounds) {
      handledFitBounds = fitBoundsTrigger
      when {
        latLngVertices.size >= 2 -> {
          val geoPoints = latLngVertices.map { GeoPoint(it.latitude, it.longitude) }
          val bbox = BoundingBox.fromGeoPoints(geoPoints).increaseByScale(1.35f)
          if (mapView.width > 0 && mapView.height > 0) {
            runCatching { mapView.zoomToBoundingBox(bbox, true, 56) }
              .onFailure {
                mapView.controller.setCenter(
                  GeoPoint(centroidLatLng.latitude, centroidLatLng.longitude)
                )
                mapView.controller.setZoom(17.2)
              }
          } else {
            mapView.controller.setCenter(
              GeoPoint(centroidLatLng.latitude, centroidLatLng.longitude)
            )
            mapView.controller.setZoom(17.2)
          }
        }
        latLngVertices.size == 1 -> {
          val single = latLngVertices.first()
          mapView.controller.animateTo(GeoPoint(single.latitude, single.longitude), 17.6, 450L)
        }
        else -> {
          mapView.controller.animateTo(
            GeoPoint(geoCenter.latitude, geoCenter.longitude),
            17.2,
            450L,
          )
        }
      }
    }
  }

  DisposableEffect(lifecycleOwner, mapViewRef) {
    val mapView = mapViewRef
    if (mapView != null && lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
      runCatching { mapView.onResume() }
    }
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_RESUME -> runCatching { mapView?.onResume() }
        Lifecycle.Event.ON_PAUSE -> runCatching { mapView?.onPause() }
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      runCatching { mapView?.onPause() }
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

      populateOsmdroidBoundaryOverlays(
        mapView = mapView,
        parcel = parcel,
        latLngVertices = latLngVertices,
        wellLatLng = wellLatLng,
        centroidLatLng = centroidLatLng,
        vertexIcons = vertexIcons,
        forageIcon = forageIcon,
        isDrawingActive = isDrawingActive,
        onTap = { latestTapCallback(it) },
        onVertexDrag = { idx, pos -> latestDragCallback(idx, pos) },
        activeTool = mapTool,
      )

      mapView.invalidate()
    },
  )
}
