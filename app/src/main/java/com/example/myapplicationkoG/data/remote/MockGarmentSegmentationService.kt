package com.example.myapplicationkoG.data.remote

import com.example.myapplicationkoG.domain.model.GarmentSegmentationResult
import com.example.myapplicationkoG.domain.model.GarmentSideId
import com.example.myapplicationkoG.domain.model.ImageAsset
import com.example.myapplicationkoG.domain.model.MaskAsset
import kotlinx.coroutines.delay
import java.io.File
import java.util.UUID

/**
 * In-memory mock used when no backend is available.
 * The mask is a placeholder; real masks come from the backend.
 * This exists so the editor can be developed offline in Part 1.
 */
class MockGarmentSegmentationService : GarmentSegmentationService {
    override suspend fun segment(
        imageFile: File,
        side: GarmentSideId
    ): GarmentSegmentationResult {
        delay(400)
        val uri = imageFile.absolutePath
        return GarmentSegmentationResult(
            sourceImage = ImageAsset(UUID.randomUUID().toString(), uri, 0, 0),
            mask = MaskAsset(UUID.randomUUID().toString(), uri, 0, 0),
            confidence = 0.0f,
            width = 0,
            height = 0
        )
    }
}
