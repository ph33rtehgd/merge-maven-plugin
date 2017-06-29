/**
 * Copyright © 2017 Mehdi Bekioui (consulting@bekioui.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bekioui.maven.plugin.merge;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "merge", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class MergePojo extends AbstractMojo {

	@Parameter(property = "merges", required = true)
	private List<Merge> merges;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		for (Merge merge : merges) {
			createTargetFile(merge.getTarget());
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(merge.getTarget(), true))) {
				for (File file : merge.getSources()) {
					if (file.isDirectory()) {
						merge(checkSourceDirectory(file), writer);
					} else {
						merge(checkSourceFile(file), writer);
					}
				}
			} catch (Exception e) {
				throw new MojoExecutionException("Failed to write into target file", e);
			}
		}
	}

	private void createTargetFile(File file) throws MojoExecutionException {
		if (file.isDirectory()) {
			throw new MojoExecutionException("Target file cannot be a directory: " + file.getAbsolutePath());
		}

		if (file.exists()) {
			throw new MojoExecutionException("Targe file already exists: " + file.getAbsolutePath());
		}

		File parentFile = file.getParentFile();
		if (!parentFile.exists() && !parentFile.mkdirs()) {
			throw new MojoExecutionException("Could not create target parent directory: " + parentFile.getAbsolutePath());
		}

		if (!parentFile.isDirectory()) {
			throw new MojoExecutionException("Target parent file is not a directory: " + parentFile.getAbsolutePath());
		}

		try {
			if (!file.createNewFile()) {
				throw new MojoExecutionException("Could not create target file: " + file.getAbsolutePath());
			}
		} catch (IOException e) {
			throw new MojoExecutionException("Failed to create target file: " + file.getAbsolutePath(), e);
		}
	}

	private File checkSourceFile(File file) throws MojoExecutionException {
		if (!file.exists()) {
			throw new MojoExecutionException("Source file " + file.getAbsolutePath() + " does not exist.");
		}
		return file;
	}

	private List<File> checkSourceDirectory(File file) throws MojoExecutionException {
		List<File> sourceFiles = new ArrayList<>();
		checkSourceFile(file);

		File[] files = file.listFiles();
		Arrays.sort(files);

		List<File> subFiles = new ArrayList<>();
		for (File subFile : files) {
			if (subFile.isDirectory()) {
				sourceFiles.addAll(checkSourceDirectory(subFile));
			} else {
				subFiles.add(checkSourceFile(subFile));
			}
		}
		sourceFiles.addAll(subFiles);

		return sourceFiles;
	}

	private void merge(File file, BufferedWriter writer) throws MojoExecutionException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
			String line;
			while ((line = reader.readLine()) != null) {
				writer.write(line);
				writer.newLine();
			}
		} catch (Exception e) {
			throw new MojoExecutionException("Failed to write source file " + file.getAbsolutePath(), e);
		}
	}

	private void merge(List<File> files, BufferedWriter writer) throws MojoExecutionException {
		for (File file : files) {
			merge(file, writer);
		}
	}

}
