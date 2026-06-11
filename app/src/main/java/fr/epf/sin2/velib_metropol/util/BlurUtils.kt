package fr.epf.sin2.velib_metropol.util

import android.app.Activity
import android.view.ViewGroup
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderEffectBlur

/**
 * Active le flou "backdrop" (glassmorphisme) : chaque BlurView floute
 * en temps réel ce qui est dessiné derrière elle (carte, listes…).
 */
object BlurUtils {

    private const val DEFAULT_RADIUS = 18f

    fun apply(activity: Activity, vararg blurViews: BlurView) {
        val decorView = activity.window.decorView
        val rootView = decorView.findViewById<ViewGroup>(android.R.id.content)

        blurViews.forEach { blurView ->
            blurView.setupWith(rootView, RenderEffectBlur())
                .setFrameClearDrawable(decorView.background)
                .setBlurRadius(DEFAULT_RADIUS)
            // Le fond arrondi du BlurView sert de masque au flou
            blurView.clipToOutline = true
        }
    }
}
