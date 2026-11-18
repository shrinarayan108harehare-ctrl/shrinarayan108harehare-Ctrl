package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SaffronAccent
import com.example.ui.theme.StatusInfo
import com.example.util.LocationUtils
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState

data class MapPinData(
    val id: String,
    val title: String,
    val subtitle: String,
    val lat: Double,
    val lng: Double,
    val isHub: Boolean = false,
    val isDriver: Boolean = false
)

@Composable
fun PharmacyInteractiveMap(
    pins: List<MapPinData>,
    centerLat: Double,
    centerLng: Double,
    routePolyline: List<Pair<Double, Double>> = emptyList(),
    allowTapToMovePin: Boolean = false,
    onMapCoordinateSelected: ((Double, Double) -> Unit)? = null,
    onPinClicked: ((MapPinData) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val hasKey = LocationUtils.hasValidMapsKey()
    if (hasKey) {
        val cameraState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(LatLng(centerLat, centerLng), 13.5f)
        }
        LaunchedEffect(centerLat, centerLng) {
            cameraState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(centerLat, centerLng), 13.5f))
        }
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .testTag("google_map_view")
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraState,
                uiSettings = MapUiSettings(zoomControlsEnabled = true, myLocationButtonEnabled = false),
                onMapClick = { latLng ->
                    if (allowTapToMovePin) {
                        onMapCoordinateSelected?.invoke(latLng.latitude, latLng.longitude)
                    }
                }
            ) {
                pins.forEach { pin ->
                    val hue = when {
                        pin.isHub -> BitmapDescriptorFactory.HUE_GREEN
                        pin.isDriver -> BitmapDescriptorFactory.HUE_AZURE
                        else -> BitmapDescriptorFactory.HUE_ORANGE
                    }
                    Marker(
                        state = MarkerState(position = LatLng(pin.lat, pin.lng)),
                        title = pin.title,
                        snippet = pin.subtitle,
                        icon = BitmapDescriptorFactory.defaultMarker(hue),
                        onClick = {
                            onPinClicked?.invoke(pin)
                            false
                        }
                    )
                }
                if (routePolyline.size >= 2) {
                    Polyline(
                        points = routePolyline.map { LatLng(it.first, it.second) },
                        color = EmeraldPrimary,
                        width = 10f
                    )
                }
            }
        }
    } else {
        // Interactive coordinate grid fallback when MAPS_API_KEY is not yet configured in AI Studio Secrets
        InteractivePinCanvasFallback(
            pins = pins,
            centerLat = centerLat,
            centerLng = centerLng,
            routePolyline = routePolyline,
            allowTapToMovePin = allowTapToMovePin,
            onMapCoordinateSelected = onMapCoordinateSelected,
            onPinClicked = onPinClicked,
            modifier = modifier
        )
    }
}

@Composable
private fun InteractivePinCanvasFallback(
    pins: List<MapPinData>,
    centerLat: Double,
    centerLng: Double,
    routePolyline: List<Pair<Double, Double>>,
    allowTapToMovePin: Boolean,
    onMapCoordinateSelected: ((Double, Double) -> Unit)?,
    onPinClicked: ((MapPinData) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val allLats = (pins.map { it.lat } + centerLat)
    val allLngs = (pins.map { it.lng } + centerLng)
    val minLat = (allLats.minOrNull() ?: centerLat) - 0.015
    val maxLat = (allLats.maxOrNull() ?: centerLat) + 0.015
    val minLng = (allLngs.minOrNull() ?: centerLng) - 0.015
    val maxLng = (allLngs.maxOrNull() ?: centerLng) + 0.015
    val latSpan = (maxLat - minLat).coerceAtLeast(0.02)
    val lngSpan = (maxLng - minLng).coerceAtLeast(0.02)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFE8F1EC))
            .testTag("interactive_map_canvas")
    ) {
        Surface(
            color = EmeraldPrimary.copy(alpha = 0.92f),
            contentColor = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (allowTapToMovePin) "Tap map canvas to adjust delivery pin" else "${pins.size} GPS Pins Plotted",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text("Add MAPS_API_KEY in Secrets for tiles", style = MaterialTheme.typography.labelSmall)
            }
        }
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(pins, centerLat, centerLng, allowTapToMovePin) {
                        detectTapGestures { offset ->
                            val tappedLng = minLng + (offset.x / size.width) * lngSpan
                            val tappedLat = maxLat - (offset.y / size.height) * latSpan
                            val nearestPin = pins.minByOrNull { p ->
                                val px = ((p.lng - minLng) / lngSpan) * size.width
                                val py = ((maxLat - p.lat) / latSpan) * size.height
                                val dx = px - offset.x
                                val dy = py - offset.y
                                dx * dx + dy * dy
                            }
                            if (nearestPin != null && onPinClicked != null) {
                                val px = (((nearestPin.lng - minLng) / lngSpan) * size.width).toFloat()
                                val py = (((maxLat - nearestPin.lat) / latSpan) * size.height).toFloat()
                                val distSq = (px - offset.x) * (px - offset.x) + (py - offset.y) * (py - offset.y)
                                if (distSq < 2500f) {
                                    onPinClicked(nearestPin)
                                    return@detectTapGestures
                                }
                            }
                            if (allowTapToMovePin) {
                                onMapCoordinateSelected?.invoke(tappedLat, tappedLng)
                            }
                        }
                    }
            ) {
                val gridColor = Color(0xFFBBD3C7)
                for (i in 1..5) {
                    val x = size.width * (i / 6f)
                    val y = size.height * (i / 6f)
                    drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.5f)
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.5f)
                }

                if (routePolyline.size >= 2) {
                    for (i in 0 until routePolyline.lastIndex) {
                        val (lat1, lng1) = routePolyline[i]
                        val (lat2, lng2) = routePolyline[i + 1]
                        val p1 = Offset(((lng1 - minLng) / lngSpan * size.width).toFloat(), ((maxLat - lat1) / latSpan * size.height).toFloat())
                        val p2 = Offset(((lng2 - minLng) / lngSpan * size.width).toFloat(), ((maxLat - lat2) / latSpan * size.height).toFloat())
                        drawLine(EmeraldPrimary, p1, p2, strokeWidth = 6f)
                    }
                }

                pins.forEach { pin ->
                    val x = (((pin.lng - minLng) / lngSpan) * size.width).toFloat().coerceIn(20f, size.width - 20f)
                    val y = (((maxLat - pin.lat) / latSpan) * size.height).toFloat().coerceIn(20f, size.height - 20f)
                    val color = when {
                        pin.isHub -> EmeraldPrimary
                        pin.isDriver -> StatusInfo
                        else -> SaffronAccent
                    }
                    drawCircle(color.copy(alpha = 0.22f), radius = 26f, center = Offset(x, y))
                    drawCircle(color, radius = 13f, center = Offset(x, y))
                    drawCircle(Color.White, radius = 5f, center = Offset(x, y))
                }
            }

            if (pins.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = SaffronAccent, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Lat ${"%.4f".format(centerLat)}, Lng ${"%.4f".format(centerLng)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Black
                    )
                }
            }
        }
    }
}
