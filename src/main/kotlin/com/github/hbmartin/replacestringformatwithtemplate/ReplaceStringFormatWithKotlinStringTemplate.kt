package com.github.hbmartin.replacestringformatwithtemplate

import com.github.hbmartin.replacestringformatwithtemplate.ReplaceEngine.replaceFormatWithTemplate
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
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType

class ReplaceStringFormatWithKotlinStringTemplate : AnAction("Replace String.format with Kotlin String Template") {

    override fun actionPerformed(anActionEvent: AnActionEvent) {
        val details = EventDetails(actionEvent = anActionEvent)

        details.element?.parentOfType<KtDotQualifiedExpression>(withSelf = true)?.let { dotQualExpr ->
            replaceFormatWithTemplate(dotQualExpr)
        }
    }

    override fun update(e: AnActionEvent) {
        val details = EventDetails(actionEvent = e)
        e.presentation.isEnabled = details.isKotlinFile && (details.element?.isEligible() == true)
    }
}

@Suppress("ReturnCount")
private fun PsiElement.isEligible(): Boolean {
    return this.parentOfType<KtDotQualifiedExpression>(withSelf = true)?.let { dotQualExpr ->
        val firstNameRefExpr = dotQualExpr.getChildOfType<KtNameReferenceExpression>()
        if (firstNameRefExpr?.getReferencedName() != "String") { return false }
        val callExpr = dotQualExpr.getChildOfType<KtCallExpression>()
        val innerNameRefExpr = callExpr?.getChildOfType<KtNameReferenceExpression>()
        if (innerNameRefExpr?.getReferencedName() != "format") { return false }
        val arguments = callExpr.getChildOfType<KtValueArgumentList>()
        (arguments?.arguments?.size ?: 0) > 1
    } ?: false
}

private data class EventDetails(
    val editor: Editor?,
    val psiFile: PsiFile?
) {
    constructor(actionEvent: AnActionEvent) : this (
        editor = actionEvent.getData(CommonDataKeys.EDITOR),
        psiFile = actionEvent.getData(CommonDataKeys.PSI_FILE)
    )

    val element: PsiElement?
        get() = editor?.caretModel?.offset?.let { psiFile?.findElementAt(it) }

    val isKotlinFile: Boolean
        get() = editor != null && psiFile != null && psiFile.fileType is KotlinFileType
}
