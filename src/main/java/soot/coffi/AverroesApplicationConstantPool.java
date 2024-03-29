package soot.coffi;

import averroes.options.AverroesOptions;
import averroes.soot.ClassFileProvider;
import averroes.soot.Hierarchy;
import averroes.util.BytecodeUtils;
import averroes.util.DexUtils;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.dexbacked.raw.FieldIdItem;
import org.jf.dexlib2.dexbacked.raw.MethodIdItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.ClassSource;
import soot.ResolutionFailedException;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootFieldRef;
import soot.SootMethod;
import soot.SourceLocator;
import soot.Type;

/**
 * A class that holds the values of library methods and fields found in the constant pool of
 * application classes. The class is in this specific package name because it accesses some
 * package-private classes (e.g., {@link CONSTANT_Fieldref_info}.
 *
 * @author karim
 */
public class AverroesApplicationConstantPool {
  private Set<SootClass> applicationClasses;
  private Set<SootMethod> libraryMethods;
  private Set<SootField> libraryFields;
  private Hierarchy hierarchy;
  private Logger logger = LoggerFactory.getLogger(getClass());
  /**
   * Initialize this constant pool with all the library methods and fields in the constant pool of
   * any application class.
   *
   * @param hierarchy
   */
  public AverroesApplicationConstantPool(Hierarchy hierarchy) {
    applicationClasses = new HashSet<SootClass>();
    libraryMethods = new HashSet<SootMethod>();
    libraryFields = new HashSet<SootField>();

    this.hierarchy = hierarchy;

    initialize();
  }

  /**
   * Get the Coffi class corresponding to the given Soot class. From this coffi class, we can get
   * the constant pool and all the related info.
   *
   * @param cls
   * @return
   */
  private static ClassFile getCoffiClass(SootClass cls) {
    return ClassFileProvider.getClassFile(cls);
  }

  /**
   * Get the set of library methods that appear in the constant pool of any application class.
   *
   * @return
   */
  public Set<SootMethod> getLibraryMethods() {
    return libraryMethods;
  }

  /**
   * Get the set of library fields that appear in the constant pool of any application class.
   *
   * @return
   */
  public Set<SootField> getLibraryFields() {
    return libraryFields;
  }

  /**
   * Get the set of classes that are referenced by name in the constant pool of any application
   * class.
   *
   * @return
   */
  public Set<SootClass> getApplicationClasses() {
    return applicationClasses;
  }

  /**
   * Check if the given field is a library field referenced by the application.
   *
   * @param field
   * @return
   */
  public boolean isLibraryFieldInApplicationConstantPool(SootField field) {
    return getLibraryFields().contains(field);
  }

  /**
   * Check if the given method is a library method referenced by the application.
   *
   * @param method
   * @return
   */
  public boolean isLibraryMethodInApplicationConstantPool(SootMethod method) {
    return getLibraryMethods().contains(method);
  }

  /** Initialize the application constant pool. */
  private void initialize() {
    findApplicationClassesReferencedByName();
    findLibraryMethodsInApplicationConstantPool();
    findLibraryFieldsInApplicationConstantPool();
  }

