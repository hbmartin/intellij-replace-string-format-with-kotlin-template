package com.github.hbmartin.replacestringformatwithtemplate

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.executeCommand
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.formatter.commitAndUnblockDocument
import org.jetbrains.kotlin.idea.inspections.RemoveCurlyBracesFromTemplateInspection
import org.jetbrains.kotlin.idea.inspections.findExistingEditor
import org.jetbrains.kotlin.psi.KtBlockStringTemplateEntry
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType

object ReplaceEngine {
    private val splitAt = "(?<!%)%\\w".toRegex()
    private val removeCurlyBracesFromTemplateInspection = RemoveCurlyBracesFromTemplateInspection()

    internal fun replaceFormatWithTemplate(dotQualExpr: KtDotQualifiedExpression) {
        val callExpr = dotQualExpr.getChildOfType<KtCallExpression>()
        callExpr?.getChildOfType<KtValueArgumentList>()?.let { args ->
            val splitFormatting = args.arguments.first().node.text.split(splitAt)
            val templatedStringParts = splitFormatting.mapIndexed { index, el ->
                args.arguments.getOrNull(index + 1)?.let { arg ->
                    "$el\${${arg.node.text}}"
                } ?: el
            }
            val templatedString = templatedStringParts.joinToString(separator = "").replace("%%", "%")
            dotQualExpr.replaceWithTemplateString(templatedString)
        }
    }

    private fun PsiElement.replaceWithTemplateString(template: String) {
        val app = ApplicationManager.getApplication()

        app.invokeLater {
            executeCommand(project = project) {
                app.runWriteAction {
                    val expression = KtPsiFactory(this.project).createExpression(template)
                    expression.getChildrenOfType<KtBlockStringTemplateEntry>().forEach {
                        removeCurlyBracesFromTemplateInspection.applyTo(it, project, this.findExistingEditor())
                    }

                    this.replace(expression)
                    this.containingFile.commitAndUnblockDocument()
                }
            }
        }
    }
}
