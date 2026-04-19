package com.example.docuspeak

import android.view.View
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce

object AnimationHelper {

    /**
     * Applies a physics-based "iPhone style" spring bounce on scale.
     * Perfect for button presses or "Island" interactions.
     */
    fun applySpringScale(view: View, targetScale: Float = 1.0f) {
        val springX = SpringAnimation(view, DynamicAnimation.SCALE_X, targetScale).apply {
            spring.dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
            spring.stiffness = SpringForce.STIFFNESS_LOW
        }
        val springY = SpringAnimation(view, DynamicAnimation.SCALE_Y, targetScale).apply {
            spring.dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
            spring.stiffness = SpringForce.STIFFNESS_LOW
        }
        springX.start()
        springY.start()
    }

    /**
     * Elastic snap for horizontal translation.
     */
    fun applySpringTranslationX(view: View, targetX: Float) {
        SpringAnimation(view, DynamicAnimation.TRANSLATION_X, targetX).apply {
            spring.dampingRatio = 0.6f // slightly more elastic
            spring.stiffness = SpringForce.STIFFNESS_MEDIUM
        }.start()
    }

    /**
     * Staggered Spring Pop-in for lists or floating buttons.
     */
    fun popIn(view: View, delay: Long = 0) {
        view.scaleX = 0f
        view.scaleY = 0f
        view.postDelayed({
            applySpringScale(view, 1.0f)
        }, delay)
    }
}
