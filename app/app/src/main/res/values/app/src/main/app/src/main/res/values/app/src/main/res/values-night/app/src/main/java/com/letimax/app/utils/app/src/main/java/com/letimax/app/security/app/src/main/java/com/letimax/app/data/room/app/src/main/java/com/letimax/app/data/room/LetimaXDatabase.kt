package com.letimax.app.data.room

import android.content.Context
import androidx.room.*

@Dao
interface LetimaXDao {
    // Biometria
    @Query("SELECT * FROM perfil_biometrico WHERE id = 1")
    suspend fun obterPerfilBiometrico(): PerfilBiometricoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarPerfilBiometrico(perfil: PerfilBiometricoEntity)

    @Insert
    suspend fun registarTentativaAcesso(tentativa: TentativaAcessoEntity)

    // Chat
    @Query("SELECT * FROM conversas ORDER BY timestamp ASC")
    suspend fun obterHistoricoChat(): List<ConversaEntity>

    @Insert
    suspend fun inserirMensagem(conversa: ConversaEntity)

    // Configurações
    @Query("SELECT valor FROM configuracoes WHERE chave = :chave")
    suspend fun obterConfiguracao(chave: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarConfiguracao(config: ConfiguracaoEntity)

    // Cofre de Passwords
    @Query("SELECT * FROM passwords ORDER BY plataforma ASC")
    suspend fun listarPasswords(): List<PasswordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarPassword(password: PasswordEntity)

    // Motor de Receita
    @Query("SELECT * FROM motor_receita_oportunidades ORDER BY pontuacao DESC")
    suspend fun listarOportunidades(): List<OportunidadeReceitaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarOportunidade(oportunidade: OportunidadeReceitaEntity)

    // Logs de Auditoria
    @Query("SELECT * FROM logs_erros ORDER BY timestamp DESC")
    suspend fun listarLogs(): List<LogSistemaEntity>

    @Insert
    suspend fun inserirLog(log: LogSistemaEntity)
}

@Database(
    entities = [
        PerfilBiometricoEntity::class,
        TentativaAcessoEntity::class,
        ConversaEntity::class,
        ConfiguracaoEntity::class,
        PasswordEntity::class,
        OportunidadeReceitaEntity::class,
        LogSistemaEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LetimaXDatabase : RoomDatabase() {
    abstract fun letimaxDao(): LetimaXDao

    companion object {
        @Volatile
        private var INSTANCE: LetimaXDatabase? = null

        fun obterInstancia(context: Context): LetimaXDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LetimaXDatabase::class.java,
                    "letimax_database.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