  /**
   * Get the referenced library methods in an application class.
   *
   * @param applicationClass
   * @return
   */
  private Set<SootMethod> findLibraryMethodsInConstantPool(SootClass applicationClass) {
    Set<SootMethod> result = new HashSet<SootMethod>();

    /*
     * This is only useful if the application class has any methods. Some classes
     * will not have any methods in them, e.g., org.jfree.data.xml.DatasetTags which
     * is an interface that has some final constants only.
     */
    if (applicationClass.getMethodCount() > 0) {
      ClassFile coffiClass = getCoffiClass(applicationClass);
      cp_info[] constantPool = coffiClass.constant_pool;

      for (cp_info constantPoolEntry : constantPool) {
        if (constantPoolEntry instanceof ICONSTANT_Methodref_info) {
          ICONSTANT_Methodref_info methodInfo = (ICONSTANT_Methodref_info) constantPoolEntry;
          // Get the method declaring class
          CONSTANT_Class_info c = (CONSTANT_Class_info) constantPool[methodInfo.getClassIndex()];
          if (c == null) continue;
          CONSTANT_Utf8_info cName = (CONSTANT_Utf8_info) (constantPool[c.name_index]);
          if (cName == null) continue;
          String className = cName.convert();
          className = className.replace('/', '.');
          // TODO why is that?
          if (className.charAt(0) == '[') {
            className = "java.lang.Object";
          }
          // Get the method name, parameter types, and return type
          CONSTANT_NameAndType_info i =
              (CONSTANT_NameAndType_info) constantPool[methodInfo.getNameAndTypeIndex()];
          if (i != null) {

            CONSTANT_Utf8_info utfMethodName = (CONSTANT_Utf8_info) constantPool[i.name_index];

            if (utfMethodName != null) {
              String methodName = utfMethodName.convert();
              CONSTANT_Utf8_info utfDescriptor =
                  (CONSTANT_Utf8_info) (constantPool[i.descriptor_index]);
              if (utfDescriptor != null) {
                String methodDescriptor = utfDescriptor.convert();
                SootMethod method =
                    BytecodeUtils.makeSootMethod(className, methodName, methodDescriptor);
                // If the resolved method is in the library, add it to the
                // result
                if (hierarchy.isLibraryMethod(method)) {
                  result.add(method);
                }
              } else {
                logger.info(
                    "AverroesApplicationConstantPool: couldn't resolve some method in "
                        + className
                        + " referenced by "
                        + applicationClass
                        + ". method descriptor is null.");
              }
            } else {
              logger.info(
                  "AverroesApplicationConstantPool: couldn't resolve some method in "
                      + className
                      + " referenced by "
                      + applicationClass
                      + ". method name is null.");
            }
          } else {
            logger.info(
                "AverroesApplicationConstantPool: couldn't resolve some method in "
                    + className
                    + " referenced by "
                    + applicationClass
                    + ". method is null.");
          }
        }
      }
    }

    return result;
  }

  /**
   * Find all the classes whose name is referenced in the constant pool of application classes.
   *
   * @throws IOException
   */
  private void findApplicationClassesReferencedByName() {
    applicationClasses = new HashSet<SootClass>();

    // If we're processing an android apk, process the global string
    // constant pool
    // if (Options.v().src_prec() == Options.src_prec_apk) {
    // applicationClasses.addAll(findAndroidApplicationClassesReferencedByName());
    // } else {
    // Add the classes whose name appear in the constant pool of application
    // classes
    // TODO
    // for (SootClass applicationClass : hierarchy.getApplicationClasses()) {
    // applicationClasses.addAll(findApplicationClassesReferencedByName(applicationClass));
    // }
    // applicationClasses.forEach(System.out::println);
    // System.out.println("averroes found " + substrings.size() +
    // " possible class name substrings");
    // substrings.forEach(System.out::println);
    // System.out.println("-------------");
    // classes.stream()
    // .filter(c -> {
    // if (substrings.stream().anyMatch(s -> c.getName().startsWith(s))
    // && substrings.stream().anyMatch(s -> c.getName().endsWith(s))) {
    // return true;
    // } else {
    // return false;
    // }
    // }).forEach(System.out::println);
    // applicationClasses.addAll(classes
    // .stream()
    // .filter(c -> {
    // if (substrings.stream().anyMatch(s -> c.getName().startsWith(s))
    // && substrings.stream().anyMatch(s -> c.getName().endsWith(s))) {
    // return true;
    // } else {
    // return false;
    // }
    // }).collect(Collectors.toSet()));
    // }
  }

