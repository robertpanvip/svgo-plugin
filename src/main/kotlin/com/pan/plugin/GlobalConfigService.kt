package com.pan.plugin

import SvgOption
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.State

object SvgOptimizeDefaults {

    val OPTIONS: List<SvgOption> = listOf(
        SvgOption("cleanupAttrs", "Clean up attributes",true),
        SvgOption("cleanupEnableBackground", "Clean up enable background",true),
        SvgOption("cleanupIds", "Clean up IDs",true),
        SvgOption("cleanupListOfValues", "Round numeric values in lists", false),
        SvgOption("cleanupNumericValues", "Round numeric values",true),
        SvgOption("collapseGroups", "Collapse groups",true),
        SvgOption("moveElemsAttrsToGroup", "Move element attributes to groups",true),
        SvgOption("moveGroupAttrsToElems", "Move group attributes to elements",true),
        SvgOption("mergePaths", "Merge paths",true),
        SvgOption("reusePaths", "Reuse paths", false),
        SvgOption("sortAttrs", "Sort attributes", false),
        SvgOption("sortDefsChildren", "Sort children of <defs>",true),
        SvgOption("prefixIds", "Prefix IDs with classname", false),
        SvgOption("convertColors", "Convert colors to RGB",true),
        SvgOption("convertPathData", "Convert path data",true),
        SvgOption("convertShapeToPath", "Convert shapes to paths",true),
        SvgOption("convertStyleToAttrs", "Convert styles to attributes",true),
        SvgOption("convertTransform", "Convert transforms",true),
        SvgOption("inlineStyles", "Inline styles",true),
        SvgOption("mergeStyles", "Merge styles",true),
        SvgOption("minifyStyles", "Minify styles",true),
        SvgOption("removeComments", "Remove comments",true),
        SvgOption("removeDesc", "Remove <desc>",true),
        SvgOption("removeDimensions", "Remove dimensions", false),
        SvgOption("removeDoctype", "Remove doctype",true),
        SvgOption("removeEditorsNSData", "Remove namespace",true),
        SvgOption("removeEmptyAttrs", "Remove empty attributes",true),
        SvgOption("removeEmptyText", "Remove empty text",true),
        SvgOption("removeHiddenElems", "Remove hidden elements",true),
        SvgOption("removeNonInheritableGroupAttrs", "Remove non-inheritable groups",true),
        SvgOption("removeOffCanvasPaths", "Remove elements outside viewbox", false),
        SvgOption("removeRasterImages", "Remove raster images", false),
        SvgOption("removeScripts", "Remove <script>",true),
        SvgOption("removeStyleElement", "Remove <style>",true),
        SvgOption("removeTitle", "Remove <title>",true),
        SvgOption("removeUnknownsAndDefaults", "Remove unknown content",true),
        SvgOption("removeUnusedNS", "Remove unused namespaces",true),
        SvgOption("removeUselessDefs", "Remove <defs> w/out <id>",true),
        SvgOption("removeUselessStrokeAndFill", "Remove unused stroke and fill",true),
        SvgOption("removeViewBox", "Remove viewBox", false),
        SvgOption("removeXMLProcInst", "Remove XML processing instructions",true)
    )
}

@State(
    name = "SvgOptimizeConfig",
    storages = [Storage("svg-optimize.xml")]
)
@Service(Service.Level.APP)
class GlobalConfigService :
    PersistentStateComponent<GlobalConfigService.State> {

    data class State(
        var optimizeOptions: MutableMap<String, Boolean> = mutableMapOf()
    )

    private var state = State()

    override fun getState(): State = state
    override fun loadState(state: State) {
        this.state = state
    }

    fun reset() {
        val result = mutableMapOf<String, Boolean>()

        SvgOptimizeDefaults.OPTIONS.forEach { opt ->
            result[opt.key] = opt.checked
        }
        this.state.optimizeOptions = result;
    }

    fun restore() {
        val saved = state.optimizeOptions
        val result = mutableMapOf<String, Boolean>()

        SvgOptimizeDefaults.OPTIONS.forEach { opt ->
            result[opt.key] = saved[opt.key] ?: opt.checked
        }
        this.state.optimizeOptions = result;
    }

    companion object {
        fun getInstance() =
            ApplicationManager.getApplication()
                .getService(GlobalConfigService::class.java)
    }
}
