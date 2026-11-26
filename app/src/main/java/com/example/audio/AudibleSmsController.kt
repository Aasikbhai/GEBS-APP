package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import java.util.UUID

class AudibleSmsController(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var activeRecordingFile: File? = null
    private var timerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var focusRequest: AudioFocusRequest? = null

    private val _playingTextId = MutableStateFlow<String?>(null)
    val playingTextId: StateFlow<String?> = _playingTextId.asStateFlow()

    private val _nowPlayingTitle = MutableStateFlow<String?>(null)
    val nowPlayingTitle: StateFlow<String?> = _nowPlayingTitle.asStateFlow()

    private val _nowPlayingText = MutableStateFlow<String?>(null)
    val nowPlayingText: StateFlow<String?> = _nowPlayingText.asStateFlow()

    private val _nowPlayingLang = MutableStateFlow(VoiceLanguageMode.ENGLISH)
    val nowPlayingLang: StateFlow<VoiceLanguageMode> = _nowPlayingLang.asStateFlow()

    private val _voiceLanguageMode = MutableStateFlow(VoiceLanguageMode.AUTO)
    val voiceLanguageMode: StateFlow<VoiceLanguageMode> = _voiceLanguageMode.asStateFlow()

    private val _speechRate = MutableStateFlow(0.95f)
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val _chimeEnabled = MutableStateFlow(true)
    val chimeEnabled: StateFlow<Boolean> = _chimeEnabled.asStateFlow()

    private val _activeVoiceQualityLabel = MutableStateFlow("HD Clear Voice")
    val activeVoiceQualityLabel: StateFlow<String> = _activeVoiceQualityLabel.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingSeconds = MutableStateFlow(0)
    val recordingSeconds: StateFlow<Int> = _recordingSeconds.asStateFlow()

    private val _playingVoicePath = MutableStateFlow<String?>(null)
    val playingVoicePath: StateFlow<String?> = _playingVoicePath.asStateFlow()

    private val _audioStatusMessage = MutableStateFlow<String?>(null)
    val audioStatusMessage: StateFlow<String?> = _audioStatusMessage.asStateFlow()

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true
                tts?.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                applyBestVoiceForLanguage(VoiceLanguageMode.ENGLISH)
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _playingTextId.value = utteranceId
                    }

                    override fun onDone(utteranceId: String?) {
                        if (_playingTextId.value == utteranceId) {
                            clearNowPlayingState()
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        if (_playingTextId.value == utteranceId) {
                            clearNowPlayingState()
                        }
                    }
                })
            }
        }
    }

    fun setVoiceLanguageMode(mode: VoiceLanguageMode) {
        _voiceLanguageMode.value = mode
        if (mode != VoiceLanguageMode.AUTO) {
            applyBestVoiceForLanguage(mode)
        } else {
            _activeVoiceQualityLabel.value = "Auto Detect • HD Clear Voice"
        }
    }

    fun setSpeechRate(rate: Float) {
        _speechRate.value = rate
        tts?.setSpeechRate(rate)
    }

    fun toggleSpeechRate() {
        val next = when {
            _speechRate.value < 0.90f -> 0.95f
            _speechRate.value < 1.05f -> 1.12f
            else -> 0.85f
        }
        setSpeechRate(next)
    }

    fun toggleChime() {
        _chimeEnabled.value = !_chimeEnabled.value
    }

    fun playAudibleSms(
        utteranceId: String,
        spokenText: String,
        title: String = "Announcement",
        withChime: Boolean = _chimeEnabled.value
    ) {
        if (_playingTextId.value == utteranceId) {
            stopAudibleSms()
            return
        }
        stopVoicePlayback()
        requestSpeechAudioFocus()

        val localized = AnnouncementVoiceFormatter.localize(
            title = title,
            body = spokenText,
            mode = _voiceLanguageMode.value
        )

        if (withChime) {
            playAlertChime()
        }

        _nowPlayingTitle.value = localized.displayTitle
        _nowPlayingText.value = localized.displayBody
        _nowPlayingLang.value = localized.resolvedLanguage

        if (!ttsReady) {
            _playingTextId.value = utteranceId
            _audioStatusMessage.value = "Playing ${localized.resolvedLanguage.displayName} alert..."
            scope.launch {
                delay(2400)
                if (_playingTextId.value == utteranceId) {
                    clearNowPlayingState()
                }
            }
            return
        }

        applyBestVoiceForLanguage(localized.resolvedLanguage)
        tts?.setPitch(1.02f)
        tts?.setSpeechRate(_speechRate.value)
        _playingTextId.value = utteranceId

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }
        tts?.speak(localized.spokenScript, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun playSampleVoice(mode: VoiceLanguageMode = _voiceLanguageMode.value) {
        val target = if (mode == VoiceLanguageMode.AUTO) VoiceLanguageMode.ENGLISH else mode
        val sample = AnnouncementVoiceFormatter.sampleGreeting(target)
        stopVoicePlayback()
        requestSpeechAudioFocus()

        _nowPlayingTitle.value = sample.displayTitle
        _nowPlayingText.value = sample.displayBody
        _nowPlayingLang.value = sample.resolvedLanguage

        if (!ttsReady) {
            _playingTextId.value = "voice_sample"
            scope.launch {
                delay(2000)
                if (_playingTextId.value == "voice_sample") clearNowPlayingState()
            }
            return
        }

        applyBestVoiceForLanguage(target)
        tts?.setPitch(1.02f)
        tts?.setSpeechRate(_speechRate.value)
        _playingTextId.value = "voice_sample"
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }
        tts?.speak(sample.spokenScript, TextToSpeech.QUEUE_FLUSH, params, "voice_sample")
    }

    fun stopAudibleSms() {
        tts?.stop()
        clearNowPlayingState()
    }

    private fun clearNowPlayingState() {
        _playingTextId.value = null
        _nowPlayingTitle.value = null
        _nowPlayingText.value = null
        abandonSpeechAudioFocus()
    }

    private fun applyBestVoiceForLanguage(lang: VoiceLanguageMode) {
        val engine = tts ?: return
        val candidateLocales: List<Locale> = when (lang) {
            VoiceLanguageMode.ENGLISH, VoiceLanguageMode.AUTO -> listOf(
                Locale.forLanguageTag("en-IN"),
                Locale.US,
                Locale.UK,
                Locale.ENGLISH
            )
            VoiceLanguageMode.HINDI -> listOf(
                Locale.forLanguageTag("hi-IN"),
                Locale.forLanguageTag("hi")
            )
            VoiceLanguageMode.NEPALI -> listOf(
                Locale.forLanguageTag("ne-NP"),
                Locale.forLanguageTag("ne"),
                Locale.forLanguageTag("hi-IN") // Clean Devanagari fallback if ne-NP pack absent
            )
        }

        val allVoices = runCatching { engine.voices?.toList().orEmpty() }.getOrDefault(emptyList())
        for (loc in candidateLocales) {
            val matching = allVoices.filter { v ->
                v.locale.language.equals(loc.language, ignoreCase = true) &&
                    (loc.country.isEmpty() || v.locale.country.equals(loc.country, ignoreCase = true)) &&
                    !v.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
            }
            val bestVoice = matching.maxWithOrNull(
                compareBy<Voice>(
                    { if (!it.isNetworkConnectionRequired) 1 else 0 },
                    { it.quality },
                    { -it.latency }
                )
            )
            if (bestVoice != null) {
                engine.voice = bestVoice
                val qualityTag = if (bestVoice.quality >= Voice.QUALITY_HIGH) "HD Clear" else "Clear"
                val langLabel = if (lang == VoiceLanguageMode.NEPALI && bestVoice.locale.language == "hi") {
                    "Nepali (Devanagari Voice)"
                } else {
                    lang.displayName
                }
                _activeVoiceQualityLabel.value = "$langLabel • $qualityTag Voice"
                return
            }

            val avail = engine.isLanguageAvailable(loc)
            if (avail >= TextToSpeech.LANG_AVAILABLE) {
                engine.language = loc
                _activeVoiceQualityLabel.value = "${lang.displayName} • Clear Voice"
                return
            }
        }
        engine.language = Locale.US
        _activeVoiceQualityLabel.value = "Standard Clear Voice"
    }

    private fun requestSpeechAudioFocus() {
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .build()
            focusRequest = req
            am.requestAudioFocus(req)
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        }
    }

    private fun abandonSpeechAudioFocus() {
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { am.abandonAudioFocusRequest(it) }
            focusRequest = null
        }
    }

    private fun playAlertChime() {
        runCatching {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 72)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 180)
            scope.launch {
                delay(280)
                runCatching { toneGen.release() }
            }
        }
    }

    fun startVoiceRecording(): Boolean {
        stopAudibleSms()
        stopVoicePlayback()
        if (_isRecording.value) return false

        val dir = File(context.filesDir, "voice_sms").apply { mkdirs() }
        val file = File(dir, "voice_${UUID.randomUUID()}.m4a")
        activeRecordingFile = file

        return try {
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            _isRecording.value = true
            _recordingSeconds.value = 0
            timerJob?.cancel()
            timerJob = scope.launch {
                while (_isRecording.value) {
                    delay(1000)
                    _recordingSeconds.value += 1
                    if (_recordingSeconds.value >= 60) {
                        stopVoiceRecording()
                    }
                }
            }
            true
        } catch (_: Exception) {
            runCatching { mediaRecorder?.release() }
            mediaRecorder = null
            activeRecordingFile = null
            _isRecording.value = false
            _audioStatusMessage.value = "Microphone unavailable on this device. You can still send an Audible Clear Voice SMS."
            false
        }
    }

    fun stopVoiceRecording(): String? {
        timerJob?.cancel()
        if (!_isRecording.value) return null
        _isRecording.value = false
        return try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null
            activeRecordingFile?.takeIf { it.exists() && it.length() > 0 }?.absolutePath
        } catch (_: Exception) {
            runCatching { mediaRecorder?.release() }
            mediaRecorder = null
            activeRecordingFile?.delete()
            null
        }
    }

    fun toggleVoicePlayback(filePath: String, fallbackSpokenText: String? = null) {
        if (_playingVoicePath.value == filePath) {
            stopVoicePlayback()
            return
        }
        stopAudibleSms()
        stopVoicePlayback()

        val file = File(filePath)
        if (!file.exists()) {
            if (!fallbackSpokenText.isNullOrBlank()) {
                playAudibleSms("voice_fallback_$filePath", fallbackSpokenText, title = "Voice SMS")
            } else {
                _audioStatusMessage.value = "Voice clip file could not be found."
            }
            return
        }

        try {
            requestSpeechAudioFocus()
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(file.absolutePath)
                prepare()
                setOnCompletionListener {
                    _playingVoicePath.value = null
                    abandonSpeechAudioFocus()
                    release()
                    mediaPlayer = null
                }
                start()
            }
            mediaPlayer = player
            _playingVoicePath.value = filePath
        } catch (_: Exception) {
            _playingVoicePath.value = null
            if (!fallbackSpokenText.isNullOrBlank()) {
                playAudibleSms("voice_fallback_$filePath", fallbackSpokenText, title = "Voice SMS")
            } else {
                _audioStatusMessage.value = "Unable to play voice recording."
            }
        }
    }

    fun stopVoicePlayback() {
        runCatching {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        }
        mediaPlayer = null
        _playingVoicePath.value = null
        abandonSpeechAudioFocus()
    }

    fun clearStatusMessage() {
        _audioStatusMessage.value = null
    }

    fun release() {
        timerJob?.cancel()
        stopAudibleSms()
        stopVoicePlayback()
        runCatching { mediaRecorder?.release() }
        mediaRecorder = null
        tts?.shutdown()
        tts = null
    }
}
