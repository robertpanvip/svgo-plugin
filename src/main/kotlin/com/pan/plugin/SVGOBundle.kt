package com.pan.plugin

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.PropertyKey

class SVGOBundle : DynamicBundle("messages.SVGOBundle") {
    companion object {
        private val INSTANCE = SVGOBundle();
        fun message(@PropertyKey(resourceBundle = "messages.SVGOBundle") key: String, vararg params: Any) = INSTANCE.getMessage(key, *params)
    }
}
fun t(@PropertyKey(resourceBundle = "messages.SVGOBundle") key: String, vararg params: Any): @Nls String {
    return SVGOBundle.message(key, *params)
}