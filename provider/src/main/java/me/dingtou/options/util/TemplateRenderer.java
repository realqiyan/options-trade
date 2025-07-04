package me.dingtou.options.util;

import freemarker.core.PlainTextOutputFormat;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;

import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.util.Map;

@Slf4j
public class TemplateRenderer {
    private static final Configuration cfg = new Configuration(Configuration.VERSION_2_3_31);

    static {
        cfg.setClassLoaderForTemplateLoading(TemplateRenderer.class.getClassLoader(), "templates");
        cfg.setDefaultEncoding("UTF-8");
        cfg.setAutoEscapingPolicy(Configuration.DISABLE_AUTO_ESCAPING_POLICY);
        cfg.setOutputFormat(PlainTextOutputFormat.INSTANCE);
    }

    /**
     * 模板渲染
     * 
     * @param templateName 模板名称
     * @param data         模板数据
     * @return 渲染结果
     */
    public static String render(String templateName, Map<String, Object> data) {
        try {
            Template tpl = cfg.getTemplate(templateName);
            return FreeMarkerTemplateUtils.processTemplateIntoString(tpl, data);
        } catch (Exception e) {
            log.error("Template:{} render failed:{} ", templateName, e.getMessage(), e);
            throw new RuntimeException("Template render failed: " + templateName, e);
        }
    }
}
