package com.example.data.repository

import com.example.data.local.BatteryHistoryEntity
import com.example.data.local.BridgeCallEventEntity
import com.example.data.local.BridgeDao
import com.example.data.local.BridgeMessageEntity
import com.example.data.local.PairedDeviceEntity
import com.example.data.local.SyncedContactEntity
import kotlinx.coroutines.flow.Flow

class BridgeRepository(private val dao: BridgeDao) {
    val pairedDevices: Flow<List<PairedDeviceEntity>> = dao.observePairedDevices()
    val messages: Flow<List<BridgeMessageEntity>> = dao.observeMessages()
    val callEvents: Flow<List<BridgeCallEventEntity>> = dao.observeCallEvents()
    val contacts: Flow<List<SyncedContactEntity>> = dao.observeContacts()

    suspend fun savePairedDevice(device: PairedDeviceEntity) = dao.upsertPairedDevice(device)
    suspend fun forgetPairedDevice(deviceId: String) = dao.deletePairedDevice(deviceId)

    suspend fun saveMessage(message: BridgeMessageEntity): Long = dao.insertMessage(message)
    suspend fun markMessageStatus(id: Long, status: String) = dao.updateMessageStatus(id, status)
    suspend fun deleteMessage(id: Long) = dao.deleteMessage(id)

    suspend fun recordCallEvent(event: BridgeCallEventEntity): Long = dao.insertCallEvent(event)
    suspend fun clearCallHistory() = dao.clearCallEvents()

    suspend fun saveContact(contact: SyncedContactEntity) = dao.upsertContact(contact)
    suspend fun saveContacts(contacts: List<SyncedContactEntity>) = dao.upsertContacts(contacts)
    suspend fun getContactsSnapshot(): List<SyncedContactEntity> = dao.getAllContactsSnapshot()
    suspend fun deleteContact(normalizedNumber: String) = dao.deleteContact(normalizedNumber)

    fun observeBatteryHistoryForDevice(
        deviceId: String,
        sinceTimestamp: Long
    ): Flow<List<BatteryHistoryEntity>> = dao.observeBatteryHistoryForDevice(deviceId, sinceTimestamp)

    fun observePeerBatteryHistory(
        excludedDeviceId: String,
        sinceTimestamp: Long
    ): Flow<List<BatteryHistoryEntity>> = dao.observePeerBatteryHistory(excludedDeviceId, sinceTimestamp)

    suspend fun recordBatterySample(sample: BatteryHistoryEntity): Long = dao.insertBatterySample(sample)
    suspend fun pruneOldBatterySamples(olderThanTimestamp: Long) = dao.pruneOldBatterySamples(olderThanTimestamp)
}
