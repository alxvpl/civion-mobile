package nl.civion.mobile.provider

import android.content.Context
import com.fsck.k9.preferences.FilePrefixProvider
import net.thunderbird.core.common.provider.AppNameProvider
import net.thunderbird.core.common.provider.BrandNameProvider
import nl.civion.mobile.R

internal class CivionAppNameProvider(
    context: Context,
) : AppNameProvider, BrandNameProvider, FilePrefixProvider {
    override val appName: String by lazy {
        context.getString(R.string.app_name)
    }

    override val brandName: String by lazy {
        context.getString(R.string.app_name)
    }

    /**
     * Prefix for exported settings files. Distinct from `k9` and `thunderbird` so a CIVION export is
     * never mistaken for one produced by another app on the same device.
     */
    override val filePrefix: String = "civion"
}
