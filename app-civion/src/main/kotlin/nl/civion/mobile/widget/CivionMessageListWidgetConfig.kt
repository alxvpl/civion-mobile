package nl.civion.mobile.widget

import app.k9mail.feature.widget.message.list.MessageListWidgetConfig
import nl.civion.mobile.widget.provider.MessageListWidgetProvider

class CivionMessageListWidgetConfig : MessageListWidgetConfig {
    override val providerClass = MessageListWidgetProvider::class.java
}
