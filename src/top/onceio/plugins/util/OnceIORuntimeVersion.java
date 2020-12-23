package top.onceio.plugins.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class OnceIORuntimeVersion {
  public String findCurrentOnceIOVersion() {
    return "0.1.0";
  }

  public Collection<String> getOnceIOJarsInProject(@NotNull Project project) {
    List<VirtualFile> pathsFiles = ProjectRootManager.getInstance(project).orderEntries().withoutSdk().librariesOnly().getPathsList().getVirtualFiles();
    return pathsFiles.stream()
      .filter(file -> Objects.equals(file.getExtension(), "jar"))
      .filter(file -> file.getNameWithoutExtension().contains("lombok-"))
      .map(VirtualFile::getNameWithoutExtension)
      .collect(Collectors.toSet());
  }
}
