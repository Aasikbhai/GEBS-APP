package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.ExamScheduleEntity
import com.example.data.local.NoticeCategory
import com.example.data.local.NoticeEntity
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassPopupDialog
import com.example.ui.components.GlassPrimaryButton
import com.example.ui.components.StatusBadge
import com.example.ui.theme.LocalGlassPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LiveExamState { LIVE, UPCOMING, COMPLETED }

data class LiveExamComputed(
    val state: LiveExamState,
    val badgeText: String,
    val countdownText: String
)

fun computeLiveExamStatus(exam: ExamScheduleEntity, nowMillis: Long): LiveExamComputed {
    return when {
        nowMillis in exam.startMillis..exam.endMillis -> {
            val remaining = (exam.endMillis - nowMillis).coerceAtLeast(0L)
            LiveExamComputed(
                state = LiveExamState.LIVE,
                badgeText = "LIVE NOW",
                countdownText = "Ends in ${formatDurationCompact(remaining)}"
            )
        }
        nowMillis < exam.startMillis -> {
            val until = (exam.startMillis - nowMillis).coerceAtLeast(0L)
            LiveExamComputed(
                state = LiveExamState.UPCOMING,
                badgeText = "UPCOMING",
                countdownText = "Starts in ${formatDurationCompact(until)}"
            )
        }
        else -> {
            LiveExamComputed(
                state = LiveExamState.COMPLETED,
                badgeText = "COMPLETED",
                countdownText = "Finished"
            )
        }
    }
}

fun formatDurationCompact(millis: Long): String {
    val totalSeconds = millis / 1000L
    val days = totalSeconds / 86400L
    val hours = (totalSeconds % 86400L) / 3600L
    val mins = (totalSeconds % 3600L) / 60L
    val secs = totalSeconds % 60L
    return when {
        days > 0 -> String.format(Locale.US, "%dd %02dh %02dm", days, hours, mins)
        hours > 0 -> String.format(Locale.US, "%02dh %02dm %02ds", hours, mins, secs)
        else -> String.format(Locale.US, "%02dm %02ds", mins, secs)
    }
}

fun formatRelativeTimestamp(millis: Long): String {
    val diff = (System.currentTimeMillis() - millis).coerceAtLeast(0L)
    val mins = diff / 60_000L
    val hours = diff / 3_600_000L
    return when {
        mins < 1 -> "Just now"
        mins < 60 -> "${mins}m ago"
        hours < 24 -> "${hours}h ago"
        else -> SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(millis))
    }
}

@Composable
fun NoticeGlassCard(
    notice: NoticeEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalGlassPalette.current
    val (color, icon) = noticeStyle(notice.category, glass.danger, glass.warning, glass.success)

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = icon,
                    contentDescription = notice.category,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = notice.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            StatusBadge(text = notice.effectiveDate, color = color)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = notice.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "By ${notice.postedBy}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = formatRelativeTimestamp(notice.createdAtMillis),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ExamScheduleGlassCard(
    exam: ExamScheduleEntity,
    nowMillis: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalGlassPalette.current
    val computed = computeLiveExamStatus(exam, nowMillis)
    val badgeColor = when (computed.state) {
        LiveExamState.LIVE -> glass.success
        LiveExamState.UPCOMING -> MaterialTheme.colorScheme.primary
        LiveExamState.COMPLETED -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exam.subject,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${exam.className} • ${exam.examDateLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusBadge(text = computed.badgeText, color = badgeColor)
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.AccessTime,
                    contentDescription = "Countdown",
                    tint = badgeColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = computed.countdownText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.LocationOn,
                    contentDescription = "Room",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = exam.roomNumber,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ExamDetailGlassPopup(
    exam: ExamScheduleEntity,
    nowMillis: Long,
    onDismiss: () -> Unit
) {
    val glass = LocalGlassPalette.current
    val computed = computeLiveExamStatus(exam, nowMillis)
    val badgeColor = when (computed.state) {
        LiveExamState.LIVE -> glass.success
        LiveExamState.UPCOMING -> MaterialTheme.colorScheme.primary
        LiveExamState.COMPLETED -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    GlassPopupDialog(
        title = exam.subject,
        subtitle = "${exam.className} • Real-Time Exam Tracker",
        onDismiss = onDismiss
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusBadge(text = computed.badgeText, color = badgeColor)
            Text(
                text = computed.countdownText,
                style = MaterialTheme.typography.titleSmall,
                color = badgeColor,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        DetailRow(icon = Icons.Rounded.Schedule, label = "Schedule", value = exam.examDateLabel)
        DetailRow(icon = Icons.Rounded.LocationOn, label = "Exam Room", value = exam.roomNumber)
        DetailRow(icon = Icons.Rounded.Person, label = "Invigilator", value = exam.invigilator)
        DetailRow(icon = Icons.Rounded.MenuBook, label = "Syllabus Topics", value = exam.syllabus)
        Spacer(modifier = Modifier.height(16.dp))
        GlassPrimaryButton(
            text = "Close",
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun NoticeDetailGlassPopup(
    notice: NoticeEntity,
    onPlaySoundSms: () -> Unit,
    onDismiss: () -> Unit
) {
    GlassPopupDialog(
        title = notice.title,
        subtitle = "Effective: ${notice.effectiveDate} • Posted by ${notice.postedBy}",
        onDismiss = onDismiss
    ) {
        Text(
            text = notice.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(18.dp))
        GlassPrimaryButton(
            text = "Listen as Audible Sound Notice",
            onClick = onPlaySoundSms,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSizeOrMinHeight()),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
    Spacer(modifier = Modifier.height(10.dp))
}

@Composable
private fun IntrinsicSizeOrMinHeight() = 24.dp.coerceAtLeast(24.dp)

@Composable
private fun noticeStyle(
    category: String,
    danger: Color,
    warning: Color,
    success: Color
): Pair<Color, ImageVector> {
    return when (category) {
        NoticeCategory.HOLIDAY.name -> warning to Icons.Rounded.Celebration
        NoticeCategory.URGENT.name -> danger to Icons.Rounded.PriorityHigh
        NoticeCategory.EVENT.name -> success to Icons.Rounded.EventAvailable
        else -> MaterialTheme.colorScheme.primary to Icons.Rounded.Campaign
    }
}
