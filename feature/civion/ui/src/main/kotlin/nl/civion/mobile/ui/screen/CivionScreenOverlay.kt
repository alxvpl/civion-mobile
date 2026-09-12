package nl.civion.mobile.ui.screen

import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView

/**
 * A CIVION screen shown over the activity that owns the drawer.
 *
 * The screens the drawer leads to - the account list first - act on the same account and folder
 * callbacks the host activity hands the drawer, so they are drawn in that activity's window
 * rather than in one of their own: a ComposeView laid over the content view, taking the whole
 * window and the Back gesture while it is up. Nothing upstream is touched for it.
 */
internal class CivionScreenOverlay(private val activity: AppCompatActivity) {

    private val host: ViewGroup = activity.findViewById(android.R.id.content)
    private var view: ComposeView? = null

    private val backCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = hide()
    }

    init {
        activity.onBackPressedDispatcher.addCallback(activity, backCallback)
    }

    val isShowing: Boolean
        get() = view != null

    fun show(content: @Composable () -> Unit) {
        if (view != null) return

        view = ComposeView(activity).apply {
            // The screen is opaque and takes every touch, so nothing reaches what is under it.
            isClickable = true
            setContent(content)
        }.also { host.addView(it, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT) }
        backCallback.isEnabled = true
    }

    fun hide() {
        view?.let(host::removeView)
        view = null
        backCallback.isEnabled = false
    }
}
