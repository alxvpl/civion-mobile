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
 */
class CivionOverrides(wrapper: Map<String, FlagOverrides>) : BaseAppVariantOverrides(wrapper) {
    companion object {
        val Factory = AppVariantOverrides.Factory { wrapper -> CivionOverrides(wrapper) }
    }
}
