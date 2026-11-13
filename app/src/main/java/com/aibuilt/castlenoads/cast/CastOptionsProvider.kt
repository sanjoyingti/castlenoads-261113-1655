package com.aibuilt.castlenoads.cast

import android.content.Context
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider
import com.google.android.gms.cast.framework.media.CastMediaOptions

class CastOptionsProvider : OptionsProvider {

    override fun getCastOptions(context: Context): CastOptions =
        CastOptions.Builder()
            .setReceiverApplicationId(CAST_RECEIVER_ID)
            .setCastMediaOptions(
                CastMediaOptions.Builder()
                    .setNotificationOptions(null)
                    .build()
            )
            .build()

    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? = null

    companion object {
        /** Google's default media receiver — no registration needed. */
        const val CAST_RECEIVER_ID = "CC1AD845"
    }
}
