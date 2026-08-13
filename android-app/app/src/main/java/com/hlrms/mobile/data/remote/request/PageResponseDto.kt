package com.hlrms.mobile.data.remote.request

data class PageResponseDto<T>(

    val content: List<T>,

    val number: Int,

    val size: Int,

    val totalElements: Long,

    val totalPages: Int,

    val first: Boolean,

    val last: Boolean,

    val hasNext: Boolean,

    val hasPrevious: Boolean
)
