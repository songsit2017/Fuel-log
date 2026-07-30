package com.songsit.fuellogpro.ui.timeline

import android.content.Intent
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.songsit.fuellogpro.data.local.isPdfPath
import com.songsit.fuellogpro.domain.model.Expense
import com.songsit.fuellogpro.domain.model.FuelEntry
import com.songsit.fuellogpro.ui.NativeAppState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

private val timelineCurrency = NumberFormat.getCurrencyInstance(Locale("th", "TH"))
private val timelineNumber = NumberFormat.getNumberInstance(Locale("th", "TH")).apply { maximumFractionDigits = 2 }

private sealed class TimelineRow(val date: String, val sortKey: String) {
    class FuelRow(val entry: FuelEntry) : TimelineRow(entry.date, entry.date + " " + entry.time)
    class ExpenseRow(val expense: Expense) : TimelineRow(expense.date, expense.date + " 00:00")
}

@Composable
fun TimelineScreen(
    state: NativeAppState,
    modifier: Modifier = Modifier,
    onImageClick: (String) -> Unit = {},
    onFuelRecordClick: (FuelEntry) -> Unit = {},
) {
    val rows = remember(state.entries, state.expenses) {
        val combined = state.entries.map { TimelineRow.FuelRow(it) as TimelineRow } +
            state.expenses.map { TimelineRow.ExpenseRow(it) as TimelineRow }
        combined.sortedByDescending { it.sortKey }
    }
    val groups = remember(rows) {
        rows.groupBy { row ->
            val parsed = runCatching { LocalDate.parse(row.date) }.getOrNull()
            parsed?.let { it.year to it.monthValue }
        }
    }
    val unknownDateLabel = stringResource(com.songsit.fuellogpro.R.string.unknown_date)
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (rows.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(com.songsit.fuellogpro.R.string.timeline_empty_title), fontWeight = FontWeight.SemiBold)
                        Text(stringResource(com.songsit.fuellogpro.R.string.timeline_empty_subtitle), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        groups.forEach { (yearMonth, groupRows) ->
            item {
                val monthNames = stringArrayResource(com.songsit.fuellogpro.R.array.month_names_full)
                val label = yearMonth?.let { (year, month) -> "${monthNames[month - 1]} $year" } ?: unknownDateLabel
                Text(
                    label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            items(groupRows, key = {
                when (it) {
                    is TimelineRow.FuelRow -> "fuel-${it.entry.id}"
                    is TimelineRow.ExpenseRow -> "expense-${it.expense.id}"
                }
            }) { row ->
                TimelineEntryRow(row, onImageClick, onFuelRecordClick)
            }
        }
    }
}

@Composable
private fun TimelineEntryRow(
    row: TimelineRow,
    onImageClick: (String) -> Unit,
    onFuelRecordClick: (FuelEntry) -> Unit,
) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        // Simple vertical line + dot, built from plain Box/Column (no external library).
        Column(
            modifier = Modifier.width(24.dp).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 18.dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary),
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceContainer),
            )
        }
        Spacer(Modifier.width(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                .let { if (row is TimelineRow.FuelRow) it.clickable { onFuelRecordClick(row.entry) } else it },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        ) {
            when (row) {
                is TimelineRow.FuelRow -> FuelTimelineContent(row.entry, onImageClick)
                is TimelineRow.ExpenseRow -> ExpenseTimelineContent(row.expense, onImageClick)
            }
        }
    }
}

@Composable
private fun FuelTimelineContent(entry: FuelEntry, onImageClick: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.LocalGasStation, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text(entry.date, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(timelineCurrency.format(entry.amount), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
        }
        val detail = listOfNotNull(
            entry.station.takeIf(String::isNotBlank),
            "${timelineNumber.format(entry.odometerKm)} ${stringResource(com.songsit.fuellogpro.R.string.unit_km_short)}",
        ).joinToString(" • ")
        Text(detail, style = MaterialTheme.typography.bodySmall)
        if (entry.photoUrls.isNotEmpty()) TimelineThumbnails(entry.photoUrls, onImageClick)
    }
}

