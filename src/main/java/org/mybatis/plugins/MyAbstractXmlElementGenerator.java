package org.mybatis.plugins;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.dom.xml.Attribute;
import org.mybatis.generator.api.dom.xml.TextElement;
import org.mybatis.generator.api.dom.xml.XmlElement;
import org.mybatis.generator.codegen.mybatis3.MyBatis3FormattingUtilities;
import org.mybatis.generator.codegen.mybatis3.xmlmapper.elements.AbstractXmlElementGenerator;

import java.sql.JDBCType;
import java.util.List;

public class MyAbstractXmlElementGenerator extends AbstractXmlElementGenerator {

    private  CodeGeneratorConfig config = null;

    public MyAbstractXmlElementGenerator(CodeGeneratorConfig config) {
        this.config = config;
    }

    @Override
    public void addElements(XmlElement parentElement) {
        // 增加base_column对应sql
        parentElement.addElement(addBaseColumn());
        parentElement.addElement(addBaseWhere());

        // 公用select
        StringBuilder sb = new StringBuilder();
        sb.append("select ");
        TextElement selectText = new TextElement(sb.toString());

        sb.setLength(0);
        sb.append("from ").append(introspectedTable.getFullyQualifiedTableNameAtRuntime());
        sb.append(" T ");
        TextElement fromText = new TextElement(sb.toString());

        XmlElement baseColInclude = new XmlElement("include");
        baseColInclude.addAttribute(new Attribute("refid", config.MAPPER_NAME_BASE_COLUMN));

        XmlElement baseWhereInclude = new XmlElement("include");
        baseWhereInclude.addAttribute(new Attribute("refid", config.MAPPER_NAME_BASE_WHERE_CLAUSE));

        XmlElement pageList = new XmlElement("select");
        pageList.addAttribute(new Attribute("id", config.METHOD_LISTBYPAGE));
        pageList.addAttribute(new Attribute("resultMap", "BaseResultMap"));
        pageList.addAttribute(new Attribute("parameterType", introspectedTable.getBaseRecordType()));

        pageList.addElement(selectText);
        pageList.addElement(baseColInclude);
        pageList.addElement(fromText);
        pageList.addElement(baseWhereInclude);
        parentElement.addElement(pageList);

        //insertBatch
        XmlElement baseColListInclude = new XmlElement("include");
        baseColListInclude.addAttribute(new Attribute("refid", config.MAPPER_NAME_BASE_COLUMN_LIST));

        XmlElement insertBatch = new XmlElement("insert");
        insertBatch.addAttribute(new Attribute("id", config.METHOD_INSERT_BATCH));
        insertBatch.addAttribute(new Attribute("parameterType", "java.util.List"));
        sb.setLength(0);
        sb.append("insert into ").append(introspectedTable.getFullyQualifiedTableNameAtRuntime());
        TextElement insertText = new TextElement(sb.toString());

        insertBatch.addElement(insertText);
        insertBatch.addElement(baseColListInclude);
        insertBatch.addElement(foreachElement(introspectedTable.getAllColumns()));

        parentElement.addElement(insertBatch);

    }


    /**
     * 判断是否为空的条件语句
     * @return
     */
    public static XmlElement foreachElement(List<IntrospectedColumn> list) {
        XmlElement foreachElement = new XmlElement("foreach"); //$NON-NLS-1$
        foreachElement.addAttribute(new Attribute("open", "("));
        foreachElement.addAttribute(new Attribute("collection", "list"));
        foreachElement.addAttribute(new Attribute("item", "item"));
        foreachElement.addAttribute(new Attribute("index", "index"));
        foreachElement.addAttribute(new Attribute("close", ")"));
        foreachElement.addAttribute(new Attribute("separator", ","));

        StringBuilder sb = new StringBuilder();
        sb.setLength(0);
        int index = list.size();
        int count = 1;
        for(IntrospectedColumn introspectedColumn : list) {
            sb.append(MyBatis3FormattingUtilities.getParameterClause(introspectedColumn,"item."));
            if(--index>0) {
                sb.append(", ");
                if(count%3==0) { //格式换行
                    sb.append("\r\n");
                    if(count>=3) { //缩进
                        sb.append("\t\t");
                    }
                }
            }
            count++;
        }
        foreachElement.addElement(new TextElement(sb.toString()));
        return foreachElement;
    }

