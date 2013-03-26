package com.attask.jenkins.templates;

import com.attask.jenkins.BuildWrapperUtils;
import com.google.common.collect.ImmutableSet;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.DescribableList;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.io.*;
import java.util.List;
import java.util.Set;

/**
 * User: Joel Johnson
 * Date: 6/18/12
 * Time: 8:30 PM
 */
@ExportedBean
public class TemplateBuildWrapper extends BuildWrapper implements Syncable {
	private final String templateName;

    /**
     * Needed for backward compatibility with systems that used an older version of this plugin
     */
    public transient boolean synced;

	@DataBoundConstructor
	public TemplateBuildWrapper(String templateName) {
		this.templateName = templateName;
	}

	public void sync() throws IOException {
		AbstractProject templateProject = Project.findNearest(templateName);
		if(templateProject != null) {
			Set<AbstractProject> implementingProjects = getImplementers();
			for (AbstractProject implementingProject : implementingProjects) {
				ImplementationBuildWrapper implementationBuildWrapper = BuildWrapperUtils.findBuildWrapper(ImplementationBuildWrapper.class, implementingProject);
				implementationBuildWrapper.syncFromTemplate(templateProject, implementingProject);
			}
		}
	}

	public String getProjectName() {
		return templateName;
	}

	public Set<ImplementationBuildWrapper> findImplementersBuildWrappers() {
		ImmutableSet.Builder<ImplementationBuildWrapper> builder = ImmutableSet.builder();
		List<AbstractProject> allProjects = Hudson.getInstance().getAllItems(AbstractProject.class);
		for (AbstractProject project : allProjects) {
			if(project instanceof BuildableItemWithBuildWrappers) {
				DescribableList<BuildWrapper,Descriptor<BuildWrapper>> buildWrappersList = ((BuildableItemWithBuildWrappers) project).getBuildWrappersList();
				for (BuildWrapper buildWrapper : buildWrappersList) {
					if(buildWrapper instanceof ImplementationBuildWrapper) {
						builder.add((ImplementationBuildWrapper) buildWrapper);
					}
				}
			}
		}
		return builder.build();
	}

	public Set<AbstractProject> getImplementers() {
		ImmutableSet.Builder<AbstractProject> builder = ImmutableSet.builder();
		List<AbstractProject> allProjects = Hudson.getInstance().getAllItems(AbstractProject.class);
		for (AbstractProject project : allProjects) {
			if(project instanceof BuildableItemWithBuildWrappers) {
				DescribableList<BuildWrapper,Descriptor<BuildWrapper>> buildWrappersList = ((BuildableItemWithBuildWrappers) project).getBuildWrappersList();
				for (BuildWrapper buildWrapper : buildWrappersList) {
					if(buildWrapper instanceof ImplementationBuildWrapper) {
						if(templateName.equals(((ImplementationBuildWrapper) buildWrapper).getTemplateName())) {
							builder.add(project);
						}
					}
				}
			}
		}
		return builder.build();
	}

	@Exported
	public String getTemplateName() {
		return templateName;
	}



	@Override
	public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
		return new Environment() {
			@Override
			public boolean tearDown(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {
				return true;
			}
		};
	}

	@Extension
	public static class DescriptorImpl extends BuildWrapperDescriptor {
		@Override
		public boolean isApplicable(AbstractProject<?, ?> item) {
			return item instanceof Describable && item instanceof BuildableItemWithBuildWrappers;
		}

		@Override
		public String getDisplayName() {
			return "Make this a Template";
		}
	}
}