  /**
   * Get the classes referenced by name in the constant pool of an application class.
   *
   * @param applicationClass
   */
  private Set<SootClass> findApplicationClassesReferencedByName(SootClass applicationClass) {
    Set<SootClass> result = new HashSet<SootClass>();
    // Set<SootClass> classes = new HashSet<SootClass>();
    // Set<String> substrings = new HashSet<String>();

    /*
     * This is only useful if the application class has any methods. Some classes
     * will not have any methods in them, e.g., org.jfree.data.xml.DatasetTags which
     * is an interface that has some final constants only.
     */
    if (applicationClass.getMethodCount() > 0) {
      ClassFile coffiClass = getCoffiClass(applicationClass);
      cp_info[] constantPool = coffiClass.constant_pool;

      for (cp_info constantPoolEntry : constantPool) {
        if (constantPoolEntry instanceof CONSTANT_String_info) {
          CONSTANT_String_info stringInfo = (CONSTANT_String_info) constantPoolEntry;

          // Get the class name
          CONSTANT_Utf8_info s = (CONSTANT_Utf8_info) constantPool[stringInfo.string_index];
          String className = s.convert(); // .trim();

          // if (className.length() > 2) {
          // Set<SootClass> set =
          // hierarchy.matchSubstrOfApplicationClass(className);
          // if (!set.isEmpty()) {
          // classes.addAll(set);
          // substrings.add(className);
          // }
          // }

          if (hierarchy.isApplicationClass(className)) {
            result.add(hierarchy.getClass(className));
          }
        }
      }

      /*
       * This filtering has to take place here (i.e., for each class separately).
       * Otherwise, we will collect a lot of class names that has substrings spanning
       * multiple class files which is both wrong and imprecise.
       */
      // result.addAll(classes
      // .stream()
      // .filter(c -> {
      // if (substrings.stream().anyMatch(s -> c.getName().startsWith(s))
      // && substrings.stream().anyMatch(s -> c.getName().endsWith(s))) {
      // return true;
      // } else {
      // return false;
      // }
      // }).collect(Collectors.toSet()));

    }

    return result;
  }

  /**
   * Find all the library methods referenced from the constant pool of application classes.
   *
   * @return
   */
  private void findLibraryMethodsInApplicationConstantPool() {
    libraryMethods = new HashSet<SootMethod>();

    // If we're processing an android apk, process the global method
    // constant pool
    if (AverroesOptions.isAndroidApk()) {
      libraryMethods.addAll(findLibraryMethodsInAndroidApplicationConstantPool());
    } else {
      // Add the library methods that appear in the constant pool of
      // application classes
      for (SootClass applicationClass : hierarchy.getApplicationClasses()) {
        libraryMethods.addAll(findLibraryMethodsInConstantPool(applicationClass));
      }
    }
  }

  private Set<SootMethod> findLibraryMethodsInAndroidApplicationConstantPool() {
    Set<SootMethod> result = new HashSet<SootMethod>();
    try {
      DexBackedDexFile rawDex = DexUtils.getRawDex(new File(AverroesOptions.getAndroidApk()), null);
      String[] methods = MethodIdItem.getMethods(rawDex);
      for (String s : methods) {
        String[] parts = s.split("->");
        String className = "";
        if (!parts[0].startsWith("L") && !parts[0].startsWith("[L")) {
          // primitive types:
          // [I->clone()Ljava/lang/Object; Integer
          // [J->clone()Ljava/lang/Object; Long
          className = "Ljava.lang.Object;";
        } else className = parts[0];

        className = className.replace('/', '.');
        // Remove "L" and ";"
        if (className.startsWith("["))
          // eg: [Landroidx/annotation/RestrictTo$Scope;->clone()Ljava/lang/Object;
          className = className.substring(2, className.length() - 1);
        else
          // eg:
          // Lorg/xmlpull/v1/XmlSerializer;->startDocument(Ljava/lang/String;Ljava/lang/Boolean;)V
          className = className.substring(1, className.length() - 1);
        if (!AverroesOptions.isLoadedApplicationClass(className)) {
          int idx = parts[1].indexOf('(');
          String methodName = parts[1].substring(0, idx);
          String methodDescriptor = parts[1].substring(idx);
          List<Type> parameterTypes = BytecodeUtils.getParameterTypes(methodDescriptor);
          Type returnType = BytecodeUtils.getReturnType(methodDescriptor);
          SootClass klass = Scene.v().getSootClass(className);
          SootMethod method = searchMethod(klass, methodName, parameterTypes, returnType);
          if (method == null && klass.getMethods().isEmpty()) {
            ClassSource cc = SourceLocator.v().getClassSource(className);
            if (cc != null) {
              cc.resolve(klass);
              method = searchMethod(klass, methodName, parameterTypes, returnType);
            }
          }
          if (method != null) result.add(method);
          else
            logger.info(
                "AverroesApplicationConstantPool (Android): couldn't resolve method "
                    + methodName
                    + " in "
                    + className);
        }
      }
    } catch (IOException e1) {
      e1.printStackTrace();
    }
    return result;
  }

