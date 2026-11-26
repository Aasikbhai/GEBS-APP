package com.example.audio

import java.util.Locale

enum class VoiceLanguageMode(
    val shortBadge: String,
    val displayName: String,
    val nativeLabel: String,
    val bcp47Tag: String
) {
    AUTO("AUTO", "Auto Detect", "Auto (EN / हि / ने)", "auto"),
    ENGLISH("EN", "English", "English", "en-IN"),
    HINDI("HI", "Hindi", "हिन्दी", "hi-IN"),
    NEPALI("NE", "Nepali", "नेपाली", "ne-NP")
}

data class LocalizedAnnouncement(
    val resolvedLanguage: VoiceLanguageMode,
    val displayTitle: String,
    val displayBody: String,
    val spokenScript: String
)

object AnnouncementVoiceFormatter {

    private val nepaliMarkers = listOf(
        "छ", "छैन", "हुनुहुन्छ", "तपाईं", "तपाईंको", "विद्यालय", "भोलि",
        "रुपैयाँ", "गर्नुहोस्", "बिदा", "हाजिरी", "छोरा", "छोरी", "लागि", "रहेको"
    )

    private val hindiMarkers = listOf(
        "है", "हैं", "हूँ", "आपका", "रुपये", "छुट्टी", "अवकाश", "विद्यालय",
        "उपस्थिति", "कृपया", "करें", "बकाया", "छात्र", "बच्चा"
    )

    fun detectLanguage(text: String): VoiceLanguageMode {
        val hasDevanagari = text.any { it in '\u0900'..'\u097F' }
        if (hasDevanagari) {
            val neScore = nepaliMarkers.count { text.contains(it) }
            val hiScore = hindiMarkers.count { text.contains(it) }
            return if (neScore > hiScore) VoiceLanguageMode.NEPALI else VoiceLanguageMode.HINDI
        }

        val lower = text.lowercase(Locale.US)
        val romanNepaliHints = listOf("bida", "hajiri", "tapai", "chha", "bholi", "nepali")
        val romanHindiHints = listOf("chutti", "chhutti", "aapka", "rupaya", "bachcha", "hindi")
        if (romanNepaliHints.any { lower.contains(it) }) return VoiceLanguageMode.NEPALI
        if (romanHindiHints.any { lower.contains(it) }) return VoiceLanguageMode.HINDI
        return VoiceLanguageMode.ENGLISH
    }

    fun localize(
        title: String,
        body: String,
        mode: VoiceLanguageMode
    ): LocalizedAnnouncement {
        val target = if (mode == VoiceLanguageMode.AUTO) {
            detectLanguage("$title $body")
        } else {
            mode
        }

        return when (target) {
            VoiceLanguageMode.ENGLISH -> {
                val cleanTitle = cleanEnglishForSpeech(title)
                val cleanBody = cleanEnglishForSpeech(body)
                LocalizedAnnouncement(
                    resolvedLanguage = VoiceLanguageMode.ENGLISH,
                    displayTitle = title,
                    displayBody = body,
                    spokenScript = "$cleanTitle. $cleanBody"
                )
            }
            VoiceLanguageMode.HINDI -> toHindiAnnouncement(title, body)
            VoiceLanguageMode.NEPALI -> toNepaliAnnouncement(title, body)
            VoiceLanguageMode.AUTO -> LocalizedAnnouncement(
                resolvedLanguage = VoiceLanguageMode.ENGLISH,
                displayTitle = title,
                displayBody = body,
                spokenScript = cleanEnglishForSpeech("$title. $body")
            )
        }
    }

