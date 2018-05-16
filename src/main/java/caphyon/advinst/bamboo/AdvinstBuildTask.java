package caphyon.advinst.bamboo;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.build.test.TestCollationService;
import com.atlassian.bamboo.process.EnvironmentVariableAccessor;
import com.atlassian.bamboo.process.ExternalProcessBuilder;
import com.atlassian.bamboo.process.ProcessService;
import com.atlassian.bamboo.task.*;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityContext;
import com.atlassian.utils.process.*;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public class AdvinstBuildTask implements TaskType
{
  private final ProcessService mProcessService;
  private final CapabilityContext mCapabilityContext;
  private final EnvironmentVariableAccessor mEnvironmentVariableAccessor;
  private final TestCollationService mTestCollationService;
  private Map<String, String> mEnvironmentVariables;

  public AdvinstBuildTask(final ProcessService processService,
                          final EnvironmentVariableAccessor environmentVariableAccessor,
                          final CapabilityContext capabilityContext,
                          TestCollationService testCollationService)
  {
    this.mProcessService = processService;
    this.mCapabilityContext = capabilityContext;
    this.mEnvironmentVariableAccessor = environmentVariableAccessor;
    this.mTestCollationService = testCollationService;
  }

  @NotNull
  public TaskResult execute(@NotNull final TaskContext taskContext) throws TaskException
  {
    final BuildLogger buildLogger = taskContext.getBuildLogger();
    AdvinstBuildContext buildContext = createBuildContext(taskContext);
    initEnvironmentVariables();

    List<String> commands = new ArrayList<String>();
    File aicFile = null;

    try
    {
      File advinstComPath = getAdvinstComPath(taskContext, buildContext);
      File aipPath = getAipPath(taskContext, buildContext);


      String aipBuild = getAipBuild(buildContext);
      if (StringUtils.isNotEmpty(aipBuild))
      {
        File outputFolder = getOutputFolder(taskContext, buildContext);
        if (null != outputFolder)
        {
          commands.add(String.format("SetOutputLocation -buildname \"%s\" -path \"%s\"", aipBuild, outputFolder));
        }


        String outputFileName = getOutputFile(buildContext);
        if (StringUtils.isNotEmpty(outputFileName))
        {
          commands.add(String.format("SetPackageName \"%s\" -buildname \"%s\"", outputFileName, aipBuild));
        }
      }

      String extraCommands = getCommands(buildContext);
      if (StringUtils.isNotEmpty(extraCommands))
      {
        StringTokenizer tokenizer = new StringTokenizer(extraCommands, "\r\n");
        while (tokenizer.hasMoreTokens())
        {
          commands.add(tokenizer.nextToken());
        }
      }

      if (getSkipDigitalSignature(buildContext))
      {
        commands.add("ResetSig");
      }

      commands.add(String.format("Build -buildslist \"%s\"", aipBuild != null ? aipBuild : ""));

      aicFile = createAicFile(taskContext, commands);

      File rootDirectory = taskContext.getRootDirectory();
      List<String> advinstCommandLine = Lists.newArrayList();
      advinstCommandLine.add(advinstComPath.getAbsoluteFile().toString());
      advinstCommandLine.add("/execute");
      advinstCommandLine.add(aipPath.getAbsoluteFile().toString());
      advinstCommandLine.add(aicFile.getAbsoluteFile().toString());

      ExternalProcessBuilder processBuilder =
        new ExternalProcessBuilder().workingDirectory(rootDirectory).command(advinstCommandLine).env(mEnvironmentVariables);

      ExternalProcess process = mProcessService.createExternalProcess(taskContext, processBuilder);
      process.execute();
      ProcessHandler handler = process.getHandler();
      if (handler instanceof PluggableProcessHandler)
      {
        OutputHandler outputHandler = ((PluggableProcessHandler) handler).getOutputHandler();
        if (outputHandler instanceof StringOutputHandler)
        {
          StringOutputHandler outputStringHandler = (StringOutputHandler) outputHandler;
          if (outputStringHandler.getOutput() != null)
          {
            buildLogger.addBuildLogEntry(outputStringHandler.getOutput());
          }
        }
      }

      return TaskResultBuilder.newBuilder(taskContext).checkReturnCode(process, 0).build();
    }
    catch (AdvinstException e)
    {
      buildLogger.addBuildLogEntry(e.getMessage());
      return TaskResultBuilder.newBuilder(taskContext).failed().build();
    }
    finally
    {
      if (null != aicFile)
        aicFile.delete();
    }
  }

  @NotNull
  private File getAdvinstComPath(final TaskContext taskContext,
                                 final AdvinstBuildContext buildContext) throws AdvinstException
  {
    String rootPathParam = buildContext.getRootPath();
    if (StringUtils.isEmpty(rootPathParam))
    {
      throw new AdvinstException("Advanced Installer root path was not specified");
    }

    File advinstComPath = new File(rootPathParam, AdvinstConstants.AdvinstComSubPath);
    if (!advinstComPath.exists())
    {
      throw new AdvinstException("Advanced Installer root path is invalid: " + rootPathParam);
    }
    return advinstComPath;
  }

  @NotNull
  private File getAipPath(final TaskContext taskContext,
                          final AdvinstBuildContext buildContext) throws AdvinstException

  {
    String aipPathParam = buildContext.getAipPath();
    if (StringUtils.isEmpty(aipPathParam))
    {
      throw new AdvinstException("Advanced Installer project file path was not specified");
    }

    File aipPath = new File(aipPathParam);
    if (!aipPath.isAbsolute())
    {
      aipPath = new File(taskContext.getRootDirectory(), aipPathParam);
    }

    if (!aipPath.exists())
    {
      throw new AdvinstException("Advanced Installer project file path is invalid: " + aipPath);
    }
    return aipPath;
  }

  private String getAipBuild(final AdvinstBuildContext buildContext)
  {
    return buildContext.getAipBuild();
  }

  private String getOutputFile(final AdvinstBuildContext buildContext) throws AdvinstException
  {
    return buildContext.getOutputFile();
  }

  private File getOutputFolder(final TaskContext taskContext,
                               final AdvinstBuildContext buildContext) throws AdvinstException
  {
    String outputFolderParam = buildContext.getOutputFolder();
    if (StringUtils.isEmpty(outputFolderParam))
      return null;

    File outputFolder = new File(outputFolderParam);
    if (!outputFolder.isAbsolute())
    {
      outputFolder = new File(taskContext.getRootDirectory(), outputFolderParam);
    }

    return outputFolder;
  }

  private String getCommands(final AdvinstBuildContext buildContext)
  {
    return buildContext.getCommands();
  }

  private boolean getSkipDigitalSignature(final AdvinstBuildContext buildContext)
  {
    return buildContext.getSkipDigitalSignature();
  }

  @NotNull
  private AdvinstBuildContext createBuildContext(final TaskContext taskContext)
  {
    Map<String, String> combinedMap = Maps.newHashMap();
    combinedMap.putAll(taskContext.getConfigurationMap());
    BuildContext parentBuildContext = taskContext.getBuildContext().getParentBuildContext();
    if (parentBuildContext != null)
    {
      Map<String, String> customBuildData = parentBuildContext.getBuildResult().getCustomBuildData();
      combinedMap.putAll(customBuildData);
    }
    return new AdvinstBuildContext(combinedMap);
  }

  private void initEnvironmentVariables()
  {
    Map<String, String> env = Maps.newHashMap();
    env.putAll(mEnvironmentVariableAccessor.getEnvironment());
    mEnvironmentVariables = env;
  }

  private File createAicFile(final TaskContext taskContext, final List<String> commands) throws AdvinstException
  {
    File aicFile;
    try
    {
      aicFile = File.createTempFile("aic", "aic", taskContext.getRootDirectory());
      String fileContent = AdvinstConstants.AdvinstAicHeader + "\r\n";
      for (String command : commands)
      {
        fileContent += command;
        fileContent += "\r\n";
      }

      FileOutputStream fos = new FileOutputStream(aicFile.getAbsoluteFile());
      OutputStreamWriter w = new OutputStreamWriter(fos, "UTF-16");

      try
      {
        w.write(fileContent);
      }
      finally
      {
        w.close();
      }
    }
    catch (IOException e)
    {
      throw new AdvinstException(e);
    }

    return aicFile;
  }
}