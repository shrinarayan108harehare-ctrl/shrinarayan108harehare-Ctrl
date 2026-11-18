package com.example.ui.manager

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.localization.AppStrings
import com.example.model.DeliveryPerson
import com.example.model.OrderStatus
import com.example.model.PharmacyOrder
import com.example.ui.components.MapPinData
import com.example.ui.components.PharmacyInteractiveMap
import com.example.ui.components.StatusPill
import com.example.ui.theme.EmeraldPrimary
import com.example.util.LocationUtils
import com.example.util.TimezoneUtils
import com.example.viewmodel.ManagerViewModel

@Composable
fun ManagerMapsAndRoutesScreen(
    vm: ManagerViewModel,
    strings: AppStrings,
    isHindi: Boolean
) {
    val orders by vm.orders.collectAsStateWithLifecycle()
    val deliveryStaff by vm.deliveryPersonnel.collectAsStateWithLifecycle()
    val deliveryLocations by vm.deliveryLocations.collectAsStateWithLifecycle()
    val routePlans by vm.routePlans.collectAsStateWithLifecycle()
    val shopSettings by vm.shopSettings.collectAsStateWithLifecycle()
    val routeResult by vm.routeResult.collectAsStateWithLifecycle()

    val shopLat = shopSettings?.shopLat ?: 26.8467
    val shopLng = shopSettings?.shopLng ?: 80.9462

    var subTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Today's Map", "Route Planner", "Live Tracking")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = subTab) {
            tabs.forEachIndexed { idx, title ->
                Tab(
                    selected = subTab == idx,
                    onClick = { subTab = idx },
                    text = { Text(title, maxLines = 1) },
                    modifier = Modifier.testTag("maps_subtab_$idx")
                )
            }
        }

        when (subTab) {
            0 -> TodayDeliveriesMapPane(
                orders = orders,
                shopLat = shopLat,
                shopLng = shopLng,
                strings = strings,
                isHindi = isHindi
            )
            1 -> RoutePlannerPane(
                vm = vm,
                orders = orders,
                deliveryStaff = deliveryStaff.filter { it.active },
                routePlans = routePlans,
                routeResult = routeResult,
                shopLat = shopLat,
                shopLng = shopLng,
                strings = strings
            )
            2 -> LiveTrackingPane(
                deliveryLocations = deliveryLocations,
                orders = orders,
                shopLat = shopLat,
                shopLng = shopLng,
                strings = strings
            )
        }
    }
}

