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
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.koushikdutta.quack.QuackContext

class SVGOAction : AnAction() {

    init {
        templatePresentation.text =
            SVGOBundle.message("svgo.action.run.name")

        templatePresentation.description =
            SVGOBundle.message("svgo.action.run.description")
    }

    companion object {

        private var jsContent: String? = null
        private var runJSContent: String? = null

        fun loadScripts() {
            if (jsContent == null) {
                jsContent =
                    SVGOAction::class.java
                        .getResourceAsStream("/web/svgo.browser.js")
                        ?.bufferedReader()
                        ?.use { it.readText() }
            }

            if (runJSContent == null) {
                runJSContent =
                    SVGOAction::class.java
                        .getResourceAsStream("/web/run.js")
                        ?.bufferedReader()
                        ?.use { it.readText() }
            }
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val service = GlobalStateConfigService.getInstance()
        service.restore()

        val options = service.state.optimizeOptions.map { (key, checked) ->
            SvgOption(
                key,
                key,
                checked
            )
        }

        val configStr = stringify(options)

        val selectedFiles =
            e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
                ?: return

        val svgFiles = selectedFiles
            .flatMap { collectSvgFiles(it) }
            .distinctBy { it.path }

        if (svgFiles.isEmpty()) {
            return
        }

        optimizeFiles(
            project,
            svgFiles,
            configStr
        )
    }

    private fun optimizeFiles(
        project: com.intellij.openapi.project.Project,
        files: List<VirtualFile>,
        configStr: String
    ) {
        ProgressManager.getInstance().run(
            object : Task.Backgroundable(
                project,
                t("svgo.progress.title"),
                false
            ) {

                override fun run(indicator: ProgressIndicator) {

                    loadScripts()

                    val script = jsContent ?: return
                    val runner = runJSContent ?: return

                    indicator.isIndeterminate = false
                    indicator.fraction = 0.0
                    indicator.text =
                        t("svgo.progress.creating_context")

                    val quack = QuackContext.create()

                    try {
                        /*
                         * 只创建一次 JS Runtime
                         */
                        quack.evaluate(
                            "window={};$script;"
                        )

                        quack.evaluate(runner)

                        val results =
                            mutableListOf<Pair<VirtualFile, String>>()

                        files.forEachIndexed { index, file ->

                            if (indicator.isCanceled) {
                                return
                            }

                            indicator.fraction =
                                index.toDouble() / files.size

                            indicator.text =
                                "${t("svgo.progress.optimizing")}: ${file.name}"

                            val psiFile =
                                PsiManager
                                    .getInstance(project)
                                    .findFile(file)
                                    ?: return@forEachIndexed

                            /*
                             * SVG 内容转义
                             */
                            val svgContent =
                                escapeSvg(psiFile.text)

                            /*
                             * 不使用 Kotlin 多行字符串嵌套 JS，
                             * 避免 Kotlin 解析问题。
                             */
                            val scriptCall =
                                "optimizeSvg(\"$svgContent\",$configStr)"

                            val result =
                                quack.evaluate(scriptCall) as? String
                                    ?: return@forEachIndexed

                            results.add(
                                file to result
                            )
                        }

                        if (results.isEmpty()) {
                            return
                        }

                        indicator.fraction = 0.9
                        indicator.text =
                            t("svgo.progress.saving")

                        /*
                         * 回到 EDT 后一次性写入
                         */
                        ApplicationManager
                            .getApplication()
                            .invokeLater {

                                WriteCommandAction.runWriteCommandAction(
                                    project
                                ) {

                                    results.forEach { (file, content) ->

                                        val psiFile =
                                            PsiManager
                                                .getInstance(project)
                                                .findFile(file)
                                                ?: return@forEach

                                        val document =
                                            PsiDocumentManager
                                                .getInstance(project)
                                                .getDocument(psiFile)
                                                ?: return@forEach

                                        document.setText(content)

                                        PsiDocumentManager
                                            .getInstance(project)
                                            .commitDocument(document)
                                    }
                                }

                                indicator.fraction = 1.0
                                indicator.text =
                                    t("svgo.progress.completed")

                                println(
                                    t("svgo.log.completed")
                                )
                            }

                    } finally {
                        quack.close()
                    }
                }
            }
        )
    }

    /**
     * 收集 SVG 文件
     *
     * 支持：
     *
     * SVG 文件
     * 文件夹
     * 多选文件
     * 多选文件夹
     */
    private fun collectSvgFiles(
        file: VirtualFile
    ): List<VirtualFile> {

        /*
         * 不使用 file.isFile。
         *
         * IntelliJ VirtualFile 判断文件/目录，
         * 直接使用 isDirectory。
         */
        if (!file.isDirectory) {
            return if (isSvg(file)) {
                listOf(file)
            } else {
                emptyList()
            }
        }

        val result =
            mutableListOf<VirtualFile>()

        VfsUtilCore.iterateChildrenRecursively(
            file,
            null
        ) { child ->

            /*
             * 只要不是目录，就是文件。
             */
            if (!child.isDirectory && isSvg(child)) {
                result.add(child)
            }

            true
        }

        return result
    }

    private fun isSvg(
        file: VirtualFile
    ): Boolean {
        return file.extension?.equals(
            "svg",
            ignoreCase = true
        ) ?: false
    }

    private fun escapeSvg(
        text: String
    ): String {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "")
    }
}
