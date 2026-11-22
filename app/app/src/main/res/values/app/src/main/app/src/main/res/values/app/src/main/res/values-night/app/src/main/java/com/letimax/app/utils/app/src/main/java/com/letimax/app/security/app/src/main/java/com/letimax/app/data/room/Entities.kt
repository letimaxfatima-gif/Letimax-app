package com.letimax.app.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "perfil_biometrico")
data class PerfilBiometricoEntity(
    @PrimaryKey val id: Int = 1,
    val assinaturaFacialEncriptada: String,
    val assinaturaVocalEncriptada: String,
    val comandoActivacao: String = "LétimaX, iniciar",
    val pinEmergenciaEncriptado: String,
    val dataCadastro: Long
)

@Entity(tableName = "tentativas_acesso")
data class TentativaAcessoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tipo: String, // "FACIAL" ou "VOZ"
    val sucesso: Boolean,
    val timestamp: Long,
    val detalhe: String
)

@Entity(tableName = "conversas")
data class ConversaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val remetente: String, // "leticio" ou "letimax"
    val mensagem: String,
    val timestamp: Long
)

@Entity(tableName = "configuracoes")
data class ConfiguracaoEntity(
    @PrimaryKey val chave: String,
    val valor: String
)

@Entity(tableName = "passwords")
data class PasswordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val plataforma: String,
    val email: String,
    val passwordEncriptada: String,
    val notas: String
)

@Entity(tableName = "motor_receita_oportunidades")
data class OportunidadeReceitaEntity(
    @PrimaryKey val id: String,
    val titulo: String,
    val receitaMinima: Double,
    val receitaMaxima: Double,
    val pontuacao: Int, // 0 a 100 (só notifica > 70)
    val esforco: String,
    val tempoResultado: String,
    val risco: String,
    val status: String // "PENDENTE", "APROVADA", "REJEITADA", "ADIADA"
)

@Entity(tableName = "logs_erros")
data class LogSistemaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tarefa: String,
    val status: String, // "SUCESSO", "FALHA", "TENTANDO"
    val detalhe: String,
    val timestamp: Long
)
