package top.onceio.plugins;


import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;

import java.util.ArrayList;
import java.util.List;

public class TableModel {
    public static final String IMPORTS = "\nimport top.onceio.core.db.model.*;\nimport top.onceio.core.util.OReflectUtil;";
    private static final String TPL =
            "\n    public static class Meta extends %s.Meta<Meta>  {\n" +
                    "%s\n" +
                    "        public Meta() {\n" +
                    "            super(\"%s\");\n" +
                    "            super.bind(this, %s.class);\n" +
                    "        }\n" +
                    "    }\n" +
                    "    public static Meta meta() {\n" +
                    "        return new Meta();\n" +
                    "    }\n";

    private static final String TPL_FIELD =
            "        public %sCol<Meta> %s = new %sCol(this, OReflectUtil.getField(%s.class, \"%s\"));";

    private String superClass;
    private String tableName;
    private String className;
    private List<String> fields = new ArrayList<>();

    private String pkg;
    private List<String> imports;

    public TableModel(String className, String superClass, String tableName) {
        this.className = className;
        this.superClass = superClass;
        this.tableName = tableName;
    }

    public void appendField(String modelType, String fieldName) {
        fields.add(String.format(TPL_FIELD, modelType, fieldName, modelType, className, fieldName));
    }

    @Override
    public String toString() {
        return String.format(TPL, superClass, String.join("\n", fields), tableName, className);
    }


    public static TableModel parse(PsiClass psiClass) {
        PsiAnnotation tbl = psiClass.getAnnotation("top.onceio.core.db.annotation.Tbl");
        String table;
        String schema;
        if (tbl != null) {
            table = tbl.findAttributeValue("name").getText().replace("\"", "");
            schema = tbl.findAttributeValue("schema").getText().replace("\"", "");
        } else {
            return null;
        }
        if ("".equals(table)) {
            table = psiClass.getName().replaceAll("([A-Z])", "_$1").toLowerCase();
            if (table.startsWith("_")) {
                table = table.substring(1);
            }
        }
        TableModel model = new TableModel(psiClass.getName(), psiClass.getSuperClass().getName(), schema + "." + table);
        for (PsiField psiField : psiClass.getFields()) {
            PsiAnnotation col = psiField.getAnnotation("top.onceio.core.db.annotation.Col");
            if (col == null) continue;
            String modelType = "Base";
            if (psiField.getType().getCanonicalText().equals("java.lang.String")) {
                modelType = "String";
            }
            String fieldName = psiField.getName();
            model.appendField(modelType, fieldName);
        }
        return model;
    }

}