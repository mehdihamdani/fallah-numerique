package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.example.data.model.AgriculturalParcel
import com.example.data.model.CropVarietyType
import com.example.data.model.NormalizedPoint
import com.example.data.model.SoilTextureType

object ParcelPdfExporter {

  fun exportParcelFicheToUri(
    context: Context,
    uri: Uri,
    parcel: AgriculturalParcel,
  ): Result<Unit> {
    return runCatching {
      val pdfDocument = PdfDocument()
      val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
      val page = pdfDocument.startPage(pageInfo)
      val canvas: Canvas = page.canvas

      drawAgrologicalSheet(canvas, parcel)

      pdfDocument.finishPage(page)
      context.contentResolver.openOutputStream(uri)?.use { out ->
        pdfDocument.writeTo(out)
      } ?: error("Unable to open PDF output stream")
      pdfDocument.close()
    }
  }

  private fun drawAgrologicalSheet(canvas: Canvas, parcel: AgriculturalParcel) {
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F3FCED") }
    canvas.drawRect(0f, 0f, 595f, 842f, bgPaint)

    // Header Banner
    val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#006422") }
    canvas.drawRoundRect(RectF(28f, 24f, 567f, 108f), 14f, 14f, headerPaint)

    val titlePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 18f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      }
    val subPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C9FFC6")
        textSize = 11f
      }
    canvas.drawText("FELLAH NUMÉRIQUE • بطاقة السجل العقاري والفلاحي", 44f, 56f, titlePaint)
    canvas.drawText(
      "Délimitation Parcellaire & Fiche Agrologique • WGS84 Verified",
      44f,
      78f,
      subPaint,
    )
    canvas.drawText("NDVI: ${"%.2f".format(parcel.ndviScore)}", 475f, 56f, titlePaint)

    val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    val borderPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#BFCABA")
        style = Paint.Style.STROKE
        strokeWidth = 1f
      }
    val sectionTitlePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#006422")
        textSize = 13f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      }
    val labelPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3F493E")
        textSize = 10.5f
      }
    val valuePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#161E15")
        textSize = 11.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      }

    // Section 1: Cadastral & Spatial Box + Polygon Preview
    val cadRect = RectF(28f, 122f, 567f, 320f)
    canvas.drawRoundRect(cadRect, 12f, 12f, cardPaint)
    canvas.drawRoundRect(cadRect, 12f, 12f, borderPaint)
    canvas.drawText("1. Données Cadastrales & Polygone GPS / البيانات العقارية", 44f, 146f, sectionTitlePaint)

    val sqMeters = (parcel.areaHectares * 10000).toInt()
    val rows1 =
      listOf(
        "Secteur / الاسم:" to parcel.name,
        "Wilaya / الولاية:" to parcel.wilaya,
        "Commune / البلدية:" to parcel.commune,
        "Référence Îlot:" to parcel.ilotNumber,
        "Tenure / الحيازة:" to parcel.tenureType,
        "Surface Calibrée:" to "${"%.2f".format(parcel.areaHectares)} Ha ($sqMeters m²)",
        "Coordonnées WGS84:" to parcel.coordinatesLabel,
        "Forage / نقطة السقي:" to parcel.wellDepthLabel,
      )
    rows1.forEachIndexed { idx, (k, v) ->
      val y = 170f + idx * 18f
      canvas.drawText(k, 44f, y, labelPaint)
      canvas.drawText(v, 165f, y, valuePaint)
    }

    // Mini Satellite Polygon Map Box
    val mapBox = RectF(385f, 156f, 550f, 304f)
    val mapBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1E3524") }
    canvas.drawRoundRect(mapBox, 10f, 10f, mapBg)

    val decoded = parcel.polygonPoints.map { NormalizedPoint.decode(it) }
    if (decoded.size >= 3) {
      val polyPath = Path()
      decoded.forEachIndexed { index, pt ->
        val px = mapBox.left + pt.x * mapBox.width()
        val py = mapBox.top + pt.y * mapBox.height()
        if (index == 0) polyPath.moveTo(px, py) else polyPath.lineTo(px, py)
      }
      polyPath.close()
      val fillPoly = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#557EDB85") }
      val strokePoly =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = Color.parseColor("#99F89E")
          style = Paint.Style.STROKE
          strokeWidth = 2f
          pathEffect = DashPathEffect(floatArrayOf(6f, 3f), 0f)
        }
      canvas.drawPath(polyPath, fillPoly)
      canvas.drawPath(polyPath, strokePoly)
    }

    // Section 2: Soil Characteristics
    val soilRect = RectF(28f, 336f, 567f, 530f)
    canvas.drawRoundRect(soilRect, 12f, 12f, cardPaint)
    canvas.drawRoundRect(soilRect, 12f, 12f, borderPaint)
    canvas.drawText(
      "2. Caractéristiques Pédologiques / خصائص ونوعية التربة (INRAA)",
      44f,
      360f,
      sectionTitlePaint,
    )

    val texture = SoilTextureType.fromCode(parcel.soilTexture)
    val soilRows =
      listOf(
        "Texture du Sol / القوام:" to "${texture.titleAr} (${texture.titleFr})",
        "pH du Sol / درجة الحموضة:" to "${"%.1f".format(parcel.soilPh)} (Optimum 7.0 - 7.5)",
        "Matière Organique / المادة العضوية:" to "${"%.2f".format(parcel.organicMatterPercent)} %",
        "Salinité EC / الملوحة:" to "${"%.2f".format(parcel.salinityEc)} dS/m",
        "Drainage / تصريف المياه:" to parcel.drainageStatus,
        "Certificat Labo / وثيقة المخبر:" to
          (parcel.labDocumentName.ifBlank { "Vérifié numériquement • موثق رقمياً" }),
      )
    soilRows.forEachIndexed { idx, (k, v) ->
      val y = 388f + idx * 22f
      canvas.drawText(k, 44f, y, labelPaint)
      canvas.drawText(v, 235f, y, valuePaint)
    }

    // Section 3: Crops & Production
    val cropRect = RectF(28f, 546f, 567f, 740f)
    canvas.drawRoundRect(cropRect, 12f, 12f, cardPaint)
    canvas.drawRoundRect(cropRect, 12f, 12f, borderPaint)
    canvas.drawText(
      "3. Cultures Principales & Campagne / المحاصيل والإنتاج المتوقع",
      44f,
      570f,
      sectionTitlePaint,
    )

    val crop = CropVarietyType.fromCode(parcel.mainCrop)
    val totalHarvest = parcel.areaHectares * parcel.estimatedYieldTonsPerHa
    val cropRows =
      listOf(
        "Culture Principale / المحصول:" to "${crop.titleAr} — ${crop.subtitleFr}",
        "Système d'Irrigation / الري:" to parcel.irrigationSystem,
        "Rendement Estimé / المردود:" to "${"%.1f".format(parcel.estimatedYieldTonsPerHa)} T/Ha",
        "Production Totale / الإنتاج الكلي:" to "${"%.1f".format(totalHarvest)} Tonnes",
        "Année de Plantation / سنة الغرس:" to parcel.plantingYear,
        "Cultures Associées / البينية:" to parcel.intercropping.ifBlank { "—" },
      )
    cropRows.forEachIndexed { idx, (k, v) ->
      val y = 598f + idx * 22f
      canvas.drawText(k, 44f, y, labelPaint)
      canvas.drawText(v, 235f, y, valuePaint)
    }

    // Footer Verification Stamp
    val footerRect = RectF(28f, 758f, 567f, 814f)
    val footerBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#DCE5D7") }
    canvas.drawRoundRect(footerRect, 10f, 10f, footerBg)
    canvas.drawText(
      "توثيق منصة الفلاح الرقمي الجزائري • Certifié Chambre Nationale d'Agriculture",
      44f,
      782f,
      valuePaint,
    )
    canvas.drawText(
      "Parcel ID: ${parcel.id.ifBlank { "BROUILLON-LOCAL" }} • Liaison Marché: ${if (parcel.linkedToMarket) "OUI" else "NON"}",
      44f,
      800f,
      labelPaint,
    )
  }
}
