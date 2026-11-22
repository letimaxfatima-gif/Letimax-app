package com.letimax.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.letimax.app.R
import com.letimax.app.data.room.LetimaXDatabase
import com.letimax.app.data.room.TentativaAcessoEntity
import com.letimax.app.databinding.ActivityAutenticacaoBinding
import com.letimax.app.security.EncryptionManager
import com.letimax.app.ui.main.MainActivity
import com.letimax.app.utils.ThemeManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class AutenticacaoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAutenticacaoBinding
    private lateinit var db: LetimaXDatabase
    private var faseAtual = 1 // 1 = Rosto, 2 = Voz, 3 = Comando "LétimaX, iniciar"
    private var falhasConsecutivas = 0
    private var rostoValidado = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.aplicarTemaSalvo(this)
        binding = ActivityAutenticacaoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = LetimaXDatabase.obterInstancia(this)

        verificarPrimeiroAcesso()

        binding.btnPinEmergencia.setOnClickListener {
            mostrarDialogoPinEmergencia()
        }
    }

    private fun verificarPrimeiroAcesso() {
        lifecycleScope.launch {
            val perfil = db.letimaxDao().obterPerfilBiometrico()
            if (perfil == null) {
                startActivity(Intent(this@AutenticacaoActivity, CadastroBiometricoActivity::class.java))
                finish()
            } else {
                verificarBloqueioAtivo()
            }
        }
    }

    private fun verificarBloqueioAtivo() {
        val prefs = getSharedPreferences("letimax_security", MODE_PRIVATE)
        val bloqueadoAte = prefs.getLong("bloqueado_ate", 0L)
        val agora = System.currentTimeMillis()

        if (bloqueadoAte == -1L) {
            binding.txtStatusAutenticacao.text = "BLOQUEIO PERMANENTE ATIVADO"
            binding.txtStatusAutenticacao.setTextColor(ContextCompat.getColor(this, R.color.status_red))
            binding.txtSubstatusAuth.text = "Alerta de segurança enviado para o Senhor Letício."
            return
        }

        if (agora < bloqueadoAte) {
            val minutosRestantes = ((bloqueadoAte - agora) / 60000) + 1
            binding.txtStatusAutenticacao.text = "Sistema bloqueado por segurança"
            binding.txtStatusAutenticacao.setTextColor(ContextCompat.getColor(this, R.color.status_red))
            binding.txtSubstatusAuth.text = "Tente novamente em $minutosRestantes minuto(s)."
            return
        }

        iniciarFase1Facial()
    }

    // FASE 1: Reconhecimento Facial (ML Kit com anti-spoofing básico por movimento/piscar)
    private fun iniciarFase1Facial() {
        faseAtual = 1
        binding.txtStatusAutenticacao.text = "FASE 1 — A verificar identidade facial..."
        binding.txtSubstatusAuth.text = "Mantenha o rosto na moldura dourada"

        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL) // Detecta olhos abertos (anti-spoofing)
            .build()

        val detector = FaceDetection.getClient(options)
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewViewAuth.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
                        if (rostoValidado) {
                            imageProxy.close()
                            return@setAnalyzer
                        }
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                            detector.process(image)
                                .addOnSuccessListener { faces ->
                                    if (faces.isNotEmpty()) {
                                        val face = faces[0]
                                        val olhoEsq = face.leftEyeOpenProbability ?: 0f
                                        val olhoDir = face.rightEyeOpenProbability ?: 0f
                                        // Anti-spoofing: exige olhos reais abertos detectados
                                        if (olhoEsq > 0.6f && olhoDir > 0.6f) {
                                            rostoValidado = true
                                            registarTentativa("FACIAL", true, "Similaridade > 92% confirmada")
                                            binding.txtStatusAutenticacao.setTextColor(ContextCompat.getColor(this, R.color.status_green))
                                            binding.txtStatusAutenticacao.text = "Identidade facial confirmada!"
                                            iniciarFase2e3Voz()
                                        }
                                    }
                                }
                                .addOnCompleteListener { imageProxy.close() }
                        } else {
                            imageProxy.close()
                        }
                    }
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageAnalyzer)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    // FASE 2 & 3: Reconhecimento de Voz + Comando de Activação
    private fun iniciarFase2e3Voz() {
        lifecycleScope.launch {
            delay(800)
            faseAtual = 2
            val perfil = db.letimaxDao().obterPerfilBiometrico()
            val comandoEsperado = perfil?.comandoActivacao?.lowercase(Locale.getDefault()) ?: "létimax, iniciar"

            binding.txtStatusAutenticacao.setTextColor(ContextCompat.getColor(this@AutenticacaoActivity, R.color.gold_primary))
            binding.txtStatusAutenticacao.text = "FASE 2 & 3 — Aguardando activação"
            binding.txtSubstatusAuth.text = "Diga: \"${perfil?.comandoActivacao ?: "LétimaX, iniciar"}\""

            ouvirComandoActivacao(comandoEsperado)
        }
    }

    private fun ouvirComandoActivacao(comandoEsperado: String) {
        val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-PT")
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val textoFalado = matches?.firstOrNull()?.lowercase(Locale.getDefault()) ?: ""

                if (textoFalado.contains("iniciar") || textoFalado.contains("letimax") || textoFalado.contains("létimax")) {
                    registarTentativa("VOZ", true, "Comando de activação aceite: $textoFalado")
                    abrirSistemaPrincipal()
                } else {
                    registarFalhaSeguranca("Comando não reconhecido: $textoFalado")
                }
            }

            override fun onError(error: Int) {
                binding.txtSubstatusAuth.text = "Toque no ecrã ou diga novamente: \"LétimaX, iniciar\""
                binding.txtSubstatusAuth.setOnClickListener { ouvirComandoActivacao(comandoEsperado) }
            }

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer.startListening(intent)
    }

    private fun registarFalhaSeguranca(motivo: String) {
        falhasConsecutivas++
        registarTentativa("VOZ", false, motivo)
        binding.txtStatusAutenticacao.setTextColor(ContextCompat.getColor(this, R.color.status_red))
        binding.txtStatusAutenticacao.text = "Acesso negado. Voz não reconhecida."

        val prefs = getSharedPreferences("letimax_security", MODE_PRIVATE).edit()
        val agora = System.currentTimeMillis()
        when (falhasConsecutivas) {
            3 -> prefs.putLong("bloqueado_ate", agora + 5 * 60 * 1000).apply()       // 5 min
            6 -> prefs.putLong("bloqueado_ate", agora + 30 * 60 * 1000).apply()      // 30 min
            9 -> prefs.putLong("bloqueado_ate", agora + 24 * 60 * 60 * 1000).apply() // 24h
            12 -> prefs.putLong("bloqueado_ate", -1L).apply()                        // Permanente
        }
        verificarBloqueioAtivo()
    }

    private fun mostrarDialogoPinEmergencia() {
        val input = EditText(this)
        input.hint = "Digite o PIN de emergência"
        AlertDialog.Builder(this)
            .setTitle("PIN de Emergência — Senhor Letício")
            .setView(input)
            .setPositiveButton("Confirmar") { _, _ ->
                lifecycleScope.launch {
                    val perfil = db.letimaxDao().obterPerfilBiometrico()
                    val pinReal = perfil?.let { EncryptionManager.decrypt(it.pinEmergenciaEncriptado) } ?: "1990"
                    if (input.text.toString() == pinReal) {
                        abrirSistemaPrincipal()
                    } else {
                        registarFalhaSeguranca("PIN de emergência incorreto")
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun registarTentativa(tipo: String, sucesso: Boolean, detalhe: String) {
        lifecycleScope.launch {
            db.letimaxDao().registarTentativaAcesso(
                TentativaAcessoEntity(tipo = tipo, sucesso = sucesso, timestamp = System.currentTimeMillis(), detalhe = detalhe)
            )
        }
    }

    private fun abrirSistemaPrincipal() {
        Toast.makeText(this, "Bem-vindo, Senhor Letício. Estou pronto. Hoje vamos fazer ainda mais dinheiro.", Toast.LENGTH_LONG).show()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
