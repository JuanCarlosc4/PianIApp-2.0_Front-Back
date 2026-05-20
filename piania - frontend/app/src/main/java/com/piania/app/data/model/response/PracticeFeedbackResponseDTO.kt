package com.piania.app.data.model.response

import com.google.gson.annotations.SerializedName

data class PracticeFeedbackResponseDTO(
    @SerializedName("practiceSessionId")
    val practiceSessionId: Long,
    @SerializedName("precisionGeneral")
    val precisionGeneral: Int,
    @SerializedName("noteErrors")
    val noteErrors: Int,
    @SerializedName("rhythmErrors")
    val rhythmErrors: Int,
    @SerializedName("detailedReport")
    val detailedReport: String?
)
