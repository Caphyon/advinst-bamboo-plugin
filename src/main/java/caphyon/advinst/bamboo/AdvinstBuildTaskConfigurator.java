package caphyon.advinst.bamboo;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.task.AbstractTaskConfigurator;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.opensymphony.xwork.TextProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class AdvinstBuildTaskConfigurator extends AbstractTaskConfigurator
{
  private TextProvider textProvider;

  @NotNull
  @Override
  public Map<String, String> generateTaskConfigMap(@NotNull final ActionParametersMap params, @Nullable final TaskDefinition previousTaskDefinition)
  {
    final Map<String, String> config = super.generateTaskConfigMap(params, previousTaskDefinition);
    config.put("root_path", params.getString("root_path"));
    config.put("aip_path", params.getString("aip_path"));
    config.put("aip_build", params.getString("aip_build"));
    config.put("output_file", params.getString("output_file"));
    config.put("output_folder", params.getString("output_folder"));
    config.put("commands", params.getString("commands"));
    return config;
  }

  @Override
  public void populateContextForCreate(@NotNull final Map<String, Object> context)
  {
    super.populateContextForCreate(context);
  }

  @Override
  public void populateContextForEdit(@NotNull final Map<String, Object> context, @NotNull final TaskDefinition taskDefinition)
  {
    super.populateContextForEdit(context, taskDefinition);
    context.put("root_path", taskDefinition.getConfiguration().get("root_path"));
    context.put("aip_path", taskDefinition.getConfiguration().get("aip_path"));
    context.put("aip_build", taskDefinition.getConfiguration().get("aip_build"));
    context.put("output_file", taskDefinition.getConfiguration().get("output_file"));
    context.put("output_folder", taskDefinition.getConfiguration().get("output_folder"));
    context.put("commands", taskDefinition.getConfiguration().get("commands"));
  }

  @Override
  public void populateContextForView(@NotNull final Map<String, Object> context, @NotNull final TaskDefinition taskDefinition)
  {
    super.populateContextForView(context, taskDefinition);
    context.put("root_path", taskDefinition.getConfiguration().get("root_path"));
    context.put("aip_path", taskDefinition.getConfiguration().get("aip_path"));
    context.put("aip_build", taskDefinition.getConfiguration().get("aip_build"));
    context.put("output_file", taskDefinition.getConfiguration().get("output_file"));
    context.put("output_folder", taskDefinition.getConfiguration().get("output_folder"));
    context.put("commands", taskDefinition.getConfiguration().get("commands"));
  }

  @Override
  public void validate(@NotNull final ActionParametersMap params, @NotNull final ErrorCollection errorCollection)
  {
    super.validate(params, errorCollection);
    final String root_path = params.getString("root_path");
    final String aip_path = params.getString("aip_path");
    if ( root_path.isEmpty() )
    {
      errorCollection.addError("root_path", textProvider.getText("advancedinstaller.root_path.error"));
    }

    if (aip_path.isEmpty())
    {
      errorCollection.addError("aip_path", textProvider.getText("advancedinstaller.aip_path.error"));
    }
  }

  public void setTextProvider(final TextProvider textProvider)
  {
    this.textProvider = textProvider;
  }
}