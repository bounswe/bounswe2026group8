package com.bounswe2026group8.emergencyhub.mesh.transport

import com.bounswe2026group8.emergencyhub.mesh.db.MeshMessage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Handles serialization of sync protocol messages between devices.
 *
 * Sync flow:
 *  1. Device A sends INVENTORY (list of message IDs it has)
 *  2. Device B compares, sends REQUEST for IDs it's missing
 *  3. Device A sends MESSAGES payload with the requested messages
 *  4. Roles reverse — B sends its inventory, A requests missing ones
 */
object MeshProtocol {

    private val gson = Gson()

    // Message type prefixes (first byte of payload)
    const val TYPE_INVENTORY: Byte = 1
    const val TYPE_REQUEST: Byte = 2
    const val TYPE_MESSAGES: Byte = 3

    /** Encode a list of message IDs this device has. */
    fun encodeInventory(messageIds: List<String>): ByteArray {
        val json = gson.toJson(messageIds)
        return byteArrayOf(TYPE_INVENTORY) + json.toByteArray()
    }

    /** Decode an inventory payload into a list of message IDs. */
    fun decodeInventory(data: ByteArray): List<String> {
        val json = String(data, 1, data.size - 1)
        return gson.fromJson(json, object : TypeToken<List<String>>() {}.type)
    }

    /** Encode a request for specific message IDs. */
    fun encodeRequest(missingIds: List<String>): ByteArray {
        val json = gson.toJson(missingIds)
        return byteArrayOf(TYPE_REQUEST) + json.toByteArray()
    }

    /** Decode a request payload into a list of requested IDs. */
    fun decodeRequest(data: ByteArray): List<String> {
        val json = String(data, 1, data.size - 1)
        return gson.fromJson(json, object : TypeToken<List<String>>() {}.type)
    }

    /** Encode a batch of messages to send. */
    fun encodeMessages(messages: List<MeshMessage>): ByteArray {
        val json = gson.toJson(messages)
        return byteArrayOf(TYPE_MESSAGES) + json.toByteArray()
    }

    /** Decode a messages payload. */
    fun decodeMessages(data: ByteArray): List<MeshMessage> {
        val json = String(data, 1, data.size - 1)
        return gson.fromJson(json, object : TypeToken<List<MeshMessage>>() {}.type)
    }

    /** Get the type byte from a payload. */
    fun getType(data: ByteArray): Byte = data[0]
}