    fun sampleGreeting(mode: VoiceLanguageMode): LocalizedAnnouncement {
        return when (mode) {
            VoiceLanguageMode.HINDI -> LocalizedAnnouncement(
                resolvedLanguage = VoiceLanguageMode.HINDI,
                displayTitle = "स्पष्ट आवाज़ परीक्षण",
                displayBody = "नमस्ते! कैंपस पल्स में आपका स्वागत है। स्कूल सूचना, फीस बिल और उपस्थिति संदेश अब स्पष्ट हिन्दी में सुनें।",
                spokenScript = "नमस्ते! कैंपस पल्स में आपका स्वागत है। स्कूल सूचना, फीस बिल और उपस्थिति संदेश अब स्पष्ट हिन्दी में सुनें।"
            )
            VoiceLanguageMode.NEPALI -> LocalizedAnnouncement(
                resolvedLanguage = VoiceLanguageMode.NEPALI,
                displayTitle = "स्पष्ट आवाज परीक्षण",
                displayBody = "नमस्ते! क्याम्पस पल्समा स्वागत छ। विद्यालय सूचना, शुल्क बिल र हाजिरी सन्देश अब स्पष्ट नेपालीमा सुन्नुहोस्।",
                spokenScript = "नमस्ते! क्याम्पस पल्समा स्वागत छ। विद्यालय सूचना, शुल्क बिल र हाजिरी सन्देश अब स्पष्ट नेपालीमा सुन्नुहोस्।"
            )
            else -> LocalizedAnnouncement(
                resolvedLanguage = VoiceLanguageMode.ENGLISH,
                displayTitle = "Clear Voice Check",
                displayBody = "Hello! Welcome to CampusPulse. School notices, fee bills, and attendance alerts are now set to crystal clear voice.",
                spokenScript = "Hello! Welcome to CampusPulse. School notices, fee bills, and attendance alerts are now set to crystal clear voice."
            )
        }
    }

