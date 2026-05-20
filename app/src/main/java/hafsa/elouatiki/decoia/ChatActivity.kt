package hafsa.elouatiki.decoia

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import hafsa.elouatiki.decoia.databinding.ActivityChatBinding
import kotlinx.coroutines.launch
import java.io.File

class ChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatBinding
    private lateinit var adapter: ChatAdapter
    private val groqService = GroqApiService()
    private val messages = mutableListOf<ChatMessage>()
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = ChatAdapter(messages)
        binding.rvMessages.layoutManager = LinearLayoutManager(this)
        binding.rvMessages.adapter = adapter

        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isNotEmpty()) sendMessage(text)
        }

        binding.btnMic.setOnClickListener {
            if (isRecording) stopRecording() else checkAndStartRecording()
        }

        binding.toolbar.setNavigationOnClickListener { finish() }
        if (messages.isEmpty()) addChatMessage("Bonjour ! Je suis DecoIA. Comment puis-je vous aider ?", false)
    }

    private fun checkAndStartRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 101)
        } else startRecording()
    }

    private fun startRecording() {
        try {
            val file = File(cacheDir, "audio.m4a")
            mediaRecorder = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(this) else MediaRecorder()).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            isRecording = true
            binding.btnMic.setImageResource(android.R.drawable.ic_media_pause)
            Toast.makeText(this, "Enregistrement...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) { Toast.makeText(this, "Erreur micro", Toast.LENGTH_SHORT).show() }
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false
            binding.btnMic.setImageResource(android.R.drawable.ic_btn_speak_now)
            transcribeAndSend(File(cacheDir, "audio.m4a"))
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun transcribeAndSend(file: File) {
        lifecycleScope.launch {
            binding.etMessage.setHint("Transcription...")
            val text = groqService.transcribeAudio(file)
            if (!text.isNullOrBlank()) sendMessage(text)
            binding.etMessage.setHint("Dites quelque chose...")
        }
    }

    private fun sendMessage(text: String) {
        addChatMessage(text, true)
        binding.etMessage.text.clear()
        lifecycleScope.launch {
            val response = groqService.getChatResponse(text)
            addChatMessage(response ?: "Désolé, une erreur est survenue.", false)
        }
    }

    private fun addChatMessage(text: String, isUser: Boolean) {
        adapter.addMessage(ChatMessage(text, isUser))
        binding.rvMessages.scrollToPosition(messages.size - 1)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.getOrNull(0) == PackageManager.PERMISSION_GRANTED) startRecording()
    }
}
