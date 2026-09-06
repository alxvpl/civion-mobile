package nl.civion.mobile.featureflag

import net.thunderbird.core.featureflag.keys.GeneratedFeatureFlagKey
import net.thunderbird.core.featureflag.model.AppVariantOverrides
import net.thunderbird.core.featureflag.model.BaseAppVariantOverrides
import net.thunderbird.core.featureflag.model.FlagOverrides

/**
 * CIVION Mobile app variant feature flag overrides.
 *
 * CIVION Mobile occupies the Thunderbird slot of the flag registry, so it inherits the same feature
 * set and navigation as Thunderbird for Android; the K-9 slot is bound to
 * [net.thunderbird.core.featureflag.model.EmptyAppVariantOverride]. Only debug and release variants
 * exist here — CIVION has no daily or beta channel.
 *
 * The overrides deserialized from the bundled catalog's Thunderbird slot are deliberately discarded
 * and replaced by [CIVION_FLAGS]. Sharing the slot is a composition decision, not an agreement to
 * follow Thunderbird's channel configuration: without this, every override upstream adds to its own
 * debug channel would silently become CIVION behaviour. CIVION's effective flag set is therefore the
 * catalog defaults overlaid with exactly the values below.
 *
 * In debug builds the runtime override provider is consulted before this, so a flag toggled by hand
 * on the device still wins.
 */
class CivionOverrides(wrapper: Map<String, FlagOverrides>) : BaseAppVariantOverrides(wrapper) {
    companion object {
        private const val DEBUG = "debug"
        private const val RELEASE = "release"

        /**
         * Flags CIVION sets for itself, applied to every build variant.
         *
         * [GeneratedFeatureFlagKey.USE_COMPOSE_FOR_MESSAGE_LIST_ITEMS] renders the message list rows
         * with the Compose implementation, which takes its colours and typography from the CIVION
         * theme instead of the classic XML row. Upstream ships it disabled while the migration is in
         * progress; it is enabled here for evaluation and is reverted by removing this entry.
         */
        private val CIVION_FLAGS: FlagOverrides = mapOf(
            GeneratedFeatureFlagKey.USE_COMPOSE_FOR_MESSAGE_LIST_ITEMS.key to true,
        )

        val Factory = AppVariantOverrides.Factory {
            CivionOverrides(
                mapOf(
                    DEBUG to CIVION_FLAGS,
                    RELEASE to CIVION_FLAGS,
                ),
            )
        }
    }
}