  /**
   * Get the referenced library fields in an application class.
   *
   * @param applicationClass
   * @return
   */
  private Set<SootField> findLibraryFieldsInConstantPool(SootClass applicationClass) {
    Set<SootField> result = new HashSet<SootField>();

    /*
     * This is only useful if the application class has any methods. Some classes
     * will not have any methods in them, e.g., org.jfree.data.xml.DatasetTags which
     * is an interface that has some final constants only.
     */
    if (applicationClass.getMethodCount() > 0) {
      ClassFile coffiClass = getCoffiClass(applicationClass);
      cp_info[] constantPool = coffiClass.constant_pool;

      for (cp_info constantPoolEntry : constantPool) {
        if (constantPoolEntry instanceof CONSTANT_Fieldref_info) {
          CONSTANT_Fieldref_info fieldInfo = (CONSTANT_Fieldref_info) constantPoolEntry;

          // Get the field declaring class
          CONSTANT_Class_info c = (CONSTANT_Class_info) constantPool[fieldInfo.class_index];
          if (c != null) {
            CONSTANT_Utf8_info utfclass = (CONSTANT_Utf8_info) (constantPool[c.name_index]);
            if (utfclass != null) {
              String className = utfclass.convert();
              className = className.replace('/', '.');
              // TODO why is that?
              if (className.charAt(0) == '[') {
                className = "java.lang.Object";
              }
              SootClass cls = Scene.v().getSootClass(className);
              // Get the field name, and type
              CONSTANT_NameAndType_info i =
                  (CONSTANT_NameAndType_info) constantPool[fieldInfo.name_and_type_index];
              if (i == null) continue;
              cp_info info = constantPool[i.name_index];
              if (info == null) continue;
              String fieldName = ((CONSTANT_Utf8_info) (info)).convert();
              String fieldDescriptor =
                  ((CONSTANT_Utf8_info) (constantPool[i.descriptor_index])).convert();
              Type fieldType = Util.v().jimpleTypeOfFieldDescriptor(fieldDescriptor);

              // Get the field ref and resolve it to a Soot field
              SootFieldRef fieldRef = Scene.v().makeFieldRef(cls, fieldName, fieldType, false);
              SootField field;

              /*
               * We have to do this ugly code. Try first and see if the field is not static.
               * If it is static, then create a new fieldRef in the catch and resolve it again
               * with isStatic = true.
               */
              try {
                field = fieldRef.resolve();
              } catch (ResolutionFailedException e) {
                fieldRef = Scene.v().makeFieldRef(cls, fieldName, fieldType, true);
              }
              field = fieldRef.resolve();

              // If the resolved field is in the library, add it to the
              // result
              if (hierarchy.isLibraryField(field)) {
                result.add(field);
              }
            } else {
              logger.info(
                  "AverroesApplicationConstantPool: couldn't resolve the declaring class of some field referenced by "
                      + applicationClass);
            }
          } else {
            logger.info(
                "AverroesApplicationConstantPool: couldn't resolve some field referenced by "
                    + applicationClass);
          }
        }
      }
    }

    return result;
  }

