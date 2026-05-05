// app/src/main/java/com/weighttracker/util/DataStoreExt.kt
package com.weighttracker.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

// Top-level extension — required by DataStore delegate API
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "weight_tracker_prefs")