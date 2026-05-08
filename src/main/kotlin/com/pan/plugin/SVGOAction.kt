package com.pan.plugin

import SvgOption
import SvgSettingsDialog
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.dokar.quickjs.*
import com.google.gson.Gson
import com.intellij.openapi.actionSystem.CommonDataKeys
import kotlinx.coroutines.*

fun stringify(json: List<SvgOption>): String {
    val gson = Gson()
    return gson.toJson(json)
}

class SVGOAction : AnAction() {
    fun execute(e: AnActionEvent, str: String) {
        val project = e.project ?: return
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return

        // 读取 HTML
        val jsStream = javaClass.getResourceAsStream("/web/svgo.browser.js")
        val jsContent = jsStream?.bufferedReader()?.use { it.readText() } ?: return

        val runJSStream = javaClass.getResourceAsStream("/web/run.js")
        val runJSContent = runJSStream?.bufferedReader()?.use { it.readText() } ?: return

        CoroutineScope(Dispatchers.Default).launch {

            QuickJs.create(Dispatchers.Default).use { js ->

                val result = js.evaluate<String>("${jsContent};${runJSContent};optimizeSvg(${psiFile.text},${str})")
                println("xxx${result}")

            }

        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val ins = GlobalConfigService.getInstance()

        ins.restore();

        val optimizeOptions = ins
            .state
            .optimizeOptions
        val options = optimizeOptions.map { (key, checked) ->
            SvgOption(key, key, checked)
        }
        val dialog = SvgSettingsDialog(options)
        if (dialog.showAndGet()) {
            val result = dialog.selectedOptions;
            val target = GlobalConfigService.getInstance()
                .state
                .optimizeOptions

            target.clear()
            result.forEach {
                target[it.key] = it.checked
            }
            val configStr = stringify(result);
            execute(e, configStr)
        }

    }
}