package com.example.myapplicationkoG.di

import android.content.Context
import com.example.myapplicationkoG.data.remote.BackendGarmentSegmentationService
import com.example.myapplicationkoG.data.remote.GarmentSegmentationService
import com.example.myapplicationkoG.data.remote.MockGarmentSegmentationService
import com.example.myapplicationkoG.storage.ProjectPreferences

/**
 * Tiny manual DI. Replaceable with Hilt later.
 * Kept here so the Editor does not depend on singletons being constructed in MainActivity.
 */
object ServiceLocator {

    @Volatile private var segmentationService: GarmentSegmentationService? = null
    @Volatile private var preferences: ProjectPreferences? = null

    fun segmentationService(context: Context): GarmentSegmentationService {
        return segmentationService ?: synchronized(this) {
            segmentationService ?: buildSegmentation(context).also { segmentationService = it }
        }
    }

    fun projectPreferences(context: Context): ProjectPreferences {
        return preferences ?: synchronized(this) {
            preferences ?: ProjectPreferences(context.applicationContext)
                .also { preferences = it }
        }
    }

    private fun buildSegmentation(context: Context): GarmentSegmentationService {
        // Backend is the production path. The mock is kept for offline development
        // and tests; flip USE_MOCK_SEGMENTATION in BuildConfig later if needed.
        return if (USE_MOCK_SEGMENTATION) MockGarmentSegmentationService()
        else BackendGarmentSegmentationService()
    }

    // No build-time flag yet; switch to MockGarmentSegmentationService() in tests.
    private const val USE_MOCK_SEGMENTATION = false
}
