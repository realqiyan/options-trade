package me.dingtou.options.util;

import freemarker.template.Configuration;
import freemarker.template.Template;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.util.Map;

public class TemplateRenderer {
    private static final Configuration cfg = new Configuration(Configuration.VERSION_2_3_31);
    
    static {
        cfg.setClassLoaderForTemplateLoading(TemplateRenderer.class.getClassLoader(), "templates/strategy");
        cfg.setDefaultEncoding("UTF-8");
    }
    
    public static String render(String templateName, Map<String, Object> data) {
        try {
            Template tpl = cfg.getTemplate(templateName);
            return FreeMarkerTemplateUtils.processTemplateIntoString(tpl, data);
        } catch (Exception e) {
            throw new RuntimeException("Template render failed: " + templateName, e);
        }
    }
}
