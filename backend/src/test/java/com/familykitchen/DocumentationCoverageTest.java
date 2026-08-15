package com.familykitchen;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.ParamTree;
import com.sun.source.doctree.ReturnTree;
import com.sun.source.doctree.ThrowsTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.DocTrees;
import com.sun.source.util.DocTreeScanner;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements.Origin;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;

/**
 * 检查生产与测试源码中需要维护者阅读的声明是否具备完整 Javadoc 覆盖。
 */
class DocumentationCoverageTest {

  private static final Path PROJECT_ROOT = locateProjectRoot();
  private static final Set<String> JUNIT_METHOD_ANNOTATIONS = Set.of(
      "org.junit.jupiter.api.Test",
      "org.junit.jupiter.api.RepeatedTest",
      "org.junit.jupiter.api.TestFactory",
      "org.junit.jupiter.api.TestTemplate",
      "org.junit.jupiter.params.ParameterizedTest");

  @Test
  void requiredDeclarationsHaveCompleteDocumentation() {
    List<Path> sources = new ArrayList<>();
    sources.addAll(javaFiles(PROJECT_ROOT.resolve("src/main/java")));
    sources.addAll(javaFiles(PROJECT_ROOT.resolve("src/test/java")));
    Set<String> scopes = requestedScopes();
    validateRequestedScopes(scopes, sources);

    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    if (compiler == null) {
      fail("Documentation coverage requires a JDK, not a JRE");
    }
    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    List<String> violations = new ArrayList<>();
    try (StandardJavaFileManager fileManager =
             compiler.getStandardFileManager(diagnostics, Locale.ROOT, StandardCharsets.UTF_8)) {
      Iterable<? extends JavaFileObject> inputs = fileManager.getJavaFileObjectsFromPaths(sources);
      List<String> options = List.of("--release", "17", "-proc:none", "-classpath",
          System.getProperty("java.class.path"));
      JavacTask task = (JavacTask) compiler.getTask(null, fileManager, diagnostics,
          options, null, inputs);
      List<CompilationUnitTree> units = new ArrayList<>();
      task.parse().forEach(units::add);
      task.analyze();
      failOnCompilerErrors(diagnostics);

      DocTrees docTrees = DocTrees.instance(task);
      Elements elements = task.getElements();
      Types types = task.getTypes();
      for (CompilationUnitTree unit : units) {
        Path source = Path.of(unit.getSourceFile().toUri()).toAbsolutePath().normalize();
        if (inScope(source, scopes)) {
          new CoverageScanner(docTrees, elements, types, source, violations).scan(unit, null);
        }
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    if (!violations.isEmpty()) {
      fail("Javadoc coverage has " + violations.size() + " violation(s):\n"
          + String.join("\n", violations));
    }
  }

  @Test
  void exceptionTagsAcceptOnlyExactSimpleOrQualifiedNames() {
    assertTrue(exactTypeName("java.io.IOException", "IOException", "IOException"));
    assertTrue(exactTypeName("java.io.IOException", "IOException", "java.io.IOException"));
    assertFalse(exactTypeName("java.io.IOException", "IOException", "io.IOException"));
    assertFalse(exactTypeName("java.io.IOException", "IOException", "MyIOException"));
  }

  /** 执行逐声明文档规则并记录全部缺口。 */
  private static final class CoverageScanner extends TreePathScanner<Void, Void> {
    private final DocTrees docTrees;
    private final Elements elements;
    private final Types types;
    private final Path source;
    private final List<String> violations;
    private final Deque<ClassContext> classes = new ArrayDeque<>();

    private CoverageScanner(DocTrees docTrees, Elements elements, Types types,
        Path source, List<String> violations) {
      this.docTrees = docTrees;
      this.elements = elements;
      this.types = types;
      this.source = source;
      this.violations = violations;
    }

    /** {@inheritDoc} */
    @Override
    public Void visitClass(ClassTree tree, Void unused) {
      if (tree.getSimpleName().length() == 0) {
        return null;
      }
      TreePath path = getCurrentPath();
      TypeElement element = asType(docTrees.getElement(path));
      DocCommentTree doc = requireDoc(path, "type " + tree.getSimpleName());
      validateTypeTags(tree, element, doc);
      boolean configurationProperties = hasAnnotation(tree, "ConfigurationProperties");
      Set<String> recordComponents = element == null ? Set.of()
          : element.getRecordComponents().stream()
              .map(component -> component.getSimpleName().toString())
              .collect(Collectors.toCollection(TreeSet::new));
      classes.push(new ClassContext(tree.getSimpleName().toString(), element,
          configurationProperties || (!classes.isEmpty() && classes.peek().configurationProperties()),
          recordComponents));
      try {
        return super.visitClass(tree, unused);
      } finally {
        classes.pop();
      }
    }

    /** {@inheritDoc} */
    @Override
    public Void visitMethod(MethodTree tree, Void unused) {
      if (isJUnitTest(tree)) {
        return super.visitMethod(tree, unused);
      }
      Element element = docTrees.getElement(getCurrentPath());
      if (element != null && elements.getOrigin(element) != Origin.EXPLICIT) {
        return super.visitMethod(tree, unused);
      }
      Set<Modifier> modifiers = element == null
          ? tree.getModifiers().getFlags() : element.getModifiers();
      boolean constructor = tree.getReturnType() == null;
      boolean visible = modifiers.contains(Modifier.PUBLIC) || modifiers.contains(Modifier.PROTECTED);
      boolean staticFactory = isStaticFactory(element);
      if (visible || staticFactory) {
        String name = constructor && !classes.isEmpty() ? classes.peek().name()
            : tree.getName().toString();
        DocCommentTree doc = requireDoc(getCurrentPath(), "method " + name);
        if (doc != null) {
          validateMethodTags(tree, element, doc, name);
        }
      }
      return super.visitMethod(tree, unused);
    }

    /** {@inheritDoc} */
    @Override
    public Void visitVariable(VariableTree tree, Void unused) {
      Element element = docTrees.getElement(getCurrentPath());
      if (element == null || elements.getOrigin(element) != Origin.EXPLICIT
          || element.getKind() == ElementKind.RECORD_COMPONENT
          || (!classes.isEmpty() && classes.peek().recordComponents()
              .contains(tree.getName().toString()))) {
        return super.visitVariable(tree, unused);
      }
      boolean required = element.getKind() == ElementKind.ENUM_CONSTANT;
      if (element.getKind() == ElementKind.FIELD) {
        Set<Modifier> modifiers = element.getModifiers();
        boolean visible = modifiers.contains(Modifier.PUBLIC)
            || modifiers.contains(Modifier.PROTECTED);
        boolean configurationProperty = !classes.isEmpty()
            && classes.peek().configurationProperties();
        boolean modelField = isModelDataFile(source)
            && !"serialVersionUID".contentEquals(tree.getName());
        required = visible || configurationProperty || modelField;
      }
      if (required) {
        requireDoc(getCurrentPath(), "field " + tree.getName());
      }
      return super.visitVariable(tree, unused);
    }

    private void validateTypeTags(ClassTree tree, TypeElement element, DocCommentTree doc) {
      if (doc == null) {
        return;
      }
      Set<String> typeParameters = tree.getTypeParameters().stream()
          .map(parameter -> parameter.getName().toString())
          .collect(Collectors.toCollection(TreeSet::new));
      Set<String> components = Set.of();
      if (tree.getKind() == Tree.Kind.RECORD && element != null) {
        components = element.getRecordComponents().stream()
            .map(component -> component.getSimpleName().toString())
            .collect(Collectors.toCollection(TreeSet::new));
      }
      validateParamTags(doc, components, typeParameters,
          (tree.getKind() == Tree.Kind.RECORD ? "record " : "type ") + tree.getSimpleName());
      rejectUnexpectedReturnAndThrows(doc, Map.of(), false, "type " + tree.getSimpleName());
    }

    private void validateMethodTags(MethodTree tree, Element element, DocCommentTree doc,
        String name) {
      if (containsInheritDoc(doc)) {
        if (!isResolvedOverride(element)) {
          violation(tree, "method " + name + " uses {@inheritDoc} but is not a resolved override");
        }
        return;
      }
      Set<String> parameters = tree.getParameters().stream()
          .map(parameter -> parameter.getName().toString())
          .collect(Collectors.toCollection(TreeSet::new));
      Set<String> typeParameters = tree.getTypeParameters().stream()
          .map(TypeParameterTree::getName).map(Object::toString)
          .collect(Collectors.toCollection(TreeSet::new));
      validateParamTags(doc, parameters, typeParameters, "method " + name);
      boolean needsReturn = tree.getReturnType() != null
          && !"void".equals(tree.getReturnType().toString());
      Map<String, String> thrown = resolvedThrownNames(element, tree);
      rejectUnexpectedReturnAndThrows(doc, thrown, needsReturn, "method " + name);
    }

    private void validateParamTags(DocCommentTree doc, Set<String> values, Set<String> types,
        String declaration) {
      List<ParamTree> tags = doc.getBlockTags().stream()
          .filter(tag -> tag.getKind() == DocTree.Kind.PARAM)
          .map(ParamTree.class::cast).toList();
      for (String expected : values) {
        validateNamedTag(tags, expected, false, declaration);
      }
      for (String expected : types) {
        validateNamedTag(tags, expected, true, declaration);
      }
      for (ParamTree tag : tags) {
        String name = tag.getName().getName().toString();
        Set<String> expected = tag.isTypeParameter() ? types : values;
        if (!expected.contains(name)) {
          violation(null, declaration + " has unexpected @param "
              + (tag.isTypeParameter() ? "<" + name + ">" : name));
        }
        if (tag.getDescription().isEmpty()) {
          violation(null, declaration + " has empty @param " + name);
        }
      }
    }

    private void validateNamedTag(List<ParamTree> tags, String expected, boolean typeParameter,
        String declaration) {
      long count = tags.stream().filter(tag -> tag.isTypeParameter() == typeParameter)
          .filter(tag -> expected.equals(tag.getName().getName().toString())).count();
      if (count == 0) {
        violation(null, declaration + " is missing @param "
            + (typeParameter ? "<" + expected + ">" : expected));
      } else if (count > 1) {
        violation(null, declaration + " has duplicate @param " + expected);
      }
    }

    private void rejectUnexpectedReturnAndThrows(DocCommentTree doc, Map<String, String> thrown,
        boolean needsReturn, String declaration) {
      List<ReturnTree> returns = doc.getBlockTags().stream()
          .filter(tag -> tag.getKind() == DocTree.Kind.RETURN)
          .map(ReturnTree.class::cast).toList();
      if (needsReturn && returns.isEmpty()) {
        violation(null, declaration + " is missing @return");
      }
      if (!needsReturn && !returns.isEmpty()) {
        violation(null, declaration + " has unexpected @return");
      }
      if (returns.size() > 1) {
        violation(null, declaration + " has duplicate @return");
      }
      if (returns.stream().anyMatch(tag -> tag.getDescription().isEmpty())) {
        violation(null, declaration + " has empty @return");
      }

      List<ThrowsTree> tags = doc.getBlockTags().stream()
          .filter(tag -> tag.getKind() == DocTree.Kind.THROWS || tag.getKind() == DocTree.Kind.EXCEPTION)
          .map(ThrowsTree.class::cast).toList();
      for (Map.Entry<String, String> expected : thrown.entrySet()) {
        long count = tags.stream().filter(tag -> exactTypeName(expected.getKey(),
            expected.getValue(), tag.getExceptionName().toString())).count();
        if (count == 0) {
          violation(null, declaration + " is missing @throws " + expected.getKey());
        } else if (count > 1) {
          violation(null, declaration + " has duplicate @throws " + expected.getKey());
        }
      }
      for (ThrowsTree tag : tags) {
        String actual = tag.getExceptionName().toString();
        if (thrown.entrySet().stream().noneMatch(expected -> exactTypeName(
            expected.getKey(), expected.getValue(), actual))) {
          violation(null, declaration + " has unexpected @throws " + actual);
        }
        if (tag.getDescription().isEmpty()) {
          violation(null, declaration + " has empty @throws " + actual);
        }
      }
    }

    private Map<String, String> resolvedThrownNames(Element element, MethodTree tree) {
      Map<String, String> result = new TreeMap<>();
      if (element instanceof ExecutableElement method) {
        method.getThrownTypes().forEach(thrownType -> {
          Element thrownElement = types.asElement(thrownType);
          String qualified = thrownElement instanceof TypeElement type
              ? type.getQualifiedName().toString() : thrownType.toString();
          String simple = thrownElement instanceof TypeElement type
              ? type.getSimpleName().toString() : thrownType.toString();
          result.put(qualified, simple);
        });
      } else {
        tree.getThrows().stream().map(Object::toString).sorted()
            .forEach(name -> result.put(name, simpleName(name)));
      }
      return result;
    }

    private boolean isStaticFactory(Element element) {
      if (!(element instanceof ExecutableElement method) || classes.isEmpty()
          || classes.peek().element() == null
          || !method.getModifiers().contains(Modifier.STATIC)
          || method.getKind() != ElementKind.METHOD) {
        return false;
      }
      return types.isSameType(types.erasure(method.getReturnType()),
          types.erasure(classes.peek().element().asType()));
    }

    private boolean isJUnitTest(MethodTree tree) {
      for (var annotation : tree.getModifiers().getAnnotations()) {
        TreePath annotationType = new TreePath(getCurrentPath(), annotation.getAnnotationType());
        Element annotationElement = docTrees.getElement(annotationType);
        if (annotationElement instanceof TypeElement type
            && JUNIT_METHOD_ANNOTATIONS.contains(type.getQualifiedName().toString())) {
          return true;
        }
      }
      return false;
    }

    private DocCommentTree requireDoc(TreePath path, String declaration) {
      DocCommentTree doc = docTrees.getDocCommentTree(path);
      if (doc == null) {
        violation(path.getLeaf(), declaration + " is missing Javadoc");
      } else if (doc.getFullBody().isEmpty() && !containsInheritDoc(doc)) {
        violation(path.getLeaf(), declaration + " has no description");
      }
      if (doc != null && containsErroneousDoc(doc)) {
        violation(path.getLeaf(), declaration + " contains invalid Javadoc syntax");
      }
      return doc;
    }

    private boolean isResolvedOverride(Element element) {
      if (!(element instanceof ExecutableElement method) || classes.isEmpty()
          || classes.peek().element() == null) {
        return false;
      }
      TypeElement owner = classes.peek().element();
      for (var supertype : types.directSupertypes(owner.asType())) {
        Element superElement = types.asElement(supertype);
        if (superElement instanceof TypeElement superOwner) {
          for (ExecutableElement candidate : ElementFilter.methodsIn(elements.getAllMembers(superOwner))) {
            if (elements.overrides(method, candidate, owner)) {
              return true;
            }
          }
        }
      }
      return false;
    }

    private void violation(Tree tree, String message) {
      Tree locatedTree = tree == null ? getCurrentPath().getLeaf() : tree;
      long line = docTrees.getSourcePositions().getStartPosition(
          getCurrentPath().getCompilationUnit(), locatedTree);
      if (line >= 0) {
        line = getCurrentPath().getCompilationUnit().getLineMap().getLineNumber(line);
      }
      violations.add(relative(source) + (line > 0 ? ":" + line : "") + " " + message);
    }
  }

  /**
   * 保存当前嵌套类型的覆盖判定上下文。
   *
   * @param name 类型简单名称
   * @param element 编译器类型元素
   * @param configurationProperties 是否属于配置属性类型
   * @param recordComponents Record 组件名称集合
   */
  private record ClassContext(String name, TypeElement element, boolean configurationProperties,
                              Set<String> recordComponents) {
  }

  private static boolean containsInheritDoc(DocCommentTree doc) {
    return doc.toString().contains("{@inheritDoc}");
  }

  private static boolean containsErroneousDoc(DocCommentTree doc) {
    Boolean result = new DocTreeScanner<Boolean, Void>() {
      /** {@inheritDoc} */
      @Override
      public Boolean scan(DocTree tree, Void unused) {
        return tree != null && (tree.getKind() == DocTree.Kind.ERRONEOUS
            || Boolean.TRUE.equals(super.scan(tree, unused)));
      }

      /** {@inheritDoc} */
      @Override
      public Boolean reduce(Boolean first, Boolean second) {
        return Boolean.TRUE.equals(first) || Boolean.TRUE.equals(second);
      }
    }.scan(doc, null);
    return Boolean.TRUE.equals(result);
  }

  private static boolean hasAnnotation(ClassTree tree, String name) {
    return tree.getModifiers().getAnnotations().stream()
        .map(annotation -> annotation.getAnnotationType().toString())
        .anyMatch(annotation -> annotation.equals(name) || annotation.endsWith("." + name));
  }

  private static boolean exactTypeName(String qualified, String simple, String actual) {
    return actual.equals(qualified) || actual.equals(simple);
  }

  private static String simpleName(String qualified) {
    int separator = Math.max(qualified.lastIndexOf('.'), qualified.lastIndexOf('$'));
    return separator < 0 ? qualified : qualified.substring(separator + 1);
  }

  private static TypeElement asType(Element element) {
    return element instanceof TypeElement type ? type : null;
  }

  private static boolean isModelDataFile(Path source) {
    String normalized = relative(source);
    return normalized.matches(".*/model/(dto|vo|bo|entity)/.*\\.java");
  }

  private static Set<String> requestedScopes() {
    String configured = System.getProperty("documentation.scope", "").trim();
    if (configured.isEmpty()) {
      return Set.of();
    }
    return java.util.Arrays.stream(configured.split(","))
        .map(String::trim).filter(value -> !value.isEmpty())
        .collect(Collectors.toCollection(TreeSet::new));
  }

  private static void validateRequestedScopes(Set<String> scopes, List<Path> sources) {
    String configured = System.getProperty("documentation.scope", "").trim();
    if (configured.isEmpty()) {
      return;
    }
    Set<String> available = sources.stream().map(DocumentationCoverageTest::moduleFor)
        .collect(Collectors.toCollection(TreeSet::new));
    Set<String> unknown = new TreeSet<>(scopes);
    unknown.removeAll(available);
    if (scopes.isEmpty() || !unknown.isEmpty()) {
      fail("Unknown documentation.scope value(s): "
          + (scopes.isEmpty() ? configured : String.join(",", unknown))
          + "; available scopes: " + String.join(",", available));
    }
  }

  private static boolean inScope(Path source, Set<String> scopes) {
    if (scopes.isEmpty()) {
      return true;
    }
    return scopes.contains(moduleFor(source));
  }

  private static String moduleFor(Path source) {
    String normalized = relative(source);
    String marker = "/com/familykitchen/";
    int index = normalized.indexOf(marker);
    if (index < 0) {
      return "";
    }
    String packagePath = normalized.substring(index + marker.length());
    int slash = packagePath.indexOf('/');
    return slash < 0 ? "root" : packagePath.substring(0, slash);
  }

  private static List<Path> javaFiles(Path root) {
    try (var stream = Files.walk(root)) {
      return stream.filter(Files::isRegularFile)
          .filter(path -> path.toString().endsWith(".java"))
          .sorted(Comparator.comparing(Path::toString)).toList();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static void failOnCompilerErrors(DiagnosticCollector<JavaFileObject> diagnostics) {
    List<String> errors = diagnostics.getDiagnostics().stream()
        .filter(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR)
        .map(Object::toString).toList();
    if (!errors.isEmpty()) {
      fail("Java source parsing/analysis failed:\n" + String.join("\n", errors));
    }
  }

  private static String relative(Path path) {
    return PROJECT_ROOT.relativize(path.toAbsolutePath().normalize())
        .toString().replace('\\', '/');
  }

  private static Path locateProjectRoot() {
    Path current = Path.of("").toAbsolutePath().normalize();
    if (Files.isDirectory(current.resolve("src/main/java"))) {
      return current;
    }
    Path backend = current.resolve("backend");
    if (Files.isDirectory(backend.resolve("src/main/java"))) {
      return backend;
    }
    throw new IllegalStateException("Cannot locate backend project from " + current);
  }
}
