package com.pan.plugin

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.psi.PsiDocumentManager
import com.koushikdutta.quack.QuackContext

class SVGOAction : AnAction() {
    init {
        templatePresentation.text = SVGOBundle.message("svgo.action.run.name")
        templatePresentation.description = SVGOBundle.message("svgo.action.run.description")
    }
    
    companion object {
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
                
                val quack = QuackContext.create()
                try {
                    indicator.fraction = 0.3
                    indicator.text = t("svgo.progress.optimizing")
                    
                    // 执行 JS 代码
                    quack.evaluate("window={};${jsContent};")
                    quack.evaluate(runJSContent)
                    
                    // 调用 optimizeSvg 函数
                    val result = quack.evaluate("optimizeSvg(\"$svgContent\", $str)") as String
                    
                    indicator.fraction = 0.7
                    indicator.text = t("svgo.progress.saving")
                    ApplicationManager.getApplication().invokeLater {
                        // 将优化后的内容写回文件
                        CommandProcessor.getInstance().executeCommand(
                            project,
                            {
                                WriteCommandAction.runWriteCommandAction(project) {
                                    val document = PsiDocumentManager
                                        .getInstance(project)
                                        .getDocument(psiFile)
                                        ?: return@runWriteCommandAction

                                    document.setText(result)

                                    PsiDocumentManager
                                        .getInstance(project)
                                        .commitDocument(document)
                                    indicator.fraction = 1.0
                                    indicator.text = t("svgo.progress.completed")

                                    println(t("svgo.log.completed"))
                                }
                            },
                            "SVGO",
                            null
                        )
                    }


                } finally {
                    quack.close()
                }
            }
        })
    }

    override fun actionPerformed(e: AnActionEvent) {
        val ins = GlobalStateConfigService.getInstance()
        ins.restore()

        val optimizeOptions = ins.state.optimizeOptions
        val options = optimizeOptions.map { (key, checked) ->
            SvgOption(key, key, checked)
        }
        
        val configStr = com.pan.plugin.stringify(options)
        execute(e, configStr)
    }
}