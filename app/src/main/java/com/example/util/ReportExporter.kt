package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.model.AuditLog
import com.example.model.BiDashboardSnapshot
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ReportExporter {

    private fun exportDir(context: Context): File =
        File(context.cacheDir, "exports").apply { mkdirs() }

    private fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun exportBiCsv(context: Context, shopName: String, snapshot: BiDashboardSnapshot): File {
        val file = File(exportDir(context), "PrayagDeen_BI_Report_${System.currentTimeMillis()}.csv")
        val sb = StringBuilder()
        sb.appendLine("Shop Name,\"$shopName\"")
        sb.appendLine("Date Range,\"${snapshot.dateRangeLabel}\"")
        sb.appendLine("Generated At (IST),\"${snapshot.generatedAtIst}\"")
        sb.appendLine("Accounting Method,\"${snapshot.accountingMethod}\"")
        sb.appendLine()
        sb.appendLine("Metric,Amount (INR)")
        val o = snapshot.overview
        sb.appendLine("Gross Sales,${"%.2f".format(o.totalGrossSales)}")
        sb.appendLine("Discounts,${"%.2f".format(o.totalDiscounts)}")
        sb.appendLine("Refunds,${"%.2f".format(o.totalRefunds)}")
        sb.appendLine("Net Product Sales,${"%.2f".format(o.totalNetSales)}")
        sb.appendLine("Cost Value,${"%.2f".format(o.totalCostValue)}")
        sb.appendLine("Product Gross Profit,${"%.2f".format(o.totalProductGrossProfit)}")
        sb.appendLine("Delivery Charges Collected,${"%.2f".format(o.totalDeliveryChargesCollected)}")
        sb.appendLine("Delivery Expenses Paid,${"%.2f".format(o.totalDeliveryExpensesPaid)}")
        sb.appendLine("Other Business Expenses,${"%.2f".format(o.totalOtherBusinessExpenses)}")
        sb.appendLine("Net Business Profit,${"%.2f".format(o.totalNetBusinessProfit)}")
        sb.appendLine("Completed Orders,${o.completedOrderCount}")
        sb.appendLine("Cancelled Orders,${o.cancelledOrderCount}")
        sb.appendLine("Average Order Value,${"%.2f".format(o.averageOrderValue)}")
        sb.appendLine()
        sb.appendLine("Area,Orders,Sales,Delivery Charges,Total KM,Net Profit")
        snapshot.areaStats.forEach { a ->
            sb.appendLine("\"${a.areaName}\",${a.orderCount},${"%.2f".format(a.totalSales)},${"%.2f".format(a.totalDeliveryCharges)},${"%.2f".format(a.totalDistanceKm)},${"%.2f".format(a.totalNetProfit)}")
        }
        sb.appendLine()
        sb.appendLine("Delivery Person,Completed,Pending,Cancelled,Distance KM,Charges Collected,Expenses Paid")
        snapshot.deliveryStats.forEach { d ->
            sb.appendLine("\"${d.deliveryPersonName}\",${d.completedDeliveries},${d.pendingDeliveries},${d.cancelledOrFailed},${"%.2f".format(d.totalDistanceKm)},${"%.2f".format(d.totalDeliveryChargesCollected)},${"%.2f".format(d.totalDeliveryExpensesPaid)}")
        }
        file.writeText(sb.toString())
        shareFile(context, file, "text/csv", "Export BI CSV Report")
        return file
    }

    fun exportBiPdf(context: Context, shopName: String, snapshot: BiDashboardSnapshot): File {
        val file = File(exportDir(context), "PrayagDeen_BI_Report_${System.currentTimeMillis()}.pdf")
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply { color = Color.rgb(6, 73, 55); textSize = 18f; isFakeBoldText = true }
        val headerPaint = Paint().apply { color = Color.rgb(6, 73, 55); textSize = 13f; isFakeBoldText = true }
        val bodyPaint = Paint().apply { color = Color.BLACK; textSize = 11f }
        val mutedPaint = Paint().apply { color = Color.DKGRAY; textSize = 10f }

        var y = 48f
        canvas.drawText("$shopName - BUSINESS INTELLIGENCE REPORT", 40f, y, titlePaint)
        y += 20f
        canvas.drawText("Period: ${snapshot.dateRangeLabel}  |  Generated: ${snapshot.generatedAtIst}", 40f, y, mutedPaint)
        y += 14f
        canvas.drawText("Accounting Method: ${snapshot.accountingMethod} (Timezone: Asia/Kolkata)", 40f, y, mutedPaint)
        y += 26f

        canvas.drawText("FINANCIAL OVERVIEW", 40f, y, headerPaint)
        y += 18f
        val o = snapshot.overview
        val lines = listOf(
            "Gross Sales: ₹${"%.2f".format(o.totalGrossSales)}  |  Discounts: ₹${"%.2f".format(o.totalDiscounts)}  |  Refunds: ₹${"%.2f".format(o.totalRefunds)}",
            "Net Product Sales: ₹${"%.2f".format(o.totalNetSales)}  |  Product Cost: ₹${"%.2f".format(o.totalCostValue)}",
            "Product Gross Profit: ₹${"%.2f".format(o.totalProductGrossProfit)}",
            "Delivery Charges Collected: ₹${"%.2f".format(o.totalDeliveryChargesCollected)}  |  Delivery Expenses: ₹${"%.2f".format(o.totalDeliveryExpensesPaid)}",
            "Other Business Expenses: ₹${"%.2f".format(o.totalOtherBusinessExpenses)}",
            "NET BUSINESS PROFIT: ₹${"%.2f".format(o.totalNetBusinessProfit)}",
            "Completed Orders: ${o.completedOrderCount}  |  Cancelled: ${o.cancelledOrderCount}  |  Avg Order: ₹${"%.2f".format(o.averageOrderValue)}"
        )
        lines.forEach { line ->
            canvas.drawText(line, 40f, y, bodyPaint)
            y += 16f
        }

        y += 16f
        canvas.drawText("TOP AREAS", 40f, y, headerPaint)
        y += 16f
        snapshot.areaStats.take(8).forEach { a ->
            canvas.drawText("${a.areaName}: ${a.orderCount} orders | Sales ₹${"%.0f".format(a.totalSales)} | Profit ₹${"%.0f".format(a.totalNetProfit)}", 40f, y, bodyPaint)
            y += 15f
        }

        y += 16f
        canvas.drawText("DELIVERY PERSONNEL SUMMARY", 40f, y, headerPaint)
        y += 16f
        snapshot.deliveryStats.take(8).forEach { d ->
            canvas.drawText("${d.deliveryPersonName}: ${d.completedDeliveries} delivered | ${"%.1f".format(d.totalDistanceKm)} km | Charges ₹${"%.0f".format(d.totalDeliveryChargesCollected)}", 40f, y, bodyPaint)
            y += 15f
        }

        doc.finishPage(page)
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        shareFile(context, file, "application/pdf", "Export BI PDF Report")
        return file
    }

    /**
     * Generates an authentic Microsoft Excel (.xlsx) OpenXML workbook without heavy dependencies.
     */
    fun exportBiXlsx(context: Context, shopName: String, snapshot: BiDashboardSnapshot): File {
        val file = File(exportDir(context), "PrayagDeen_BI_Report_${System.currentTimeMillis()}.xlsx")
        val o = snapshot.overview
        val rows = mutableListOf(
            listOf(shopName, "Business Intelligence Report"),
            listOf("Date Range", snapshot.dateRangeLabel),
            listOf("Generated At", snapshot.generatedAtIst),
            listOf("Accounting Method", snapshot.accountingMethod),
            listOf(),
            listOf("Metric", "Value (INR)"),
            listOf("Total Gross Sales", "%.2f".format(o.totalGrossSales)),
            listOf("Discounts", "%.2f".format(o.totalDiscounts)),
            listOf("Refunds", "%.2f".format(o.totalRefunds)),
            listOf("Net Product Sales", "%.2f".format(o.totalNetSales)),
            listOf("Total Cost Value", "%.2f".format(o.totalCostValue)),
            listOf("Product Gross Profit", "%.2f".format(o.totalProductGrossProfit)),
            listOf("Delivery Charges Collected", "%.2f".format(o.totalDeliveryChargesCollected)),
            listOf("Delivery Expenses Paid", "%.2f".format(o.totalDeliveryExpensesPaid)),
            listOf("Other Business Expenses", "%.2f".format(o.totalOtherBusinessExpenses)),
            listOf("Total Net Business Profit", "%.2f".format(o.totalNetBusinessProfit)),
            listOf("Completed Orders", o.completedOrderCount.toString()),
            listOf("Cancelled Orders", o.cancelledOrderCount.toString()),
            listOf("Average Order Value", "%.2f".format(o.averageOrderValue))
        )
        rows.add(listOf())
        rows.add(listOf("Area", "Orders", "Sales", "Delivery Charges", "Distance KM", "Net Profit"))
        snapshot.areaStats.forEach { a ->
            rows.add(listOf(a.areaName, a.orderCount.toString(), "%.2f".format(a.totalSales), "%.2f".format(a.totalDeliveryCharges), "%.2f".format(a.totalDistanceKm), "%.2f".format(a.totalNetProfit)))
        }

        val sheetXml = buildString {
            append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")
            rows.forEachIndexed { rIdx, cols ->
                append("""<row r="${rIdx + 1}">""")
                cols.forEachIndexed { cIdx, value ->
                    val colRef = ('A' + cIdx).toString() + (rIdx + 1)
                    val esc = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                    val num = value.toDoubleOrNull()
                    if (num != null && rIdx > 4) {
                        append("""<c r="$colRef"><v>$num</v></c>""")
                    } else {
                        append("""<c r="$colRef" t="inlineStr"><is><t>$esc</t></is></c>""")
                    }
                }
                append("</row>")
            }
            append("</sheetData></worksheet>")
        }

        ZipOutputStream(FileOutputStream(file)).use { zip ->
            fun put(name: String, content: String) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            put("[Content_Types].xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/></Types>""")
            put("_rels/.rels", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>""")
            put("xl/workbook.xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="BI Report" sheetId="1" r:id="rId1"/></sheets></workbook>""")
            put("xl/_rels/workbook.xml.rels", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/></Relationships>""")
            put("xl/worksheets/sheet1.xml", sheetXml)
        }
        shareFile(context, file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Export Excel XLSX Report")
        return file
    }

    fun exportAuditLogsCsv(context: Context, shopName: String, logs: List<AuditLog>): File {
        val file = File(exportDir(context), "PrayagDeen_AuditLogs_${System.currentTimeMillis()}.csv")
        val sb = StringBuilder()
        sb.appendLine("Shop,\"$shopName\"")
        sb.appendLine("Date,Time,Manager ID,Manager Name,Action,Record Type,Record ID,Field,Previous Value,New Value,Reason,Status,Entry Hash")
        logs.forEach { l ->
            sb.appendLine("\"${l.dateKey}\",\"${l.timeDisplay}\",\"${l.managerId}\",\"${l.managerName}\",\"${l.actionPerformed}\",\"${l.recordType}\",\"${l.recordId}\",\"${l.fieldChanged}\",\"${l.previousValue}\",\"${l.newValue}\",\"${l.modificationReason}\",\"${l.actionStatus}\",\"${l.entryHash}\"")
        }
        file.writeText(sb.toString())
        shareFile(context, file, "text/csv", "Export Audit Logs CSV")
        return file
    }

    fun exportAuditLogsPdf(context: Context, shopName: String, logs: List<AuditLog>): File {
        val file = File(exportDir(context), "PrayagDeen_AuditLogs_${System.currentTimeMillis()}.pdf")
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas
        val titlePaint = Paint().apply { color = Color.rgb(6, 73, 55); textSize = 16f; isFakeBoldText = true }
        val bodyPaint = Paint().apply { color = Color.BLACK; textSize = 9.5f }
        var y = 44f
        canvas.drawText("$shopName - MANAGER ACTIVITY & AUDIT LOG", 36f, y, titlePaint)
        y += 22f
        logs.take(35).forEach { l ->
            val line = "${l.dateKey} ${l.timeDisplay} | ${l.managerName} | ${l.actionPerformed} (${l.recordId}): ${l.previousValue} -> ${l.newValue}"
            canvas.drawText(line.take(96), 36f, y, bodyPaint)
            y += 18f
        }
        doc.finishPage(page)
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        shareFile(context, file, "application/pdf", "Export Audit Logs PDF")
        return file
    }

    fun exportBusinessBackupJson(context: Context, jsonContent: String): File {
        val file = File(exportDir(context), "PrayagDeen_Backup_${TimezoneUtils.todayDateKey()}.json")
        file.writeText(jsonContent)
        shareFile(context, file, "application/json", "Export Business Backup JSON")
        return file
    }
}
