package averroes.gencg.android;

import averroes.options.AverroesOptions;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * This class parses configuration files for entry point classes.
 *
 * @author Linghui Luo
 */
public class EntryPointConfigurationReader {

  private HashMap<String, Set<String>> entryPointClasses;
  private HashMap<String, Set<String>> entryPointMethods;

  public EntryPointConfigurationReader() {
    String epClassPath = AverroesOptions.getEntryPointClasses();
    String epMethodPath = AverroesOptions.getEntryPointMethods();
    entryPointClasses = read(epClassPath);
    entryPointMethods = read(epMethodPath);
  }

  private HashMap<String, Set<String>> read(String path) {
    HashMap<String, Set<String>> ret = new HashMap<>();
    File file = new File(path);
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      reader
          .lines()
          .forEach(
              line -> {
                String[] words = line.split("_");
                if (words.length == 2) {
                  String signature = words[0].trim();
                  String type = words[1];
                  if (!ret.containsKey(type)) ret.put(type, new HashSet<>());
                  ret.get(type).add(signature);
                }
              });
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      throw new RuntimeException(e.getMessage());
    } catch (IOException e1) {
      e1.printStackTrace();
      throw new RuntimeException(e1.getMessage());
    }
    return ret;
  }

  public Set<String> getEntryPointClasses(String frameworkType) {
    return this.entryPointClasses.get(frameworkType);
  }

  public Set<String> getEntryPointMethods(String frameworkType) {
    return this.entryPointMethods.get(frameworkType);
  }
}
