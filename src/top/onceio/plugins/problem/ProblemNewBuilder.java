package top.onceio.plugins.problem;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemHighlightType;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Liar
 */
public class ProblemNewBuilder implements ProblemBuilder {
  private Set<OnceIOProblem> problems;

  public ProblemNewBuilder() {
    this(1);
  }

  public ProblemNewBuilder(int size) {
    this.problems = new HashSet<>(size);
  }

  public Set<OnceIOProblem> getProblems() {
    return problems;
  }

  public void addWarning(String message) {
    addProblem(message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
  }

  public void addWarning(String message, Object... params) {
    addProblem(String.format(message, params), ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
  }

  public void addError(String message) {
    addProblem(message, ProblemHighlightType.GENERIC_ERROR);
  }

  public void addError(String message, Object... params) {
    addProblem(String.format(message, params), ProblemHighlightType.GENERIC_ERROR);
  }

  public void addWarning(String message, LocalQuickFix... quickFixes) {
    addProblem(message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, quickFixes);
  }

  public void addError(String message, LocalQuickFix... quickFixes) {
    addProblem(message, ProblemHighlightType.GENERIC_ERROR, quickFixes);
  }

  public void addProblem(String message, ProblemHighlightType highlightType, LocalQuickFix... quickFixes) {
    problems.add(new OnceIOProblem(message, highlightType, quickFixes));
  }
}
