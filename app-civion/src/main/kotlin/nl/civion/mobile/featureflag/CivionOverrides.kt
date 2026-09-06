package nl.civion.mobile.featureflag

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
         * Empty: CIVION currently runs on the catalog defaults.
         *
         * `use_compose_for_message_list_items` was enabled here and evaluated on device in
         * `0.1.0.10`, then rejected. The Compose row replaces typography with chrome — unread became
         * a separate dot in its own gutter instead of a heavier subject, the sender showed the raw
         * address on a line of its own rather than the display name, and the monogram colours spread
         * accent across the list. The classic row carries the same information in less space and
         * reads better. Upstream ships the flag disabled and marks the bridge deprecated while the
         * migration is in progress; revisit when it is promoted, not before.
         */
        private val CIVION_FLAGS: FlagOverrides = emptyMap()

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
