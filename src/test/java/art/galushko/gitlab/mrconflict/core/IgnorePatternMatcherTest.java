package art.galushko.gitlab.mrconflict.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for IgnorePatternMatcher.
 */
class IgnorePatternMatcherTest {

    private final IgnorePatternMatcher matcher = new IgnorePatternMatcher();
    private final IgnorePatternMatcher caseInsensitiveMatcher = new IgnorePatternMatcher(false);

    @Test
    @DisplayName("Should return false for null or empty inputs")
    void shouldReturnFalseForNullOrEmptyInputs() {
        // Given null or empty inputs

        // Then
        assertThat(matcher.matches(null, "file.txt")).isFalse();
        assertThat(matcher.matches("", "file.txt")).isFalse();
        assertThat(matcher.matches("*.txt", null)).isFalse();
        assertThat(matcher.matches("*.txt", "")).isFalse();
        assertThat(matcher.matches(null, null)).isFalse();
        assertThat(matcher.matches("", "")).isFalse();
    }

    @ParameterizedTest
    @DisplayName("Should match file paths correctly")
    @MethodSource("filePathMatchingProvider")
    void shouldMatchFilePaths(String pattern, String filePath, boolean expected) {
        // When
        boolean result = matcher.matches(pattern, filePath);

        // Then
        assertThat(result).isEqualTo(expected);
    }

    static Stream<Arguments> filePathMatchingProvider() {
        return Stream.of(
                // Exact file paths
                Arguments.of("file.txt", "file.txt", true),
                Arguments.of("path/to/file.txt", "path/to/file.txt", true),
                Arguments.of("file.txt", "other.txt", false),
                Arguments.of("path/to/file.txt", "path/to/other.txt", false),

                // Self-matching paths
                Arguments.of("file with spaces.txt", "file with spaces.txt", true),
                Arguments.of("file-with-dashes.txt", "file-with-dashes.txt", true),
                Arguments.of("file_with_underscores.txt", "file_with_underscores.txt", true),
                Arguments.of("file.with.dots.txt", "file.with.dots.txt", true),
                Arguments.of("file123.txt", "file123.txt", true),
                Arguments.of("123file.txt", "123file.txt", true),
                Arguments.of("very/deep/path/to/some/file.txt", "very/deep/path/to/some/file.txt", true),

                // Directory patterns
                Arguments.of("temp/", "temp/file.txt", true),
                Arguments.of("temp/", "temp/subdir/file.txt", true),
                Arguments.of("temp/", "temp", true),
                Arguments.of("temp/", "other/file.txt", false),
                Arguments.of("src/main/", "src/main/file.txt", true),
                Arguments.of("src/main/", "src/main/java/file.txt", true),
                Arguments.of("path/with spaces/", "path/with spaces/file.txt", true),
                Arguments.of("path-with-dashes/", "path-with-dashes/file.txt", true),
                Arguments.of("path_with_underscores/", "path_with_underscores/file.txt", true),
                Arguments.of("path.with.dots/", "path.with.dots/file.txt", true),
                Arguments.of("path123/", "path123/file.txt", true),
                Arguments.of("123path/", "123path/file.txt", true),

                // Glob patterns
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

    @ParameterizedTest
    @DisplayName("Should handle path normalization correctly")
    @MethodSource("pathNormalizationProvider")
    void shouldHandlePathNormalization(String pattern, String filePath, String modifiedPath) {
        // When
        boolean originalResult = matcher.matches(pattern, filePath);
        boolean modifiedResult = matcher.matches(pattern, modifiedPath);

        // Then
        assertThat(modifiedResult).isEqualTo(originalResult);
    }

    static Stream<Arguments> pathNormalizationProvider() {
        return Stream.of(
                // Path separator normalization
                Arguments.of("path/to/file.txt", "path/to/file.txt", "path\\to\\file.txt"),
                Arguments.of("path/to/*", "path/to/file.txt", "path\\to\\file.txt"),
                Arguments.of("src/main/java/*", "src/main/java/file.txt", "src\\main\\java\\file.txt"),
                Arguments.of("**/*.java", "src/main/java/file.java", "src\\main\\java\\file.java"),

                // Leading slashes
                Arguments.of("file.txt", "file.txt", "/file.txt"),
                Arguments.of("path/to/file.txt", "path/to/file.txt", "/path/to/file.txt"),
                Arguments.of("*.txt", "file.txt", "/file.txt"),
                Arguments.of("src/*.java", "src/Main.java", "/src/Main.java")
        );
    }

    @ParameterizedTest
    @DisplayName("Should handle case sensitivity correctly")
    @MethodSource("caseSensitivityProvider")
    void shouldHandleCaseSensitivity(String pattern, String filePath, String modifiedCase) {
        // When
        boolean caseSensitiveResult = matcher.matches(pattern, filePath);
        boolean caseInsensitiveResult = caseInsensitiveMatcher.matches(pattern, modifiedCase);
        boolean originalCaseInsensitiveMatch = caseInsensitiveMatcher.matches(pattern, filePath);

        // Then
        assertThat(caseSensitiveResult).isEqualTo(true);
        assertThat(caseInsensitiveResult).isEqualTo(originalCaseInsensitiveMatch);
    }

    static Stream<Arguments> caseSensitivityProvider() {
        return Stream.of(
                Arguments.of("file.txt", "file.txt", "FILE.TXT"),
                Arguments.of("path/to/file.txt", "path/to/file.txt", "PATH/TO/FILE.TXT"),
                Arguments.of("*.txt", "file.txt", "FILE.TXT"),
                Arguments.of("src/*.java", "src/Main.java", "SRC/MAIN.JAVA"),
                Arguments.of("temp/*", "temp/file.txt", "TEMP/FILE.TXT"),
                Arguments.of("**/*.js", "src/app.js", "SRC/APP.JS"),
                Arguments.of("**/*.js", "src/subdir/app.js", "SRC/SUBDIR/APP.JS"),
                Arguments.of("File.txt", "File.txt", "file.txt")
        );
    }
}
