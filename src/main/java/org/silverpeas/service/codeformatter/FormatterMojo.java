/**
 * ====
 *     Copyright (C) 2000 - 2009 Silverpeas
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     As a special exception to the terms and conditions of version 3.0 of
 *     the GPL, you may redistribute this Program in connection with Free/Libre
 *     and Open Source Software ("FLOSS") applications as described in Alfresco's
 *     FLOSS exception.  You should have recieved a copy of the text describing
 *     the FLOSS exception, and it is also available here:
 *     "http://repository.silverpeas.com/legal/licensing"
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * ====
 * Copyright (C) 2000 - 2009 Silverpeas
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * As a special exception to the terms and conditions of version 3.0 of
 * the GPL, you may redistribute this Program in connection with Free/Libre
 * Open Source Software ("FLOSS") applications as described in Silverpeas's
 * FLOSS exception.  You should have recieved a copy of the text describing
 * the FLOSS exception, and it is also available here:
 * "http://repository.silverpeas.com/legal/licensing"
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.silverpeas.service.codeformatter;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
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
      getLog().debug("Formatting file " + file.getAbsolutePath());
      String contents = new String(org.eclipse.jdt.internal.compiler.util.Util
          .getFileCharContent(file, null));
      // format the file (the meat and potatoes)
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
      final BufferedWriter out = new BufferedWriter(new FileWriter(file));
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