package com.letimax.app.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.letimax.app.data.room.ConversaEntity
import com.letimax.app.data.room.LetimaXDatabase
import com.letimax.app.databinding.FragmentChatBinding
import kotlinx.coroutines.launch

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: LetimaXDatabase
    private val listaMensagens = mutableListOf<ConversaEntity>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        db = LetimaXDatabase.obterInstancia(requireContext())

        setupUI()
        carregarHistorico()

        return binding.root
    }

    private fun setupUI() {
        binding.btnEnviarChat.setOnClickListener {
            val texto = binding.editMensagemChat.text.toString().trim()
            if (texto.isNotEmpty()) {
                enviarMensagem(texto)
                binding.editMensagemChat.setText("")
            }
        }
    }

    private fun carregarHistorico() {
        lifecycleScope.launch {
            val historico = db.letimaxDao().obterHistoricoChat()
            listaMensagens.clear()
            listaMensagens.addAll(historico)
            
            if (listaMensagens.isEmpty()) {
                // Saudação inicial oficial
                val saudacao = ConversaEntity(
                    remetente = "letimax",
                    mensagem = "Bem-vindo, Senhor Letício. Estou pronto. Hoje vamos fazer ainda mais dinheiro.",
                    timestamp = System.currentTimeMillis()
                )
                db.letimaxDao().inserirMensagem(saudacao)
                listaMensagens.add(saudacao)
            }
        }
    }

    private fun enviarMensagem(mensagem: String) {
        lifecycleScope.launch {
            val msgUtilizador = ConversaEntity(
                remetente = "leticio",
                mensagem = mensagem,
                timestamp = System.currentTimeMillis()
            )
            db.letimaxDao().inserirMensagem(msgUtilizador)
            listaMensagens.add(msgUtilizador)

            // Resposta autónoma simulada da LétimaX baseada na filosofia central
            val respostaTexto = processarComandoOuMensagem(mensagem)
            val msgLetimax = ConversaEntity(
                remetente = "letimax",
                mensagem = respostaTexto,
                timestamp = System.currentTimeMillis()
            )
            db.letimaxDao().inserirMensagem(msgLetimax)
            listaMensagens.add(msgLetimax)
        }
    }

    private fun processarComandoOuMensagem(comando: String): String {
        return when {
            comando.contains("quanto ganhei hoje", ignoreCase = true) -> 
                "Senhor Letício, a receita de hoje em tempo real está a ser calculada pelas integrações ativas. Nenhuma venda perdida."
            comando.contains("modo autónomo", ignoreCase = true) -> 
                "Comando aceite. Modo autónomo ativado com sucesso nos limites rigorosos de segurança."
            else -> 
                "Processado, Senhor Letício. A analisar estratégias de riqueza passiva e construção de ativos."
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
