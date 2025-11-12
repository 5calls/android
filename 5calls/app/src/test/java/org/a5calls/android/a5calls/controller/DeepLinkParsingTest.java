package org.a5calls.android.a5calls.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.net.Uri;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

/**
 * Unit tests for deep link parsing logic.
 * Tests the URL parsing and path extraction without requiring a full Activity.
 */
@RunWith(RobolectricTestRunner.class)
public class DeepLinkParsingTest {

    /**
     * Helper method to extract path from URI, mimicking MainActivity.maybeHandleDeepLink logic
     */
    private String extractPathFromUri(Uri uri) {
        if (uri == null || uri.getHost() == null) {
            return null;
        }

        if (!uri.getHost().equals("5calls.org")) {
            return null;
        }

        List<String> pathSegments = uri.getPathSegments();
        if (pathSegments.size() < 2 || !pathSegments.getFirst().equals("issue")) {
            return null;
        }

        // Build the full path from segments
        StringBuilder pathBuilder = new StringBuilder();
        for (int i = 0; i < pathSegments.size(); i++) {
            if (i > 0) {
                pathBuilder.append("/");
            }
            pathBuilder.append(pathSegments.get(i));
        }
        return pathBuilder.toString();
    }

    /**
     * Helper to normalize path for matching (add leading/trailing slashes)
     */
    private String normalizePath(String path) {
        if (path == null) {
            return null;
        }
        return "/" + path + "/";
    }

    @Test
    public void testExtractPath_simpleIssue() {
        Uri uri = Uri.parse("https://5calls.org/issue/my-slug");
        String path = extractPathFromUri(uri);

        assertNotNull(path);
        assertEquals("issue/my-slug", path);
    }

    @Test
    public void testExtractPath_withTrailingSlash() {
        Uri uri = Uri.parse("https://5calls.org/issue/my-slug/");
        String path = extractPathFromUri(uri);

        assertNotNull(path);
        assertEquals("issue/my-slug", path);
    }

    @Test
    public void testExtractPath_stateSpecificIssue() {
        Uri uri = Uri.parse("https://5calls.org/issue/state/utah/my-slug");
        String path = extractPathFromUri(uri);

        assertNotNull(path);
        assertEquals("issue/state/utah/my-slug", path);
    }

    @Test
    public void testExtractPath_invalidHost() {
        Uri uri = Uri.parse("https://wronghost.com/issue/my-slug");
        String path = extractPathFromUri(uri);

        assertNull(path);
    }

    @Test
    public void testExtractPath_nonIssuePath() {
        Uri uri = Uri.parse("https://5calls.org/about");
        String path = extractPathFromUri(uri);

        assertNull(path);
    }

    @Test
    public void testExtractPath_onlyIssue() {
        // Path with just "issue" but no slug
        Uri uri = Uri.parse("https://5calls.org/issue");
        String path = extractPathFromUri(uri);

        assertNull(path);
    }

    @Test
    public void testExtractPath_emptyPath() {
        Uri uri = Uri.parse("https://5calls.org/");
        String path = extractPathFromUri(uri);

        assertNull(path);
    }

    @Test
    public void testNormalizePath_simple() {
        String normalized = normalizePath("issue/my-slug");

        assertEquals("/issue/my-slug/", normalized);
    }

    @Test
    public void testNormalizePath_stateSpecific() {
        String normalized = normalizePath("issue/state/utah/my-slug");

        assertEquals("/issue/state/utah/my-slug/", normalized);
    }

    @Test
    public void testPathMatching_exactMatch() {
        String extractedPath = "issue/federal-budget-government-shutdown";
        String normalizedPath = normalizePath(extractedPath);
        String apiPermalink = "/issue/federal-budget-government-shutdown/";

        assertEquals(apiPermalink, normalizedPath);
    }

    @Test
    public void testPathMatching_noFalsePositives() {
        // Ensure "issue/my-slug" doesn't match "issue/state/utah/my-slug"
        String extractedPath1 = "issue/my-slug";
        String normalizedPath1 = normalizePath(extractedPath1);
        String apiPermalink2 = "/issue/state/utah/my-slug/";

        assertEquals("/issue/my-slug/", normalizedPath1);
        assertNotEquals(apiPermalink2, normalizedPath1);
    }

    @Test
    public void testPathMatching_casePreserved() {
        // URLs should be case-sensitive
        String extractedPath = "issue/My-Slug";
        String normalizedPath = normalizePath(extractedPath);

        assertEquals("/issue/My-Slug/", normalizedPath);
    }

    @Test
    public void testExtractPath_withQueryParameters() {
        // Query parameters should be ignored
        Uri uri = Uri.parse("https://5calls.org/issue/my-slug?utm_source=test");
        String path = extractPathFromUri(uri);

        assertNotNull(path);
        assertEquals("issue/my-slug", path);
    }

    @Test
    public void testExtractPath_withFragment() {
        // Fragments should be ignored
        Uri uri = Uri.parse("https://5calls.org/issue/my-slug#section");
        String path = extractPathFromUri(uri);

        assertNotNull(path);
        assertEquals("issue/my-slug", path);
    }

    @Test
    public void testExtractPath_multipleSlashes() {
        // Test URL with multiple consecutive slashes
        Uri uri = Uri.parse("https://5calls.org//issue//my-slug");
        String path = extractPathFromUri(uri);

        // Uri.parse handles this, but we should still get valid segments
        assertNotNull(path);
    }

    @Test
    public void testExtractPath_specialCharacters() {
        // Test slug with hyphens and numbers
        Uri uri = Uri.parse("https://5calls.org/issue/h-r-1234-bill-name");
        String path = extractPathFromUri(uri);

        assertNotNull(path);
        assertEquals("issue/h-r-1234-bill-name", path);
    }
}
