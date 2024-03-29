package me.haroldmartin.replacestringformatwithtemplate

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.executeCommand
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.formatter.commitAndUnblockDocument
import org.jetbrains.kotlin.idea.inspections.findExistingEditor
import org.jetbrains.kotlin.idea.intentions.RemoveUnnecessaryParenthesesIntention
import org.jetbrains.kotlin.psi.KtBlockStringTemplateEntry
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtParenthesizedExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType

object ReplaceEngine {
    private val splitAt = "(?<!%)%\\w".toRegex()

    // Looks like this is getting moved in 231, revisit with subsequent IJ releases
    // https://github.com/JetBrains/intellij-community/commit/0dc3935b3fd9577c55887e80392327c4fa48e974
    private val removeUnnecessaryParenthesesIntention = RemoveUnnecessaryParenthesesIntention()

    @Suppress("AvoidMutableCollections", "NestedScopeFunctions")
    internal fun replaceFormatWithTemplate(dotQualExpr: KtDotQualifiedExpression) {
        val callExpr = dotQualExpr.getChildOfType<KtCallExpression>()
        callExpr?.getChildOfType<KtValueArgumentList>()?.let { args ->
            if (args.arguments.size == 1) {
                @Suppress("AvoidFirstOrLastOnList")
                dotQualExpr.replaceWithTemplateString(args.arguments.first().node.text)
            } else {
                @Suppress("MaxChainedCallsOnSameLine")
                val splitFormatting = args.arguments.firstOrNull()?.run { node.text.split(splitAt) }
                val templatedStringParts = splitFormatting?.mapIndexed { index, el ->
                    args.arguments.getOrNull(index + 1)?.let { arg ->
                        "$el\${${arg.node.text}}"
                    } ?: el
                }
                val templatedString = templatedStringParts?.run {
                    joinToString(separator = "").replace("%%", "%")
                }
                templatedString?.let { dotQualExpr.replaceWithTemplateString(it) }
            }
        }
    }

    private fun PsiElement.replaceWithTemplateString(template: String) {
        val app = ApplicationManager.getApplication()

        app.invokeLater {
            executeCommand(project = project) {
                app.runWriteAction {
                    val expression = KtPsiFactory(this.project).createExpression(template)
                    expression.getChildrenOfType<KtBlockStringTemplateEntry>().forEach { blockStringEntry ->
                        blockStringEntry.children.firstOrNull()?.let { innerElement ->
                            @Suppress("NestedScopeFunctions")
                            when (innerElement) {
                                is KtStringTemplateExpression -> {
                                    innerElement
                                        .node
                                        .text
                                        .takeIf { it.length > 2 }
                                        ?.let { nodeText ->
                                            val innerText = nodeText.substring(1, nodeText.length - 1)

                                            blockStringEntry.replace(
                                                KtPsiFactory(this.project).createExpression(innerText),
                                            )
                                        }
                                }
                                is KtParenthesizedExpression -> {
                                    removeUnnecessaryParenthesesIntention
                                        .applyTo(
                                            element = innerElement,
                                            editor = this.findExistingEditor(),
                                        )
                                }
                            }
                        }
                    }

                    this.replace(expression)
                    this.containingFile.commitAndUnblockDocument()
                }
            }
        }
    }
}
