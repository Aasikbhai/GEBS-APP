package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudibleSmsController
import com.example.data.local.AttendanceStatus
import com.example.data.local.CampusDatabase
import com.example.data.local.CampusRepository
import com.example.data.local.ComplaintEntity
import com.example.data.local.ExamScheduleEntity
import com.example.data.local.FeeItemEntity
import com.example.data.local.NoticeCategory
import com.example.data.local.NoticeEntity
import com.example.data.local.SalaryInquiryEntity
import com.example.data.local.SmsAlertEntity
import com.example.data.local.StudentRecordEntity
import com.example.data.local.TeacherLeaveEntity
import com.example.data.sync.PcSync
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UserSession {
    data object LoggedOut : UserSession
    data class Student(
        val username: String = "",
        val displayName: String = "",
        val registrationNo: String = "",
        val className: String = "",
        val rollNumber: String = "",
        val parentName: String = "",
        val parentPhone: String = ""
    ) : UserSession

    data class Teacher(
        val username: String = "",
        val displayName: String = "",
        val registrationNo: String = "",
        val designation: String = "Teacher"
    ) : UserSession
}

data class PortalDataState(
    val students: List<StudentRecordEntity> = emptyList(),
    val smsAlerts: List<SmsAlertEntity> = emptyList(),
    val fees: List<FeeItemEntity> = emptyList(),
    val notices: List<NoticeEntity> = emptyList(),
    val complaints: List<ComplaintEntity> = emptyList(),
    val exams: List<ExamScheduleEntity> = emptyList(),
    val leaves: List<TeacherLeaveEntity> = emptyList(),
    val salaryInquiries: List<SalaryInquiryEntity> = emptyList()
)

class CampusViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CampusRepository
    val audioController = AudibleSmsController(application.applicationContext)

    private val _session = MutableStateFlow<UserSession>(UserSession.LoggedOut)
    val session: StateFlow<UserSession> = _session.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _feedbackBanner = MutableStateFlow<String?>(null)
    val feedbackBanner: StateFlow<String?> = _feedbackBanner.asStateFlow()

    private val _currentTimeMillis = MutableStateFlow(System.currentTimeMillis())
    val currentTimeMillis: StateFlow<Long> = _currentTimeMillis.asStateFlow()

    private val _draftAttendance = MutableStateFlow<Map<Int, AttendanceStatus>>(emptyMap())
    val draftAttendance: StateFlow<Map<Int, AttendanceStatus>> = _draftAttendance.asStateFlow()

    val dataState: StateFlow<PortalDataState>

    init {
        PcSync.init(application)
        val dao = CampusDatabase.getInstance(application).campusDao()
        repository = CampusRepository(dao)

        viewModelScope.launch {
            repository.ensureSeeded()
        }

        // Live 1-second clock for Real-Time Exam Schedules
        viewModelScope.launch {
            while (true) {
                _currentTimeMillis.value = System.currentTimeMillis()
                delay(1000L)
            }
        }

        val firstFour = combine(
            repository.students,
            repository.smsAlerts,
            repository.fees,
            repository.notices
        ) { students, smsAlerts, fees, notices ->
            Quad(students, smsAlerts, fees, notices)
        }

        val lastFour = combine(
            repository.complaints,
            repository.exams,
            repository.leaves,
            repository.salaryInquiries
        ) { complaints, exams, leaves, salaryInquiries ->
            Quad(complaints, exams, leaves, salaryInquiries)
        }

        dataState = combine(firstFour, lastFour) { a, b ->
            PortalDataState(
                students = a.first,
                smsAlerts = a.second,
                fees = a.third,
                notices = a.fourth,
                complaints = b.first,
                exams = b.second,
                leaves = b.third,
                salaryInquiries = b.fourth
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = PortalDataState()
        )
    }

    private val _loginBusy = MutableStateFlow(false)
    val loginBusy: StateFlow<Boolean> = _loginBusy.asStateFlow()

    private val _peerAddress = MutableStateFlow(PcSync.peer)
    val peerAddress: StateFlow<String> = _peerAddress.asStateFlow()

    private val _syncText = MutableStateFlow("")
    val syncText: StateFlow<String> = _syncText.asStateFlow()

    private val syncMutex = Mutex()
    private var syncJob: Job? = null

    fun setPeerAddress(value: String) {
        PcSync.peer = value
        _peerAddress.value = value.trim()
    }

    /**
     * Sign in with Full Name + Registration No. (the Registration No. is the password).
     * The school software on the PC checks them. When the PC is not reachable, people who
     * already signed in once on this phone can still open the app offline.
     */
    fun login(usernameInput: String, passwordInput: String) {
        val user = usernameInput.trim().replace(Regex("\\s+"), " ")
        val pass = passwordInput.trim()

        if (user.isEmpty() || pass.isEmpty()) {
            _loginError.value = "Please enter both your Full Name and Registration No."
            return
        }
        if (PcSync.peer.isBlank()) {
            _loginError.value = "Type the School PC address first (shown in the school software: Settings)."
            return
        }

        _loginBusy.value = true
        viewModelScope.launch {
            val res = PcSync.sync(user, pass)
            val bundle = res.bundle
            when {
                res.status == PcSync.Status.OK && bundle != null -> {
                    val profile = bundle.getJSONObject("profile")
                    rememberMoney(profile)
                    switchDataOwner(PcSync.ownerKey(profile.optString("name"), pass))
                    repository.applyBundle(bundle)
                    PcSync.saveSession(profile, pass)
                    startSession(profile, pass)
                    _syncText.value = "Connected to school PC"
                }
                res.status == PcSync.Status.BAD_LOGIN -> {
                    _loginError.value = "Name or Registration No. did not match the school software. Ask the school office."
                }
                else -> {
                    val saved = PcSync.findSavedSession(user, pass)
                    if (saved != null) {
                        rememberMoney(saved)
                        switchDataOwner(PcSync.ownerKey(saved.optString("name"), pass))
                        startSession(saved, pass)
                        _syncText.value = "Offline - changes will be sent when the school PC is reachable"
                        _feedbackBanner.value = "School PC not reachable: working offline. Changes will sync automatically."
                    } else {
                        _loginError.value = "Cannot reach the school PC. Check the PC address and that the phone is on the same Wi-Fi. (The first sign-in needs the PC.)"
                    }
                }
            }
            _loginBusy.value = false
        }
    }

    private suspend fun switchDataOwner(key: String) {
        if (PcSync.dataOwner != key) {
            repository.clearSyncedTables()
            PcSync.dataOwner = key
        }
    }

    private fun rememberMoney(profile: JSONObject) {
        PcSync.basicPay = profile.optDouble("basicPay", 0.0)
        PcSync.allowances = profile.optDouble("allowances", 0.0)
    }

    private fun startSession(profile: JSONObject, reg: String) {
        val name = profile.optString("name")
        PcSync.owner = PcSync.ownerKey(name, reg)
        _loginError.value = null
        _session.value = if (profile.optString("role") == "teacher") {
            UserSession.Teacher(
                username = name,
                displayName = name,
                registrationNo = reg,
                designation = profile.optString("designation").ifBlank { "Teacher" }
            )
        } else {
            UserSession.Student(
                username = name,
                displayName = name,
                registrationNo = reg,
                className = profile.optString("className"),
                rollNumber = profile.optString("rollNumber"),
                parentName = profile.optString("parentName"),
                parentPhone = profile.optString("parentPhone")
            )
        }
        startAutoSync()
    }

    private fun startAutoSync() {
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            while (true) {
                delay(20_000L)
                doSync()
            }
        }
    }

    /** Send waiting changes right now and pull the newest data (also runs every 20 seconds). */
    fun syncNow() {
        viewModelScope.launch { doSync() }
    }

    private suspend fun doSync() {
        val (name, reg) = when (val s = _session.value) {
            is UserSession.Student -> s.displayName to s.registrationNo
            is UserSession.Teacher -> s.displayName to s.registrationNo
            else -> return
        }
        syncMutex.withLock {
            val res = PcSync.sync(name, reg)
            val bundle = res.bundle
            when {
                res.status == PcSync.Status.OK && bundle != null -> {
                    repository.applyBundle(bundle)
                    val t = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                    _syncText.value = "Synced with school PC at $t"
                }
                res.status == PcSync.Status.BAD_LOGIN ->
                    _syncText.value = "School software no longer accepts this login"
                else -> {
                    val n = PcSync.pendingCount()
                    _syncText.value = if (n > 0) "Offline - $n change(s) waiting to send" else "Offline - school PC not reachable"
                }
            }
        }
    }

    fun clearLoginError() {
        _loginError.value = null
    }

    fun logout() {
        audioController.stopAudibleSms()
        audioController.stopVoicePlayback()
        syncJob?.cancel()
        PcSync.owner = ""
        _draftAttendance.value = emptyMap()
        _session.value = UserSession.LoggedOut
    }

    fun setDraftAttendance(studentId: Int, status: AttendanceStatus) {
        _draftAttendance.value = _draftAttendance.value + (studentId to status)
    }

    fun markClassAllPresent(studentsInClass: List<StudentRecordEntity>) {
        val updates = studentsInClass.associate { it.id to AttendanceStatus.PRESENT }
        _draftAttendance.value = _draftAttendance.value + updates
    }

    fun saveAttendanceAndDispatchSms(
        studentsInClass: List<StudentRecordEntity>,
        onCompleted: (sentCount: Int, absentOrLateList: List<StudentRecordEntity>) -> Unit
    ) {
        val teacherName = (_session.value as? UserSession.Teacher)?.displayName ?: "Teacher"
        val updated = studentsInClass.map { student ->
            val chosen = _draftAttendance.value[student.id]?.name ?: student.attendanceStatus
            student.copy(attendanceStatus = chosen)
        }
        viewModelScope.launch {
            val sent = repository.saveAttendanceBatchAndSendSms(updated, teacherName)
            val flagged = updated.filter {
                it.attendanceStatus == AttendanceStatus.ABSENT.name ||
                    it.attendanceStatus == AttendanceStatus.LATE.name
            }
            syncNow()
            _feedbackBanner.value = "Saved attendance for ${updated.size} students & dispatched $sent Audible SMS alerts."
            onCompleted(sent, flagged)
        }
    }

    fun payFee(fee: FeeItemEntity) {
        viewModelScope.launch {
            repository.payFeeAndGenerateBillSms(fee)
            syncNow()
            _feedbackBanner.value = "Paid ${fee.feeTitle}! Bill Receipt SMS sent."
        }
    }

    fun sendBillSmsReminder(fee: FeeItemEntity) {
        viewModelScope.launch {
            repository.sendBillReminderSms(fee)
            syncNow()
            _feedbackBanner.value = "Bill Reminder SMS sent to ${fee.studentName}'s parent."
        }
    }

    fun postNotice(
        title: String,
        description: String,
        category: NoticeCategory,
        effectiveDate: String
    ) {
        val author = (_session.value as? UserSession.Teacher)?.displayName ?: "School Office"
        viewModelScope.launch {
            repository.postNoticeAndSms(title, description, category, effectiveDate, author)
            syncNow()
            _feedbackBanner.value = "Published '${title.trim()}' & sent Audible Notice SMS!"
        }
    }

    fun submitParentComplaint(
        category: String,
        message: String,
        voiceFilePath: String?
    ) {
        val student = (_session.value as? UserSession.Student) ?: UserSession.Student()
        viewModelScope.launch {
            repository.submitComplaint(
                senderRole = "PARENT",
                senderName = "${student.parentName} (Parent of ${student.displayName})",
                targetStudentName = student.displayName,
                category = category,
                message = message,
                voiceFilePath = voiceFilePath,
                recipientPhone = student.parentPhone
            )
            syncNow()
            _feedbackBanner.value = "Parent complaint submitted & SMS alert sent to Class Teacher."
        }
    }

    fun submitTeacherComplaint(
        targetStudent: StudentRecordEntity,
        category: String,
        message: String,
        voiceFilePath: String?
    ) {
        val teacher = (_session.value as? UserSession.Teacher)?.displayName ?: "Teacher"
        viewModelScope.launch {
            repository.submitComplaint(
                senderRole = "TEACHER",
                senderName = teacher,
                targetStudentName = targetStudent.name,
                category = category,
                message = message,
                voiceFilePath = voiceFilePath,
                recipientPhone = targetStudent.parentPhone
            )
            syncNow()
            _feedbackBanner.value = if (voiceFilePath != null) {
                "Voice SMS & Complaint dispatched for ${targetStudent.name}!"
            } else {
                "Teacher SMS Complaint dispatched to ${targetStudent.parentName}."
            }
        }
    }

    fun replyToParentComplaint(
        complaint: ComplaintEntity,
        replyText: String,
        replyVoicePath: String?
    ) {
        val teacher = (_session.value as? UserSession.Teacher)?.displayName ?: "Teacher"
        viewModelScope.launch {
            repository.replyToComplaint(complaint, replyText, replyVoicePath, teacher)
            syncNow()
            _feedbackBanner.value = "Reply SMS sent to ${complaint.senderName}."
        }
    }

    fun submitTeacherLeave(
        leaveType: String,
        startDate: String,
        endDate: String,
        substituteTeacher: String,
        reason: String
    ) {
        val teacher = (_session.value as? UserSession.Teacher)?.displayName ?: "Teacher"
        viewModelScope.launch {
            repository.submitLeave(teacher, leaveType, startDate, endDate, substituteTeacher, reason)
            syncNow()
            _feedbackBanner.value = "Leave application submitted to Principal Office."
        }
    }

    fun cancelTeacherLeave(id: Int) {
        viewModelScope.launch {
            repository.cancelLeave(id)
            syncNow()
            _feedbackBanner.value = "Leave application withdrawn."
        }
    }

    fun submitSalaryInquiry(
        monthLabel: String,
        topic: String,
        message: String
    ) {
        val teacher = (_session.value as? UserSession.Teacher)?.displayName ?: "Teacher"
        viewModelScope.launch {
            repository.submitSalaryInquiry(teacher, monthLabel, topic, message)
            syncNow()
            _feedbackBanner.value = "Salary inquiry submitted to Accounts Department."
        }
    }

    fun addExamSchedule(
        subject: String,
        className: String,
        examDateLabel: String,
        hoursFromNow: Int,
        durationHours: Int,
        roomNumber: String,
        syllabus: String
    ) {
        val teacher = (_session.value as? UserSession.Teacher)?.displayName ?: "Teacher"
        viewModelScope.launch {
            repository.addExamSchedule(
                subject = subject,
                className = className,
                examDateLabel = examDateLabel,
                hoursFromNow = hoursFromNow,
                durationHours = durationHours,
                roomNumber = roomNumber,
                invigilator = teacher,
                syllabus = syllabus
            )
            syncNow()
            _feedbackBanner.value = "Added $subject exam to real-time schedule."
        }
    }

    fun markSmsAsRead(id: Int) {
        viewModelScope.launch {
            repository.markSmsAsRead(id)
        }
    }

    fun clearFeedbackBanner() {
        _feedbackBanner.value = null
    }

    override fun onCleared() {
        super.onCleared()
        audioController.release()
    }
}

private data class Quad<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
