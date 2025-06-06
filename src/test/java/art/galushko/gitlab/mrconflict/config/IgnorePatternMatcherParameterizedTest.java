package art.galushko.gitlab.mrconflict.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Parameterized tests for IgnorePatternMatcher.
 * These tests simulate property-based testing by using a variety of inputs.
 */
class IgnorePatternMatcherParameterizedTest {

    private final IgnorePatternMatcher matcher = new IgnorePatternMatcher();
    private final IgnorePatternMatcher caseInsensitiveMatcher = new IgnorePatternMatcher(false);

    @ParameterizedTest
    @MethodSource("exactFilePathsProvider")
    @DisplayName("Exact file path always matches itself")
    void exactFilePathAlwaysMatchesItself(String filePath) {
        // When matching against itself
        boolean result = matcher.matches(filePath, filePath);
        
        // Then it should match
        assertThat(result).isTrue();
    }

    static Stream<String> exactFilePathsProvider() {
        return Stream.of(
                "file.txt",
                "path/to/file.txt",
                "path\\to\\file.txt",
                "file with spaces.txt",
                "file-with-dashes.txt",
                "file_with_underscores.txt",
                "file.with.dots.txt",
                "file123.txt",
                "123file.txt",
                "very/deep/path/to/some/file.txt"
        );
    }

    @ParameterizedTest
    @MethodSource("directoryPatternsProvider")
    @DisplayName("Directory pattern matches files in that directory")
    void directoryPatternMatchesFilesInThatDirectory(String directoryPattern, String filePath) {
        // When matching the file against the directory pattern
        boolean result = matcher.matches(directoryPattern, filePath);
        
        // Then it should match
        assertThat(result).isTrue();
    }

    static Stream<Arguments> directoryPatternsProvider() {
        return Stream.of(
                Arguments.of("temp/", "temp/file.txt"),
                Arguments.of("temp/", "temp/subdir/file.txt"),
                Arguments.of("src/main/", "src/main/file.txt"),
                Arguments.of("src/main/", "src/main/java/file.txt"),
                Arguments.of("path/with spaces/", "path/with spaces/file.txt"),
                Arguments.of("path-with-dashes/", "path-with-dashes/file.txt"),
                Arguments.of("path_with_underscores/", "path_with_underscores/file.txt"),
                Arguments.of("path.with.dots/", "path.with.dots/file.txt"),
                Arguments.of("path123/", "path123/file.txt"),
                Arguments.of("123path/", "123path/file.txt")
        );
    }

    @ParameterizedTest
    @MethodSource("caseInsensitiveProvider")
    @DisplayName("Case insensitive matcher ignores case")
    void caseInsensitiveMatcherIgnoresCase(String pattern, String filePath) {
        // Create uppercase versions
        String uppercasePattern = pattern.toUpperCase();
        String uppercasePath = filePath.toUpperCase();
        
        // When matching with case insensitive matcher
        boolean result1 = caseInsensitiveMatcher.matches(pattern, uppercasePath);
        boolean result2 = caseInsensitiveMatcher.matches(uppercasePattern, filePath);
        
        // Then the results should be the same as the original match
        boolean originalMatch = caseInsensitiveMatcher.matches(pattern, filePath);
        assertThat(result1).isEqualTo(originalMatch);
        assertThat(result2).isEqualTo(originalMatch);
    }

    static Stream<Arguments> caseInsensitiveProvider() {
        return Stream.of(
                Arguments.of("file.txt", "file.txt"),
                Arguments.of("path/to/file.txt", "path/to/file.txt"),
                Arguments.of("*.txt", "file.txt"),
                Arguments.of("src/*.java", "src/Main.java"),
                Arguments.of("temp/", "temp/file.txt"),
                Arguments.of("**/*.js", "src/app.js"),
                Arguments.of("**/*.js", "src/subdir/app.js")
        );
    }

