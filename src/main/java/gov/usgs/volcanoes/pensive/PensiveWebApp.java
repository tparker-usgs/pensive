/**
 * I waive copyright and related rights in the this work worldwide
 * through the CC0 1.0 Universal public domain dedication.
 * https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.pensive;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

import gov.usgs.volcanoes.core.configfile.ConfigFile;
import gov.usgs.volcanoes.pensive.plot.SubnetPlotter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;


/**
 * Create single page application for Pensive.
 * 
 * @author Tom Parker
 */
public class PensiveWebApp {
  /** my logger. */
  private static final Logger LOGGER = LoggerFactory.getLogger(PensiveWebApp.class);

  public static final String DEFAULT_PATH_ROOT = "html/";

  /** filename of html file. */
  public static final String FILENAME = "index.html";

  /** Freemarker settings. */
  private final Map<String, Object> root;

  /** my configuration. */
  private Configuration cfg;

  /** My subnets. */
  private final Map<String, List<String>> subnets;

  /** root of output. */
  private final String pathRoot;

  /**
   * Class constructor.
   *
   * @param config My configuration stanza
   */
  public PensiveWebApp(final ConfigFile config) {
    pathRoot = config.getString("pathRoot", DEFAULT_PATH_ROOT);

    root = new HashMap<String, Object>();

    subnets = new HashMap<String, List<String>>();
    root.put("subnets", subnets);

    root.put("refreshPeriod", SubnetPlotter.DURATION_S);
    root.put("filePathFormat",
        config.getString("filePathFormat", SubnetPlotter.DEFAULT_FILE_PATH_FORMAT));
    root.put("fileSuffixFormat",
        config.getString("fileSuffixFormat", SubnetPlotter.DEFAULT_FILE_SUFFIX_FORMAT));
    root.put("selectedNetwork", config.getString("selectedNetwork"));
    root.put("version", Version.VERSION_STRING);

    try {
      initializeTemplateEngine();
    } catch (final IOException e) {
      LOGGER.error("cannot write HTML");
    }

  }

  /**
   * Initialize FreeMarker.
   *
   * @throws IOException When things go wrong
   */
  protected void initializeTemplateEngine() throws IOException {
    cfg = new Configuration();
    cfg.setTemplateLoader(new ClassTemplateLoader(getClass(), "/html"));
    final DefaultObjectWrapper obj = new DefaultObjectWrapper();
    obj.setExposeFields(true);
    cfg.setObjectWrapper(obj);

    cfg.setDefaultEncoding("UTF-8");
    cfg.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
    cfg.setIncompatibleImprovements(new freemarker.template.Version(2, 3, 20));
  }

  /**
   * Write my html page.
   */
  public void writeHtml() {
    try {
      final File pRoot = new File(pathRoot);
      if (!pRoot.exists()) {
        pRoot.mkdirs();
      }
      
      final Template template = cfg.getTemplate("pensive.ftl");
      String file = pathRoot + '/' + FILENAME;
      file = file.replace("/+", "/");
      file = file.replace("/", Matcher.quoteReplacement(File.separator));
      final FileWriter fw = new FileWriter(file);
      template.process(root, fw);
      fw.close();
    } catch (final IOException e) {
      LOGGER.error(e.getLocalizedMessage());
    } catch (final TemplateException e) {
      LOGGER.error(e.getLocalizedMessage());
    }
  }

  /** 
   * add a subnet to a network list.
   */
  public void addSubnet(final String network, final String subnet) {
    List<String> subs = subnets.get(network);
    if (subs == null) {
      subs = new ArrayList<String>();
      subnets.put(network, subs);
    }
    subs.add(subnet);
  }
}
