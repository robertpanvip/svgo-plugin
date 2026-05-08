package com.pan.plugin

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.DefaultActionGroup

class SVGOGroup : DefaultActionGroup() {
    
    init {
        templatePresentation.text = "SVGO"
        templatePresentation.description = "SVGO 工具"
        
        // 添加子动作
        add(SVGOAction())
        add(SVGOConfigAction())
    }
    
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
    
    override fun update(e: AnActionEvent) {
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        val isSvg = virtualFile?.extension?.equals("svg", ignoreCase = true) == true
        e.presentation.isVisible = isSvg
    }
}
