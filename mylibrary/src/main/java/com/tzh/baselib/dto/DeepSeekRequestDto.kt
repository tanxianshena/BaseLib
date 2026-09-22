package com.tzh.baselib.dto

class DeepSeekRequestDto(
    var messages : MutableList<MessageDto> ?= null,

    var model : String = "deepseek-chat",

    var stream : Boolean = true,

    var max_tokens : Int = 4096
)