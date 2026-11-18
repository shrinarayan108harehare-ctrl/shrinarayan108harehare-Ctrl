package com.example.ui.manager

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.model.DateRangePreset
import com.example.ui.components.MapPinData
import com.example.ui.components.PharmacyInteractiveMap
import com.example.ui.components.SalesAndProfitChart
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SaffronAccent
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.util.ReportExporter
import com.example.viewmodel.ManagerViewModel

@Composable
fun ManagerBiAnalyticsScreen(
    vm: ManagerViewModel,
    strings: AppStrings,
    isHindi: Boolean
) {
    val context = LocalContext.current
    val preset by vm.datePreset.collectAsStateWithLifecycle()
    val customStart by vm.customStartKey.collectAsStateWithLifecycle()
    val customEnd by vm.customEndKey.collectAsStateWithLifecycle()
    val snap by vm.biSnapshot.collectAsStateWithLifecycle()
    val shopSettings by vm.shopSettings.collectAsStateWithLifecycle()
    val aiInsights by vm.aiBiInsights.collectAsStateWithLifecycle()
    val aiBusy by vm.aiBusy.collectAsStateWithLifecycle()

    var showCustomDialog by remember { mutableStateOf(false) }
    val shopName = shopSettings?.shopName ?: "PRAYAG DEEN & SONS"

    LazyColumn(
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                if (isHindi) "बिजनेस इंटेलिजेंस और वित्तीय विश्लेषण" else "Business Intelligence & Financial Analytics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (isHindi) "अवधि: ${snap.dateRangeLabel} • विधि: ${snap.accountingMethod} • तैयार: ${snap.generatedAtIst}"
                else "Period: ${snap.dateRangeLabel} • Method: ${snap.accountingMethod} • Generated: ${snap.generatedAtIst}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 9 Date Presets
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(DateRangePreset.entries) { p ->
                    FilterChip(
                        selected = preset == p,
                        onClick = {
                            if (p == DateRangePreset.CUSTOM) showCustomDialog = true
                            else vm.setDatePreset(p)
                        },
                        label = { Text(if (isHindi) p.labelHi else p.labelEn) },
                        modifier = Modifier.testTag("bi_preset_${p.name}")
                    )
                }
            }
        }

        // Export Bar
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilledTonalButton(
                    onClick = {
                        ReportExporter.exportBiPdf(context, shopName, snap)
                        vm.logReportExport("PDF")
                    },
                    modifier = Modifier.weight(1f).testTag("export_bi_pdf_btn")
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("PDF", maxLines = 1)
                }
                FilledTonalButton(
                    onClick = {
                        ReportExporter.exportBiXlsx(context, shopName, snap)
                        vm.logReportExport("XLSX")
                    },
                    modifier = Modifier.weight(1f).testTag("export_bi_xlsx_btn")
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Excel", maxLines = 1)
                }
                OutlinedButton(
                    onClick = {
                        ReportExporter.exportBiCsv(context, shopName, snap)
                        vm.logReportExport("CSV")
                    },
                    modifier = Modifier.weight(1f).testTag("export_bi_csv_btn")
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("CSV", maxLines = 1)
                }
            }
        }

        // Strict Financial Overview KPIs
        item {
            val o = snap.overview
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (isHindi) "वित्तीय सारांश (सटीक फार्मेसी गणना)" else "Financial Overview (Exact Pharmacy Formulas)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        KpiTile(if (isHindi) "शुद्ध उत्पाद बिक्री" else "Net Product Sales", "₹${"%.0f".format(o.totalNetSales)}", EmeraldPrimary, Modifier.weight(1f))
                        KpiTile(
                            if (isHindi) "शुद्ध व्यवसाय लाभ" else "Net Business Profit",
                            "₹${"%.0f".format(o.totalNetBusinessProfit)}",
                            if (o.totalNetBusinessProfit >= 0) StatusSuccess else StatusError,
                            Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        KpiTile(if (isHindi) "कुल बिक्री" else "Gross Sales", "₹${"%.0f".format(o.totalGrossSales)}", Color.DarkGray, Modifier.weight(1f))
                        KpiTile(if (isHindi) "उत्पाद लागत" else "Product Cost", "₹${"%.0f".format(o.totalCostValue)}", SaffronAccent, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        KpiTile(if (isHindi) "सकल उत्पाद लाभ" else "Product Gross Profit", "₹${"%.0f".format(o.totalProductGrossProfit)}", EmeraldPrimary, Modifier.weight(1f))
                        KpiTile(if (isHindi) "पूर्ण / रद्द" else "Completed / Cancel", "${o.completedOrderCount} / ${o.cancelledOrderCount}", Color.DarkGray, Modifier.weight(1f))
                    }
                    Text(
                        if (isHindi) "छूट: ₹${"%.0f".format(o.totalDiscounts)} • रिफंड: ₹${"%.0f".format(o.totalRefunds)} • औसत ऑर्डर: ₹${"%.0f".format(o.averageOrderValue)} • रद्दीकरण दर: ${"%.1f".format(o.cancellationRatePercent)}%"
                        else "Discounts: ₹${"%.0f".format(o.totalDiscounts)} • Refunds: ₹${"%.0f".format(o.totalRefunds)} • AOV: ₹${"%.0f".format(o.averageOrderValue)} • Cancel Rate: ${"%.1f".format(o.cancellationRatePercent)}%",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        if (isHindi) "डिलीवरी शुल्क: ₹${"%.0f".format(o.totalDeliveryChargesCollected)} | डिलीवरी खर्च: ₹${"%.0f".format(o.totalDeliveryExpensesPaid)} | अन्य खर्च: ₹${"%.0f".format(o.totalOtherBusinessExpenses)}"
                        else "Delivery Charges Collected: ₹${"%.0f".format(o.totalDeliveryChargesCollected)} | Delivery Expenses: ₹${"%.0f".format(o.totalDeliveryExpensesPaid)} | Other Expenses: ₹${"%.0f".format(o.totalOtherBusinessExpenses)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Sales & Profit Chart
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(if (isHindi) "बिक्री व लाभ ट्रेंड" else "Sales & Profit Trend", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    SalesAndProfitChart(points = snap.salesTrend)
                }
            }
        }

        // Gemini Deep BI Advisor (ThinkingLevel.HIGH on gemini-4-carbon)
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (isHindi) "AI वित्तीय सलाहकार (डीप एनालिसिस)" else "AI Financial Strategist (Deep Reasoning)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Button(
                            onClick = { vm.runDeepBiAnalysis() },
                            enabled = !aiBusy,
                            modifier = Modifier.testTag("run_ai_bi_btn")
                        ) {
                            if (aiBusy) CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (isHindi) "विश्लेषण" else "Analyze")
                            }
                        }
                    }
                    if (!aiInsights.isNullOrBlank()) {
                        Text(aiInsights!!, style = MaterialTheme.typography.bodySmall)
                    } else {
                        Text(
                            if (isHindi) "लाभ मार्जिन, डिलीवरी खर्च और क्षेत्रीय सुझाव पाने के लिए 'विश्लेषण' दबाएं।"
                            else "Tap Analyze to generate profit margin, delivery expense, and area-level recommendations using Gemini.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // Customer Analytics
        item {
            Card {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(if (isHindi) "ग्राहक विश्लेषण" else "Customer Analytics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        if (isHindi) "सक्रिय ग्राहक: ${snap.totalCustomers} • नए: ${snap.newCustomers} • नियमित: ${snap.returningCustomers}"
                        else "Total Active: ${snap.totalCustomers} • New: ${snap.newCustomers} • Returning: ${snap.returningCustomers}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    snap.topCustomers.take(5).forEach { c ->
                        Text("• ${c.customerName} (${c.area}): ${c.totalOrders} orders • ₹${"%.0f".format(c.totalSales)} (AOV ₹${"%.0f".format(c.averageOrderValue)})", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Location-Wise Area Density Map & Stats
        item {
            Card {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (isHindi) "क्षेत्र-वार डिलीवरी हीटमैप" else "Location-Wise Delivery & Area Heatmap", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (snap.areaStats.isNotEmpty()) {
                        PharmacyInteractiveMap(
                            pins = snap.areaStats.mapIndexed { idx, a ->
                                MapPinData("area_$idx", a.areaName, "${a.orderCount} orders • ₹${"%.0f".format(a.totalSales)}", a.centerLat, a.centerLng)
                            },
                            centerLat = snap.areaStats.first().centerLat,
                            centerLng = snap.areaStats.first().centerLng,
                            modifier = Modifier.height(170.dp)
                        )
                    }
                    snap.areaStats.take(8).forEach { a ->
                        Text(
                            "• ${a.areaName}: ${a.orderCount} orders | Sales ₹${"%.0f".format(a.totalSales)} | ${"%.1f".format(a.totalDistanceKm)} km | Profit ₹${"%.0f".format(a.totalNetProfit)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // Delivery Person Performance
        item {
            Card {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(if (isHindi) "डिलीवरी पार्टनर प्रदर्शन रिपोर्ट" else "Delivery Person Financial & Performance Report", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (snap.deliveryStats.isEmpty()) {
                        Text(if (isHindi) "इस अवधि में कोई डिलीवरी रिकॉर्ड नहीं है।" else "No delivery activity recorded in this period.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        snap.deliveryStats.forEach { d ->
                            Text(
                                "• ${d.deliveryPersonName}: Done ${d.completedDeliveries}, Pending ${d.pendingDeliveries}, Cancel ${d.cancelledOrFailed} | ${"%.1f".format(d.totalDistanceKm)} km | Avg ${"%.0f".format(d.avgDurationMinutes)} min | Charges ₹${"%.0f".format(d.totalDeliveryChargesCollected)} / Exp ₹${"%.0f".format(d.totalDeliveryExpensesPaid)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        // Product Demand Analytics
        item {
            Card {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(if (isHindi) "दवा व उत्पाद मांग" else "Medicine & Product Demand", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (snap.productStats.isEmpty()) {
                        Text(if (isHindi) "इस अवधि में कोई उत्पाद मांग दर्ज नहीं है।" else "No product keywords extracted yet in this period.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        snap.productStats.take(10).forEach { p ->
                            Text("• ${p.medicineName}: ${p.requestCount} requests • Est. Sales ₹${"%.0f".format(p.estimatedSales)}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }

    if (showCustomDialog) {
        var startInput by remember { mutableStateOf(customStart) }
        var endInput by remember { mutableStateOf(customEnd) }
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text("Custom Date Range (IST)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = startInput, onValueChange = { startInput = it }, label = { Text("Start Date (YYYY-MM-DD)") })
                    OutlinedTextField(value = endInput, onValueChange = { endInput = it }, label = { Text("End Date (YYYY-MM-DD)") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    vm.setCustomDates(startInput.trim(), endInput.trim())
                    showCustomDialog = false
                }) { Text("Apply Range") }
            },
            dismissButton = { TextButton(onClick = { showCustomDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun KpiTile(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.08f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = accent)
        }
    }
}
