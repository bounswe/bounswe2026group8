package com.bounswe2026group8.emergencyhub.mesh.transport

/**
 * Abstraction over the mesh communication layer.
 *
 * Implementations:
 *  - BleTransport → BLE discovery + GATT data transfer
 */
interface MeshTransport {

    /** Start advertising this device so others can discover it. */
    fun startAdvertising()

    /** Start scanning for nearby advertising devices. */
    fun startDiscovery()

    /** Stop all advertising and discovery. */
    fun stop()

    /** Send a byte payload to a connected device. */
    fun sendPayload(deviceId: String, data: ByteArray)

    /** Register a callback for when a nearby device is found. */
    fun onDeviceFound(callback: (DeviceInfo) -> Unit)

    /** Register a callback for when a payload is received from a device. */
    fun onPayloadReceived(callback: (deviceId: String, data: ByteArray) -> Unit)

    /** Register a callback for when a device disconnects. */
    fun onDeviceDisconnected(callback: (deviceId: String) -> Unit)

    /** Get the IDs of all currently connected peers. */
    fun getConnectedPeerIds(): Set<String>
}

data class DeviceInfo(
    val deviceId: String,
    val displayName: String?
)
