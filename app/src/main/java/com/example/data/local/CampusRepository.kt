package com.example.data.local

import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.data.sync.PcSync
import org.json.JSONArray
import org.json.JSONObject

class CampusRepository(private val dao: CampusDao) {

    val students: Flow<List<StudentRecordEntity>> = dao.observeAllStudents()
    val smsAlerts: Flow<List<SmsAlertEntity>> = dao.observeAllSmsAlerts()
    val fees: Flow<List<FeeItemEntity>> = dao.observeAllFees()
    val notices: Flow<List<NoticeEntity>> = dao.observeAllNotices()
    val complaints: Flow<List<ComplaintEntity>> = dao.observeAllComplaints()
    val exams: Flow<List<ExamScheduleEntity>> = dao.observeAllExams()
    val leaves: Flow<List<TeacherLeaveEntity>> = dao.observeAllLeaves()
    val salaryInquiries: Flow<List<SalaryInquiryEntity>> = dao.observeAllSalaryInquiries()

    suspend fun getAllStudentsOnce(): List<StudentRecordEntity> = dao.getAllStudentsOnce()

    /** No built-in demo data any more: everything comes from the school software. */
    suspend fun ensureSeeded() {
        // intentionally empty
    }

    private fun stableId(remote: String): Int {
        val h = remote.hashCode() and 0x7fffffff
        return if (h == 0) 1 else h
    }

    private fun JSONObject.str(key: String): String = if (isNull(key)) "" else optString(key, "")
    private fun JSONObject.strOrNull(key: String): String? = if (isNull(key)) null else optString(key, "").ifBlank { null }

    /** Wipes data of a previous user (used when somebody else signs in while offline). */
    suspend fun clearSyncedTables() {
        dao.clearStudents(); dao.clearFees(); dao.clearNotices(); dao.clearComplaints()
        dao.clearExams(); dao.clearLeaves(); dao.clearSalaryInquiries(); dao.clearSmsAlerts()
    }

