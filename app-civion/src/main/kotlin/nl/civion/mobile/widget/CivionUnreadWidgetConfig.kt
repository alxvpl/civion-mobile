package nl.civion.mobile.widget

import app.k9mail.feature.widget.unread.UnreadWidgetConfig
import nl.civion.mobile.widget.provider.UnreadWidgetProvider

class CivionUnreadWidgetConfig : UnreadWidgetConfig {
    override val providerClass = UnreadWidgetProvider::class.java
}