@Composable
private fun ExpenseTimelineContent(expense: Expense, onImageClick: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (expense.income) Icons.Filled.Savings else Icons.Filled.Receipt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(expense.date, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(
                "${if (expense.income) "+" else "−"}${timelineCurrency.format(expense.amount)}",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        val detail = listOf(expense.category, expense.description).filter(String::isNotBlank).joinToString(" • ")
        if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.bodySmall)
        if (expense.photoUrls.isNotEmpty()) TimelineThumbnails(expense.photoUrls, onImageClick)
    }
}

// Reuses the same decode-a-local-file-path pattern as PhotoAttachmentRow in FuelLogApp.kt,
// but read-only and capped at 3 thumbnails per Item C/E.
@Composable
private fun TimelineThumbnails(photoUris: List<String>, onImageClick: (String) -> Unit) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        photoUris.take(3).forEach { uri ->
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.size(52.dp).clickable {
                    if (isPdfPath(uri)) openPdfExternally(context, uri) else onImageClick(uri)
                },
            ) {
                if (isPdfPath(uri)) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = stringResource(com.songsit.fuellogpro.R.string.content_desc_pdf_file), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    AsyncImage(
                        model = if (uri.startsWith("/")) java.io.File(uri) else uri,
                        contentDescription = stringResource(com.songsit.fuellogpro.R.string.content_desc_attached_photo),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        }
    }
}

// Opens a locally-stored PDF in an external reader app — Coil/AsyncImage can't preview a PDF, so
// tapping one skips the in-app FullScreenImageViewer entirely instead of showing a broken image.
fun openPdfExternally(context: android.content.Context, path: String) {
    runCatching {
        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", java.io.File(path))
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }.onFailure {
        Toast.makeText(context, context.getString(com.songsit.fuellogpro.R.string.open_pdf_failed), Toast.LENGTH_SHORT).show()
    }
}

// Full-screen photo viewer (Dialog rather than a nav route — this app has no NavController;
// screens are toggled boolean/state like the rest of FuelLogApp.kt). dismissOnBackPress is the
// Dialog default, so the system Back button already closes this without extra wiring.
@Composable
fun FullScreenImageViewer(imagePath: String, onDismiss: () -> Unit, onDelete: (() -> Unit)? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            AsyncImage(
                model = if (imagePath.startsWith("/")) java.io.File(imagePath) else imagePath,
                contentDescription = stringResource(com.songsit.fuellogpro.R.string.content_desc_attached_photo),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(com.songsit.fuellogpro.R.string.action_close), tint = Color.White)
            }
            Row(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                if (onDelete != null) {
                    IconButton(onClick = { onDelete(); onDismiss() }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(com.songsit.fuellogpro.R.string.content_desc_delete_photo), tint = Color.White)
                    }
                }
                IconButton(onClick = { scope.launch { shareTimelineImage(context, imagePath) } }) {
                    Icon(Icons.Filled.Share, contentDescription = stringResource(com.songsit.fuellogpro.R.string.action_share), tint = Color.White)
                }
            }
        }
    }
}

// Local photos share as the actual image via FileProvider (same authority the camera capture
// flow already registers in the manifest). Remote URLs are downloaded to a cache file first so
// every share target still gets a real image attachment instead of a bare link. Wrapped
// end-to-end in try/catch: a bad/missing file or unreachable URL should show an error toast,
// not crash the app mid-share.
private suspend fun shareTimelineImage(context: android.content.Context, imagePath: String) {
    try {
        val file = if (imagePath.startsWith("/")) {
            java.io.File(imagePath)
        } else {
            withContext(Dispatchers.IO) {
                val cacheDir = java.io.File(context.cacheDir, "shared").apply { mkdirs() }
                val dest = java.io.File(cacheDir, "${System.currentTimeMillis()}.jpg")
                java.net.URL(imagePath).openStream().use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                }
                dest
            }
        }
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file,
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, context.getString(com.songsit.fuellogpro.R.string.share_image_chooser_title)))
    } catch (e: Exception) {
        Toast.makeText(context, context.getString(com.songsit.fuellogpro.R.string.share_image_failed), Toast.LENGTH_SHORT).show()
    }
}