    /** Replaces the phone copy with the newest data sent by the school software. */
    suspend fun applyBundle(b: JSONObject) {
        val now = System.currentTimeMillis()
        val oldComplaints = dao.getAllComplaintsOnce().associateBy { it.remoteId }

        val students = b.optJSONArray("students") ?: JSONArray()
        val fees = b.optJSONArray("fees") ?: JSONArray()
        val notices = b.optJSONArray("notices") ?: JSONArray()
        val exams = b.optJSONArray("exams") ?: JSONArray()
        val complaints = b.optJSONArray("complaints") ?: JSONArray()
        val leaves = b.optJSONArray("leaves") ?: JSONArray()
        val salary = b.optJSONArray("salaryInquiries") ?: JSONArray()

        dao.clearStudents()
        dao.insertStudents((0 until students.length()).map { students.getJSONObject(it) }.map { o ->
            StudentRecordEntity(
                id = stableId(o.str("remoteId")), name = o.str("name"), registrationNo = o.str("registrationNo"),
                rollNumber = o.str("rollNumber"), className = o.str("className"), parentName = o.str("parentName"),
                parentPhone = o.str("parentPhone"), attendanceStatus = o.str("attendanceStatus").ifBlank { AttendanceStatus.PRESENT.name },
                attendanceDate = o.str("attendanceDate"), lastUpdatedMillis = now, remoteId = o.str("remoteId")
            )
        })
        dao.clearFees()
        dao.insertFees((0 until fees.length()).map { fees.getJSONObject(it) }.map { o ->
            FeeItemEntity(
                id = stableId(o.str("remoteId")), studentName = o.str("studentName"), rollNumber = o.str("rollNumber"),
                className = o.str("className"), feeTitle = o.str("feeTitle"), amount = o.optDouble("amount", 0.0),
                paidAmount = o.optDouble("paidAmount", 0.0), dueDate = o.str("dueDate"), status = o.str("status").ifBlank { FeeStatus.DUE.name }
            )
        })
        dao.clearNotices()
        dao.insertNotices((0 until notices.length()).map { i -> notices.getJSONObject(i) to i }.map { (o, i) ->
            NoticeEntity(
                id = stableId(o.str("remoteId")), title = o.str("title"), description = o.str("description"),
                category = o.str("category").ifBlank { NoticeCategory.EVENT.name }, postedBy = o.str("postedBy"),
                effectiveDate = o.str("effectiveDate"), createdAtMillis = now - i * 1000L
            )
        })
        dao.clearExams()
        dao.insertExams((0 until exams.length()).map { exams.getJSONObject(it) }.map { o ->
            ExamScheduleEntity(
                id = stableId(o.str("remoteId")), subject = o.str("subject"), className = o.str("className"),
                examDateLabel = o.str("examDateLabel"), startMillis = o.optLong("startMillis"), endMillis = o.optLong("endMillis"),
                roomNumber = o.str("roomNumber"), invigilator = o.str("invigilator"), syllabus = o.str("syllabus")
            )
        })
        dao.clearComplaints()
        dao.insertComplaints((0 until complaints.length()).map { complaints.getJSONObject(it) }.map { o ->
            val rid = o.str("remoteId")
            val old = oldComplaints[rid]
            ComplaintEntity(
                id = stableId(rid), senderRole = o.str("senderRole"), senderName = o.str("senderName"),
                targetStudentName = o.str("targetStudentName"), category = o.str("category"), message = o.str("message"),
                voiceFilePath = old?.voiceFilePath, smsDispatched = true, replyMessage = o.strOrNull("replyMessage"),
                replyVoicePath = old?.replyVoicePath, status = o.str("status").ifBlank { "Open" },
                createdAtMillis = o.optLong("createdAtMillis").let { if (it > 0) it else now },
                clientId = rid.removePrefix("app-"), remoteId = rid
            )
        })
        dao.clearLeaves()
        dao.insertLeaves((0 until leaves.length()).map { leaves.getJSONObject(it) }.map { o ->
            TeacherLeaveEntity(
                id = stableId(o.str("remoteId")), teacherName = o.str("teacherName"), leaveType = o.str("leaveType"),
                startDate = o.str("startDate"), endDate = o.str("endDate"), substituteTeacher = o.str("substituteTeacher"),
                reason = o.str("reason"), status = o.str("status").ifBlank { "Pending" },
                submittedAtMillis = o.optLong("submittedAtMillis").let { if (it > 0) it else now },
                clientId = o.str("remoteId").removePrefix("app-")
            )
        })
        dao.clearSalaryInquiries()
        dao.insertSalaryInquiries((0 until salary.length()).map { salary.getJSONObject(it) }.map { o ->
            SalaryInquiryEntity(
                id = stableId(o.str("remoteId")), teacherName = o.str("teacherName"), monthLabel = o.str("monthLabel"),
                basicPay = o.optDouble("basicPay", 0.0), allowances = o.optDouble("allowances", 0.0),
                deductions = o.optDouble("deductions", 0.0), inquiryTopic = o.str("inquiryTopic"),
                inquiryMessage = o.str("inquiryMessage"), responseNote = o.strOrNull("responseNote"),
                status = o.str("status").ifBlank { "In Review" },
                createdAtMillis = o.optLong("createdAtMillis").let { if (it > 0) it else now },
                clientId = o.str("remoteId").removePrefix("app-")
            )
        })
    }

    suspend fun saveAttendanceBatchAndSendSms(
        updatedStudents: List<StudentRecordEntity>,
        teacherName: String
    ): Int {
        val now = System.currentTimeMillis()
        val todayStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(now))
        val stamped = updatedStudents.map {
            it.copy(attendanceDate = todayStr, lastUpdatedMillis = now)
        }
        dao.updateStudents(stamped)
        val entries = JSONArray()
        stamped.filter { it.remoteId.isNotBlank() }.forEach {
            entries.put(JSONObject().put("studentId", it.remoteId).put("status", it.attendanceStatus))
        }
        if (entries.length() > 0) {
            PcSync.enqueue(JSONObject().put("type", "attendance").put("teacherName", teacherName).put("entries", entries))
        }

