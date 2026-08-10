package com.pan.plugin

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
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
                        ?.use {
                            it.readText()
                        }
            }


            if (runJSContent == null) {
                runJSContent =
                    SVGOAction::class.java
                        .getResourceAsStream("/web/run.js")
                        ?.bufferedReader()
                        ?.use {
                            it.readText()
                        }
            }
        }
    }



    override fun actionPerformed(
        e: AnActionEvent
    ) {

        val project =
            e.project ?: return


        val service =
            GlobalStateConfigService.getInstance()

        service.restore()


        val options =
            service.state.optimizeOptions.map { (key, checked) ->

                SvgOption(
                    key,
                    key,
                    checked
                )
            }


        val configStr =
            stringify(options)



        val selectedFiles =
            e.getData(
                CommonDataKeys.VIRTUAL_FILE_ARRAY
            )
            ?: return



        val svgFiles =
            selectedFiles
                .flatMap {
                    collectSvgFiles(it)
                }
                .distinctBy {
                    it.path
                }



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


        ProgressManager.getInstance()
            .run(
                object : Task.Backgroundable(
                    project,
                    "SVGO",
                    false
                ) {


                    override fun run(
                        indicator: ProgressIndicator
                    ) {


                        loadScripts()


                        val js =
                            jsContent ?: return


                        val runJs =
                            runJSContent ?: return



                        val results =
                            mutableListOf<Pair<VirtualFile, String>>()



                        val quack =
                            QuackContext.create()



                        try {


                            // 初始化一次
                            quack.evaluate(
                                "window={};$js;"
                            )


                            quack.evaluate(runJs)



                            files.forEachIndexed { index, file ->


                                indicator.fraction =
                                    index.toDouble()
                                        /
                                    files.size



                                indicator.text =
                                    "Optimizing ${file.name}"



                                val psiFile =
                                    PsiManager
                                        .getInstance(project)
                                        .findFile(file)
                                        ?: return@forEachIndexed



                                val svg =
                                    escapeSvg(
                                        psiFile.text
                                    )



                                val result =
                                    quack.evaluate(
                                        """optimizeSvg("$svg", $configStr)"""
                                    )
                                        as String



                                results.add(
                                    file to result
                                )
                            }


                        } finally {

                            quack.close()

                        }



                        // 一次写入
                        ApplicationManager
                            .getApplication()
                            .invokeLater {


                                WriteCommandAction
                                    .runWriteCommandAction(
                                        project,
                                        "SVGO",
                                        null,
                                        {

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
                                    )
                            }
                    }
                }
            )
    }





    private fun collectSvgFiles(
        file: VirtualFile
    ): List<VirtualFile> {


        if (file.isFile) {

            return if (isSvg(file)) {

                listOf(file)

            } else {

                emptyList()

            }
        }



        val result =
            mutableListOf<VirtualFile>()



        VfsUtilCore
            .iterateChildrenRecursively(
                file,
                null
            ) { child ->


                if (
                    child.isFile &&
                    isSvg(child)
                ) {

                    result.add(child)
                }


                true
            }



        return result
    }





    private fun isSvg(
        file: VirtualFile
    ): Boolean {

        return file.extension
            ?.equals(
                "svg",
                ignoreCase = true
            )
            ?: false
    }





    private fun escapeSvg(
        text: String
    ): String {

        return text
            .replace(
                "\\",
                "\\\\"
            )
            .replace(
                "\"",
                "\\\""
            )
            .replace(
                "\n",
                "\\n"
            )
            .replace(
                "\r",
                ""
            )
    }
}
