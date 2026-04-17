package com.avocor.commander.model

/**
 * Abstracts the target of a command — either a single device or all devices in a room/group.
 */
sealed interface CommandTarget {
    data class SingleDevice(val deviceId: Int) : CommandTarget
    data class AllInRoom(val groupId: Int) : CommandTarget
}
