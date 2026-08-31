package com.example.myapplicationkoG.network

import com.example.myapplicationkoG.network.dto.SegmentResponseDto
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * Application-level backend contract.
 * The Android side only knows about /v1 endpoints.
 * Provider-specific (Roboflow) details live on the backend.
 */
interface BackendApi {

    @Multipart
    @POST("v1/garments/segment")
    suspend fun segmentGarment(
        @Part image: MultipartBody.Part,
        @Part side: MultipartBody.Part
    ): SegmentResponseDto
}
