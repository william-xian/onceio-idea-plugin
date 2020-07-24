package top.onceio.plugins;


import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiType;
import java.util.ArrayList;
import java.util.List;

public class TableModel {
    public static final String IMPORTS = "\nimport top.onceio.core.db.model.*;\nimport top.onceio.core.util.OReflectUtil;";
    private static final String TPL =
            "\n    public static class Meta extends %s.Meta<Meta>  {\n" +
                    "%s\n" +
                    "        public Meta(String alias) {\n" +
                    "            super(\"%s\", alias);\n" +
                    "            super.bind(this, %s.class);\n" +
                    "        }\n" +
                    "        public static Meta meta() {\n" +
                    "            return new Meta(\"t\");\n" +
                    "        }\n" +
                    "        public static Meta meta(String alias) {\n" +
                    "            return new Meta(alias);\n" +
                    "        }\n" +
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

    public void appendField(String modelType, String fieldName, String colName) {
        fields.add(String.format(TPL_FIELD, modelType, fieldName, modelType, className, colName));
    }

    @Override
    public String toString() {
        return String.format(TPL, superClass, String.join("\n", fields), tableName, className);
    }


    public static TableModel parse(PsiClass psiClass) {
        PsiAnnotation tbl = psiClass.getAnnotation("top.onceio.core.db.annotation.Tbl");
        if(tbl == null) {
            return null;
        }
        String table = tbl.findAttributeValue("name").getText().replace("\"","");
        String schema = tbl.findAttributeValue("schema").getText().replace("\"","");
        if ("".equals(table)) {
            table = psiClass.getName();
        }
        TableModel model = new TableModel(psiClass.getName(), psiClass.getSuperClass().getName(), schema + "." + table);
        for (PsiField psiField : psiClass.getFields()) {
            PsiAnnotation col = psiField.getAnnotation("top.onceio.core.db.annotation.Col");
            if(col == null) continue;

            String modelType = "Base";
            if (psiField.getType().equals(PsiType.CHAR)) {
                modelType = "String";
            }
            String fieldName = psiField.getName();
            String colName = col.findAttributeValue("name").getText().replace("\"","");
            if (colName.equals("")) {
                colName = fieldName;
            }
            model.appendField(modelType, fieldName, colName);
        }
        return model;
    }

}