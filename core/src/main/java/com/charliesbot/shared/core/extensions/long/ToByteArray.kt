package com.charliesbot.shared.core.extensions.long

import java.nio.ByteBuffer

fun Long.toByteArray(): ByteArray =
    ByteBuffer.allocate(Long.SIZE_BYTES).putLong(this).array()
