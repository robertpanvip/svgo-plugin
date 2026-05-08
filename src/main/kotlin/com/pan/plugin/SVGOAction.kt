package com.pan.plugin

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import org.graalvm.polyglot.*

class SVGOAction : AnAction() {
    init {
        templatePresentation.text = SVGOBundle.message("svgo.action.run.name")
        templatePresentation.description = SVGOBundle.message("svgo.action.run.description")
    }
    
    companion object {
        // 预创建并复用 Engine，避免重复初始化开销
        private val engine: Engine by lazy {
            Engine.newBuilder()
                .allowExperimentalOptions(true)
                .option("engine.WarnInterpreterOnly", "false")
                .build()
        }
        
        // 预加载 JS 脚本
        private var jsContent: String? = null
        private var runJSContent: String? = null
        
        fun loadScripts() {
            if (jsContent == null) {
                jsContent = SVGOAction::class.java.getResourceAsStream("/web/svgo.browser.js")
                    ?.bufferedReader()?.use { it.readText() }
            }
            if (runJSContent == null) {
                runJSContent = SVGOAction::class.java.getResourceAsStream("/web/run.js")
                    ?.bufferedReader()?.use { it.readText() }
            }
        }
    }
    
    fun execute(e: AnActionEvent, str: String) {
        val project = e.project ?: return
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return
        val virtualFile = psiFile.virtualFile ?: return

        // 预加载脚本
        loadScripts()
        val jsContent = jsContent ?: return
        val runJSContent = runJSContent ?: return

        val svgContent = psiFile.text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "")
        
        // 使用后台任务执行优化，带进度条
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, t("svgo.progress.title"), false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = false
                indicator.fraction = 0.0
                indicator.text = t("svgo.progress.creating_context")
                
                // 复用 Engine 创建 Context，大幅提升性能
                val context = Context.newBuilder()
                    .engine(engine)
                    .build()
                
                try {
                    indicator.fraction = 0.3
                    indicator.text = t("svgo.progress.optimizing")
                    
                    val result: String = context.eval("js", "window={};${jsContent};${runJSContent};optimizeSvg(\"${svgContent}\",${str})").asString()
                    
                    indicator.fraction = 0.7
                    indicator.text = t("svgo.progress.saving")
                    
                    // 将优化后的内容写回文件
                    WriteCommandAction.runWriteCommandAction(project) {
                        VfsUtil.saveText(virtualFile, result)
                    }
                    
                    indicator.fraction = 1.0
                    indicator.text = t("svgo.progress.completed")
                    
                    println(t("svgo.log.completed"))
                } finally {
                    context.close()
                }
            }
        })
    }

    override fun actionPerformed(e: AnActionEvent) {
        val ins = GlobalConfigService.getInstance()
        ins.restore()

        val optimizeOptions = ins.state.optimizeOptions
        val options = optimizeOptions.map { (key, checked) ->
            SvgOption(key, key, checked)
        }
        
        val configStr = com.pan.plugin.stringify(options)
        execute(e, configStr)
    }
}