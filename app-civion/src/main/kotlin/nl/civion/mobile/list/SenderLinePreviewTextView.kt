package nl.civion.mobile.list

import android.content.Context
import android.text.SpannableStringBuilder
import android.util.AttributeSet
import com.google.android.material.textview.MaterialTextView

/**
 * The message list's preview text, with the sender on a line of its own.
 *
 * The engine hands this view one string - `sender – preview` - and then styles the sender part
 * by its length. This view keeps that contract and only turns the separator into a line break,
 * so the row reads subject / sender / preview, one line each, which is the accepted Inbox shape
 * (2026-09-12). The sender span is unaffected, since it covers the characters before the
 * separator; the preview span is applied by the engine after this and measures the text as it
 * then is.
 *
 * Referenced from the row layout override; nothing in upstream is touched.
 */
class SenderLinePreviewTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : MaterialTextView(context, attrs) {

    override fun setText(text: CharSequence?, type: BufferType?) {
        val separatorAt = text?.indexOf(SEPARATOR) ?: -1
        if (separatorAt <= 0) {
            super.setText(text, type)
            return
        }

        val withLineBreak = SpannableStringBuilder(text).replace(separatorAt, separatorAt + SEPARATOR.length, "\n")
        super.setText(withLineBreak, type)
    }

    private companion object {
        /** What `MessageViewHolder` puts between the sender and the preview. */
        const val SEPARATOR = " – "
    }
}
