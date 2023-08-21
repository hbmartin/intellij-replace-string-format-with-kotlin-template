package me.haroldmartin.replacestringformatwithtemplate

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType

class ReplaceStringFormatWithKotlinStringTemplate : AnAction("Replace String.format with Kotlin String Template") {
    override fun actionPerformed(anActionEvent: AnActionEvent) {
        val details = EventDetails(actionEvent = anActionEvent)

        details.element?.parentOfType<KtDotQualifiedExpression>(withSelf = true)?.let { dotQualExpr ->
            ReplaceEngine.replaceFormatWithTemplate(dotQualExpr)
        }
    }

    override fun update(e: AnActionEvent) {
        val details = EventDetails(actionEvent = e)
        @Suppress("UnnecessaryParentheses")
        e.presentation.isEnabled = details.isKotlinFile && (details.element?.isEligible() == true)
    }
}

@Suppress("ReturnCount")
private fun PsiElement.isEligible(): Boolean =
    this.parentOfType<KtDotQualifiedExpression>(withSelf = true)?.let { dotQualExpr ->
        val firstNameRefExpr = dotQualExpr.getChildOfType<KtNameReferenceExpression>()
        if (firstNameRefExpr?.getReferencedName() != "String") {
            false
        } else {
            val callExpr = dotQualExpr.getChildOfType<KtCallExpression>()
            val innerNameRefExpr = callExpr?.getChildOfType<KtNameReferenceExpression>()
            innerNameRefExpr?.getReferencedName() == "format"
        }
    } == true

private data class EventDetails(
    val editor: Editor?,
    val psiFile: PsiFile?,
) {
    val element: PsiElement?
        get() = editor?.run { caretModel.offset.let { psiFile?.findElementAt(it) } }

    val isKotlinFile: Boolean
        get() = editor != null && psiFile != null && psiFile.fileType is KotlinFileType

    constructor(actionEvent: AnActionEvent) : this (
        editor = actionEvent.getData(CommonDataKeys.EDITOR),
        psiFile = actionEvent.getData(CommonDataKeys.PSI_FILE),
    )
}
