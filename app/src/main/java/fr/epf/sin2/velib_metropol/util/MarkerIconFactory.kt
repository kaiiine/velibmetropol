package fr.epf.sin2.velib_metropol.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable

/**
 * Dessine les pastilles de la carte : un cercle "verre" coloré selon
 * la disponibilité, avec le nombre de vélos au centre.
 * Les bitmaps sont mis en cache pour garder la carte fluide.
 */
class MarkerIconFactory(private val context: Context) {

    private val cache = HashMap<String, Drawable>()
    private val density = context.resources.displayMetrics.density

    fun iconFor(bikes: Int, isOperating: Boolean): Drawable {
        val color = when {
            !isOperating -> Color.parseColor("#7C8AA5")
            bikes == 0 -> Color.parseColor("#F87171")
            bikes <= 4 -> Color.parseColor("#FBBF24")
            else -> Color.parseColor("#4ADE80")
        }
        val label = if (isOperating) bikes.coerceAtMost(99).toString() else "×"
        val key = "$label-$color"

        return cache.getOrPut(key) {
            BitmapDrawable(context.resources, drawPin(label, color))
        }
    }

    private fun drawPin(label: String, color: Int): Bitmap {
        val size = (38 * density).toInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val center = size / 2f

        // Halo translucide (effet verre)
        val halo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            alpha = 60
        }
        canvas.drawCircle(center, center, center, halo)

        // Disque principal
        val disc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
        }
        canvas.drawCircle(center, center, center - 5 * density, disc)

        // Liseré blanc
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.WHITE
            alpha = 220
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * density
        }
        canvas.drawCircle(center, center, center - 5 * density, ring)

        // Nombre de vélos
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.parseColor("#0B1220")
            textSize = 13 * density
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        val textY = center - (text.descent() + text.ascent()) / 2
        canvas.drawText(label, center, textY, text)

        return bitmap
    }
}
