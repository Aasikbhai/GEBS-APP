package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.audio.AudibleSmsController
import com.example.audio.SmsIntentHelper
import com.example.data.local.ComplaintEntity
import com.example.data.local.NoticeCategory
import com.example.data.local.StudentRecordEntity
import com.example.ui.components.AcousticWaveVisualizer
import com.example.ui.components.GlassPillChip
import com.example.ui.components.GlassPopupDialog
import com.example.ui.components.GlassPrimaryButton
import com.example.ui.components.GlassTextField
import com.example.ui.theme.LocalGlassPalette

@Composable
fun AttendanceSmsSummaryPopup(
    sentCount: Int,
    flaggedStudents: List<StudentRecordEntity>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val glass = LocalGlassPalette.current

    GlassPopupDialog(
        title = "Attendance SMS Dispatched",
        subtitle = "$sentCount parent alerts queued in CampusPulse Sound SMS",
        onDismiss = onDismiss
    ) {
        Text(
            text = if (flaggedStudents.isEmpty()) {
                "All selected students were marked Present or Excused. Audible attendance confirmations have been delivered."
            } else {
                "${flaggedStudents.size} student(s) marked Absent or Late. You can also send a carrier SMS via your phone's SMS app:"
            },
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(12.dp))
        flaggedStudents.forEach { student ->
            Surface(
                shape = MaterialTheme.shapes.small,
                color = glass.danger.copy(alpha = 0.1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${student.name} (Roll ${student.rollNumber})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${student.attendanceStatus} • ${student.parentPhone}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            val body = "Dear ${student.parentName}, your child ${student.name} (Roll ${student.rollNumber}, ${student.className}) is ${student.attendanceStatus} today (${student.attendanceDate})."
                            SmsIntentHelper.launchDeviceSmsComposer(context, student.parentPhone, body)
                        }
                    ) {
                        Icon(Icons.Rounded.Sms, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SMS")
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        GlassPrimaryButton(
            text = "Done",
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TeacherVoiceComplaintPopup(
    students: List<StudentRecordEntity>,
    audioController: AudibleSmsController,
    onSubmit: (student: StudentRecordEntity, category: String, message: String, voicePath: String?, openDeviceSms: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val glass = LocalGlassPalette.current
    val isRecording by audioController.isRecording.collectAsStateWithLifecycle()
    val recSeconds by audioController.recordingSeconds.collectAsStateWithLifecycle()
    val playingVoicePath by audioController.playingVoicePath.collectAsStateWithLifecycle()

    val categories = listOf("Discipline", "Homework", "Attendance", "Academic Progress")
    var selectedStudent by remember { mutableStateOf(students.firstOrNull()) }
    var selectedCategory by rememberSaveable { mutableStateOf(categories.first()) }
    var message by rememberSaveable { mutableStateOf("") }
    var recordedFilePath by rememberSaveable { mutableStateOf<String?>(null) }
    var permissionDeniedNote by rememberSaveable { mutableStateOf(false) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionDeniedNote = false
            audioController.startVoiceRecording()
        } else {
            permissionDeniedNote = true
        }
    }

    GlassPopupDialog(
        title = "Teacher Complain & Voice SMS",
        subtitle = "Send text SMS and/or record a Voice SMS to parents",
        onDismiss = {
            if (isRecording) audioController.stopVoiceRecording()
            onDismiss()
        }
    ) {
        Text("Select Student", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            students.take(6).forEach { st ->
                GlassPillChip(
                    label = "${st.name} (${st.rollNumber})",
                    selected = selectedStudent?.id == st.id,
                    onClick = { selectedStudent = st }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("Topic", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                GlassPillChip(
                    label = cat,
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        GlassTextField(
            value = message,
            onValueChange = { message = it },
            label = "Complaint SMS message (or describe voice note)",
            singleLine = false,
            minLines = 3,
            modifier = Modifier.testTag("teacher_complaint_input")
        )

        Spacer(modifier = Modifier.height(12.dp))
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AcousticWaveVisualizer(
                            isActive = isRecording || (recordedFilePath != null && playingVoicePath == recordedFilePath),
                            tint = if (isRecording) glass.danger else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                isRecording -> "Recording Voice SMS (${recSeconds}s)"
                                recordedFilePath != null -> "Voice SMS Clip Ready"
                                else -> "Optional Voice SMS Recording"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (permissionDeniedNote) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Microphone permission denied. You can still send a Text + Audible TTS SMS.",
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.danger
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassPrimaryButton(
                        text = if (isRecording) "Stop (${recSeconds}s)" else "Record Voice",
                        onClick = {
                            if (isRecording) {
                                recordedFilePath = audioController.stopVoiceRecording()
                            } else {
                                val granted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (granted) {
                                    audioController.startVoiceRecording()
                                } else {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        containerColor = if (isRecording) glass.danger else MaterialTheme.colorScheme.primary,
                        leadingIcon = {
                            Icon(
                                imageVector = if (isRecording) Icons.Rounded.Stop else Icons.Rounded.Mic,
                                contentDescription = stringResource(R.string.cd_record_voice),
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("record_voice_sms_button")
                    )

                    if (recordedFilePath != null && !isRecording) {
                        val isPlayingPreview = playingVoicePath == recordedFilePath
                        OutlinedButton(
                            onClick = { audioController.toggleVoicePlayback(recordedFilePath!!, message) }
                        ) {
                            Icon(
                                imageVector = if (isPlayingPreview) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                contentDescription = "Preview Voice SMS"
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isPlayingPreview) "Stop" else "Play")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        val target = selectedStudent
        val canSubmit = target != null && (message.trim().length >= 3 || recordedFilePath != null)

        GlassPrimaryButton(
            text = "Send SMS & Voice Alert",
            enabled = canSubmit && !isRecording,
            onClick = {
                if (target != null) {
                    onSubmit(target, selectedCategory, message, recordedFilePath, false)
                    onDismiss()
                }
            },
            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("send_teacher_complaint_button")
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            enabled = canSubmit && !isRecording,
            onClick = {
                if (target != null) {
                    onSubmit(target, selectedCategory, message, recordedFilePath, true)
                    onDismiss()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Rounded.Sms, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Send & Open Device SMS App")
        }
    }
}

@Composable
fun ReplyComplaintPopup(
    complaint: ComplaintEntity,
    audioController: AudibleSmsController,
    onReply: (replyText: String, voicePath: String?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val glass = LocalGlassPalette.current
    val isRecording by audioController.isRecording.collectAsStateWithLifecycle()
    val recSeconds by audioController.recordingSeconds.collectAsStateWithLifecycle()
    var replyText by rememberSaveable { mutableStateOf("") }
    var voicePath by rememberSaveable { mutableStateOf<String?>(null) }

    val micLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) audioController.startVoiceRecording()
    }

    GlassPopupDialog(
        title = "Reply to ${complaint.senderName}",
        subtitle = "Topic: ${complaint.category} • ${complaint.targetStudentName}",
        onDismiss = {
            if (isRecording) audioController.stopVoiceRecording()
            onDismiss()
        }
    ) {
        Text(
            text = complaint.message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        GlassTextField(
            value = replyText,
            onValueChange = { replyText = it },
            label = "Write SMS reply to parent",
            singleLine = false,
            minLines = 3
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    if (isRecording) {
                        voicePath = audioController.stopVoiceRecording()
                    } else {
                        val granted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) audioController.startVoiceRecording()
                        else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Rounded.Stop else Icons.Rounded.Mic,
                    contentDescription = null,
                    tint = if (isRecording) glass.danger else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    when {
                        isRecording -> "Stop (${recSeconds}s)"
                        voicePath != null -> "Voice Attached ✓"
                        else -> "Attach Voice Reply"
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        GlassPrimaryButton(
            text = "Send Reply SMS",
            enabled = !isRecording && (replyText.trim().length >= 3 || voicePath != null),
            onClick = {
                onReply(replyText, voicePath)
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PostNoticePopup(
    onPost: (title: String, description: String, category: NoticeCategory, effectiveDate: String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by rememberSaveable { mutableStateOf("Holiday Today: Special Announcement") }
    var description by rememberSaveable { mutableStateOf("") }
    var effectiveDate by rememberSaveable { mutableStateOf("Today") }
    var selectedCategory by rememberSaveable { mutableStateOf(NoticeCategory.HOLIDAY) }

    GlassPopupDialog(
        title = "Post School Notice & SMS",
        subtitle = "Broadcasts in English, हिन्दी, or नेपाली with Auto Voice Detection",
        onDismiss = onDismiss
    ) {
        Text("Quick Announcement Templates", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GlassPillChip(
                label = "Holiday (EN)",
                selected = false,
                onClick = {
                    selectedCategory = NoticeCategory.HOLIDAY
                    title = "Holiday Today: School Closed"
                    description = "School is closed today due to heavy rain advisory. Stay safe at home."
                    effectiveDate = "Today"
                }
            )
            GlassPillChip(
                label = "अवकाश (हिन्दी)",
                selected = false,
                onClick = {
                    selectedCategory = NoticeCategory.HOLIDAY
                    title = "आज स्कूल अवकाश सूचना"
                    description = "आज मौसम की स्थिति के कारण विद्यालय बंद रहेगा। सभी छात्र घर पर सुरक्षित रहें।"
                    effectiveDate = "आज"
                }
            )
            GlassPillChip(
                label = "बिदा (नेपाली)",
                selected = false,
                onClick = {
                    selectedCategory = NoticeCategory.HOLIDAY
                    title = "आज विद्यालय बिदा सूचना"
                    description = "आज विशेष कारणले विद्यालय बन्द रहनेछ। कृपया भोलि समयमै विद्यालय आउनुहोस्।"
                    effectiveDate = "आज"
                }
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text("Category", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NoticeCategory.entries.forEach { cat ->
                GlassPillChip(
                    label = cat.label,
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat }
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        GlassTextField(
            value = title,
            onValueChange = { title = it },
            label = "Notice Headline (English / हिन्दी / नेपाली)"
        )
        Spacer(modifier = Modifier.height(10.dp))
        GlassTextField(
            value = effectiveDate,
            onValueChange = { effectiveDate = it },
            label = "Effective Date (e.g., Today, Tomorrow, आज)"
        )
        Spacer(modifier = Modifier.height(10.dp))
        GlassTextField(
            value = description,
            onValueChange = { description = it },
            label = "Notice details for parents & students",
            singleLine = false,
            minLines = 3
        )
        Spacer(modifier = Modifier.height(14.dp))
        GlassPrimaryButton(
            text = "Publish Notice & Send Audible SMS",
            enabled = title.trim().isNotEmpty() && description.trim().length >= 5,
            onClick = {
                onPost(title, description, selectedCategory, effectiveDate)
                onDismiss()
            },
            leadingIcon = { Icon(Icons.Rounded.Campaign, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("publish_notice_button")
        )
    }
}

@Composable
fun AddExamPopup(
    onAdd: (subject: String, className: String, dateLabel: String, hoursFromNow: Int, room: String, syllabus: String) -> Unit,
    onDismiss: () -> Unit
) {
    var subject by rememberSaveable { mutableStateOf("") }
    var className by rememberSaveable { mutableStateOf("Class 8-A") }
    var dateLabel by rememberSaveable { mutableStateOf("In 2 Days • 10:00 AM") }
    var hoursAhead by rememberSaveable { mutableIntStateOf(48) }
    var room by rememberSaveable { mutableStateOf("Hall B-104") }
    var syllabus by rememberSaveable { mutableStateOf("") }

    GlassPopupDialog(
        title = "Add Real-Time Exam Slot",
        subtitle = "Updates live countdowns for students immediately",
        onDismiss = onDismiss
    ) {
        GlassTextField(value = subject, onValueChange = { subject = it }, label = "Subject Name")
        Spacer(modifier = Modifier.height(10.dp))
        GlassTextField(value = className, onValueChange = { className = it }, label = "Class (e.g., Class 8-A)")
        Spacer(modifier = Modifier.height(10.dp))
        GlassTextField(value = dateLabel, onValueChange = { dateLabel = it }, label = "Display Date & Time")
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(2 to "2 hrs", 24 to "1 day", 48 to "2 days", 96 to "4 days").forEach { (hrs, lbl) ->
                GlassPillChip(
                    label = lbl,
                    selected = hoursAhead == hrs,
                    onClick = { hoursAhead = hrs }
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        GlassTextField(value = room, onValueChange = { room = it }, label = "Exam Room / Hall")
        Spacer(modifier = Modifier.height(10.dp))
        GlassTextField(value = syllabus, onValueChange = { syllabus = it }, label = "Syllabus Topics", singleLine = false, minLines = 2)
        Spacer(modifier = Modifier.height(14.dp))
        GlassPrimaryButton(
            text = "Save Exam Schedule",
            enabled = subject.trim().isNotEmpty() && room.trim().isNotEmpty(),
            onClick = {
                onAdd(subject, className, dateLabel, hoursAhead, room, syllabus.ifBlank { "Full Term Syllabus" })
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubmitLeavePopup(
    onSubmit: (leaveType: String, startDate: String, endDate: String, substitute: String, reason: String) -> Unit,
    onDismiss: () -> Unit
) {
    val types = listOf("Sick Leave", "Casual Leave", "Personal / Family", "Official Duty")
    var selectedType by rememberSaveable { mutableStateOf(types.first()) }
    var startDate by rememberSaveable { mutableStateOf("24 Nov 2026") }
    var endDate by rememberSaveable { mutableStateOf("25 Nov 2026") }
    var substitute by rememberSaveable { mutableStateOf("Mrs. S. Banerjee") }
    var reason by rememberSaveable { mutableStateOf("") }

    GlassPopupDialog(
        title = "Submit Leave Application",
        subtitle = "Sent to the Principal Office",
        onDismiss = onDismiss
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            types.forEach { t ->
                GlassPillChip(
                    label = t,
                    selected = selectedType == t,
                    onClick = { selectedType = t }
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        GlassTextField(value = startDate, onValueChange = { startDate = it }, label = "From Date")
        Spacer(modifier = Modifier.height(10.dp))
        GlassTextField(value = endDate, onValueChange = { endDate = it }, label = "To Date")
        Spacer(modifier = Modifier.height(10.dp))
        GlassTextField(value = substitute, onValueChange = { substitute = it }, label = "Suggested Substitute Teacher")
        Spacer(modifier = Modifier.height(10.dp))
        GlassTextField(
            value = reason,
            onValueChange = { reason = it },
            label = "Reason for Leave",
            singleLine = false,
            minLines = 3,
            modifier = Modifier.testTag("leave_reason_input")
        )
        Spacer(modifier = Modifier.height(14.dp))
        GlassPrimaryButton(
            text = "Submit Leave Application",
            enabled = startDate.trim().isNotEmpty() && reason.trim().length >= 5,
            onClick = {
                onSubmit(selectedType, startDate, endDate, substitute, reason)
                onDismiss()
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("confirm_submit_leave_button")
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AskSalaryPopup(
    onSubmit: (monthLabel: String, topic: String, message: String) -> Unit,
    onDismiss: () -> Unit
) {
    val topics = listOf("Salary Slip Request", "Pending Allowance", "Disbursement Date", "Salary Advance")
    var selectedTopic by rememberSaveable { mutableStateOf(topics.first()) }
    var monthLabel by rememberSaveable { mutableStateOf("November 2026") }
    var message by rememberSaveable { mutableStateOf("") }

    GlassPopupDialog(
        title = "Ask About Salary",
        subtitle = "Payroll Desk",
        onDismiss = onDismiss
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            topics.forEach { topic ->
                GlassPillChip(
                    label = topic,
                    selected = selectedTopic == topic,
                    onClick = { selectedTopic = topic }
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        GlassTextField(value = monthLabel, onValueChange = { monthLabel = it }, label = "Pay Month")
        Spacer(modifier = Modifier.height(10.dp))
        GlassTextField(
            value = message,
            onValueChange = { message = it },
            label = "Write your salary question to Accounts",
            singleLine = false,
            minLines = 3,
            modifier = Modifier.testTag("salary_inquiry_input")
        )
        Spacer(modifier = Modifier.height(14.dp))
        GlassPrimaryButton(
            text = "Send Salary Inquiry",
            enabled = message.trim().length >= 5,
            onClick = {
                onSubmit(monthLabel, selectedTopic, message)
                onDismiss()
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("confirm_salary_inquiry_button")
        )
    }
}
