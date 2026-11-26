package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.EventNote
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.audio.AnnouncementVoiceFormatter
import com.example.audio.SmsIntentHelper
import com.example.audio.VoiceLanguageMode
import com.example.data.local.AttendanceStatus
import com.example.data.local.ComplaintEntity
import com.example.data.local.ExamScheduleEntity
import com.example.data.local.FeeItemEntity
import com.example.data.local.NoticeEntity
import com.example.data.local.SalaryInquiryEntity
import com.example.data.local.StudentRecordEntity
import com.example.data.local.TeacherLeaveEntity
import com.example.ui.CampusViewModel
import com.example.ui.UserSession
import com.example.ui.components.AuroraGlassBackground
import com.example.ui.components.FloatingGlassAudioPlayerBar
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassPillChip
import com.example.ui.components.GlassPrimaryButton
import com.example.ui.components.GlassTextField
import com.example.ui.components.StatusBadge
import com.example.ui.components.VoiceLanguageBar
import com.example.ui.components.VoiceLanguageStudioPopup
import com.example.ui.theme.LocalGlassPalette

@Composable
fun TeacherPortalScreen(
    session: UserSession.Teacher,
    viewModel: CampusViewModel,
    onLogout: () -> Unit
) {
    val state by viewModel.dataState.collectAsStateWithLifecycle()
    val nowMillis by viewModel.currentTimeMillis.collectAsStateWithLifecycle()
    val draftAttendance by viewModel.draftAttendance.collectAsStateWithLifecycle()
    val playingTextId by viewModel.audioController.playingTextId.collectAsStateWithLifecycle()
    val playingVoicePath by viewModel.audioController.playingVoicePath.collectAsStateWithLifecycle()
    val speechRate by viewModel.audioController.speechRate.collectAsStateWithLifecycle()
    val voiceLanguage by viewModel.audioController.voiceLanguageMode.collectAsStateWithLifecycle()
    val qualityLabel by viewModel.audioController.activeVoiceQualityLabel.collectAsStateWithLifecycle()
    val chimeEnabled by viewModel.audioController.chimeEnabled.collectAsStateWithLifecycle()
    val nowPlayingTitle by viewModel.audioController.nowPlayingTitle.collectAsStateWithLifecycle()
    val nowPlayingText by viewModel.audioController.nowPlayingText.collectAsStateWithLifecycle()
    val nowPlayingLang by viewModel.audioController.nowPlayingLang.collectAsStateWithLifecycle()
    val feedback by viewModel.feedbackBanner.collectAsStateWithLifecycle()
    val audioMsg by viewModel.audioController.audioStatusMessage.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var attendanceSummary by remember { mutableStateOf<Pair<Int, List<StudentRecordEntity>>?>(null) }
    var showTeacherComplaintPopup by rememberSaveable { mutableStateOf(false) }
    var replyingComplaint by remember { mutableStateOf<ComplaintEntity?>(null) }
    var showPostNoticePopup by rememberSaveable { mutableStateOf(false) }
    var showAddExamPopup by rememberSaveable { mutableStateOf(false) }
    var showLeavePopup by rememberSaveable { mutableStateOf(false) }
    var showSalaryPopup by rememberSaveable { mutableStateOf(false) }
    var showVoiceStudioPopup by rememberSaveable { mutableStateOf(false) }
    var selectedExam by remember { mutableStateOf<ExamScheduleEntity?>(null) }
    var selectedNotice by remember { mutableStateOf<NoticeEntity?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    BackHandler(enabled = selectedTab != 0) {
        selectedTab = 0
    }

    LaunchedEffect(feedback) {
        feedback?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedbackBanner()
        }
    }
    LaunchedEffect(audioMsg) {
        audioMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.audioController.clearStatusMessage()
        }
    }

    val tabs = listOf("Attendance", "Complain", "Exams & Notice", "Leave & Pay")
    val selectedIcons = listOf(
        Icons.Rounded.Checklist,
        Icons.Rounded.RecordVoiceOver,
        Icons.Rounded.Campaign,
        Icons.Rounded.AccountBalance
    )
    val unselectedIcons = listOf(
        Icons.Outlined.Checklist,
        Icons.Outlined.RecordVoiceOver,
        Icons.Outlined.Campaign,
        Icons.Outlined.AccountBalance
    )

    AuroraGlassBackground {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 700.dp

            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    Column {
                        FloatingGlassAudioPlayerBar(
                            isPlaying = playingTextId != null,
                            title = nowPlayingTitle,
                            spokenText = nowPlayingText,
                            language = nowPlayingLang,
                            onStop = { viewModel.audioController.stopAudibleSms() }
                        )
                        if (!useRail) {
                            NavigationBar(
                                containerColor = LocalGlassPalette.current.glassPopup,
                                modifier = Modifier.navigationBarsPadding()
                            ) {
                                tabs.forEachIndexed { index, title ->
                                    NavigationBarItem(
                                        selected = selectedTab == index,
                                        onClick = { selectedTab = index },
                                        icon = {
                                            Icon(
                                                imageVector = if (selectedTab == index) selectedIcons[index] else unselectedIcons[index],
                                                contentDescription = title
                                            )
                                        },
                                        label = { Text(title, maxLines = 1) },
                                        modifier = Modifier.testTag("teacher_tab_$index")
                                    )
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useRail) {
                        NavigationRail(
                            containerColor = LocalGlassPalette.current.glassPopup,
                            modifier = Modifier
                                .fillMaxHeight()
                                .statusBarsPadding()
                        ) {
                            Spacer(modifier = Modifier.height(12.dp))
                            tabs.forEachIndexed { index, title ->
                                NavigationRailItem(
                                    selected = selectedTab == index,
                                    onClick = { selectedTab = index },
                                    icon = {
                                        Icon(
                                            imageVector = if (selectedTab == index) selectedIcons[index] else unselectedIcons[index],
                                            contentDescription = title
                                        )
                                    },
                                    label = { Text(title) }
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                    ) {
                        TeacherTopHeader(session = session, onLogout = onLogout)

                        VoiceLanguageBar(
                            currentMode = voiceLanguage,
                            qualityLabel = qualityLabel,
                            onSelectMode = { viewModel.audioController.setVoiceLanguageMode(it) },
                            onOpenStudio = { showVoiceStudioPopup = true }
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        when (selectedTab) {
                            0 -> TeacherAttendanceTab(
                                students = state.students,
                                draftAttendance = draftAttendance,
                                onSelectStatus = { studentId, status ->
                                    viewModel.setDraftAttendance(studentId, status)
                                },
                                onMarkAllPresent = { list ->
                                    viewModel.markClassAllPresent(list)
                                },
                                onSaveAndSendSms = { list ->
                                    viewModel.saveAttendanceAndDispatchSms(list) { count, flagged ->
                                        attendanceSummary = count to flagged
                                    }
                                },
                                onQuickPostNotice = { showPostNoticePopup = true }
                            )
                            1 -> TeacherComplainVoiceTab(
                                complaints = state.complaints,
                                playingTextId = playingTextId,
                                playingVoicePath = playingVoicePath,
                                onNewTeacherComplaint = { showTeacherComplaintPopup = true },
                                onReplyClick = { replyingComplaint = it },
                                onPlaySound = { id, title, text ->
                                    viewModel.audioController.playAudibleSms(id, text, title = title)
                                },
                                onPlayVoice = { path, fallback ->
                                    viewModel.audioController.toggleVoicePlayback(path, fallback)
                                }
                            )
                            2 -> TeacherExamsAndNoticesTab(
                                exams = state.exams,
                                notices = state.notices,
                                fees = state.fees,
                                nowMillis = nowMillis,
                                voiceLanguage = voiceLanguage,
                                onAddExamClick = { showAddExamPopup = true },
                                onPostNoticeClick = { showPostNoticePopup = true },
                                onSendBillReminder = { viewModel.sendBillSmsReminder(it) },
                                onExamClick = { selectedExam = it },
                                onNoticeClick = { selectedNotice = it }
                            )
                            3 -> TeacherLeaveAndSalaryTab(
                                leaves = state.leaves,
                                salaryInquiries = state.salaryInquiries,
                                onSubmitLeaveClick = { showLeavePopup = true },
                                onCancelLeave = { viewModel.cancelTeacherLeave(it) },
                                onAskSalaryClick = { showSalaryPopup = true }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showVoiceStudioPopup) {
        VoiceLanguageStudioPopup(
            selectedMode = voiceLanguage,
            speechRate = speechRate,
            chimeEnabled = chimeEnabled,
            qualityLabel = qualityLabel,
            onSelectMode = { viewModel.audioController.setVoiceLanguageMode(it) },
            onSelectRate = { viewModel.audioController.setSpeechRate(it) },
            onToggleChime = { viewModel.audioController.toggleChime() },
            onTestVoice = { viewModel.audioController.playSampleVoice(voiceLanguage) },
            onDismiss = { showVoiceStudioPopup = false }
        )
    }

    attendanceSummary?.let { (count, flagged) ->
        AttendanceSmsSummaryPopup(
            sentCount = count,
            flaggedStudents = flagged,
            onDismiss = { attendanceSummary = null }
        )
    }

    if (showTeacherComplaintPopup) {
        TeacherVoiceComplaintPopup(
            students = state.students,
            audioController = viewModel.audioController,
            onSubmit = { student, category, message, voicePath, openDeviceSms ->
                viewModel.submitTeacherComplaint(student, category, message, voicePath)
                if (openDeviceSms) {
                    SmsIntentHelper.launchDeviceSmsComposer(
                        context = context,
                        phoneNumber = student.parentPhone,
                        messageBody = "Teacher Alert [$category] for ${student.name}: ${message.ifBlank { "Voice note sent in CampusPulse." }}"
                    )
                }
            },
            onDismiss = { showTeacherComplaintPopup = false }
        )
    }

    replyingComplaint?.let { comp ->
        ReplyComplaintPopup(
            complaint = comp,
            audioController = viewModel.audioController,
            onReply = { text, voicePath ->
                viewModel.replyToParentComplaint(comp, text, voicePath)
            },
            onDismiss = { replyingComplaint = null }
        )
    }

    if (showPostNoticePopup) {
        PostNoticePopup(
            onPost = { title, desc, category, date ->
                viewModel.postNotice(title, desc, category, date)
            },
            onDismiss = { showPostNoticePopup = false }
        )
    }

    if (showAddExamPopup) {
        AddExamPopup(
            onAdd = { subj, cls, label, hrs, room, syl ->
                viewModel.addExamSchedule(subj, cls, label, hrs, 2, room, syl)
            },
            onDismiss = { showAddExamPopup = false }
        )
    }

    if (showLeavePopup) {
        SubmitLeavePopup(
            onSubmit = { type, start, end, sub, reason ->
                viewModel.submitTeacherLeave(type, start, end, sub, reason)
            },
            onDismiss = { showLeavePopup = false }
        )
    }

    if (showSalaryPopup) {
        AskSalaryPopup(
            onSubmit = { month, topic, msg ->
                viewModel.submitSalaryInquiry(month, topic, msg)
            },
            onDismiss = { showSalaryPopup = false }
        )
    }

    selectedExam?.let { exam ->
        ExamDetailGlassPopup(
            exam = exam,
            nowMillis = nowMillis,
            onDismiss = { selectedExam = null }
        )
    }

    selectedNotice?.let { notice ->
        val localized = AnnouncementVoiceFormatter.localize(notice.title, notice.description, voiceLanguage)
        NoticeDetailGlassPopup(
            notice = notice.copy(title = localized.displayTitle, description = localized.displayBody),
            onPlaySoundSms = {
                viewModel.audioController.playAudibleSms(
                    utteranceId = "t_notice_${notice.id}",
                    spokenText = notice.description,
                    title = notice.title
                )
            },
            onDismiss = { selectedNotice = null }
        )
    }
}

@Composable
private fun TeacherTopHeader(
    session: UserSession.Teacher,
    onLogout: () -> Unit
) {
    GlassCard(
        cornerRadius = 20.dp,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Image(
                    painter = painterResource(id = R.drawable.img_teacher_avatar),
                    contentDescription = stringResource(R.string.cd_teacher_avatar),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = session.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Reg: ${session.registrationNo} • ${session.designation}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(
                onClick = onLogout,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("logout_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Logout,
                    contentDescription = stringResource(R.string.logout_button),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TeacherAttendanceTab(
    students: List<StudentRecordEntity>,
    draftAttendance: Map<Int, AttendanceStatus>,
    onSelectStatus: (Int, AttendanceStatus) -> Unit,
    onMarkAllPresent: (List<StudentRecordEntity>) -> Unit,
    onSaveAndSendSms: (List<StudentRecordEntity>) -> Unit,
    onQuickPostNotice: () -> Unit
) {
    val glass = LocalGlassPalette.current
    val classOptions = remember(students) {
        listOf("All Classes") + students.map { it.className }.distinct()
    }
    var selectedClass by rememberSaveable { mutableStateOf("All Classes") }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val visibleStudents = remember(students, selectedClass, searchQuery) {
        students.filter { st ->
            (selectedClass == "All Classes" || st.className == selectedClass) &&
                (searchQuery.isBlank() ||
                    st.name.contains(searchQuery, ignoreCase = true) ||
                    st.rollNumber.contains(searchQuery, ignoreCase = true) ||
                    st.registrationNo.contains(searchQuery, ignoreCase = true))
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Attendance Selection System",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Filter by Class, Name, Roll or Reg No., then send SMS",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(onClick = onQuickPostNotice) {
                        Icon(Icons.Rounded.Campaign, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Holiday")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    classOptions.forEach { cls ->
                        GlassPillChip(
                            label = cls,
                            selected = selectedClass == cls,
                            onClick = { selectedClass = cls }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                GlassTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = "Search name, roll number, or Reg No.",
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) }
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onMarkAllPresent(visibleStudents) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("All Present")
                    }
                    GlassPrimaryButton(
                        text = "Save & Send SMS",
                        onClick = { onSaveAndSendSms(visibleStudents) },
                        leadingIcon = { Icon(Icons.Rounded.Sms, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("save_attendance_sms_button")
                    )
                }
            }
        }

        items(visibleStudents, key = { it.id }) { student ->
            val currentStatus = draftAttendance[student.id]
                ?: runCatching { AttendanceStatus.valueOf(student.attendanceStatus) }.getOrDefault(AttendanceStatus.PRESENT)

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${student.name}  •  Roll ${student.rollNumber}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${student.className} • Reg: ${student.registrationNo} • ${student.parentPhone}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AttendanceStatus.entries.forEach { st ->
                        val chipColor = when (st) {
                            AttendanceStatus.PRESENT -> glass.success
                            AttendanceStatus.ABSENT -> glass.danger
                            AttendanceStatus.LATE -> glass.warning
                            AttendanceStatus.EXCUSED -> MaterialTheme.colorScheme.primary
                        }
                        GlassPillChip(
                            label = st.label,
                            selected = currentStatus == st,
                            selectedColor = chipColor,
                            onClick = { onSelectStatus(student.id, st) },
                            modifier = Modifier.testTag("att_${student.id}_${st.name}")
                        )
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun TeacherComplainVoiceTab(
    complaints: List<ComplaintEntity>,
    playingTextId: String?,
    playingVoicePath: String?,
    onNewTeacherComplaint: () -> Unit,
    onReplyClick: (ComplaintEntity) -> Unit,
    onPlaySound: (id: String, title: String, text: String) -> Unit,
    onPlayVoice: (path: String, fallback: String?) -> Unit
) {
    val glass = LocalGlassPalette.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Teacher Complain Box (SMS & Voice SMS)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Record Voice SMS or send text complaints to parents & reply to parent queries.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                GlassPrimaryButton(
                    text = "Send SMS / Record Voice SMS Complaint",
                    onClick = onNewTeacherComplaint,
                    leadingIcon = { Icon(Icons.Rounded.Mic, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_teacher_voice_complaint_button")
                )
            }
        }

        items(complaints, key = { it.id }) { item ->
            val isResolved = item.status.equals("Resolved", ignoreCase = true)
            val isPlaying = playingTextId == "t_comp_${item.id}"
            val voicePath = item.voiceFilePath ?: item.replyVoicePath
            val isVoicePlaying = !voicePath.isNullOrBlank() && playingVoicePath == voicePath

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${item.senderRole}: ${item.targetStudentName}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    StatusBadge(
                        text = item.status.uppercase(),
                        color = if (isResolved) glass.success else glass.warning
                    )
                }
                Text(
                    text = "${item.category} • From ${item.senderName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(item.message, style = MaterialTheme.typography.bodyMedium)

                if (!item.replyMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Reply: ${item.replyMessage}",
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.success,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onPlaySound("t_comp_${item.id}", item.category, item.message) }
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.VolumeUp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isPlaying) "Stop" else "Listen")
                    }
                    if (!voicePath.isNullOrBlank()) {
                        OutlinedButton(onClick = { onPlayVoice(voicePath, item.message) }) {
                            Icon(
                                imageVector = if (isVoicePlaying) Icons.Rounded.Stop else Icons.Rounded.RecordVoiceOver,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isVoicePlaying) "Stop" else "Voice")
                        }
                    }
                    if (item.senderRole == "PARENT" && !isResolved) {
                        GlassPrimaryButton(
                            text = "Reply",
                            onClick = { onReplyClick(item) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Reply, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun TeacherExamsAndNoticesTab(
    exams: List<ExamScheduleEntity>,
    notices: List<NoticeEntity>,
    fees: List<FeeItemEntity>,
    nowMillis: Long,
    voiceLanguage: VoiceLanguageMode,
    onAddExamClick: () -> Unit,
    onPostNoticeClick: () -> Unit,
    onSendBillReminder: (FeeItemEntity) -> Unit,
    onExamClick: (ExamScheduleEntity) -> Unit,
    onNoticeClick: (NoticeEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlassPrimaryButton(
                    text = "Post Holiday / Notice",
                    onClick = onPostNoticeClick,
                    leadingIcon = { Icon(Icons.Rounded.Campaign, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("post_notice_open_button")
                )
                OutlinedButton(
                    onClick = onAddExamClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Exam")
                }
            }
        }

        item {
            Text(
                text = "Real-Time Exam Schedules",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        items(exams, key = { "exam_${it.id}" }) { exam ->
            ExamScheduleGlassCard(
                exam = exam,
                nowMillis = nowMillis,
                onClick = { onExamClick(exam) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Send Student Bill SMS Reminders",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        items(fees.filter { it.amount > it.paidAmount }, key = { "fee_${it.id}" }) { fee ->
            val due = (fee.amount - fee.paidAmount).toInt()
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${fee.studentName} • ${fee.feeTitle}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("Pending Dues: Rs $due • Due ${fee.dueDate}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    GlassPrimaryButton(
                        text = "Bill SMS",
                        onClick = { onSendBillReminder(fee) }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Published School Notices",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        items(notices, key = { "notice_${it.id}" }) { notice ->
            val localized = AnnouncementVoiceFormatter.localize(notice.title, notice.description, voiceLanguage)
            NoticeGlassCard(
                notice = notice.copy(title = localized.displayTitle, description = localized.displayBody),
                onClick = { onNoticeClick(notice) }
            )
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun TeacherLeaveAndSalaryTab(
    leaves: List<TeacherLeaveEntity>,
    salaryInquiries: List<SalaryInquiryEntity>,
    onSubmitLeaveClick: () -> Unit,
    onCancelLeave: (Int) -> Unit,
    onAskSalaryClick: () -> Unit
) {
    val glass = LocalGlassPalette.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Teacher Salary & Payroll",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Monthly Breakdown",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.Payments,
                        contentDescription = null,
                        tint = glass.success,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Basic Pay", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Rs 42,000", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("Allowances", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("+Rs 9,500", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = glass.success)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Net Payable", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Rs 49,200", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                GlassPrimaryButton(
                    text = "Ask About Salary",
                    onClick = onAskSalaryClick,
                    leadingIcon = { Icon(Icons.Rounded.AccountBalance, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ask_salary_open_button")
                )
            }
        }

        items(salaryInquiries, key = { "sal_${it.id}" }) { sal ->
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${sal.inquiryTopic} (${sal.monthLabel})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    StatusBadge(
                        text = sal.status.uppercase(),
                        color = if (sal.status == "Disbursed") glass.success else glass.warning
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(sal.inquiryMessage, style = MaterialTheme.typography.bodySmall)
                if (!sal.responseNote.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = sal.responseNote,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Leave Application Submission",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Apply for sick, casual, or personal leave with substitute teacher assignment.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                GlassPrimaryButton(
                    text = "Submit New Leave Application",
                    onClick = onSubmitLeaveClick,
                    leadingIcon = { Icon(Icons.AutoMirrored.Rounded.EventNote, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("submit_leave_open_button")
                )
            }
        }

        items(leaves, key = { "leave_${it.id}" }) { leave ->
            val color = when (leave.status) {
                "Approved" -> glass.success
                "Rejected" -> glass.danger
                else -> glass.warning
            }
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(leave.leaveType, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("${leave.startDate} to ${leave.endDate} • Sub: ${leave.substituteTeacher}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    StatusBadge(text = leave.status.uppercase(), color = color)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(leave.reason, style = MaterialTheme.typography.bodySmall)
                if (leave.status == "Pending") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { onCancelLeave(leave.id) }) {
                            Text("Withdraw Request", color = glass.danger)
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
