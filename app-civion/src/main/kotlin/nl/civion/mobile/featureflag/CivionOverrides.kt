package nl.civion.mobile.featureflag

import net.thunderbird.core.featureflag.model.AppVariantOverrides
import net.thunderbird.core.featureflag.model.BaseAppVariantOverrides
import net.thunderbird.core.featureflag.model.FlagOverrides

/**
 * CIVION Mobile app variant feature flag overrides.
 *
 * CIVION Mobile occupies the K-9 derived variant slot of the flag registry; the Thunderbird slot is
 * bound to [net.thunderbird.core.featureflag.model.EmptyAppVariantOverride].
 */
class CivionOverrides(wrapper: Map<String, FlagOverrides>) : BaseAppVariantOverrides(wrapper) {
    companion object {
        val Factory = AppVariantOverrides.Factory { wrapper -> CivionOverrides(wrapper) }
    }
}