    private fun cleanEnglishForSpeech(text: String): String {
        return text
            .replace("•", ", ")
            .replace("—", ", ")
            .replace(Regex("""\bRs\.?\s*""", RegexOption.IGNORE_CASE), "Rupees ")
            .replace("₹", "Rupees ")
            .replace("PTM", "Parent Teacher Meeting")
            .replace("SMS", "S M S")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun toHindiAnnouncement(title: String, body: String): LocalizedAnnouncement {
        if (title.any { it in '\u0900'..'\u097F' } || body.any { it in '\u0900'..'\u097F' }) {
            val clean = cleanDevanagariForSpeech("$title। $body", isNepali = false)
            return LocalizedAnnouncement(VoiceLanguageMode.HINDI, title, body, clean)
        }

        val combined = "$title $body".lowercase(Locale.US)
        val amountMatch = Regex("""(?:rs\.?|₹)\s*([\d,]+)""", RegexOption.IGNORE_CASE).find("$title $body")
        val amount = amountMatch?.groupValues?.getOrNull(1)?.replace(",", "")
        val studentName = extractStudentName("$title $body")

        return when {
            combined.contains("absent") -> {
                val hTitle = "उपस्थिति सूचना: अनुपस्थित"
                val hBody = "प्रिय अभिभावक, आपका बच्चा $studentName आज विद्यालय में अनुपस्थित है। यदि यह जानकारी सही नहीं है तो कृपया कक्षा शिक्षक को सूचित करें।"
                LocalizedAnnouncement(VoiceLanguageMode.HINDI, hTitle, hBody, "$hTitle। $hBody")
            }
            combined.contains("late") -> {
                val hTitle = "उपस्थिति सूचना: विलंब से आगमन"
                val hBody = "प्रिय अभिभावक, आपका बच्चा $studentName आज विद्यालय में देर से पहुंचा है। कृपया समय पर भेजने का ध्यान रखें।"
                LocalizedAnnouncement(VoiceLanguageMode.HINDI, hTitle, hBody, "$hTitle। $hBody")
            }
            combined.contains("present") -> {
                val hTitle = "उपस्थिति सूचना: उपस्थित"
                val hBody = "प्रिय अभिभावक, आपका बच्चा $studentName आज समय पर विद्यालय पहुंचा है और उपस्थित दर्ज किया गया है।"
                LocalizedAnnouncement(VoiceLanguageMode.HINDI, hTitle, hBody, "$hTitle। $hBody")
            }
            combined.contains("excused") -> {
                val hTitle = "उपस्थिति सूचना: स्वीकृत अवकाश"
                val hBody = "प्रिय अभिभावक, $studentName की आज की अनुपस्थिति विद्यालय द्वारा स्वीकृत की गई है।"
                LocalizedAnnouncement(VoiceLanguageMode.HINDI, hTitle, hBody, "$hTitle। $hBody")
            }
            combined.contains("payment received") || combined.contains("receipt") || combined.contains("paid") -> {
                val amtPart = if (amount != null) "$amount रुपये की " else ""
                val hTitle = "फीस भुगतान रसीद संदेश"
                val hBody = "भुगतान प्राप्त हुआ! $studentName की ${amtPart}फीस सफलतापूर्वक जमा हो गई है।"
                LocalizedAnnouncement(VoiceLanguageMode.HINDI, hTitle, hBody, "$hTitle। $hBody")
            }
            combined.contains("bill") || combined.contains("tuition") || combined.contains("fee") || combined.contains("dues") -> {
                val amtPart = if (amount != null) "$amount रुपये " else "बकाया "
                val hTitle = "फीस बिल सूचना"
                val hBody = "प्रिय अभिभावक, $studentName की स्कूल फीस की ${amtPart}राशि देय है। कृपया समय पर भुगतान करें।"
                LocalizedAnnouncement(VoiceLanguageMode.HINDI, hTitle, hBody, "$hTitle। $hBody")
            }
            combined.contains("holiday") -> {
                val hTitle = "स्कूल अवकाश सूचना"
                val hBody = "ध्यान दें, विद्यालय अवकाश की घोषणा की गई है। ${translateSchoolPhrasesToHindi(body)}"
                LocalizedAnnouncement(VoiceLanguageMode.HINDI, hTitle, hBody, "$hTitle। $hBody")
            }
            combined.contains("ptm") || combined.contains("parent-teacher") -> {
                val hTitle = "अभिभावक-शिक्षक बैठक सूचना"
                val hBody = "प्रिय अभिभावक, मध्य-सत्र प्रगति रिपोर्ट के लिए अभिभावक-शिक्षक बैठक आयोजित की जा रही है। कृपया समय पर पधारें।"
                LocalizedAnnouncement(VoiceLanguageMode.HINDI, hTitle, hBody, "$hTitle। $hBody")
            }
            combined.contains("uniform") || combined.contains("assembly") -> {
                val hTitle = "स्कूल ड्रेस और प्रार्थना सभा सूचना"
                val hBody = "सभी छात्रों को सूचित किया जाता है कि नियमित स्कूल यूनिफॉर्म पहनें और सुबह समय पर प्रार्थना सभा में उपस्थित रहें।"
                LocalizedAnnouncement(VoiceLanguageMode.HINDI, hTitle, hBody, "$hTitle। $hBody")
            }
            combined.contains("complaint") || combined.contains("replied") || combined.contains("voice") -> {
                val hTitle = "शिकायत एवं संदेश सूचना"
                val hBody = "विद्यालय संवाद संदेश: ${translateSchoolPhrasesToHindi(body)}"
                LocalizedAnnouncement(VoiceLanguageMode.HINDI, hTitle, hBody, "$hTitle। $hBody")
            }
            else -> {
                val hTitle = "स्कूल सूचना"
                val hBody = "${translateSchoolPhrasesToHindi(title)}। ${translateSchoolPhrasesToHindi(body)}"
                LocalizedAnnouncement(VoiceLanguageMode.HINDI, hTitle, hBody, "$hTitle। $hBody")
            }
        }
    }

    private fun toNepaliAnnouncement(title: String, body: String): LocalizedAnnouncement {
        if (title.any { it in '\u0900'..'\u097F' } || body.any { it in '\u0900'..'\u097F' }) {
            val clean = cleanDevanagariForSpeech("$title। $body", isNepali = true)
            return LocalizedAnnouncement(VoiceLanguageMode.NEPALI, title, body, clean)
        }

        val combined = "$title $body".lowercase(Locale.US)
        val amountMatch = Regex("""(?:rs\.?|₹)\s*([\d,]+)""", RegexOption.IGNORE_CASE).find("$title $body")
        val amount = amountMatch?.groupValues?.getOrNull(1)?.replace(",", "")
        val studentName = extractStudentName("$title $body")

        return when {
            combined.contains("absent") -> {
                val nTitle = "हाजिरी सूचना: अनुपस्थित"
                val nBody = "प्रिय अभिभावक, तपाईंको छोरा $studentName आज विद्यालयमा अनुपस्थित हुनुहुन्छ। यदि जानकारी फरक छ भने कृपया कक्षा शिक्षकलाई खबर गर्नुहोस्।"
                LocalizedAnnouncement(VoiceLanguageMode.NEPALI, nTitle, nBody, "$nTitle। $nBody")
            }
            combined.contains("late") -> {
                val nTitle = "हाजिरी सूचना: ढिलो आगमन"
                val nBody = "प्रिय अभिभावक, तपाईंको छोरा $studentName आज विद्यालयमा ढिलो आइपुग्नुभएको छ। कृपया समयमै पठाउनुहोस्।"
                LocalizedAnnouncement(VoiceLanguageMode.NEPALI, nTitle, nBody, "$nTitle। $nBody")
            }
            combined.contains("present") -> {
                val nTitle = "हाजिरी सूचना: उपस्थित"
                val nBody = "प्रिय अभिभावक, तपाईंको छोरा $studentName आज समयमै विद्यालय आउनुभएको छ र उपस्थित दर्ता गरिएको छ।"
                LocalizedAnnouncement(VoiceLanguageMode.NEPALI, nTitle, nBody, "$nTitle। $nBody")
            }
            combined.contains("excused") -> {
                val nTitle = "हाजिरी सूचना: स्वीकृत बिदा"
                val nBody = "प्रिय अभिभावक, $studentName को आजको अनुपस्थिति विद्यालयबाट स्वीकृत गरिएको छ।"
                LocalizedAnnouncement(VoiceLanguageMode.NEPALI, nTitle, nBody, "$nTitle। $nBody")
            }
            combined.contains("payment received") || combined.contains("receipt") || combined.contains("paid") -> {
                val amtPart = if (amount != null) "$amount रुपैयाँको " else ""
                val nTitle = "शुल्क भुक्तानी रसिद सन्देश"
                val nBody = "भुक्तानी प्राप्त भयो! $studentName को ${amtPart}शुल्क सफलतापूर्वक बुझाइएको छ।"
                LocalizedAnnouncement(VoiceLanguageMode.NEPALI, nTitle, nBody, "$nTitle। $nBody")
            }
            combined.contains("bill") || combined.contains("tuition") || combined.contains("fee") || combined.contains("dues") -> {
                val amtPart = if (amount != null) "$amount रुपैयाँ " else "बाँकी "
                val nTitle = "शुल्क बिल सूचना"
                val nBody = "प्रिय अभिभावक, $studentName को विद्यालय शुल्कको ${amtPart}रकम बुझाउन बाँकी छ। कृपया समयमै भुक्तानी गर्नुहोस्।"
                LocalizedAnnouncement(VoiceLanguageMode.NEPALI, nTitle, nBody, "$nTitle। $nBody")
            }
            combined.contains("holiday") -> {
                val nTitle = "विद्यालय बिदा सूचना"
                val nBody = "ध्यान दिनुहोस्, विद्यालय बिदाको घोषणा गरिएको छ। ${translateSchoolPhrasesToNepali(body)}"
                LocalizedAnnouncement(VoiceLanguageMode.NEPALI, nTitle, nBody, "$nTitle। $nBody")
            }
            combined.contains("ptm") || combined.contains("parent-teacher") -> {
                val nTitle = "अभिभावक-शिक्षक भेटघाट सूचना"
                val nBody = "प्रिय अभिभावक, प्रगति विवरण समीक्षाका लागि अभिभावक-शिक्षक बैठक आयोजना हुँदैछ। कृपया समयमै उपस्थित हुनुहोस्।"
                LocalizedAnnouncement(VoiceLanguageMode.NEPALI, nTitle, nBody, "$nTitle। $nBody")
            }
            combined.contains("uniform") || combined.contains("assembly") -> {
                val nTitle = "पोशाक र बिहानी सभा सूचना"
                val nBody = "सबै विद्यार्थीहरूलाई नियमित विद्यालय पोशाक लगाएर समयमै बिहानी सभामा उपस्थित हुन अनुरोध गरिन्छ।"
                LocalizedAnnouncement(VoiceLanguageMode.NEPALI, nTitle, nBody, "$nTitle। $nBody")
            }
            combined.contains("complaint") || combined.contains("replied") || combined.contains("voice") -> {
                val nTitle = "गुनासो तथा सन्देश सूचना"
                val nBody = "विद्यालय सन्देश: ${translateSchoolPhrasesToNepali(body)}"
                LocalizedAnnouncement(VoiceLanguageMode.NEPALI, nTitle, nBody, "$nTitle। $nBody")
            }
            else -> {
                val nTitle = "विद्यालय सूचना"
                val nBody = "${translateSchoolPhrasesToNepali(title)}। ${translateSchoolPhrasesToNepali(body)}"
                LocalizedAnnouncement(VoiceLanguageMode.NEPALI, nTitle, nBody, "$nTitle। $nBody")
            }
        }
    }

    private fun extractStudentName(text: String): String {
        val known = listOf(
            "Aasik Ansari", "Zoya Khan", "Rohan Verma", "Fatima Sheikh",
            "Kabir Patel", "Ananya Das", "Arjun Sharma", "Nadia Siddiqui"
        )
        return known.firstOrNull { text.contains(it, ignoreCase = true) } ?: "the student"
    }

    private fun cleanDevanagariForSpeech(text: String, isNepali: Boolean): String {
        val rupeeWord = if (isNepali) "रुपैयाँ " else "रुपये "
        return text
            .replace("•", "। ")
            .replace("—", ", ")
            .replace(Regex("""\bRs\.?\s*""", RegexOption.IGNORE_CASE), rupeeWord)
            .replace("₹", rupeeWord)
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun translateSchoolPhrasesToHindi(input: String): String {
        return input
            .replace("School will observe a holiday tomorrow", "कल विद्यालय में अवकाश रहेगा", ignoreCase = true)
            .replace("School is closed today", "आज विद्यालय बंद रहेगा", ignoreCase = true)
            .replace("Holiday Today", "आज अवकाश है", ignoreCase = true)
            .replace("Holiday", "अवकाश", ignoreCase = true)
            .replace("Tomorrow", "कल", ignoreCase = true)
            .replace("Today", "आज", ignoreCase = true)
            .replace("Classes resume at", "कक्षाएं शुरू होंगी", ignoreCase = true)
            .replace("Annual Cultural Foundation Day", "वार्षिक सांस्कृतिक स्थापना दिवस", ignoreCase = true)
            .replace("Class Teacher", "कक्षा शिक्षक", ignoreCase = true)
            .replace("Dear Parent,", "प्रिय अभिभावक,", ignoreCase = true)
            .replace("Rs", "रुपये", ignoreCase = true)
    }

    private fun translateSchoolPhrasesToNepali(input: String): String {
        return input
            .replace("School will observe a holiday tomorrow", "भोलि विद्यालयमा बिदा रहनेछ", ignoreCase = true)
            .replace("School is closed today", "आज विद्यालय बन्द रहनेछ", ignoreCase = true)
            .replace("Holiday Today", "आज बिदा छ", ignoreCase = true)
            .replace("Holiday", "बिदा", ignoreCase = true)
            .replace("Tomorrow", "भोलि", ignoreCase = true)
            .replace("Today", "आज", ignoreCase = true)
            .replace("Classes resume at", "कक्षा सुरु हुने समय", ignoreCase = true)
            .replace("Annual Cultural Foundation Day", "वार्षिक सांस्कृतिक स्थापना दिवस", ignoreCase = true)
            .replace("Class Teacher", "कक्षा शिक्षक", ignoreCase = true)
            .replace("Dear Parent,", "प्रिय अभिभावक,", ignoreCase = true)
            .replace("Rs", "रुपैयाँ", ignoreCase = true)
    }
}
