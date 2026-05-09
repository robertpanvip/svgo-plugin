package com.pan.plugin
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.google.gson.Gson

fun stringify(json: List<SvgOption>): String {
    val gson = Gson()
    return gson.toJson(json)
}

class SVGOConfigAction : AnAction() {
    init {
        templatePresentation.text = SVGOBundle.message("svgo.action.config.name")
        templatePresentation.description = SVGOBundle.message("svgo.action.config.description")
    }
    
    override fun actionPerformed(e: AnActionEvent) {
        val ins = GlobalStateConfigService.getInstance()
        ins.restore()

        val optimizeOptions = ins.state.optimizeOptions
        
        // 从默认配置中获取国际化的 label
        val options = SvgOptimizeDefaults.OPTIONS.map { default ->
            val checked = optimizeOptions[default.key] ?: default.checked
            SvgOption(default.key, default.label, checked)
        }
        
        val dialog = SvgSettingsDialog(options)
        if (dialog.showAndGet()) {
            val result = dialog.selectedOptions
            val target = GlobalStateConfigService.getInstance().state.optimizeOptions

            target.clear()
            result.forEach {
                target[it.key] = it.checked
            }
        }
    }
}