        val alerts = stamped.map { student ->
            val statusPhrase = when (student.attendanceStatus) {
                AttendanceStatus.ABSENT.name -> "is ABSENT today ($todayStr). Please inform the class teacher if this is unexpected."
                AttendanceStatus.LATE.name -> "arrived LATE today ($todayStr). Please ensure timely morning arrival."
                AttendanceStatus.EXCUSED.name -> "has an EXCUSED absence today ($todayStr)."
                else -> "is marked PRESENT today ($todayStr) in ${student.className}."
            }
            SmsAlertEntity(
                title = "Attendance SMS: ${student.name} (${AttendanceStatus.valueOf(student.attendanceStatus).label})",
                message = "Dear ${student.parentName}, your child ${student.name} (Roll ${student.rollNumber}, ${student.className}) $statusPhrase - $teacherName",
                recipientName = student.parentName,
                recipientPhone = student.parentPhone,
                category = SmsCategory.ATTENDANCE.name,
                isAudible = true,
                isRead = false,
                createdAtMillis = now
            )
        }
        dao.insertSmsAlerts(alerts)
        return alerts.size
    }

    suspend fun payFeeAndGenerateBillSms(fee: FeeItemEntity) {
        val now = System.currentTimeMillis()
        val updated = fee.copy(
            paidAmount = fee.amount,
            status = FeeStatus.PAID.name,
            lastBilledAtMillis = now
        )
        dao.updateFee(updated)
        dao.insertSmsAlert(
            SmsAlertEntity(
                title = "Bill Receipt SMS: ${fee.feeTitle}",
                message = "Payment Received! Rs ${fee.amount.toInt()} paid for ${fee.feeTitle} (${fee.studentName}, Roll ${fee.rollNumber}, ${fee.className}). Status: PAID.",
                recipientName = "Gulab Ansari",
                recipientPhone = "",
                category = SmsCategory.BILL.name,
                isAudible = true,
                isRead = false,
                createdAtMillis = now
            )
        )
    }

    suspend fun sendBillReminderSms(fee: FeeItemEntity) {
        val now = System.currentTimeMillis()
        val dueAmount = (fee.amount - fee.paidAmount).coerceAtLeast(0.0)
        dao.updateFee(fee.copy(lastBilledAtMillis = now))
        dao.insertSmsAlert(
            SmsAlertEntity(
                title = "Bill Reminder SMS: ${fee.feeTitle}",
                message = "Fee Bill Alert: Rs ${dueAmount.toInt()} is pending for ${fee.feeTitle} of ${fee.studentName} (${fee.className}, Roll ${fee.rollNumber}). Due Date: ${fee.dueDate}.",
                recipientName = "Gulab Ansari",
                recipientPhone = "",
                category = SmsCategory.BILL.name,
                isAudible = true,
                isRead = false,
                createdAtMillis = now
            )
        )
    }

    suspend fun postNoticeAndSms(
        title: String,
        description: String,
        category: NoticeCategory,
        effectiveDate: String,
        postedBy: String
    ) {
        val now = System.currentTimeMillis()
        dao.insertNotice(
            NoticeEntity(
                title = title.trim(),
                description = description.trim(),
                category = category.name,
                postedBy = postedBy,
                effectiveDate = effectiveDate.trim().ifEmpty { "Today" },
                createdAtMillis = now
            )
        )
        dao.insertSmsAlert(
            SmsAlertEntity(
                title = "${category.label} Notice SMS: ${title.trim()}",
                message = "School Notice (${effectiveDate.trim().ifEmpty { "Today" }}): ${title.trim()} — ${description.trim()}",
                recipientName = "All Parents & Students",
                recipientPhone = "",
                category = SmsCategory.NOTICE.name,
                isAudible = true,
                isRead = false,
                createdAtMillis = now
            )
        )
    }

    suspend fun submitComplaint(
        senderRole: String,
        senderName: String,
        targetStudentName: String,
        category: String,
        message: String,
        voiceFilePath: String?,
        recipientPhone: String
    ) {
        val now = System.currentTimeMillis()
        val cid = PcSync.newClientId()
        PcSync.enqueue(
            JSONObject().put("type", "complaint").put("clientId", cid)
                .put("targetName", targetStudentName).put("category", category)
                .put("message", message.trim().ifEmpty { "(Voice message - see school office)" })
        )
        dao.insertComplaint(
            ComplaintEntity(
                clientId = cid,
                remoteId = "app-$cid",
                senderRole = senderRole,
                senderName = senderName,
                targetStudentName = targetStudentName,
                category = category,
                message = message.trim(),
                voiceFilePath = voiceFilePath,
                smsDispatched = true,
                status = "Open",
                createdAtMillis = now
            )
        )
        val hasVoice = !voiceFilePath.isNullOrBlank()
        dao.insertSmsAlert(
            SmsAlertEntity(
                title = if (hasVoice) "Voice + Text SMS ($category)" else "Complaint SMS ($category)",
                message = "$senderName regarding $targetStudentName: ${message.trim().ifEmpty { "Voice message attached. Tap Play Voice to listen." }}",
                recipientName = if (senderRole == "TEACHER") "Parent of $targetStudentName" else "Class Teacher",
                recipientPhone = recipientPhone,
                category = if (hasVoice) SmsCategory.VOICE.name else SmsCategory.COMPLAINT.name,
                audioFilePath = voiceFilePath,
                isAudible = true,
                isRead = false,
                createdAtMillis = now
            )
        )
    }

    suspend fun replyToComplaint(
        complaint: ComplaintEntity,
        replyText: String,
        replyVoicePath: String?,
        replierName: String
    ) {
        val now = System.currentTimeMillis()
        if (complaint.remoteId.isNotBlank()) {
            PcSync.enqueue(
                JSONObject().put("type", "complaintReply")
                    .put("complaintId", complaint.remoteId).put("replyText", replyText.trim().ifEmpty { "Replied by voice message" })
            )
        }
        dao.updateComplaint(
            complaint.copy(
                replyMessage = replyText.trim(),
                replyVoicePath = replyVoicePath,
                status = "Resolved"
            )
        )
        dao.insertSmsAlert(
            SmsAlertEntity(
                title = "Teacher Reply SMS: ${complaint.category}",
                message = "$replierName replied on '${complaint.category}': ${replyText.trim().ifEmpty { "Voice SMS reply attached." }}",
                recipientName = complaint.senderName,
                recipientPhone = "",
                category = if (!replyVoicePath.isNullOrBlank()) SmsCategory.VOICE.name else SmsCategory.COMPLAINT.name,
                audioFilePath = replyVoicePath,
                isAudible = true,
                isRead = false,
                createdAtMillis = now
            )
        )
    }

    suspend fun submitLeave(
        teacherName: String,
        leaveType: String,
        startDate: String,
        endDate: String,
        substituteTeacher: String,
        reason: String
    ) {
        val cid = PcSync.newClientId()
        PcSync.enqueue(
            JSONObject().put("type", "leave").put("clientId", cid).put("title", leaveType)
                .put("startDate", startDate.trim()).put("endDate", endDate.trim())
                .put("substitute", substituteTeacher.trim()).put("detail", reason.trim())
        )
        dao.insertLeave(
            TeacherLeaveEntity(
                clientId = cid,
                teacherName = teacherName,
                leaveType = leaveType,
                startDate = startDate.trim(),
                endDate = endDate.trim(),
                substituteTeacher = substituteTeacher.trim().ifEmpty { "Auto Assign" },
                reason = reason.trim(),
                status = "Pending",
                submittedAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun cancelLeave(id: Int) {
        val cid = dao.getLeaveClientId(id)
        dao.deleteLeaveById(id)
        if (!cid.isNullOrBlank()) {
            PcSync.enqueue(JSONObject().put("type", "cancelRequest").put("clientId", cid))
        }
    }

    suspend fun submitSalaryInquiry(
        teacherName: String,
        monthLabel: String,
        topic: String,
        message: String
    ) {
        val cid = PcSync.newClientId()
        PcSync.enqueue(
            JSONObject().put("type", "salary").put("clientId", cid)
                .put("title", "$monthLabel - $topic").put("detail", message.trim())
        )
        dao.insertSalaryInquiry(
            SalaryInquiryEntity(
                clientId = cid,
                teacherName = teacherName,
                monthLabel = monthLabel,
                basicPay = PcSync.basicPay,
                allowances = PcSync.allowances,
                deductions = 0.0,
                inquiryTopic = topic,
                inquiryMessage = message.trim(),
                responseNote = null,
                status = "In Review",
                createdAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun addExamSchedule(
        subject: String,
        className: String,
        examDateLabel: String,
        hoursFromNow: Int,
        durationHours: Int,
        roomNumber: String,
        invigilator: String,
        syllabus: String
    ) {
        val now = System.currentTimeMillis()
        val start = now + hoursFromNow * 3_600_000L
        val end = start + durationHours.coerceAtLeast(1) * 3_600_000L
        dao.insertExam(
            ExamScheduleEntity(
                subject = subject.trim(),
                className = className,
                examDateLabel = examDateLabel.trim(),
                startMillis = start,
                endMillis = end,
                roomNumber = roomNumber.trim(),
                invigilator = invigilator.trim(),
                syllabus = syllabus.trim()
            )
        )
    }

    suspend fun markSmsAsRead(id: Int) = dao.markSmsRead(id)
}
