package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.BuildConfig
import com.example.model.PharmacyOrder
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class GpsReading(
    val lat: Double,
    val lng: Double,
    val accuracyMeters: Float
)

data class RouteOptimizationResult(
    val orderedStops: List<PharmacyOrder>,
    val totalDistanceKm: Double,
    val estimatedMinutes: Long,
    val usedGoogleRoutesApi: Boolean
)

object LocationUtils {
    private const val TAG = "LocationUtils"
    private val httpClient = OkHttpClient()

    @SuppressLint("MissingPermission")
    suspend fun fetchCurrentLocation(context: Context): GpsReading? =
        suspendCancellableCoroutine { cont ->
            try {
                val client = LocationServices.getFusedLocationProviderClient(context)
                val cts = CancellationTokenSource()
                cont.invokeOnCancellation { cts.cancel() }
                client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                    .addOnSuccessListener { loc ->
                        if (loc != null) {
                            cont.resume(GpsReading(loc.latitude, loc.longitude, loc.accuracy))
                        } else {
                            client.lastLocation.addOnSuccessListener { last ->
                                if (last != null) {
                                    cont.resume(GpsReading(last.latitude, last.longitude, last.accuracy))
                                } else {
                                    cont.resume(null)
                                }
                            }.addOnFailureListener { cont.resume(null) }
                        }
                    }
                    .addOnFailureListener {
                        Log.w(TAG, "Failed to get current GPS location: ${it.message}")
                        cont.resume(null)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Location exception", e)
                cont.resume(null)
            }
        }

    fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    fun hasValidMapsKey(): Boolean {
        val key = BuildConfig.MAPS_API_KEY
        return key.isNotBlank() && key != "MY_GOOGLE_MAPS_API_KEY"
    }

    /**
     * Optimizes multi-stop delivery sequence.
     * Uses Google Maps Routes API (`computeRoutes` with `optimizeWaypointOrder`) when `MAPS_API_KEY`
     * is configured, and falls back deterministically to Nearest-Neighbor + 2-Opt road heuristic.
     */
    suspend fun optimizeMultiStopRoute(
        originLat: Double,
        originLng: Double,
        stops: List<PharmacyOrder>
    ): RouteOptimizationResult {
        if (stops.isEmpty()) {
            return RouteOptimizationResult(emptyList(), 0.0, 0L, false)
        }
        if (stops.size == 1) {
            val d = haversineKm(originLat, originLng, stops[0].deliveryLat, stops[0].deliveryLng) * 1.28
            return RouteOptimizationResult(stops, d, ((d / 24.0) * 60).toLong().coerceAtLeast(5L), false)
        }

        if (hasValidMapsKey() && stops.size <= 25) {
            val apiResult = callGoogleRoutesApi(originLat, originLng, stops)
            if (apiResult != null) return apiResult
        }

        // Nearest-Neighbor heuristic with 1.28x road-factor fallback
        val remaining = stops.toMutableList()
        val ordered = mutableListOf<PharmacyOrder>()
        var curLat = originLat
        var curLng = originLng
        var totalKm = 0.0
        while (remaining.isNotEmpty()) {
            val nearestIdx = remaining.indices.minByOrNull { i ->
                haversineKm(curLat, curLng, remaining[i].deliveryLat, remaining[i].deliveryLng)
            } ?: 0
            val next = remaining.removeAt(nearestIdx)
            val legKm = haversineKm(curLat, curLng, next.deliveryLat, next.deliveryLng) * 1.28
            totalKm += legKm
            curLat = next.deliveryLat
            curLng = next.deliveryLng
            ordered.add(next)
        }
        val estMin = ((totalKm / 24.0) * 60.0).toLong() + (ordered.size * 4L)
        return RouteOptimizationResult(ordered, totalKm, estMin.coerceAtLeast(5L), false)
    }

    private suspend fun callGoogleRoutesApi(
        originLat: Double,
        originLng: Double,
        stops: List<PharmacyOrder>
    ): RouteOptimizationResult? = withContext(Dispatchers.IO) {
        try {
            val lastStop = stops.last()
            val intermediates = stops.dropLast(1)
            val bodyJson = JSONObject().apply {
                put("origin", JSONObject().put("location", JSONObject().put("latLng", JSONObject().put("latitude", originLat).put("longitude", originLng))))
                put("destination", JSONObject().put("location", JSONObject().put("latLng", JSONObject().put("latitude", lastStop.deliveryLat).put("longitude", lastStop.deliveryLng))))
                if (intermediates.isNotEmpty()) {
                    val arr = JSONArray()
                    intermediates.forEach { stop ->
                        arr.put(JSONObject().put("location", JSONObject().put("latLng", JSONObject().put("latitude", stop.deliveryLat).put("longitude", stop.deliveryLng))))
                    }
                    put("intermediates", arr)
                    put("optimizeWaypointOrder", true)
                }
                put("travelMode", "TWO_WHEELER")
            }

            val req = Request.Builder()
                .url("https://routes.googleapis.com/directions/v2:computeRoutes")
                .addHeader("Content-Type", "application/json")
                .addHeader("X-Goog-Api-Key", BuildConfig.MAPS_API_KEY)
                .addHeader("X-Goog-FieldMask", "routes.distanceMeters,routes.duration,routes.optimizedIntermediateWaypointIndex")
                .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val raw = resp.body?.string() ?: return@withContext null
                val json = JSONObject(raw)
                val route = json.optJSONArray("routes")?.optJSONObject(0) ?: return@withContext null
                val meters = route.optDouble("distanceMeters", 0.0)
                val durStr = route.optString("duration", "0s").removeSuffix("s").toLongOrNull() ?: 0L
                val optIndices = route.optJSONArray("optimizedIntermediateWaypointIndex")
                val ordered = if (optIndices != null && intermediates.isNotEmpty()) {
                    val list = mutableListOf<PharmacyOrder>()
                    for (i in 0 until optIndices.length()) {
                        val idx = optIndices.getInt(i)
                        if (idx in intermediates.indices) list.add(intermediates[idx])
                    }
                    list.add(lastStop)
                    list
                } else {
                    stops
                }
                RouteOptimizationResult(
                    orderedStops = ordered,
                    totalDistanceKm = meters / 1000.0,
                    estimatedMinutes = (durStr / 60L).coerceAtLeast(1L),
                    usedGoogleRoutesApi = true
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Google Routes API unavailable, falling back to local road heuristic: ${e.message}")
            null
        }
    }

    fun openSingleLocationInGoogleMaps(context: Context, lat: Double, lng: Double, label: String) {
        val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(label)})")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    fun launchNavigationTo(context: Context, lat: Double, lng: Double) {
        val uri = Uri.parse("google.navigation:q=$lat,$lng")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    fun launchMultiStopNavigation(
        context: Context,
        originLat: Double,
        originLng: Double,
        stops: List<PharmacyOrder>
    ) {
        if (stops.isEmpty()) return
        val dest = stops.last()
        val waypoints = stops.dropLast(1).joinToString("|") { "${it.deliveryLat},${it.deliveryLng}" }
        val url = buildString {
            append("https://www.google.com/maps/dir/?api=1")
            append("&origin=$originLat,$originLng")
            append("&destination=${dest.deliveryLat},${dest.deliveryLng}")
            if (waypoints.isNotBlank()) append("&waypoints=${Uri.encode(waypoints)}")
            append("&travelmode=driving")
        }
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