@Composable
private fun TodayDeliveriesMapPane(
    orders: List<PharmacyOrder>,
    shopLat: Double,
    shopLng: Double,
    strings: AppStrings,
    isHindi: Boolean
) {
    val context = LocalContext.current
    val todayKey = TimezoneUtils.todayDateKey()
    val todaysOrders = orders.filter {
        TimezoneUtils.dateKeyFromTimestamp(it.createdAt) == todayKey && it.status != OrderStatus.CANCELLED.wireValue
    }
    var selectedPinOrder by remember { mutableStateOf<PharmacyOrder?>(null) }

    val pins = buildList {
        add(MapPinData("shop", "Prayag Deen & Sons", "Pharmacy Hub", shopLat, shopLng, isHub = true))
        todaysOrders.forEach { o ->
            add(MapPinData(o.id, o.customerName, "${o.orderNumber} • ${o.deliveryArea}", o.deliveryLat, o.deliveryLng))
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(strings.viewAllTodayDeliveries, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            PharmacyInteractiveMap(
                pins = pins,
                centerLat = shopLat,
                centerLng = shopLng,
                onPinClicked = { pin ->
                    selectedPinOrder = todaysOrders.firstOrNull { it.id == pin.id }
                },
                modifier = Modifier.height(260.dp)
            )
        }
        selectedPinOrder?.let { ord ->
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${ord.orderNumber} — ${ord.customerName}", fontWeight = FontWeight.Bold)
                            StatusPill(ord.status, isHindi)
                        }
                        Text("${ord.customerPhone} • ${ord.deliveryAddress}", style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { LocationUtils.openSingleLocationInGoogleMaps(context, ord.deliveryLat, ord.deliveryLng, ord.customerName) }) {
                                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(strings.openInMaps)
                            }
                            TextButton(onClick = { selectedPinOrder = null }) { Text("Close") }
                        }
                    }
                }
            }
        }
        if (todaysOrders.isEmpty()) {
            item { Text("No active orders scheduled for today yet.", style = MaterialTheme.typography.bodyMedium) }
        } else {
            items(todaysOrders, key = { it.id }) { ord ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${ord.orderNumber} • ${ord.customerName}", fontWeight = FontWeight.Bold)
                            Text("${ord.deliveryArea} — ${ord.deliveryAddress}", style = MaterialTheme.typography.bodySmall)
                            StatusPill(ord.status, isHindi)
                        }
                        IconButton(onClick = { LocationUtils.openSingleLocationInGoogleMaps(context, ord.deliveryLat, ord.deliveryLng, ord.customerName) }) {
                            Icon(Icons.Default.OpenInNew, contentDescription = strings.openInMaps)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoutePlannerPane(
    vm: ManagerViewModel,
    orders: List<PharmacyOrder>,
    deliveryStaff: List<DeliveryPerson>,
    routePlans: List<com.example.model.RoutePlan>,
    routeResult: com.example.util.RouteOptimizationResult?,
    shopLat: Double,
    shopLng: Double,
    strings: AppStrings
) {
    val context = LocalContext.current
    val deliverable = orders.filter { it.status !in listOf("DELIVERED", "CANCELLED") }
    var selectedIds by remember { mutableStateOf(deliverable.map { it.id }.toSet()) }
    var showSaveDialog by remember { mutableStateOf(false) }

    val activeStops = routeResult?.orderedStops ?: deliverable.filter { it.id in selectedIds }
    val polyline = remember(activeStops, shopLat, shopLng) {
        if (activeStops.isEmpty()) emptyList()
        else listOf(shopLat to shopLng) + activeStops.map { it.deliveryLat to it.deliveryLng }
    }
    val pins = buildList {
        add(MapPinData("shop", "Pharmacy Hub", "Start", shopLat, shopLng, isHub = true))
        activeStops.forEachIndexed { idx, o ->
            add(MapPinData(o.id, "${idx + 1}. ${o.customerName}", o.deliveryArea, o.deliveryLat, o.deliveryLng))
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(strings.routePlanner, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            PharmacyInteractiveMap(
                pins = pins,
                centerLat = shopLat,
                centerLng = shopLng,
                routePolyline = polyline,
                modifier = Modifier.height(210.dp)
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { vm.optimizeRoute(deliverable.filter { it.id in selectedIds }) },
                    enabled = selectedIds.isNotEmpty(),
                    modifier = Modifier.weight(1f).testTag("optimize_route_btn")
                ) {
                    Icon(Icons.Default.Route, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Optimize (${selectedIds.size})")
                }
                FilledTonalButton(
                    onClick = {
                        if (activeStops.isNotEmpty()) {
                            LocationUtils.launchMultiStopNavigation(context, shopLat, shopLng, activeStops)
                        }
                    },
                    enabled = activeStops.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Navigation, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Navigate Route")
                }
            }
        }
        if (routeResult != null) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "Total Route: ${"%.1f".format(routeResult.totalDistanceKm)} km • ~${routeResult.estimatedMinutes} min (${if (routeResult.usedGoogleRoutesApi) "Google Routes API" else "Local Road Optimizer"})",
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedButton(onClick = { showSaveDialog = true }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Save & Share Route with Delivery Partner")
                        }
                    }
                }
            }
            itemsIndexed(routeResult.orderedStops, key = { idx, it -> "${idx}_${it.id}" }) { idx, stop ->
                Card {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${idx + 1}.", fontWeight = FontWeight.Bold, color = EmeraldPrimary, modifier = Modifier.width(26.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${stop.customerName} (${stop.orderNumber})", fontWeight = FontWeight.SemiBold)
                            Text(stop.deliveryAddress, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(
                            onClick = {
                                if (idx > 0) {
                                    val mutable = routeResult.orderedStops.toMutableList()
                                    val item = mutable.removeAt(idx)
                                    mutable.add(idx - 1, item)
                                    vm.reorderRouteStops(mutable)
                                }
                            },
                            enabled = idx > 0
                        ) { Icon(Icons.Default.ArrowUpward, contentDescription = "Move stop up") }
                        IconButton(
                            onClick = {
                                if (idx < routeResult.orderedStops.lastIndex) {
                                    val mutable = routeResult.orderedStops.toMutableList()
                                    val item = mutable.removeAt(idx)
                                    mutable.add(idx + 1, item)
                                    vm.reorderRouteStops(mutable)
                                }
                            },
                            enabled = idx < routeResult.orderedStops.lastIndex
                        ) { Icon(Icons.Default.ArrowDownward, contentDescription = "Move stop down") }
                    }
                }
            }
        } else {
            item { Text("Select Stops to Include:", style = MaterialTheme.typography.labelLarge) }
            items(deliverable, key = { it.id }) { ord ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Checkbox(
                        checked = ord.id in selectedIds,
                        onCheckedChange = { checked ->
                            selectedIds = if (checked) selectedIds + ord.id else selectedIds - ord.id
                        }
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${ord.orderNumber} — ${ord.customerName}", fontWeight = FontWeight.SemiBold)
                        Text("${ord.deliveryArea}: ${ord.deliveryAddress}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        if (routePlans.isNotEmpty()) {
            item { Text("Saved Route Plans", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
            items(routePlans.take(5), key = { it.id }) { plan ->
                Card {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("${plan.name} → ${plan.deliveryPersonName.ifBlank { "Shared Team" }}", fontWeight = FontWeight.Bold)
                        Text("${plan.stopOrderIds.size} stops • ${"%.1f".format(plan.totalEstimatedDistanceKm)} km • ${plan.totalEstimatedMinutes} min", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        var routeName by remember { mutableStateOf("Morning Route ${TimezoneUtils.todayDateKey()}") }
        var pickedDp by remember { mutableStateOf(deliveryStaff.firstOrNull()) }
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save & Share Route Plan") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = routeName, onValueChange = { routeName = it }, label = { Text("Route Name") })
                    Text("Assign to Delivery Partner:", style = MaterialTheme.typography.labelLarge)
                    deliveryStaff.forEach { dp ->
                        FilledTonalButton(onClick = { pickedDp = dp }, modifier = Modifier.fillMaxWidth()) {
                            Text("${if (pickedDp?.id == dp.id) "✓ " else ""}${dp.name}")
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    vm.saveAndShareRoute(routeName.trim(), pickedDp)
                    showSaveDialog = false
                }) { Text("Save & Share") }
            },
            dismissButton = { TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun LiveTrackingPane(
    deliveryLocations: List<com.example.model.DeliveryLocation>,
    orders: List<PharmacyOrder>,
    shopLat: Double,
    shopLng: Double,
    strings: AppStrings
) {
    val context = LocalContext.current
    val activeLiveCustomers = orders.filter { it.liveLocationShared && it.status in listOf("ASSIGNED", "PICKED_UP", "OUT_FOR_DELIVERY") }
    val pins = buildList {
        add(MapPinData("shop", "Pharmacy Hub", "Prayag Deen & Sons", shopLat, shopLng, isHub = true))
        deliveryLocations.filter { it.trackingActive }.forEach { dl ->
            add(MapPinData(dl.deliveryPersonId, dl.deliveryPersonName, dl.currentStatus, dl.lat, dl.lng, isDriver = true))
        }
        activeLiveCustomers.forEach { o ->
            add(MapPinData(o.id, "Customer: ${o.customerName}", o.orderNumber, o.deliveryLat, o.deliveryLng))
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(strings.liveTracking, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Shows Delivery Staff on active shift + Customers sharing consent-gated live delivery location.", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(6.dp))
            PharmacyInteractiveMap(
                pins = pins,
                centerLat = shopLat,
                centerLng = shopLng,
                modifier = Modifier.height(240.dp)
            )
        }
        items(deliveryLocations, key = { it.id }) { loc ->
            Card {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(loc.deliveryPersonName, fontWeight = FontWeight.Bold)
                        Text("${loc.currentStatus} • Active orders: ${loc.activeOrderCount}", style = MaterialTheme.typography.bodySmall)
                        Text("Last update: ${TimezoneUtils.formatTimeDisplay(loc.lastUpdatedAt)} (±${loc.accuracyMeters.toInt()}m)", style = MaterialTheme.typography.labelSmall)
                    }
                    Text(
                        if (loc.trackingActive) "LIVE" else "OFF",
                        fontWeight = FontWeight.Bold,
                        color = if (loc.trackingActive) EmeraldPrimary else Color.Gray
                    )
                }
            }
        }
        if (activeLiveCustomers.isNotEmpty()) {
            item {
                Spacer(Modifier.height(4.dp))
                Text("Customers Actively Sharing Live Location", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            items(activeLiveCustomers, key = { "cust_live_${it.id}" }) { ord ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f))) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${ord.customerName} (${ord.orderNumber})", fontWeight = FontWeight.Bold)
                            Text("LIVE SHARE", style = MaterialTheme.typography.labelSmall, color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                        }
                        Text("${ord.deliveryArea} — ${ord.deliveryAddress}", style = MaterialTheme.typography.bodySmall)
                        Text("GPS: ${"%.4f".format(ord.deliveryLat)}, ${"%.4f".format(ord.deliveryLng)}", style = MaterialTheme.typography.labelSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = {
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${ord.customerPhone}")))
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Call")
                            }
                            FilledTonalButton(
                                onClick = {
                                    LocationUtils.openSingleLocationInGoogleMaps(context, ord.deliveryLat, ord.deliveryLng, "${ord.customerName} (${ord.orderNumber})")
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(strings.openInMaps)
                            }
                        }
                    }
                }
            }
        }
    }
}
