package com.pan.plugin

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.koushikdutta.quack.QuackContext


class SVGOAction : AnAction() {


    companion object {

        private var jsContent: String? = null
        private var runJSContent: String? = null


        fun loadScripts() {

            if (jsContent == null) {
                jsContent =
                    SVGOAction::class.java
                        .getResourceAsStream(
                            "/web/svgo.browser.js"
                        )
                        ?.bufferedReader()
                        ?.use {
                            it.readText()
                        }
            }


            if (runJSContent == null) {
                runJSContent =
                    SVGOAction::class.java
                        .getResourceAsStream(
                            "/web/run.js"
                        )
                        ?.bufferedReader()
                        ?.use {
                            it.readText()
                        }
            }
        }
    }


    data class OptimizeResult(
        val psiFile: PsiFile,
        val content: String
    )



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


        val config =
            stringify(options)


        val files =
            e.getData(
                CommonDataKeys.VIRTUAL_FILE_ARRAY
            )
            ?: return


        val svgFiles =
            files
                .flatMap {
                    collectSvgFiles(it)
                }
                .distinctBy {
                    it.path
                }


        optimize(
            project,
            svgFiles,
            config
        )
    }



    private fun optimize(
        project: com.intellij.openapi.project.Project,
        files: List<VirtualFile>,
        config: String
    ) {


        ProgressManager
            .getInstance()
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
                            mutableListOf<OptimizeResult>()



                        val quack =
                            QuackContext.create()


                        try {


                            quack.evaluate(
                                "window={};$js;"
                            )


                            quack.evaluate(
                                runJs
                            )



                            files.forEachIndexed { index, file ->


                                indicator.fraction =
                                    index.toDouble()
                                        /
                                    files.size



                                indicator.text =
                                    "Optimizing ${file.name}"



                                /*
                                 * PSI读取必须在ReadAction
                                 */
                                val svgText =
                                    ReadAction.compute<String?, RuntimeException> {


                                        val psiFile =
                                            PsiManager
                                                .getInstance(project)
                                                .findFile(file)
                                                ?: return@compute null


                                        psiFile.text
                                    }
                                        ?: return@forEachIndexed



                                val escaped =
                                    escapeSvg(
                                        svgText
                                    )


                                val result =
                                    quack.evaluate(
                                        "optimizeSvg(\"$escaped\",$config)"
                                    )
                                        as? String
                                        ?: return@forEachIndexed



                                val psiFile =
                                    ReadAction.compute<PsiFile?, RuntimeException> {

                                        PsiManager
                                            .getInstance(project)
                                            .findFile(file)

                                    }
                                        ?: return@forEachIndexed



                                results.add(
                                    OptimizeResult(
                                        psiFile,
                                        result
                                    )
                                )
                            }



                        } finally {

                            quack.close()

                        }



                        ApplicationManager
                            .getApplication()
                            .invokeLater {


                                WriteCommandAction
                                    .runWriteCommandAction(
                                        project
                                    ) {


                                        results.forEach {


                                            val document =
                                                PsiDocumentManager
                                                    .getInstance(project)
                                                    .getDocument(
                                                        it.psiFile
                                                    )
                                                    ?: return@forEach



                                            document.setText(
                                                it.content
                                            )


                                            PsiDocumentManager
                                                .getInstance(project)
                                                .commitDocument(
                                                    document
                                                )
                                        }
                                    }
                            }
                    }
                }
            )
    }



    private fun collectSvgFiles(
        file: VirtualFile
    ): List<VirtualFile> {


        if (!file.isDirectory) {

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
            ) {


                if (
                    !it.isDirectory &&
                    isSvg(it)
                ) {
                    result.add(it)
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
                true
            )
            ?: false
    }



    private fun escapeSvg(
        value: String
    ): String {

        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "")
    }
}
