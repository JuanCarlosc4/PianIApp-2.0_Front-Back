package com.piania.app.data.model.request

import com.google.gson.annotations.SerializedName

data class PracticeSessionUpdateRequestDTO(
    @SerializedName("studentObservations")
    val studentObservations: String?,
    @SerializedName("teacherCorrections")
    val teacherCorrections: String?
)
