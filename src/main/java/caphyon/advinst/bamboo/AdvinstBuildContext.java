package caphyon.advinst.bamboo;


import java.util.Map;

public class AdvinstBuildContext
{
  private final Map<String, String> env;

  public AdvinstBuildContext(Map<String, String> env)
  {
    this.env = env;
  }

  public String getRootPath()
  {
    return env.get("root_path");
  }

  public String getAipPath()
  {
    return env.get("aip_path");
  }

  public String getAipBuild()
  {
    return env.get("aip_build");
  }

  public String getOutputFile()
  {
    return env.get("output_file");
  }

  public String getOutputFolder()
  {
    return env.get("output_folder");
  }

  public String getCommands()
  {
    return env.get("commands");
  }

  public Boolean getSkipDigitalSignature()
  {
    return Boolean.parseBoolean(env.get("digital_signature"));
  }
}
