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
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MarkChatUnread
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AddComment
import androidx.compose.material.icons.rounded.Feedback
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MarkChatUnread
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.data.local.FeeStatus
import com.example.data.local.NoticeEntity
import com.example.data.local.SmsAlertEntity
import com.example.data.local.SmsCategory
import com.example.ui.CampusViewModel
import com.example.ui.UserSession
import com.example.ui.components.AcousticWaveVisualizer
import com.example.ui.components.AuroraGlassBackground
import com.example.ui.components.FloatingGlassAudioPlayerBar
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassPillChip
import com.example.ui.components.GlassPrimaryButton
import com.example.ui.components.StatusBadge
import com.example.ui.components.VoiceLanguageBar
import com.example.ui.components.VoiceLanguageStudioPopup
import com.example.ui.theme.LocalGlassPalette

@Composable
fun StudentPortalScreen(
    session: UserSession.Student,
    viewModel: CampusViewModel,
    onLogout: () -> Unit
) {
    val state by viewModel.dataState.collectAsStateWithLifecycle()
    val nowMillis by viewModel.currentTimeMillis.collectAsStateWithLifecycle()
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
    var selectedNotice by remember { mutableStateOf<NoticeEntity?>(null) }
    var selectedExam by remember { mutableStateOf<ExamScheduleEntity?>(null) }
    var selectedFee by remember { mutableStateOf<FeeItemEntity?>(null) }
    var showNewComplaintPopup by rememberSaveable { mutableStateOf(false) }
    var showVoiceStudioPopup by rememberSaveable { mutableStateOf(false) }

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

    val tabs = listOf("Home", "Sound SMS", "Fees & Exams", "Complain")
    val selectedIcons = listOf(
        Icons.Rounded.Home,
        Icons.Rounded.MarkChatUnread,
        Icons.Rounded.AccountBalanceWallet,
        Icons.Rounded.Feedback
    )
    val unselectedIcons = listOf(
        Icons.Outlined.Home,
        Icons.Outlined.MarkChatUnread,
        Icons.Outlined.AccountBalanceWallet,
        Icons.Outlined.Feedback
    )

    val studentRecord = state.students.firstOrNull {
        it.registrationNo.equals(session.registrationNo, ignoreCase = true) ||
            it.name.equals(session.displayName, ignoreCase = true)
    }
    val unreadSmsCount = state.smsAlerts.count { !it.isRead }

    AuroraGlassBackground {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 700.dp

            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = { SnackbarHost(snackbarHostState) },
                floatingActionButton = {
                    if (selectedTab == 3) {
                        FloatingActionButton(
                            onClick = { showNewComplaintPopup = true },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White,
                            modifier = Modifier.testTag("new_parent_complaint_fab")
                        ) {
                            Icon(Icons.Rounded.AddComment, contentDescription = "New Parent Complaint")
                        }
                    }
                },
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
                                        label = {
                                            val labelText = if (index == 1 && unreadSmsCount > 0) "$title ($unreadSmsCount)" else title
                                            Text(labelText, maxLines = 1)
                                        },
                                        modifier = Modifier.testTag("student_tab_$index")
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
                        StudentTopHeader(
                            session = session,
                            onLogout = onLogout
                        )

                        VoiceLanguageBar(
                            currentMode = voiceLanguage,
                            qualityLabel = qualityLabel,
                            onSelectMode = { viewModel.audioController.setVoiceLanguageMode(it) },
                            onOpenStudio = { showVoiceStudioPopup = true }
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        when (selectedTab) {
                            0 -> StudentHomeTab(
                                session = session,
                                attendanceStatus = studentRecord?.attendanceStatus ?: AttendanceStatus.PRESENT.name,
                                attendanceDate = studentRecord?.attendanceDate ?: "Today",
                                latestAttendanceSms = state.smsAlerts.firstOrNull { it.category == SmsCategory.ATTENDANCE.name },
                                notices = state.notices,
                                fees = state.fees,
                                nextExam = state.exams.firstOrNull { computeLiveExamStatus(it, nowMillis).state != LiveExamState.COMPLETED },
                                nowMillis = nowMillis,
                                voiceLanguage = voiceLanguage,
                                playingTextId = playingTextId,
                                onPlaySoundSms = { id, title, text ->
                                    viewModel.audioController.playAudibleSms(id, text, title = title)
                                },
                                onOpenNotice = { selectedNotice = it },
                                onOpenExam = { selectedExam = it },
                                onGoToTab = { selectedTab = it }
                            )
                            1 -> StudentSoundSmsTab(
                                alerts = state.smsAlerts,
                                voiceLanguage = voiceLanguage,
                                playingTextId = playingTextId,
                                playingVoicePath = playingVoicePath,
                                speechRate = speechRate,
                                onToggleRate = { viewModel.audioController.toggleSpeechRate() },
                                onOpenVoiceStudio = { showVoiceStudioPopup = true },
                                onPlayTextSms = { alert ->
                                    viewModel.markSmsAsRead(alert.id)
                                    viewModel.audioController.playAudibleSms(
                                        utteranceId = "sms_${alert.id}",
                                        spokenText = alert.message,
                                        title = alert.title
                                    )
                                },
                                onPlayVoiceSms = { alert ->
                                    viewModel.markSmsAsRead(alert.id)
                                    val path = alert.audioFilePath
                                    if (!path.isNullOrBlank()) {
                                        viewModel.audioController.toggleVoicePlayback(path, alert.message)
                                    }
                                }
                            )
                            2 -> StudentFeesAndExamsTab(
                                fees = state.fees,
                                exams = state.exams,
                                nowMillis = nowMillis,
                                onFeeClick = { selectedFee = it },
                                onExamClick = { selectedExam = it }
                            )
                            3 -> ParentComplainTab(
                                complaints = state.complaints,
                                playingTextId = playingTextId,
                                playingVoicePath = playingVoicePath,
                                onNewComplaintClick = { showNewComplaintPopup = true },
                                onPlayComplaintSound = { id, title, text ->
                                    viewModel.audioController.playAudibleSms(id, text, title = title)
                                },
                                onPlayVoiceReply = { path, fallback ->
                                    viewModel.audioController.toggleVoicePlayback(path, fallback)
                                }
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

    selectedNotice?.let { notice ->
        val localized = AnnouncementVoiceFormatter.localize(notice.title, notice.description, voiceLanguage)
        NoticeDetailGlassPopup(
            notice = notice.copy(title = localized.displayTitle, description = localized.displayBody),
            onPlaySoundSms = {
                viewModel.audioController.playAudibleSms(
                    utteranceId = "notice_${notice.id}",
                    spokenText = notice.description,
                    title = notice.title
                )
            },
            onDismiss = { selectedNotice = null }
        )
    }

    selectedExam?.let { exam ->
        ExamDetailGlassPopup(
            exam = exam,
            nowMillis = nowMillis,
            onDismiss = { selectedExam = null }
        )
    }

    selectedFee?.let { fee ->
        FeeDetailGlassPopup(
            fee = fee,
            onPayFee = { viewModel.payFee(it) },
            onPlayBillSms = {
                val due = (it.amount - it.paidAmount).coerceAtLeast(0.0).toInt()
                viewModel.audioController.playAudibleSms(
                    utteranceId = "fee_${it.id}",
                    spokenText = "Bill SMS for ${it.feeTitle}. Total amount Rs ${it.amount.toInt()}. Pending dues Rs $due. Due date ${it.dueDate}.",
                    title = "Bill SMS: ${it.feeTitle}"
                )
            },
            onDismiss = { selectedFee = null }
        )
    }

    if (showNewComplaintPopup) {
        NewParentComplaintPopup(
            studentName = session.displayName,
            onSubmit = { category, msg, alsoSms ->
                viewModel.submitParentComplaint(category, msg, null)
                if (alsoSms) {
                    SmsIntentHelper.launchDeviceSmsComposer(
                        context = context,
                        phoneNumber = "+91 98000 11223",
                        messageBody = "Parent Complaint [$category] for ${session.displayName}: $msg"
                    )
                }
            },
            onDismiss = { showNewComplaintPopup = false }
        )
    }
}

@Composable
private fun StudentTopHeader(
    session: UserSession.Student,
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
                    painter = painterResource(id = R.drawable.img_student_avatar),
                    contentDescription = stringResource(R.string.cd_student_avatar),
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
                        text = "${session.className} • Roll ${session.rollNumber} • Reg: ${session.registrationNo}",
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

@Composable
private fun StudentHomeTab(
    session: UserSession.Student,
    attendanceStatus: String,
    attendanceDate: String,
    latestAttendanceSms: SmsAlertEntity?,
    notices: List<NoticeEntity>,
    fees: List<FeeItemEntity>,
    nextExam: ExamScheduleEntity?,
    nowMillis: Long,
    voiceLanguage: VoiceLanguageMode,
    playingTextId: String?,
    onPlaySoundSms: (id: String, title: String, text: String) -> Unit,
    onOpenNotice: (NoticeEntity) -> Unit,
    onOpenExam: (ExamScheduleEntity) -> Unit,
    onGoToTab: (Int) -> Unit
) {
    val glass = LocalGlassPalette.current
    val statusEnum = runCatching { AttendanceStatus.valueOf(attendanceStatus) }.getOrDefault(AttendanceStatus.PRESENT)
    val statusColor = when (statusEnum) {
        AttendanceStatus.PRESENT -> glass.success
        AttendanceStatus.ABSENT -> glass.danger
        AttendanceStatus.LATE -> glass.warning
        AttendanceStatus.EXCUSED -> MaterialTheme.colorScheme.primary
    }
    val totalPending = fees.sumOf { (it.amount - it.paidAmount).coerceAtLeast(0.0) }.toInt()

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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Today's Attendance ($attendanceDate)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${session.displayName} is ${statusEnum.label}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                    StatusBadge(text = statusEnum.label.uppercase(), color = statusColor)
                }

                if (latestAttendanceSms != null) {
                    val localizedAtt = AnnouncementVoiceFormatter.localize(
                        latestAttendanceSms.title,
                        latestAttendanceSms.message,
                        voiceLanguage
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = localizedAtt.displayBody,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    val isPlaying = playingTextId == "home_att_sms"
                    GlassPrimaryButton(
                        text = if (isPlaying) "Stop Sound SMS" else "Listen (${localizedAtt.resolvedLanguage.nativeLabel})",
                        onClick = {
                            onPlaySoundSms("home_att_sms", latestAttendanceSms.title, latestAttendanceSms.message)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.VolumeUp,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("play_home_attendance_sms")
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassCard(
                    modifier = Modifier.weight(1f),
                    onClick = { onGoToTab(2) }
                ) {
                    Text(
                        text = "Student Dues",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Rs $totalPending",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (totalPending > 0) glass.danger else glass.success
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Tap for Bill SMS & Pay",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                GlassCard(
                    modifier = Modifier.weight(1f),
                    onClick = { onGoToTab(3) }
                ) {
                    Text(
                        text = "Complain Box",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Parent Desk",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Report activity issue",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (nextExam != null) {
            item {
                Text(
                    text = "Live Exam Tracker",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            item {
                ExamScheduleGlassCard(
                    exam = nextExam,
                    nowMillis = nowMillis,
                    onClick = { onOpenExam(nextExam) }
                )
            }
        }

        item {
            Text(
                text = "School Notices & Holidays",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        items(notices, key = { it.id }) { notice ->
            val localized = AnnouncementVoiceFormatter.localize(notice.title, notice.description, voiceLanguage)
            NoticeGlassCard(
                notice = notice.copy(title = localized.displayTitle, description = localized.displayBody),
                onClick = { onOpenNotice(notice) }
            )
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StudentSoundSmsTab(
    alerts: List<SmsAlertEntity>,
    voiceLanguage: VoiceLanguageMode,
    playingTextId: String?,
    playingVoicePath: String?,
    speechRate: Float,
    onToggleRate: () -> Unit,
    onOpenVoiceStudio: () -> Unit,
    onPlayTextSms: (SmsAlertEntity) -> Unit,
    onPlayVoiceSms: (SmsAlertEntity) -> Unit
) {
    val filters = listOf("ALL", SmsCategory.ATTENDANCE.name, SmsCategory.BILL.name, SmsCategory.NOTICE.name, SmsCategory.VOICE.name)
    val filterLabels = mapOf(
        "ALL" to "All SMS",
        SmsCategory.ATTENDANCE.name to "Attendance",
        SmsCategory.BILL.name to "Bill SMS",
        SmsCategory.NOTICE.name to "Notices",
        SmsCategory.VOICE.name to "Voice/Comp"
    )
    var activeFilter by rememberSaveable { mutableStateOf("ALL") }
    val filtered = remember(alerts, activeFilter) {
        if (activeFilter == "ALL") alerts
        else if (activeFilter == SmsCategory.VOICE.name) alerts.filter {
            it.category == SmsCategory.VOICE.name || it.category == SmsCategory.COMPLAINT.name
        }
        else alerts.filter { it.category == activeFilter }
    }

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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Audible Sound SMS Box",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Listen clearly in Auto, English, हिन्दी, or नेपाली",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            onClick = onToggleRate,
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Speed, contentDescription = "Voice Speed", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${speechRate}x", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        Surface(
                            onClick = onOpenVoiceStudio,
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.GraphicEq, contentDescription = "Voice Studio", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("HD", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filters.forEach { key ->
                        GlassPillChip(
                            label = filterLabels[key] ?: key,
                            selected = activeFilter == key,
                            onClick = { activeFilter = key }
                        )
                    }
                }
            }
        }

        items(filtered, key = { it.id }) { alert ->
            val isTextPlaying = playingTextId == "sms_${alert.id}"
            val isVoicePlaying = !alert.audioFilePath.isNullOrBlank() && playingVoicePath == alert.audioFilePath
            val localized = AnnouncementVoiceFormatter.localize(alert.title, alert.message, voiceLanguage)

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = localized.displayTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    StatusBadge(
                        text = localized.resolvedLanguage.shortBadge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    StatusBadge(
                        text = if (alert.isRead) "READ" else "NEW",
                        color = if (alert.isRead) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = localized.displayBody,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AcousticWaveVisualizer(isActive = isTextPlaying || isVoicePlaying)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formatRelativeTimestamp(alert.createdAtMillis),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!alert.audioFilePath.isNullOrBlank()) {
                            OutlinedButton(onClick = { onPlayVoiceSms(alert) }) {
                                Icon(
                                    imageVector = if (isVoicePlaying) Icons.Rounded.Stop else Icons.Rounded.GraphicEq,
                                    contentDescription = "Play voice recording",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isVoicePlaying) "Stop" else "Voice")
                            }
                        }
                        GlassPrimaryButton(
                            text = if (isTextPlaying) "Stop" else "Listen",
                            onClick = { onPlayTextSms(alert) },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isTextPlaying) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.VolumeUp,
                                    contentDescription = stringResource(R.string.cd_play_sound_sms),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun StudentFeesAndExamsTab(
    fees: List<FeeItemEntity>,
    exams: List<ExamScheduleEntity>,
    nowMillis: Long,
    onFeeClick: (FeeItemEntity) -> Unit,
    onExamClick: (ExamScheduleEntity) -> Unit
) {
    val glass = LocalGlassPalette.current
    val totalAmount = fees.sumOf { it.amount }.toInt()
    val totalPaid = fees.sumOf { it.paidAmount }.toInt()
    val totalDue = (totalAmount - totalPaid).coerceAtLeast(0)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Student Dues & Bill Summary",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Total Billed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Rs $totalAmount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("Paid", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Rs $totalPaid", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = glass.success)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Remaining Due", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Rs $totalDue", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (totalDue > 0) glass.danger else glass.success)
                    }
                }
            }
        }

        items(fees, key = { it.id }) { fee ->
            val due = (fee.amount - fee.paidAmount).coerceAtLeast(0.0).toInt()
            val badgeColor = when (fee.status) {
                FeeStatus.PAID.name -> glass.success
                FeeStatus.PARTIAL.name -> glass.warning
                else -> glass.danger
            }
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("fee_item_${fee.id}"),
                onClick = { onFeeClick(fee) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(fee.feeTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Due Date: ${fee.dueDate} • Total Rs ${fee.amount.toInt()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    StatusBadge(text = if (due > 0) "DUE Rs $due" else "PAID", color = badgeColor)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Real-Time Exam Schedules",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Live ticking countdowns & exam halls",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(exams, key = { it.id }) { exam ->
            ExamScheduleGlassCard(
                exam = exam,
                nowMillis = nowMillis,
                onClick = { onExamClick(exam) }
            )
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun ParentComplainTab(
    complaints: List<ComplaintEntity>,
    playingTextId: String?,
    playingVoicePath: String?,
    onNewComplaintClick: () -> Unit,
    onPlayComplaintSound: (id: String, title: String, text: String) -> Unit,
    onPlayVoiceReply: (path: String, fallbackText: String?) -> Unit
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
                    text = "Parent Complain Box",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Raise concerns about student activity, homework, transport, or listen to teacher voice replies.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                GlassPrimaryButton(
                    text = "Write New Complaint",
                    onClick = onNewComplaintClick,
                    leadingIcon = { Icon(Icons.Rounded.AddComment, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_parent_complaint_button")
                )
            }
        }

        items(complaints, key = { it.id }) { item ->
            val isResolved = item.status.equals("Resolved", ignoreCase = true)
            val isPlaying = playingTextId == "comp_${item.id}"
            val isVoicePlaying = !item.replyVoicePath.isNullOrBlank() && playingVoicePath == item.replyVoicePath

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${item.category} • ${item.targetStudentName}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    StatusBadge(
                        text = item.status.uppercase(),
                        color = if (isResolved) glass.success else glass.warning
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "From: ${item.senderName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = item.message,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (!item.replyMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = glass.success.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Teacher Reply:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = glass.success
                            )
                            Text(
                                text = item.replyMessage,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val spoken = "${item.message}. ${item.replyMessage ?: ""}"
                            onPlayComplaintSound("comp_${item.id}", item.category, spoken)
                        }
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.VolumeUp,
                            contentDescription = "Listen to complaint",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isPlaying) "Stop" else "Sound SMS")
                    }

                    val voiceFile = item.replyVoicePath ?: item.voiceFilePath
                    if (!voiceFile.isNullOrBlank()) {
                        OutlinedButton(
                            onClick = { onPlayVoiceReply(voiceFile, item.replyMessage ?: item.message) }
                        ) {
                            Icon(
                                imageVector = if (isVoicePlaying) Icons.Rounded.Stop else Icons.Rounded.RecordVoiceOver,
                                contentDescription = "Play voice message",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isVoicePlaying) "Stop Voice" else "Voice SMS")
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}
