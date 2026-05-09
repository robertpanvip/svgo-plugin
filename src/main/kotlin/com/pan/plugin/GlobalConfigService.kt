package com.pan.plugin
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.State

object SvgOptimizeDefaults {

    val OPTIONS: List<SvgOption> = listOf(
        SvgOption("cleanupAttrs", t("svgo.option.cleanupAttrs"), true),
        SvgOption("cleanupEnableBackground", t("svgo.option.cleanupEnableBackground"), true),
        SvgOption("cleanupIds", t("svgo.option.cleanupIds"), true),
        SvgOption("cleanupListOfValues", t("svgo.option.cleanupListOfValues"), false),
        SvgOption("cleanupNumericValues", t("svgo.option.cleanupNumericValues"), true),
        SvgOption("collapseGroups", t("svgo.option.collapseGroups"), true),
        SvgOption("moveElemsAttrsToGroup", t("svgo.option.moveElemsAttrsToGroup"), true),
        SvgOption("moveGroupAttrsToElems", t("svgo.option.moveGroupAttrsToElems"), true),
        SvgOption("mergePaths", t("svgo.option.mergePaths"), true),
        SvgOption("reusePaths", t("svgo.option.reusePaths"), false),
        SvgOption("sortAttrs", t("svgo.option.sortAttrs"), false),
        SvgOption("sortDefsChildren", t("svgo.option.sortDefsChildren"), true),
        SvgOption("prefixIds", t("svgo.option.prefixIds"), false),
        SvgOption("convertColors", t("svgo.option.convertColors"), true),
        SvgOption("convertPathData", t("svgo.option.convertPathData"), true),
        SvgOption("convertShapeToPath", t("svgo.option.convertShapeToPath"), true),
        SvgOption("convertStyleToAttrs", t("svgo.option.convertStyleToAttrs"), true),
        SvgOption("convertTransform", t("svgo.option.convertTransform"), true),
        SvgOption("inlineStyles", t("svgo.option.inlineStyles"), true),
        SvgOption("mergeStyles", t("svgo.option.mergeStyles"), true),
        SvgOption("minifyStyles", t("svgo.option.minifyStyles"), true),
        SvgOption("removeComments", t("svgo.option.removeComments"), true),
        SvgOption("removeDesc", t("svgo.option.removeDesc"), true),
        SvgOption("removeDimensions", t("svgo.option.removeDimensions"), false),
        SvgOption("removeDoctype", t("svgo.option.removeDoctype"), true),
        SvgOption("removeEditorsNSData", t("svgo.option.removeEditorsNSData"), true),
        SvgOption("removeEmptyAttrs", t("svgo.option.removeEmptyAttrs"), true),
        SvgOption("removeEmptyText", t("svgo.option.removeEmptyText"), true),
        SvgOption("removeHiddenElems", t("svgo.option.removeHiddenElems"), true),
        SvgOption("removeNonInheritableGroupAttrs", t("svgo.option.removeNonInheritableGroupAttrs"), true),
        SvgOption("removeOffCanvasPaths", t("svgo.option.removeOffCanvasPaths"), false),
        SvgOption("removeRasterImages", t("svgo.option.removeRasterImages"), false),
        SvgOption("removeScripts", t("svgo.option.removeScripts"), true),
        SvgOption("removeStyleElement", t("svgo.option.removeStyleElement"), true),
        SvgOption("removeTitle", t("svgo.option.removeTitle"), true),
        SvgOption("removeUnknownsAndDefaults", t("svgo.option.removeUnknownsAndDefaults"), true),
        SvgOption("removeUnusedNS", t("svgo.option.removeUnusedNS"), true),
        SvgOption("removeUselessDefs", t("svgo.option.removeUselessDefs"), true),
        SvgOption("removeUselessStrokeAndFill", t("svgo.option.removeUselessStrokeAndFill"), true),
        SvgOption("removeViewBox", t("svgo.option.removeViewBox"), false),
        SvgOption("removeXMLProcInst", t("svgo.option.removeXMLProcInst"), true),
        SvgOption("removeMetadata", t("svgo.option.removeMetadata"), true),
        SvgOption("convertEllipseToCircle", t("svgo.option.convertEllipseToCircle"), true),
        SvgOption("removeEmptyContainers", t("svgo.option.removeEmptyContainers"), true)
    )
}

@State(
    name = "SvgOOptimizeConfig",
    storages = [Storage("svgo-optimize.xml")]
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
