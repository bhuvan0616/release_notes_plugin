package dev.bhuvan.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import dev.bhuvan.dialog.ReleaseDialog

class CreateRelease : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        e.project.let {
            if (it != null) ReleaseDialog(it).show()
        }
    }
}