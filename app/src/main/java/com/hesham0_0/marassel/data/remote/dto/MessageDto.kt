package com.hesham0_0.marassel.data.remote.dto

import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.PropertyName


@IgnoreExtraProperties
data class MessageDto(

    @get:PropertyName("sender_uid")
    @set:PropertyName("sender_uid")
    var senderUid: String? = null,

    @get:PropertyName("sender_name")
    @set:PropertyName("sender_name")
    var senderName: String? = null,

    @get:PropertyName("text")
    @set:PropertyName("text")
    var text: String? = null,

    @get:PropertyName("media_url")
    @set:PropertyName("media_url")
    var mediaUrl: String? = null,

    @get:PropertyName("media_type")
    @set:PropertyName("media_type")
    var mediaType: String? = null,

    @get:PropertyName("timestamp")
    @set:PropertyName("timestamp")
    var timestamp: Long? = null,

    @get:PropertyName("type")
    @set:PropertyName("type")
    var type: String? = null,

    @get:PropertyName("local_id")
    @set:PropertyName("local_id")
    var localId: String? = null,

    @get:PropertyName("reply_to_id")
    @set:PropertyName("reply_to_id")
    var replyToId: String? = null,
) {
    constructor() : this(
        senderUid  = null,
        senderName = null,
        text       = null,
        mediaUrl   = null,
        mediaType  = null,
        timestamp  = null,
        type       = null,
        localId    = null,
        replyToId  = null,
    )
}