  /**
   * Get all the library fields referenced from the constant pool of application classes.
   *
   * @return
   */
  private void findLibraryFieldsInApplicationConstantPool() {
    libraryFields = new HashSet<SootField>();
    // If we're processing an android apk, process the global field constant
    // pool
    if (AverroesOptions.isAndroidApk()) {
      libraryFields.addAll(findLibraryFieldsInAndroidApplicationConstantPool());
    } else {
      // Add the library methods that appear in the constant pool of
      // application classes
      for (SootClass applicationClass : hierarchy.getApplicationClasses()) {
        libraryFields.addAll(findLibraryFieldsInConstantPool(applicationClass));
      }
    }
  }

  private Set<SootField> findLibraryFieldsInAndroidApplicationConstantPool() {
    Set<SootField> result = new HashSet<SootField>();
    try {
      DexBackedDexFile rawDex = DexUtils.getRawDex(new File(AverroesOptions.getAndroidApk()), null);
      String[] fields = FieldIdItem.getFields(rawDex);
      for (String s : fields) {
        String[] parts = s.split("(->|:)");
        String className = parts[0];
        className = className.replace('/', '.');
        // Remove "L" and ";"
        className = className.substring(1, className.length() - 1);
        String fieldName = parts[1];
        String fieldDescriptor = parts[2];
        Type fieldType = Util.v().jimpleTypeOfFieldDescriptor(fieldDescriptor);
        SootClass klass = Scene.v().getSootClass(className);
        SootField field = searchField(klass, fieldName, fieldType);
        if (field == null && klass.getFields().isEmpty()) {
          ClassSource cc = SourceLocator.v().getClassSource(className);
          if (cc != null) {
            cc.resolve(klass);
            field = searchField(klass, fieldName, fieldType);
          }
        }
        // If the resolved field is in the library, add it to the
        // result
        if (field != null && hierarchy.isLibraryField(field)) {
          result.add(field);
        }
      }
    } catch (IOException e1) {
      e1.printStackTrace();
    }
    return result;
  }

  /**
   * This method searches a field in a class and its parent classes.
   *
   * @param klass
   * @param fieldName
   * @param fieldType
   * @return
   */
  private SootField searchField(SootClass klass, String fieldName, Type fieldType) {
    SootField field = klass.getFieldUnsafe(fieldName, fieldType);
    if (field != null) return field;
    if (klass.hasSuperclass()) field = searchField(klass.getSuperclass(), fieldName, fieldType);
    if (field != null) return field;
    for (SootClass i : klass.getInterfaces()) {
      field = searchField(i, fieldName, fieldType);
      if (field != null) return field;
    }
    if (klass.hasOuterClass()) field = searchField(klass.getOuterClass(), fieldName, fieldType);
    return field;
  }

  /**
   * This method searches a method in a class and its parent classes.
   *
   * @param klass
   * @param methodName
   * @param parameterTypes
   * @param returnType
   * @return
   */
  private SootMethod searchMethod(
      SootClass klass, String methodName, List<Type> parameterTypes, Type returnType) {
    SootMethod method = klass.getMethodUnsafe(methodName, parameterTypes, returnType);
    if (method != null) return method;
    if (klass.hasSuperclass()) {
      SootClass superClass = klass.getSuperclass();
      if (superClass.getMethods().isEmpty()) {
        ClassSource cc = SourceLocator.v().getClassSource(superClass.getName());
        cc.resolve(superClass);
      }
      method = searchMethod(superClass, methodName, parameterTypes, returnType);
    }
    if (method != null) return method;
    for (SootClass i : klass.getInterfaces()) {
      method = searchMethod(i, methodName, parameterTypes, returnType);
      if (method != null) return method;
    }
    if (klass.hasOuterClass())
      method = searchMethod(klass.getOuterClass(), methodName, parameterTypes, returnType);
    return method;
  }
}
