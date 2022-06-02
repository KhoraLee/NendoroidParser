package data

import com.google.gson.annotations.SerializedName

enum class Gender {
    @SerializedName("M")
    MALE,
    @SerializedName("F")
    FEMALE,
    @SerializedName("M&F")
    MALE_AND_FEMALE,
    @SerializedName("A")
    ANDROGYNY,
    @SerializedName("DAYO")
    DAYO,
    @SerializedName("?")
    UNKNOWN,
}