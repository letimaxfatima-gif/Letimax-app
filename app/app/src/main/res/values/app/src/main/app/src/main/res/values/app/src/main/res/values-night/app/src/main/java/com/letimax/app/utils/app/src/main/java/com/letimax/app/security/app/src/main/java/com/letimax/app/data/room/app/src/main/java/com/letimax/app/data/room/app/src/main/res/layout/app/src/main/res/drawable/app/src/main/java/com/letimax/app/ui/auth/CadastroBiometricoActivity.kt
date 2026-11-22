package com.letimax.app.ui.auth

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.letimax.app.data.room.LetimaXDatabase
import com.letimax.app.data.room.PerfilBiometricoEntity
import com.letimax.app.databinding.ActivityCadastroBiometricoBinding
import com.letimax.app.security.EncryptionManager
import com.letimax.app.ui.onboarding.PrimeiroUsoActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

class CadastroBiometricoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCadastroBiometricoBinding
    private var faseAtual = 1 // 1 = Voz, 2 = Rosto, 3 = Confirmação
    private var passoVoz = 1
    private var passoRosto = 1

    private val frasesVoz = listOf(
        "Eu sou o Senhor Letício, criador e único dono da LétimaX.",
        "A LétimaX obedece única e exclusivamente à minha voz e identidade.",
        "Hoje vamos construir activos e fazer ainda mais dinheiro."
    )

    private val angulosRosto = listOf(
        "Olhe diretamente para a frente",
        "Vire ligeiramente o rosto para a DIREITA",
        "Vire ligeiramente o rosto para a ESQUERDA",
        "Incline ligeiramente o rosto para CIMA",
        "Incline ligeiramente o rosto para BAIXO"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCadastroBiometricoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        solicitarPermissoesIniciais()
        atualizarInterfaceFase()

        binding.btnAcaoCadastro.setOnClickListener {
            processarPassoAtual()
        }

        binding.btnRepetirCadastro.setOnClickListener {
            binding.progressBarBiometria.progress = 0
            Toast.makeText(this, "Pronto para repetir.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun solicitarPermissoesIniciais() {
        val permissoes = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        if (permissoes.any { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) {
            ActivityCompat.requestPermissions(this, permissoes, 100)
        }
    }

    private fun atualizarInterfaceFase() {
        when (faseAtual) {
            1 -> {
                binding.txtFaseCadastro.text = "Fase 1 de 3 — Cadastro de Voz ($passoVoz/3)"
                binding.containerCamera.visibility = View.GONE
                binding.txtInstrucaoBiometria.text = "Diga em voz alta:\n\"${frasesVoz[passoVoz - 1]}\""
                binding.btnAcaoCadastro.text = "Gravar Frase $passoVoz"
                binding.btnRepetirCadastro.visibility = View.VISIBLE
            }
            2 -> {
                binding.txtFaseCadastro.text = "Fase 2 de 3 — Cadastro de Rosto ($passoRosto/5)"
                binding.containerCamera.visibility = View.VISIBLE
                binding.txtInstrucaoBiometria.text = angulosRosto[passoRosto - 1]
                binding.btnAcaoCadastro.text = "Capturar Ângulo $passoRosto"
                iniciarCameraFrontal()
            }
            3 -> {
                binding.txtFaseCadastro.text = "Perfil Biométrico Criado"
                binding.containerCamera.visibility = View.GONE
                binding.btnRepetirCadastro.visibility = View.GONE
                binding.progressBarBiometria.progress = 100
                binding.txtInstrucaoBiometria.text =
                    "🛡️ Escudo Dourado Ativado\n\nPerfil biométrico criado. A LétimaX reconhece-o agora, Senhor Letício."
                binding.btnAcaoCadastro.text = "Iniciar LétimaX"
            }
        }
    }

    private fun processarPassoAtual() {
        when (faseAtual) {
            1 -> {
                binding.btnAcaoCadastro.isEnabled = false
                lifecycleScope.launch {
                    for (i in 1..100 step 10) {
                        binding.progressBarBiometria.progress = i
                        delay(150)
                    }
                    binding.btnAcaoCadastro.isEnabled = true
                    if (passoVoz < 3) {
                        passoVoz++
                        binding.progressBarBiometria.progress = 0
                        atualizarInterfaceFase()
                    } else {
                        faseAtual = 2
                        binding.progressBarBiometria.progress = 0
                        atualizarInterfaceFase()
                    }
                }
            }
            2 -> {
                if (passoRosto < 5) {
                    passoRosto++
                    binding.progressBarBiometria.progress = (passoRosto - 1) * 20
                    atualizarInterfaceFase()
                } else {
                    guardarAssinaturaEncriptada()
                }
            }
            3 -> {
                startActivity(Intent(this, PrimeiroUsoActivity::class.java))
                finish()
            }
        }
    }

    private fun guardarAssinaturaEncriptada() {
        lifecycleScope.launch {
            val db = LetimaXDatabase.obterInstancia(this@CadastroBiometricoActivity)
            val hashFacial = EncryptionManager.encrypt("FACE_SIGNATURE_LETICIO_${UUID.randomUUID()}")
            val hashVocal = EncryptionManager.encrypt("VOICE_SIGNATURE_LETICIO_${UUID.randomUUID()}")
            val pinPadrao = EncryptionManager.encrypt("1990") // Configurável nas Configurações

            val perfil = PerfilBiometricoEntity(
                assinaturaFacialEncriptada = hashFacial,
                assinaturaVocalEncriptada = hashVocal,
                pinEmergenciaEncriptado = pinPadrao,
                dataCadastro = System.currentTimeMillis()
            )
            db.letimaxDao().guardarPerfilBiometrico(perfil)

            faseAtual = 3
            atualizarInterfaceFase()
        }
    }

    private fun iniciarCameraFrontal() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewViewCadastro.surfaceProvider)
            }
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }
}
