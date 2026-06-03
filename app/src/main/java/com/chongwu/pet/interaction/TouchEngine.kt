package com.chongwu.pet.interaction

import com.chongwu.pet.PetType
import kotlin.math.*

class TouchEngine {

    enum class BodyPart { NONE, HEAD, BODY, LEG, TAIL, HORN, FIN }

    data class PartRegion(val part: BodyPart, val cx: Float, val cy: Float, val radius: Float)

    private val sheepRegions = listOf(
        PartRegion(BodyPart.HEAD, 0.5f, 0.35f, 0.12f),
        PartRegion(BodyPart.BODY, 0.5f, 0.55f, 0.2f),
        PartRegion(BodyPart.LEG, 0.5f, 0.78f, 0.08f),
        PartRegion(BodyPart.TAIL, 0.5f, 0.88f, 0.06f),
        PartRegion(BodyPart.HORN, 0.5f, 0.25f, 0.08f)
    )

    private val fishRegions = listOf(
        PartRegion(BodyPart.HEAD, 0.55f, 0.45f, 0.12f),
        PartRegion(BodyPart.BODY, 0.45f, 0.55f, 0.18f),
        PartRegion(BodyPart.TAIL, 0.2f, 0.55f, 0.1f),
        PartRegion(BodyPart.FIN, 0.4f, 0.7f, 0.06f)
    )

    fun detectPart(screenX: Float, screenY: Float, viewW: Float, viewH: Float, petType: PetType): BodyPart {
        val nx = screenX / viewW; val ny = screenY / viewH
        val regions = if (petType == PetType.SHEEP) sheepRegions else fishRegions
        var closestDist = Float.MAX_VALUE
        var closestPart = BodyPart.NONE
        for (region in regions) {
            val dx = nx - region.cx; val dy = ny - region.cy
            val dist = sqrt(dx * dx + dy * dy)
            if (dist < region.radius && dist < closestDist) {
                closestDist = dist; closestPart = region.part
            }
        }
        return closestPart
    }

    data class TouchFeedback(val intensity: Float, val emoji: String, val effect: String)

    fun getFeedback(part: BodyPart, petType: PetType): TouchFeedback {
        if (petType == PetType.FISH) {
            return when (part) {
                BodyPart.HEAD -> TouchFeedback(0.3f, "🥰", "hearts")
                BodyPart.BODY -> TouchFeedback(0.2f, "✨", "bubbles")
                BodyPart.TAIL -> TouchFeedback(0.5f, "💨", "speed")
                BodyPart.FIN -> TouchFeedback(0.2f, "~", "ripple")
                else -> TouchFeedback(0f, "", "")
            }
        }
        return when (part) {
            BodyPart.HEAD -> TouchFeedback(0.3f, "😊", "hearts")
            BodyPart.BODY -> TouchFeedback(0.2f, "😊", "blush")
            BodyPart.LEG -> TouchFeedback(0.3f, "!!", "exclamation")
            BodyPart.TAIL -> TouchFeedback(0.6f, "🎵", "notes")
            BodyPart.HORN -> TouchFeedback(0.4f, "✨", "stars")
            else -> TouchFeedback(0f, "", "")
        }
    }
}
