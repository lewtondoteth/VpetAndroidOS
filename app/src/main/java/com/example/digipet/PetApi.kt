package com.example.digipet

import retrofit2.http.GET
import retrofit2.Call

data class PetStats(
    val hunger: Float,
    val energy: Float
)

data class PetResponse(
    val stats: PetStats
)

interface PetApi {
    @GET("pet")
    fun getPet(): Call<PetResponse>
}
