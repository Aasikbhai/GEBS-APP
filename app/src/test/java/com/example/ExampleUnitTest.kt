package com.example

import com.example.audio.AnnouncementVoiceFormatter
import com.example.audio.VoiceLanguageMode
import com.example.data.local.ExamScheduleEntity
import com.example.ui.screens.LiveExamState
import com.example.ui.screens.computeLiveExamStatus
import com.example.ui.screens.formatDurationCompact
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `computeLiveExamStatus detects live upcoming and completed exams`() {
        val now = 1_000_000_000L
        val liveExam = ExamScheduleEntity(
            subject = "Mathematics",
            className = "Class 8-A",
            examDateLabel = "Now",
            startMillis = now - 10_000L,
            endMillis = now + 60_000L,
            roomNumber = "Hall B-102",
            invigilator = "Md Khushi Akatar Ansari",
            syllabus = "Geometry"
        )
        val upcomingExam = liveExam.copy(startMillis = now + 120_000L, endMillis = now + 300_000L)
        val completedExam = liveExam.copy(startMillis = now - 300_000L, endMillis = now - 120_000L)

        assertEquals(LiveExamState.LIVE, computeLiveExamStatus(liveExam, now).state)
        assertEquals(LiveExamState.UPCOMING, computeLiveExamStatus(upcomingExam, now).state)
        assertEquals(LiveExamState.COMPLETED, computeLiveExamStatus(completedExam, now).state)
    }

    @Test
    fun `formatDurationCompact formats minutes hours and days cleanly`() {
        assertEquals("05m 30s", formatDurationCompact(330_000L))
        assertEquals("02h 01m 05s", formatDurationCompact(7_265_000L))
        assertEquals("1d 02h 00m", formatDurationCompact(93_600_000L))
    }

    @Test
    fun `detectLanguage identifies English Hindi and Nepali scripts`() {
        assertEquals(
            VoiceLanguageMode.ENGLISH,
            AnnouncementVoiceFormatter.detectLanguage("School will observe a holiday tomorrow.")
        )
        assertEquals(
            VoiceLanguageMode.HINDI,
            AnnouncementVoiceFormatter.detectLanguage("आज स्कूल में अवकाश है। कृपया ध्यान दें।")
        )
        assertEquals(
            VoiceLanguageMode.NEPALI,
            AnnouncementVoiceFormatter.detectLanguage("आज विद्यालयमा बिदा छ। कृपया भोलि आउनुहोस्।")
        )
    }

    @Test
    fun `localize translates English attendance alert into Hindi and Nepali`() {
        val title = "Attendance SMS"
        val body = "Your child Aasik Ansari is marked ABSENT today."

        val hindi = AnnouncementVoiceFormatter.localize(title, body, VoiceLanguageMode.HINDI)
        assertEquals(VoiceLanguageMode.HINDI, hindi.resolvedLanguage)
        assertTrue(hindi.spokenScript.contains("अनुपस्थित"))

        val nepali = AnnouncementVoiceFormatter.localize(title, body, VoiceLanguageMode.NEPALI)
        assertEquals(VoiceLanguageMode.NEPALI, nepali.resolvedLanguage)
        assertTrue(nepali.spokenScript.contains("अनुपस्थित"))
    }
}
