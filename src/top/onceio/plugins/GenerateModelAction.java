package top.onceio.plugins;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiImportList;

public class GenerateModelAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getData(PlatformDataKeys.PROJECT);
        PsiFile psiFile = event.getData(CommonDataKeys.PSI_FILE);
        StringBuilder message = new StringBuilder();
        message.append(psiFile.getVirtualFile().getPath() + "\n");
        Editor editor = event.getData(CommonDataKeys.EDITOR);
        Document doc = editor.getDocument();
        StringBuilder text = new StringBuilder(doc.getText());

        PsiImportList importList = null;
        PsiClass psiClass = null;
        for (PsiElement element : psiFile.getChildren()) {
            if (PsiClass.class.isAssignableFrom(element.getClass())) {
                psiClass = (PsiClass) element;
            }
            if (PsiImportList.class.isAssignableFrom(element.getClass())) {
                importList = (PsiImportList) element;
            }
        }

        TableModel model = TableModel.parse(psiClass);
        if (model != null) {
            message.append(model.toString());
        } else {
            message.append("必须添加Model注解");
        }

        int metaIndex = psiClass.getTextRange().getEndOffset() - 2;
        String meta = model.toString();
        text.insert(metaIndex, meta);
        doc.setText(text);

    }

}
