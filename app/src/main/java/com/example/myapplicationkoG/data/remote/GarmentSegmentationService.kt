package com.example.myapplicationkoG.data.remote

import com.example.myapplicationkoG.domain.model.GarmentSegmentationResult
import com.example.myapplicationkoG.domain.model.GarmentSideId
import com.example.myapplicationkoG.domain.model.ImageAsset
import com.example.myapplicationkoG.domain.model.MaskAsset
import com.example.myapplicationkoG.network.BackendApi
import com.example.myapplicationkoG.network.NetworkModule
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.UUID

/**
 * Application-level interface: Android -> Your Backend.
 * Implementations can be swapped (mock for tests, real for prod).
 * The Android domain layer never sees provider specifics.
 */
interface GarmentSegmentationService {
    suspend fun segment(
        imageFile: File,
        side: GarmentSideId
    ): GarmentSegmentationResult
}

class BackendGarmentSegmentationService(
    private val api: BackendApi = NetworkModule.backendApi
) : GarmentSegmentationService {

    override suspend fun segment(
        imageFile: File,
        side: GarmentSideId
    ): GarmentSegmentationResult {
        val mime = guessMime(imageFile.name)
        val imagePart = MultipartBody.Part.createFormData(
            name = "image",
            filename = imageFile.name,
            body = imageFile.asRequestBody(mime.toMediaTypeOrNull())
        )
        val sidePart = MultipartBody.Part.createFormData(
            "side",
            side.name
        )
        val resp = api.segmentGarment(imagePart, sidePart)

        return GarmentSegmentationResult(
            sourceImage = ImageAsset(
                id = UUID.randomUUID().toString(),
                uri = resp.imageUrl,
                width = resp.width,
                height = resp.height
            ),
            mask = MaskAsset(
                id = UUID.randomUUID().toString(),
                uri = resp.maskUrl,
                width = resp.width,
                height = resp.height
            ),
            confidence = null,
            width = resp.width,
            height = resp.height
        )
    }

    private fun guessMime(name: String): String = when {
        name.endsWith(".png", true) -> "image/png"
        name.endsWith(".webp", true) -> "image/webp"
        name.endsWith(".gif", true) -> "image/gif"
        else -> "image/jpeg"
    }
}
