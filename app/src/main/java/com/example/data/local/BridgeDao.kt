package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BridgeDao {
    // Paired Devices
    @Query("SELECT * FROM paired_devices ORDER BY lastConnectedAt DESC")
    fun observePairedDevices(): Flow<List<PairedDeviceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPairedDevice(device: PairedDeviceEntity)

    @Query("DELETE FROM paired_devices WHERE deviceId = :deviceId")
    suspend fun deletePairedDevice(deviceId: String)

    // Relayed Messages / OTP / SMS Handoffs
    @Query("SELECT * FROM bridge_messages ORDER BY timestamp DESC")
    fun observeMessages(): Flow<List<BridgeMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: BridgeMessageEntity): Long

    @Query("UPDATE bridge_messages SET deliveryStatus = :status WHERE id = :id")
    suspend fun updateMessageStatus(id: Long, status: String)

    @Query("DELETE FROM bridge_messages WHERE id = :id")
    suspend fun deleteMessage(id: Long)

    // Call Events
    @Query("SELECT * FROM bridge_call_events ORDER BY timestamp DESC LIMIT 100")
    fun observeCallEvents(): Flow<List<BridgeCallEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallEvent(event: BridgeCallEventEntity): Long

    @Query("DELETE FROM bridge_call_events")
    suspend fun clearCallEvents()

    // Synced Contacts
    @Query("SELECT * FROM synced_contacts ORDER BY displayName ASC")
    fun observeContacts(): Flow<List<SyncedContactEntity>>

    @Query("SELECT * FROM synced_contacts ORDER BY displayName ASC")
    suspend fun getAllContactsSnapshot(): List<SyncedContactEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertContact(contact: SyncedContactEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertContacts(contacts: List<SyncedContactEntity>)

    @Query("DELETE FROM synced_contacts WHERE normalizedNumber = :normalizedNumber")
    suspend fun deleteContact(normalizedNumber: String)

    // Historical Battery Telemetry
    @Query(
        "SELECT * FROM battery_history WHERE deviceId = :deviceId AND timestamp >= :sinceTimestamp " +
            "ORDER BY timestamp ASC LIMIT 360"
    )
    fun observeBatteryHistoryForDevice(
        deviceId: String,
        sinceTimestamp: Long
    ): Flow<List<BatteryHistoryEntity>>

    @Query(
        "SELECT * FROM battery_history WHERE deviceId != :excludedDeviceId AND timestamp >= :sinceTimestamp " +
            "ORDER BY timestamp ASC LIMIT 360"
    )
    fun observePeerBatteryHistory(
        excludedDeviceId: String,
        sinceTimestamp: Long
    ): Flow<List<BatteryHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatterySample(sample: BatteryHistoryEntity): Long

    @Query("DELETE FROM battery_history WHERE timestamp < :olderThanTimestamp")
    suspend fun pruneOldBatterySamples(olderThanTimestamp: Long)
}
