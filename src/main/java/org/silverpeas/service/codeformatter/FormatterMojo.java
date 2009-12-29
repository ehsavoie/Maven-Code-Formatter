package org.silverpeas.service.codeformatter;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.Map;
import java.util.Properties;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;

/**
 * Goal which reformat some code using Eclipse own Code Formatter.
 * 
 * @goal format
 * @phase process-sources
 */
public class FormatterMojo extends AbstractMojo {
  /**
   * File containing the Eclipse Formatter Configuration.
   * 
   * @parameter expression="${formatter.configurationFile}"
   *            default-value="${basedir}/src/main/resources/formatter/formatter.prefs"
   */
  private String configurationFile;
  /**
   * File containing Java files.
   * 
   * @parameter expression="${formatter.sourceFiles}"
   *            default-value="${basedir}/src/main/java"
   */
  private File sourceFiles;
  /**
   * File containing Java files.
   * 
   * @parameter expression="${formatter.sourceEncoding}"
   *            default-value="${project.build.sourceEncoding}"
   */
  private String sourceEncoding;

  public void execute() throws MojoExecutionException {
    Map options = readConfig(getConfigurationFile());
    final CodeFormatter codeFormatter = ToolFactory
        .createCodeFormatter(options);
    formatDirTree(getSourceFiles(), codeFormatter);
  }

  /**
   * Recursively format the Java source code that is contained in the directory
   * rooted at dir.
   */
  private void formatDirTree(File dir, CodeFormatter codeFormatter) {
    File[] files = dir.listFiles();
    if (files == null) {
      return;
    }
    for (int i = 0; i < files.length; i++) {
      File file = files[i];
      if (file.isDirectory()) {
        formatDirTree(file, codeFormatter);
      } else if (isJavaLikeFileName(file)) {
        formatFile(file, codeFormatter);
      }
    }
  }

  private boolean isJavaLikeFileName(File file) {
    int index = file.getName().lastIndexOf('.');
    if (index >= 0) {
      String extension = file.getName().substring(index + 1);
      return "java".equalsIgnoreCase(extension);
    }
    return false;
  }

  /**
   * Format the given Java source file.
   */
  private void formatFile(File file, CodeFormatter codeFormatter) {
    IDocument doc = new Document();
    try {
      // read the file
      getLog().debug(
          "Formatting file " + file.getAbsolutePath() + " in encoding "
              + sourceEncoding);
      String contents = new String(org.eclipse.jdt.internal.compiler.util.Util
          .getFileCharContent(file, sourceEncoding));
      // format the file (the meat and potatoes)
      getLog().debug("Formatting this " + contents);
      doc.set(contents);
      TextEdit edit = codeFormatter.format(CodeFormatter.K_COMPILATION_UNIT
          | CodeFormatter.F_INCLUDE_COMMENTS, contents, 0, contents.length(),
          0, null);
      if (edit != null) {
        edit.apply(doc);
      } else {
        getLog().error("Error formatting file " + file.getAbsolutePath());
        return;
      }

      // write the file
      final BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
          new FileOutputStream(file), sourceEncoding));
      try {
        out.write(doc.get());
        out.flush();
      } finally {
        try {
          out.close();
        } catch (IOException e) {
          /* ignore */
        }
      }
    } catch (IOException e) {
      getLog().error("Error formatting file " + file.getAbsolutePath(), e);
    } catch (BadLocationException e) {
      getLog().error("Error formatting file " + file.getAbsolutePath(), e);
    }
  }

  /**
   * Return a Java Properties file representing the options that are in the
   * specified config file.
   */
  private Properties readConfig(String configFile) {
    BufferedInputStream stream = null;
    try {
      stream = new BufferedInputStream(getResource(configFile));
      final Properties formatterOptions = new Properties();
      formatterOptions.load(stream);
      return formatterOptions;
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (stream != null) {
        try {
          stream.close();
        } catch (IOException e) {
          /* ignore */
        }
      }
    }
    return null;
  }

  protected InputStream getResource(String configFile)
      throws FileNotFoundException {
    File file = new File(configFile);
    if (file.exists() && file.isFile()) {
      return new FileInputStream(file);
    }
    return this.getClass().getClassLoader().getResourceAsStream(configFile);
  }

  /**
   * @return the configurationFile
   */
  public String getConfigurationFile() {
    return configurationFile;
  }

  /**
   * @param configurationFile
   *          the configurationFile to set
   */
  public void setConfigurationFile(String configurationFile) {
    this.configurationFile = configurationFile;
  }

  /**
   * @return the sourceFiles
   */
  public File getSourceFiles() {
    return sourceFiles;
  }

  /**
   * @param sourceFiles
   *          the sourceFiles to set
   */
  public void setSourceFiles(File sourceFiles) {
    this.sourceFiles = sourceFiles;
  }
}
