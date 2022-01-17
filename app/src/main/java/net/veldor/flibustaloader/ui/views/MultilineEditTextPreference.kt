package net.veldor.flibustaloader.ui.views

import android.content.Context
import androidx.preference.EditTextPreference
import android.util.AttributeSet


open class MultilineEditTextPreference : EditTextPreference {
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {}
    constructor(context: Context?) : super(context) {}

}