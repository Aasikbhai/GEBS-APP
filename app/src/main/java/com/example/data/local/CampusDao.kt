package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CampusDao {
    // Students & Attendance
    @Query("SELECT * FROM students ORDER BY className ASC, CAST(rollNumber AS INTEGER) ASC, rollNumber ASC")
    fun observeAllStudents(): Flow<List<StudentRecordEntity>>

    @Query("SELECT * FROM students")
    suspend fun getAllStudentsOnce(): List<StudentRecordEntity>

    @Query("SELECT COUNT(*) FROM students")
    suspend fun getStudentCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudents(students: List<StudentRecordEntity>)

    @Update
    suspend fun updateStudent(student: StudentRecordEntity)

    @Update
    suspend fun updateStudents(students: List<StudentRecordEntity>)

    // SMS Alerts
    @Query("SELECT * FROM sms_alerts ORDER BY createdAtMillis DESC")
    fun observeAllSmsAlerts(): Flow<List<SmsAlertEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSmsAlert(alert: SmsAlertEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSmsAlerts(alerts: List<SmsAlertEntity>)

    @Query("UPDATE sms_alerts SET isRead = 1 WHERE id = :id")
    suspend fun markSmsRead(id: Int)

    // Fees & Bill SMS
    @Query("SELECT * FROM fee_items ORDER BY id ASC")
    fun observeAllFees(): Flow<List<FeeItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFees(fees: List<FeeItemEntity>)

    @Update
    suspend fun updateFee(fee: FeeItemEntity)

    // Notices
    @Query("SELECT * FROM notices ORDER BY createdAtMillis DESC")
    fun observeAllNotices(): Flow<List<NoticeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotice(notice: NoticeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotices(notices: List<NoticeEntity>)

    // Complaints (Parent & Teacher)
    @Query("SELECT * FROM complaints ORDER BY createdAtMillis DESC")
    fun observeAllComplaints(): Flow<List<ComplaintEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComplaint(complaint: ComplaintEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComplaints(complaints: List<ComplaintEntity>)

    @Update
    suspend fun updateComplaint(complaint: ComplaintEntity)

    // Exam Schedules
    @Query("SELECT * FROM exam_schedules ORDER BY startMillis ASC")
    fun observeAllExams(): Flow<List<ExamScheduleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: ExamScheduleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExams(exams: List<ExamScheduleEntity>)

    // Teacher Leaves
    @Query("SELECT * FROM teacher_leaves ORDER BY submittedAtMillis DESC")
    fun observeAllLeaves(): Flow<List<TeacherLeaveEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeave(leave: TeacherLeaveEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeaves(leaves: List<TeacherLeaveEntity>)

    @Query("DELETE FROM teacher_leaves WHERE id = :id")
    suspend fun deleteLeaveById(id: Int)

    // Salary Inquiries
    @Query("SELECT * FROM salary_inquiries ORDER BY createdAtMillis DESC")
    fun observeAllSalaryInquiries(): Flow<List<SalaryInquiryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSalaryInquiry(inquiry: SalaryInquiryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSalaryInquiries(inquiries: List<SalaryInquiryEntity>)

    // --- Clear tables before a fresh copy from the school PC arrives ---
    @Query("SELECT * FROM complaints") suspend fun getAllComplaintsOnce(): List<ComplaintEntity>
    @Query("DELETE FROM students") suspend fun clearStudents()
    @Query("DELETE FROM fee_items") suspend fun clearFees()
    @Query("DELETE FROM notices") suspend fun clearNotices()
    @Query("DELETE FROM complaints") suspend fun clearComplaints()
    @Query("DELETE FROM exam_schedules") suspend fun clearExams()
    @Query("DELETE FROM teacher_leaves") suspend fun clearLeaves()
    @Query("DELETE FROM salary_inquiries") suspend fun clearSalaryInquiries()
    @Query("DELETE FROM sms_alerts") suspend fun clearSmsAlerts()
    @Query("SELECT clientId FROM teacher_leaves WHERE id = :id") suspend fun getLeaveClientId(id: Int): String?
}
