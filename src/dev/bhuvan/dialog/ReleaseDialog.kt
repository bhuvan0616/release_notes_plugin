package dev.bhuvan.dialog

import com.google.gson.Gson
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import dev.bhuvan.util.exec
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.GridLayout
import java.io.File
import javax.swing.JComponent
import javax.swing.JPanel

class ReleaseDialog(private val project: Project) : DialogWrapper(project, true) {
    private val root by lazy { project.basePath }

    private val contentPanel = JPanel(GridLayout(2, 0)).apply {
        preferredSize = Dimension(600, 400)
    }

    val featuresText = JBTextArea().apply {
        border = JBUI.Borders.empty(5, 5, 5, 5)
    }

    val bugsText = JBTextArea().apply {
        border = JBUI.Borders.empty(5, 5, 5, 5)
    }

    init {
        init()
        title = "Release Notes"
    }

    override fun createCenterPanel(): JComponent? {
        val featuresBlock = JPanel(BorderLayout())
        val disclimer = JBLabel("Use * followed by space to separate the lines.").apply {
            fontColor = UIUtil.FontColor.NORMAL
            font = JBUI.Fonts.miniFont()
            border = JBUI.Borders.empty(5, 10)
        }

        val scrollPaneFeatures = JBScrollPane(featuresText).apply {
            border = JBUI.Borders.customLine(Color.GRAY, 1)
        }
        featuresBlock.add(getLabel("Features:"), BorderLayout.NORTH)
        featuresBlock.add(JBBox.createHorizontalStrut(10), BorderLayout.WEST)
        featuresBlock.add(JBBox.createHorizontalStrut(10), BorderLayout.EAST)
        featuresBlock.add(scrollPaneFeatures, BorderLayout.CENTER)

        val bugsBlock = JPanel(BorderLayout())

        val scrollPaneBugs = JBScrollPane(bugsText).apply {
            border = JBUI.Borders.customLine(Color.GRAY, 1)
        }
        bugsBlock.add(getLabel("Bugs:"), BorderLayout.NORTH)
        bugsBlock.add(JBBox.createHorizontalStrut(10), BorderLayout.WEST)
        bugsBlock.add(JBBox.createHorizontalStrut(10), BorderLayout.EAST)
        bugsBlock.add(disclimer, BorderLayout.SOUTH)
        bugsBlock.add(scrollPaneBugs, BorderLayout.CENTER)

        contentPanel.add(featuresBlock)
        contentPanel.add(bugsBlock)
        readAndDisplayReleaseNotes()
        return contentPanel
    }

    override fun doOKAction() {
        collectAndSaveData()
        super.doOKAction()

    }

    private fun collectAndSaveData() {
        try {
            val branch = (File(root) exec "git branch --show-current")
                    .replace("\n", "")
                    .replace("/", "_")
            val ciPath = File("${root}/.ci")
            if (!ciPath.exists()) {
                val isCreated = ciPath.mkdir()
            }
            val releaseNotes = File("$ciPath/${branch}_release.json")
            releaseNotes.createNewFile()
            releaseNotes.writeText(buildJsonString())
        } catch (e: Exception) {
            Messages.showDialog(project, e.message, "Unable to save release notes", arrayOf("OK"), 0, null)
        }
    }

    private fun buildJsonString(): String {
        return Gson().toJson(
                ReleaseNotesModel(
                        getFeaturesList(),
                        getBugsList(),
                        (File(root) exec "git config user.name").replace("\n", ""),
                        (File(root) exec "git config user.email").replace("\n", "")
                )
        )
    }

    private fun readAndDisplayReleaseNotes() {
        try {
            val branch = (File(root) exec "git branch --show-current")
                    .replace("\n", "")
                    .replace("/", "_")
            val releaseNotes = File("${root}/.ci/${branch}_release.json")
            if (releaseNotes.exists()) {
                val notes = Gson().fromJson(releaseNotes.readText(), ReleaseNotesModel::class.java)
                featuresText.text = notes.features.joinToString("") { "* $it\n" }
                bugsText.text = notes.bugs.joinToString("") { "* $it\n" }
            }
        } catch (e: Exception) {
            Messages.showDialog(project, e.message, "Unable to save release notes", arrayOf("OK"), 0, null)
        }
    }

    private fun getFeaturesList(): List<String> {
        return featuresText.text.parseList()
    }

    private fun getBugsList(): List<String> {
        return bugsText.text.parseList()
    }

    private fun String.parseList(): List<String> {
        val formatted = replace("\n", "").trim()
        return if (!formatted.isBlank()) {
            split("* ")
                    .map { it.trim().replace("\n", "") }
                    .filter { !it.isBlank() }
        } else emptyList()
    }


    private fun getLabel(text: String): JComponent {
        val label = JBLabel(text)
        label.componentStyle = UIUtil.ComponentStyle.SMALL
        label.fontColor = UIUtil.FontColor.BRIGHTER
        label.border = JBUI.Borders.empty(10, 5, 5, 0)
        return label
    }
}