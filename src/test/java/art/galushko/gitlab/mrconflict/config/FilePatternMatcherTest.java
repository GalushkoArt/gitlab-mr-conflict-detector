package art.galushko.gitlab.mrconflict.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FilePatternMatcherTest {

    @Test
    void shouldIncludeAllFilesWhenNoPatterns() {
        // Given
        FileFilterConfig config = FileFilterConfig.builder().build();
        FilePatternMatcher matcher = new FilePatternMatcher(config);

        // When & Then
        assertThat(matcher.shouldInclude("src/main/java/Test.java")).isTrue();
        assertThat(matcher.shouldInclude("README.md")).isTrue();
        assertThat(matcher.shouldInclude("any/path/file.txt")).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "'**/*.java', 'src/main/java/Test.java', true",
            "'**/*.java', 'src/test/java/TestClass.java', true",
            "'**/*.java', 'README.md', false",
            "'src/**', 'src/main/java/Test.java', true",
            "'src/**', 'docs/README.md', false",
            "'*.md', 'README.md', true",
            "'*.md', 'docs/README.md', false"
    })
    void shouldMatchIncludePatterns(String pattern, String filePath, boolean expected) {
        // Given
        FileFilterConfig config = FileFilterConfig.builder()
                .includePatterns(List.of(pattern))
                .build();
        FilePatternMatcher matcher = new FilePatternMatcher(config);

        // When & Then
        assertThat(matcher.shouldInclude(filePath)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "'**/*.class', 'target/classes/Test.class', false",
            "'**/*.class', 'src/main/java/Test.java', true",
            "'**/target/**', 'target/classes/Test.class', false",
            "'**/target/**', 'src/main/java/Test.java', true",
            "'**/.git/**', '.git/config', false",
            "'**/.git/**', 'src/main/java/Test.java', true"
    })
    void shouldMatchExcludePatterns(String pattern, String filePath, boolean expected) {
        // Given
        FileFilterConfig config = FileFilterConfig.builder()
                .excludePatterns(List.of(pattern))
                .build();
        FilePatternMatcher matcher = new FilePatternMatcher(config);

        // When & Then
        assertThat(matcher.shouldInclude(filePath)).isEqualTo(expected);
    }

    @Test
    void shouldApplyBothIncludeAndExcludePatterns() {
        // Given
        FileFilterConfig config = FileFilterConfig.builder()
                .includePatterns(List.of("**/*.java"))
                .excludePatterns(List.of("**/test/**"))
                .build();
        FilePatternMatcher matcher = new FilePatternMatcher(config);

        // When & Then
        assertThat(matcher.shouldInclude("src/main/java/Test.java")).isTrue();
        assertThat(matcher.shouldInclude("src/test/java/TestClass.java")).isFalse();
        assertThat(matcher.shouldInclude("README.md")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Test.java",
            "test.java",
            "TEST.JAVA"
    })
    void shouldHandleCaseInsensitiveMatching(String filePath) {
        // Given
        FileFilterConfig config = FileFilterConfig.builder()
                .includePatterns(List.of("*.java"))
                .caseSensitive(false)
                .build();
        FilePatternMatcher matcher = new FilePatternMatcher(config);

        // When & Then
        assertThat(matcher.shouldInclude(filePath)).isTrue();
    }

    @Test
    void shouldFilterListOfPaths() {
        // Given
        var config = FileFilterConfig.builder()
                .includePatterns(List.of("**/*.java"))
                .excludePatterns(List.of("**/test/**"))
                .build();
        var matcher = new FilePatternMatcher(config);

        var inputPaths = List.of(
                "src/main/java/Main.java",
                "src/test/java/TestClass.java",
                "README.md",
                "src/main/java/util/Helper.java"
        );

        // When
        var filteredPaths = matcher.filterPaths(inputPaths);

        // Then
        assertThat(filteredPaths).containsExactly(
                "src/main/java/Main.java",
                "src/main/java/util/Helper.java"
        );
    }

    @Test
    void shouldHandleNullAndEmptyPaths() {
        // Given
        FileFilterConfig config = FileFilterConfig.defaultConfig();
        FilePatternMatcher matcher = new FilePatternMatcher(config);

        // When & Then
        assertThat(matcher.shouldInclude(null)).isFalse();
        assertThat(matcher.shouldInclude("")).isFalse();
    }

    @Test
    void shouldNormalizePathSeparators() {
        // Given
        FileFilterConfig config = FileFilterConfig.builder()
                .includePatterns(List.of("src/**/*.java"))
                .build();
        FilePatternMatcher matcher = new FilePatternMatcher(config);

        // When & Then
        assertThat(matcher.shouldInclude("src/main/java/Test.java")).isTrue();
        assertThat(matcher.shouldInclude("src\\main\\java\\Test.java")).isTrue();
    }

    @Test
    void shouldReportPatternCounts() {
        // Given
        FileFilterConfig config = FileFilterConfig.builder()
                .includePatterns(List.of("**/*.java", "**/*.kt"))
                .excludePatterns(List.of("**/test/**", "**/build/**", "**/.git/**"))
                .build();
        FilePatternMatcher matcher = new FilePatternMatcher(config);

        // When & Then
        assertThat(matcher.getIncludePatternCount()).isEqualTo(2);
        assertThat(matcher.getExcludePatternCount()).isEqualTo(3);
        assertThat(matcher.hasPatterns()).isTrue();
    }
}

