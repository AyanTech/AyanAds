package ir.ayantech.ayanadmanager.model.api

import ir.ayantech.ayanadmanager.utils.constant.ErrorCode

data class Status(
    val Code: ErrorCode,
    val Description: String
)