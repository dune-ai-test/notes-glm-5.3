package com.fieldnotes.app.util

/** Picks the singular form when [count] is exactly 1, otherwise the plural. */
fun plural(count: Int, singular: String, plural: String = singular + "s"): String =
    if (count == 1) singular else plural