    private static String notNull(IntrospectedColumn introspectedColumn, boolean notBlank) {
        StringBuffer sb = new StringBuffer();
        sb.append("null != ").append(introspectedColumn.getJavaProperty());
        if(notBlank && !isNumber(introspectedColumn)) {
            sb.append(" and ");
            sb.append("'' != ").append(introspectedColumn.getJavaProperty());
        }
        return sb.toString();
    }

    private static boolean isNumber(IntrospectedColumn introspectedColumn) {
        if(JDBCType.valueOf(introspectedColumn.getJdbcType())==JDBCType.BIT ||
                JDBCType.valueOf(introspectedColumn.getJdbcType())==JDBCType.SMALLINT ||
                JDBCType.valueOf(introspectedColumn.getJdbcType())==JDBCType.TINYINT ||
                JDBCType.valueOf(introspectedColumn.getJdbcType())==JDBCType.INTEGER ||
                JDBCType.valueOf(introspectedColumn.getJdbcType())==JDBCType.BIGINT ||
                JDBCType.valueOf(introspectedColumn.getJdbcType())==JDBCType.FLOAT ||
                JDBCType.valueOf(introspectedColumn.getJdbcType())==JDBCType.DOUBLE ||
                JDBCType.valueOf(introspectedColumn.getJdbcType())==JDBCType.NUMERIC ||
                JDBCType.valueOf(introspectedColumn.getJdbcType())==JDBCType.DECIMAL ||
                JDBCType.valueOf(introspectedColumn.getJdbcType())==JDBCType.ROWID ||
                JDBCType.valueOf(introspectedColumn.getJdbcType())==JDBCType.REAL
        ) {
            return true;
        }
        return false;
    }


    private XmlElement addBaseColumn() {
        // 增加base_query
        XmlElement sql = new XmlElement("sql");
        sql.addAttribute(new Attribute("id", CodeGeneratorConfig.MAPPER_NAME_BASE_COLUMN));
        StringBuilder sb = new StringBuilder();
        int index = introspectedTable.getAllColumns().size();
        for(IntrospectedColumn introspectedColumn : introspectedTable.getAllColumns()) {
            // 添加别名t
            sb.append("T.");
            sb.append(MyBatis3FormattingUtilities.getEscapedColumnName(introspectedColumn));
            //去掉最后一个
            if(--index>0) {
                sb.append(", ");
            }
        }
        //sb.subSequence(0, sb.toString().lastIndexOf(","));
        sql.addElement(new TextElement(sb.toString()));
        return sql;
    }

    private XmlElement addBaseWhere() {
        // 增加base_query
        XmlElement sql = new XmlElement("sql");
        sql.addAttribute(new Attribute("id", CodeGeneratorConfig.MAPPER_NAME_BASE_WHERE_CLAUSE));
        //在这里添加where条件
        XmlElement selectTrimElement = new XmlElement("trim"); //设置trim标签
        selectTrimElement.addAttribute(new Attribute("prefix", "WHERE"));
        selectTrimElement.addAttribute(new Attribute("prefixOverrides", "AND | OR")); //添加where和and
        StringBuilder sb = new StringBuilder();
        for(IntrospectedColumn introspectedColumn : introspectedTable.getAllColumns()) {
            XmlElement selectNotNullElement = new XmlElement("if"); //$NON-NLS-1$
            selectNotNullElement.addAttribute(new Attribute("test", notNull(introspectedColumn, true)));
            sb.setLength(0);
            // 添加and
            sb.append(" and ");
            // 添加别名t
            sb.append("T.");
            sb.append(MyBatis3FormattingUtilities.getEscapedColumnName(introspectedColumn));
            // 添加等号
            sb.append(" = ");
            sb.append(MyBatis3FormattingUtilities.getParameterClause(introspectedColumn));
            selectNotNullElement.addElement(new TextElement(sb.toString()));
            selectTrimElement.addElement(selectNotNullElement);
        }
        sql.addElement(selectTrimElement);

        return sql;
    }



}
