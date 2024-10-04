package com.hawkerapp.app.utils

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import com.hawkerapp.app.models.HawkerInfo

class DataProcessingUtils {
    companion object {

        fun Bitmap.resize(width: Int, height: Int): Bitmap {
            val mutableBitmap = this.copy(Bitmap.Config.ARGB_8888, true)
            Log.d("DataProcessingUtils", "$mutableBitmap")
            return Bitmap.createScaledBitmap(mutableBitmap, width, height, false)
        }

        fun findHawkerById(intent: Intent, id: String): HawkerInfo? {
            val hawkers = intent.extras?.getParcelableArray("hawkers")
            if (hawkers != null) {
                for (hawker in hawkers) {
                    val hawkerInfo = hawker as HawkerInfo
                    if (hawkerInfo.id == id) {
                        return hawkerInfo
                    }
                }
            }
            return null
        }
    }
}