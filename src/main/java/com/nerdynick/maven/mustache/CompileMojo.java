package com.nerdynick.maven.mustache;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.google.common.base.Strings;
import com.google.common.io.Files;

@Mojo(name="compile", defaultPhase=LifecyclePhase.GENERATE_SOURCES)
public class CompileMojo extends AbstractMojo {
	@Parameter(name="project", defaultValue="${project}")
	private MavenProject project;
	
	@Parameter(name="properties", required=false)
	private Properties props;
	
	@Parameter(defaultValue = "${project.build.sourceEncoding}")
    private String encoding;
	
	@Parameter(name="templates", required=true)
	private List<TemplateConfig> templates;
	
	private DefaultMustacheFactory mf = new DefaultMustacheFactory();

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		Charset charset;
		if (encoding == null || encoding.isEmpty()) {
            getLog().warn("Using platform encoding " + Charset.defaultCharset());
            charset = Charset.defaultCharset();
        } else {
            charset = Charset.forName(encoding);
        }
		
		Map<String, String> projectInfo = new HashMap<>();
		projectInfo.put("artifactId", project.getArtifactId());
		projectInfo.put("groupId", project.getGroupId());
		projectInfo.put("name", project.getName());
		projectInfo.put("description", Strings.nullToEmpty(project.getDescription()));
		
		final List<Map<?,?>> scope = new ArrayList<>();
		if(props != null){
			scope.add(props);
		}
		scope.add(project.getProperties());
		scope.add(projectInfo);
		
		getLog().debug("Mustache Scope: "+ scope);
		
		for(TemplateConfig t: templates){
			try {
				List<TemplateConfig.Template> temps = t.getTemplates();
				for(TemplateConfig.Template temp: temps){
					getLog().info("Compiling '"+ temp.input +"' to '"+ temp.output +"'");
					final Mustache mustache = createTemplate(temp.input, charset);
					final Writer writer = Files.newWriter(temp.output, charset);
					mustache.execute(writer, scope).close();
				}
			} catch (IOException e) {
				throw new MojoFailureException("Failed to process Template", e);
			}
			
		}
	}
	
	private Mustache createTemplate(File template, Charset charset) throws IOException {
        Reader reader = Files.newReader(template, charset);
        return mf.compile(reader, "template");
    }
}
