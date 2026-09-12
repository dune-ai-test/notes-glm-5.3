package com.fieldnotes.app.util

import android.content.Context
import android.content.ContextWrapper
import androidx.fragment.app.FragmentActivity

/** Walks the context wrapper chain to the hosting [FragmentActivity]. */
tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}
