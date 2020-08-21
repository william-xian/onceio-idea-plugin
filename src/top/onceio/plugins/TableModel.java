package top.onceio.plugins;


import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import top.onceio.core.db.model.BaseTable;

import java.util.ArrayList;
import java.util.List;

public class TableModel {
    private static final String TPL =
            "\n    public static class Meta extends " + BaseTable.class.getName() + "<Meta>  {\n" +
                    "%s\n" +
                    "        public Meta() {\n" +
                    "            super.bind(\"%s\",this, %s.class);\n" +
                    "        }\n" +
                    "    }\n" +
                    "    public static Meta meta() {\n" +
                    "        return new Meta();\n" +
                    "    }\n";

    private static final String TPL_FIELD =
            "        public %sCol<Meta> %s = new %sCol(this, top.onceio.core.util.OReflectUtil.getField(%s.class, \"%s\"));";

    private String tableName;
    private String className;
    private List<String> fields = new ArrayList<>();

    public TableModel(String className, String tableName) {
        this.className = className;
        this.tableName = tableName;
    }

    public void appendField(String modelType, String fieldName) {
        fields.add(String.format(TPL_FIELD, modelType, fieldName, modelType, className, fieldName));
    }

    @Override
    public String toString() {
        return String.format(TPL, String.join("\n", fields), tableName, className);
    }


    public static TableModel parse(PsiClass psiClass) {
        PsiAnnotation tbl = psiClass.getAnnotation("top.onceio.core.db.annotation.Tbl");
        String table;
        if (tbl != null) {
            table = tbl.findAttributeValue("name").getText().replace("\"", "").toLowerCase().replace("public.", "");
        } else {
            return null;
        }
        if ("".equals(table)) {
            table = psiClass.getName().replaceAll("([A-Z])", "_$1").toLowerCase();
            if (table.startsWith("_")) {
                table = table.substring(1);
            }
        }
        TableModel model = new TableModel(psiClass.getName(), table);

        List<PsiClass> classList = new ArrayList<>();
        for (PsiClass cur = psiClass; cur != null && !cur.getName().equals(Object.class); cur = cur.getSuperClass()) {
            classList.add(0, cur);
        }

        for (PsiClass cur:classList) {
            for (PsiField psiField : cur.getFields()) {
                PsiAnnotation col = psiField.getAnnotation("top.onceio.core.db.annotation.Col");
                if (col == null) continue;
                String modelType = "top.onceio.core.db.model.Base";
                if (psiField.getType().getCanonicalText().equals("java.lang.String")) {
                    modelType = "top.onceio.core.db.model.String";
                }
                String fieldName = psiField.getName();
                model.appendField(modelType, fieldName);
            }
        }
        return model;
    }

}