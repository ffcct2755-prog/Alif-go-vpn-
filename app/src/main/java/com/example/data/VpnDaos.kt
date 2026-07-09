package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VpnServerDao {
    @Query("SELECT * FROM servers ORDER BY id DESC")
    fun getAllServersFlow(): Flow<List<VpnServer>>

    @Query("SELECT * FROM servers WHERE isEnabled = 1")
    suspend fun getActiveServers(): List<VpnServer>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServer(server: VpnServer)

    @Update
    suspend fun updateServer(server: VpnServer)

    @Query("SELECT * FROM servers WHERE id = :id LIMIT 1")
    suspend fun getServerById(id: Int): VpnServer?

    @Query("DELETE FROM servers WHERE id = :id")
    suspend fun deleteServerById(id: Int)

    @Query("DELETE FROM servers")
    suspend fun clearAll()
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    fun getAllUsersFlow(): Flow<List<UserSession>>

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserSession?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    fun getUserByEmailFlow(email: String): Flow<UserSession?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserSession)

    @Update
    suspend fun updateUser(user: UserSession)

    @Query("DELETE FROM users WHERE email = :email")
    suspend fun deleteUserByEmail(email: String)
}

@Dao
interface USDTTransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactionsFlow(): Flow<List<USDTTransaction>>

    @Query("SELECT * FROM transactions WHERE userEmail = :email ORDER BY timestamp DESC")
    fun getTransactionsByUserFlow(email: String): Flow<List<USDTTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(tx: USDTTransaction)

    @Update
    suspend fun updateTransaction(tx: USDTTransaction)
}

@Dao
interface SupportTicketDao {
    @Query("SELECT * FROM support_tickets ORDER BY timestamp DESC")
    fun getAllTicketsFlow(): Flow<List<SupportTicket>>

    @Query("SELECT * FROM support_tickets WHERE userEmail = :email ORDER BY timestamp DESC")
    fun getTicketsByUserFlow(email: String): Flow<List<SupportTicket>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicket(ticket: SupportTicket)

    @Update
    suspend fun updateTicket(ticket: SupportTicket)
}

@Dao
interface AdmobConfigDao {
    @Query("SELECT * FROM admob_config WHERE id = 1 LIMIT 1")
    fun getConfigFlow(): Flow<AdmobConfig?>

    @Query("SELECT * FROM admob_config WHERE id = 1 LIMIT 1")
    suspend fun getConfig(): AdmobConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: AdmobConfig)
}

@Dao
interface AppConfigDao {
    @Query("SELECT * FROM app_config WHERE id = 1 LIMIT 1")
    fun getAppConfigFlow(): Flow<AppConfig?>

    @Query("SELECT * FROM app_config WHERE id = 1 LIMIT 1")
    suspend fun getAppConfig(): AppConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppConfig(config: AppConfig)
}

@Dao
interface ConnectionLogDao {
    @Query("SELECT * FROM connection_logs ORDER BY connectedAt DESC")
    fun getAllLogsFlow(): Flow<List<ConnectionLog>>

    @Query("SELECT * FROM connection_logs WHERE userEmail = :email ORDER BY connectedAt DESC")
    fun getLogsByUserFlow(email: String): Flow<List<ConnectionLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ConnectionLog)
}

@Dao
interface SubscriptionPlanDao {
    @Query("SELECT * FROM subscription_plans ORDER BY priceUsdt ASC")
    fun getAllPlansFlow(): Flow<List<SubscriptionPlan>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: SubscriptionPlan)

    @Query("DELETE FROM subscription_plans WHERE id = :id")
    suspend fun deletePlanById(id: String)
}

@Dao
interface ResellerPinDao {
    @Query("SELECT * FROM reseller_pins ORDER BY generatedAt DESC")
    fun getAllPinsFlow(): Flow<List<ResellerPin>>

    @Query("SELECT * FROM reseller_pins WHERE pinCode = :code LIMIT 1")
    suspend fun getPinByCode(code: String): ResellerPin?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPin(pin: ResellerPin)

    @Query("DELETE FROM reseller_pins WHERE pinCode = :code")
    suspend fun deletePinByCode(code: String)
}
