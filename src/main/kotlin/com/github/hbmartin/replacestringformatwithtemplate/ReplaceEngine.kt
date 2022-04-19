package com.github.hbmartin.replacestringformatwithtemplate

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.executeCommand
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.formatter.commitAndUnblockDocument
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType

object ReplaceEngine {
    private val splitAt = "%\\w".toRegex()

    internal fun replaceFormatWithTemplate(dotQualExpr: KtDotQualifiedExpression) {
        val callExpr = dotQualExpr.getChildOfType<KtCallExpression>()
        callExpr?.getChildOfType<KtValueArgumentList>()?.let { args ->
            println(args.arguments.first().node.text)
            val splitFormatting = args.arguments.first().node.text.split(splitAt)
            println(splitFormatting)
            val templatedStringParts = splitFormatting.mapIndexed { index, el ->
                args.arguments.getOrNull(index + 1)?.let { arg ->
                    "$el\${${arg.node.text}}"
                } ?: el
            }
            val templatedString = templatedStringParts.joinToString(separator = "").replace("%%", "%")
            println(templatedString)
            dotQualExpr.replaceWithTemplateString(templatedString)
        }
    }

    private fun PsiElement.replaceWithTemplateString(template: String) {
        val app = ApplicationManager.getApplication()

        app.invokeLater {
            executeCommand(project = project) {
                app.runWriteAction {
                    this.replace(
                        KtPsiFactory(this.project).createExpression(template)
                    )
                    this.containingFile.commitAndUnblockDocument()
                }
            }
        }
    }
}
