package com.letimax.app.ui.onboarding

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.letimax.app.data.room.ConfiguracaoEntity
import com.letimax.app.data.room.LetimaXDatabase
import com.letimax.app.databinding.ActivityPrimeiroUsoBinding
import com.letimax.app.ui.main.MainActivity
import com.letimax.app.utils.ThemeManager
import kotlinx.coroutines.launch

class PrimeiroUsoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPrimeiroUsoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.aplicarTemaSalvo(this)
        
        binding = ActivityPrimeiroUsoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.linkAnthropic.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://anthropic.com"))
            startActivity(intent)
        }

        binding.btnGuardarChave.setOnClickListener {
            val chave = binding.editClaudeKey.text.toString().trim()
            if (chave.isEmpty()) {
                Toast.makeText(this, "Por favor, insira a chave da API do Claude.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val db = LetimaXDatabase.obterInstancia(this@PrimeiroUsoActivity)
                db.letimaxDao().guardarConfiguracao(
                    ConfiguracaoEntity(chave = "ANTHROPIC_API_KEY", valor = chave)
                )
                Toast.makeText(this@PrimeiroUsoActivity, "Chave guardada com sucesso, Senhor Letício.", Toast.LENGTH_SHORT).show()
                
                startActivity(Intent(this@PrimeiroUsoActivity, MainActivity::class.java))
                finish()
            }
        }
    }
}
