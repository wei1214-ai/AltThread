package com.example.myapplicationkoG.di

import android.content.Context
import com.example.myapplicationkoG.inference.ClothingInferencePipeline
import com.example.myapplicationkoG.storage.ProjectPreferences

/**
 * Tiny manual DI. Replaceable with Hilt later.
 * Kept here so the Editor does not depend on singletons being constructed in MainActivity.
 */
object ServiceLocator {

    @Volatile private var inference: ClothingInferencePipeline? = null
    @Volatile private var preferences: ProjectPreferences? = null

    fun inferencePipeline(context: Context): ClothingInferencePipeline {
        return inference ?: synchronized(this) {
            inference ?: ClothingInferencePipeline(context.applicationContext)
                .also { inference = it }
        }
    }

    fun projectPreferences(context: Context): ProjectPreferences {
        return preferences ?: synchronized(this) {
            preferences ?: ProjectPreferences(context.applicationContext)
                .also { preferences = it }
        }
    }
}
