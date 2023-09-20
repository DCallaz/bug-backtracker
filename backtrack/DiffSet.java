  public static List<String> includes = null;
  public static List<String> excludes = null;
  public DiffSet(String full_diff, String sha) {
    super(sha, sha, processDiff(sha, full_diff));
  public DiffSet(String full_diff, String sha, String id) {
    super(sha, id, processDiff(sha, full_diff));
  /**
   * Helper function to check the include/exclude conditions set.
   * @param file The file path to check.
   * @return Returns the string stripped of any source directories if matching,
   * otherwise returns null.
   */
  private static String checkInclExcl(String file) {
    boolean matches = false;
    Matcher m_save = null;
    if (includes != null) {
      for (String incl : includes) {
        incl = (incl.endsWith("/")) ? incl.substring(0, incl.length()-1) : incl;
        Pattern p = Pattern.compile("(?:\\w)*/"+incl+"/(\\S+)");
        Matcher m = p.matcher(file);
        matches |= m.lookingAt();
        if (m.lookingAt()) {
          m_save = m;
        }
      }
    } else { // If no source given, remove only first (git diff added) "directory"
      Pattern p = Pattern.compile("(?:\\w)/(\\S+)");
      Matcher m = p.matcher(file);
      matches |= m.lookingAt();
      if (m.lookingAt()) {
        m_save = m;
      }
    }
    if (excludes != null) {
      for (String excl : excludes) {
        excl = (excl.endsWith("/")) ? excl.substring(0, excl.length()-1) : excl;
        matches &= !file.matches("(?:\\w)*/"+excl+"/.*");
      }
    }
    if (matches) {
      return m_save.replaceFirst("$1");
    } else {
      return null;
    }
  }

  private static List<BugFile> processDiff(String sha, String full_diff) {
    outer:
          String file = line.substring(4);
          file = checkInclExcl(file);
          if (file != null && file.endsWith(EXT)) {
          // Checks if the file in included src, and updates the path if it is
          file = checkInclExcl(file);
          if (file == null) { //check if in the source directory
            String newFile = line.substring(4);
            newFile = checkInclExcl(newFile);
            if (newFile == null) { // File is named to something untracked, so remove it
              if (!rename) { // If this is not a rename, the file names must be =
                assert(file.equals(newFile));