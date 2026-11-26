package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AttendanceStatus(val label: String) {
    PRESENT("Present"),
    ABSENT("Absent"),
    LATE("Late"),
    EXCUSED("Excused")
}

enum class SmsCategory(val label: String) {
    BILL("Bill SMS"),
    ATTENDANCE("Attendance"),
    NOTICE("School Notice"),
    COMPLAINT("Complaint"),
    VOICE("Voice SMS")
}

enum class NoticeCategory(val label: String) {
    HOLIDAY("Holiday"),
    ACADEMIC("Academic"),
    EVENT("Event"),
    URGENT("Urgent")
}

enum class FeeStatus(val label: String) {
    DUE("Due"),
    PARTIAL("Partial"),
    PAID("Paid")
}

@Entity(tableName = "students")
data class StudentRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val registrationNo: String,
    val rollNumber: String,
    val className: String,
    val parentName: String,
    val parentPhone: String,
    val attendanceStatus: String = AttendanceStatus.PRESENT.name,
    val attendanceDate: String,
    val lastUpdatedMillis: Long = System.currentTimeMillis(),
    val remoteId: String = ""
)

@Entity(tableName = "sms_alerts")
data class SmsAlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val message: String,
    val recipientName: String,
    val recipientPhone: String,
    val category: String,
    val audioFilePath: String? = null,
    val isAudible: Boolean = true,
    val isRead: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "fee_items")
data class FeeItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentName: String,
    val rollNumber: String,
    val className: String,
    val feeTitle: String,
    val amount: Double,
    val paidAmount: Double,
    val dueDate: String,
    val status: String,
    val lastBilledAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "notices")
data class NoticeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val category: String,
    val postedBy: String,
    val effectiveDate: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "complaints")
data class ComplaintEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val senderRole: String, // PARENT or TEACHER
    val senderName: String,
    val targetStudentName: String,
    val category: String,
    val message: String,
    val voiceFilePath: String? = null,
    val smsDispatched: Boolean = true,
    val replyMessage: String? = null,
    val replyVoicePath: String? = null,
    val status: String = "Open",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val clientId: String = "",
    val remoteId: String = ""
)

@Entity(tableName = "exam_schedules")
data class ExamScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subject: String,
    val className: String,
    val examDateLabel: String,
    val startMillis: Long,
    val endMillis: Long,
    val roomNumber: String,
    val invigilator: String,
    val syllabus: String
)

@Entity(tableName = "teacher_leaves")
data class TeacherLeaveEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val teacherName: String,
    val leaveType: String,
    val startDate: String,
    val endDate: String,
    val substituteTeacher: String,
    val reason: String,
    val status: String = "Pending",
    val submittedAtMillis: Long = System.currentTimeMillis(),
    val clientId: String = ""
)

@Entity(tableName = "salary_inquiries")
data class SalaryInquiryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val teacherName: String,
    val monthLabel: String,
    val basicPay: Double,
    val allowances: Double,
    val deductions: Double,
    val inquiryTopic: String,
    val inquiryMessage: String,
    val responseNote: String? = null,
    val status: String = "In Review",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val clientId: String = ""
)