    @ParameterizedTest
    @MethodSource("pathSeparatorProvider")
    @DisplayName("Path separator normalization works consistently")
    void pathSeparatorNormalizationWorksConsistently(String pattern, String filePath) {
        // Create versions with different separators
        String backslashPattern = pattern.replace('/', '\\');
        String backslashPath = filePath.replace('/', '\\');
        
        // When matching with different separator styles
        boolean result1 = matcher.matches(pattern, filePath);
        boolean result2 = matcher.matches(backslashPattern, filePath);
        boolean result3 = matcher.matches(pattern, backslashPath);
        boolean result4 = matcher.matches(backslashPattern, backslashPath);
        
        // Then all results should be the same
        assertThat(result2).isEqualTo(result1);
        assertThat(result3).isEqualTo(result1);
        assertThat(result4).isEqualTo(result1);
    }

    static Stream<Arguments> pathSeparatorProvider() {
        return Stream.of(
                Arguments.of("path/to/file.txt", "path/to/file.txt"),
                Arguments.of("path/to/*", "path/to/file.txt"),
                Arguments.of("src/main/java/*", "src/main/java/file.txt"),
                Arguments.of("**/*.java", "src/main/java/file.java")
        );
    }

    @ParameterizedTest
    @MethodSource("leadingSlashProvider")
    @DisplayName("Leading slashes in file paths are handled consistently")
    void leadingSlashesInFilePathsAreHandledConsistently(String pattern, String filePath) {
        // Create versions with leading slashes
        String leadingSlashPath = "/" + filePath;
        
        // When matching with and without leading slashes
        boolean result1 = matcher.matches(pattern, filePath);
        boolean result2 = matcher.matches(pattern, leadingSlashPath);
        
        // Then the results should be the same
        assertThat(result2).isEqualTo(result1);
    }

    static Stream<Arguments> leadingSlashProvider() {
        return Stream.of(
                Arguments.of("file.txt", "file.txt"),
                Arguments.of("path/to/file.txt", "path/to/file.txt"),
                Arguments.of("*.txt", "file.txt"),
                Arguments.of("src/*.java", "src/Main.java")
        );
    }

    @Test
    @DisplayName("Multiple patterns are checked correctly")
    void multiplePatternAreCheckedCorrectly() {
        // Given a list of patterns and a file path
        List<String> patterns = Arrays.asList("*.txt", "src/*.java", "temp/");
        String filePath = "src/Main.java";
        
        // When checking if any pattern matches
        boolean anyMatch = patterns.stream()
                .anyMatch(pattern -> matcher.matches(pattern, filePath));
        
        // Then the result should be consistent with individual checks
        boolean expectedMatch = false;
        for (String pattern : patterns) {
            if (matcher.matches(pattern, filePath)) {
                expectedMatch = true;
                break;
            }
        }
        
        assertThat(anyMatch).isEqualTo(expectedMatch);
    }

    @ParameterizedTest
    @MethodSource("globPatternProvider")
    @DisplayName("Glob patterns match correctly")
    void globPatternsMatchCorrectly(String pattern, String filePath, boolean shouldMatch) {
        // When matching with glob pattern
        boolean result = matcher.matches(pattern, filePath);
        
        // Then the result should match the expected value
        assertThat(result).isEqualTo(shouldMatch);
    }

    static Stream<Arguments> globPatternProvider() {
        return Stream.of(
                Arguments.of("*.txt", "file.txt", true),
                Arguments.of("*.txt", "file.jpg", false),
                Arguments.of("src/*.js", "src/app.js", true),
                Arguments.of("src/*.js", "src/subdir/app.js", false),
                Arguments.of("**/*.js", "src/app.js", true),
                Arguments.of("**/*.js", "src/subdir/app.js", true),
                Arguments.of("src/**/*.java", "src/main/java/file.java", true),
                Arguments.of("src/**/*.java", "other/main/java/file.java", false),
                Arguments.of("**/test/**", "src/test/java/file.java", true),
                Arguments.of("**/test/**", "src/main/java/file.java", false)
        );
    }
}