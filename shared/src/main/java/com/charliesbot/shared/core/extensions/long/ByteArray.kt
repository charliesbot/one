package com.charliesbot.shared.core.extensions.long

import java.nio.ByteBuffer

fun ByteArray.toLong() =
    ByteBuffer.wrap(this).long
