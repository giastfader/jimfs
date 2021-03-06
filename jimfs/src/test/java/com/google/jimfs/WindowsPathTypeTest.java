/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.jimfs;

import static com.google.jimfs.PathType.windows;
import static com.google.jimfs.PathTypeTest.assertParseResult;
import static com.google.jimfs.PathTypeTest.assertUriRoundTripsCorrectly;
import static com.google.jimfs.PathTypeTest.fileSystemUri;
import static org.junit.Assert.fail;
import static org.truth0.Truth.ASSERT;

import com.google.common.collect.ImmutableList;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.net.URI;
import java.nio.file.InvalidPathException;

/**
 * Tests for {@link WindowsPathType}.
 *
 * @author Colin Decker
 */
@RunWith(JUnit4.class)
public class WindowsPathTypeTest {

  @Test
  public void testWindows() {
    PathType windows = PathType.windows();
    ASSERT.that(windows.getSeparator()).is("\\");
    ASSERT.that(windows.getOtherSeparators()).is("/");

    // "C:\\foo\bar" results from "C:\", "foo", "bar" passed to getPath
    PathType.ParseResult path = windows.parsePath("C:\\\\foo\\bar");
    assertParseResult(path, "C:\\", "foo", "bar");
    ASSERT.that(windows.toString(path.root(), path.names())).is("C:\\foo\\bar");

    PathType.ParseResult path2 = windows.parsePath("foo/bar/");
    assertParseResult(path2, null, "foo", "bar");
    ASSERT.that(windows.toString(path2.root(), path2.names())).is("foo\\bar");

    PathType.ParseResult path3 = windows.parsePath("hello world/foo/bar");
    assertParseResult(path3, null, "hello world", "foo", "bar");
    ASSERT.that(windows.toString(null, path3.names())).is("hello world\\foo\\bar");
  }

  @Test
  public void testWindows_relativePathsWithDriveRoot_unsupported() {
    try {
      windows().parsePath("C:");
      fail();
    } catch (InvalidPathException expected) {
    }

    try {
      windows().parsePath("C:foo\\bar");
      fail();
    } catch (InvalidPathException expected) {
    }
  }

  @Test
  public void testWindows_absolutePathOnCurrentDrive_unsupported() {
    try {
      windows().parsePath("\\foo\\bar");
      fail();
    } catch (InvalidPathException expected) {
    }

    try {
      windows().parsePath("\\");
      fail();
    } catch (InvalidPathException expected) {
    }
  }

  @Test
  public void testWindows_uncPaths() {
    PathType windows = PathType.windows();
    PathType.ParseResult path = windows.parsePath("\\\\host\\share");
    assertParseResult(path, "\\\\host\\share\\");

    path = windows.parsePath("\\\\HOST\\share\\foo\\bar");
    assertParseResult(path, "\\\\HOST\\share\\", "foo", "bar");

    try {
      windows.parsePath("\\\\");
      fail();
    } catch (InvalidPathException expected) {
      ASSERT.that(expected.getInput()).is("\\\\");
      ASSERT.that(expected.getReason()).is("UNC path is missing hostname");
    }

    try {
      windows.parsePath("\\\\host");
      fail();
    } catch (InvalidPathException expected) {
      ASSERT.that(expected.getInput()).is("\\\\host");
      ASSERT.that(expected.getReason()).is("UNC path is missing sharename");
    }

    try {
      windows.parsePath("\\\\host\\");
      fail();
    } catch (InvalidPathException expected) {
      ASSERT.that(expected.getInput()).is("\\\\host\\");
      ASSERT.that(expected.getReason()).is("UNC path is missing sharename");
    }

    try {
      windows.parsePath("//host");
      fail();
    } catch (InvalidPathException expected) {
      ASSERT.that(expected.getInput()).is("//host");
      ASSERT.that(expected.getReason()).is("UNC path is missing sharename");
    }
  }

  @Test
  public void testWindows_illegalNames() {
    try {
      windows().parsePath("foo<bar");
      fail();
    } catch (InvalidPathException expected) {
    }

    try {
      windows().parsePath("foo?");
      fail();
    } catch (InvalidPathException expected) {
    }

    try {
      windows().parsePath("foo ");
      fail();
    } catch (InvalidPathException expected) {
    }

    try {
      windows().parsePath("foo \\bar");
      fail();
    } catch (InvalidPathException expected) {
    }
  }

  @Test
  public void testWindows_toUri_normal() {
    URI fileUri = PathType.windows().toUri(fileSystemUri, "C:\\", ImmutableList.of("foo", "bar"));
    ASSERT.that(fileUri.toString()).is("jimfs://foo/C:/foo/bar");
    ASSERT.that(fileUri.getPath()).is("/C:/foo/bar");

    URI rootUri = PathType.windows().toUri(fileSystemUri, "C:\\", ImmutableList.<String>of());
    ASSERT.that(rootUri.toString()).is("jimfs://foo/C:/");
    ASSERT.that(rootUri.getPath()).is("/C:/");
  }

  @Test
  public void testWindows_toUri_unc() {
    URI fileUri = PathType.windows()
        .toUri(fileSystemUri, "\\\\host\\share\\", ImmutableList.of("foo", "bar"));
    ASSERT.that(fileUri.toString()).is("jimfs://foo//host/share/foo/bar");
    ASSERT.that(fileUri.getPath()).is("//host/share/foo/bar");

    URI rootUri = PathType.windows()
        .toUri(fileSystemUri, "\\\\host\\share\\", ImmutableList.<String>of());
    ASSERT.that(rootUri.toString()).is("jimfs://foo//host/share/");
    ASSERT.that(rootUri.getPath()).is("//host/share/");
  }

  @Test
  public void testWindows_toUri_escaping() {
    URI uri = PathType.windows()
        .toUri(fileSystemUri, "C:\\", ImmutableList.of("Users", "foo", "My Documents"));
    ASSERT.that(uri.toString()).is("jimfs://foo/C:/Users/foo/My%20Documents");
    ASSERT.that(uri.getRawPath()).is("/C:/Users/foo/My%20Documents");
    ASSERT.that(uri.getPath()).is("/C:/Users/foo/My Documents");
  }

  @Test
  public void testWindows_uriRoundTrips_normal() {
    assertUriRoundTripsCorrectly(PathType.windows(), "C:\\");
    assertUriRoundTripsCorrectly(PathType.windows(), "C:\\foo");
    assertUriRoundTripsCorrectly(PathType.windows(), "C:\\foo\\bar\\baz");
    assertUriRoundTripsCorrectly(PathType.windows(), "C:\\Users\\foo\\My Documents\\");
    assertUriRoundTripsCorrectly(PathType.windows(), "C:\\foo bar");
    assertUriRoundTripsCorrectly(PathType.windows(), "C:\\foo bar\\baz");
  }

  @Test
  public void testWindows_uriRoundTrips_unc() {
    assertUriRoundTripsCorrectly(PathType.windows(), "\\\\host\\share");
    assertUriRoundTripsCorrectly(PathType.windows(), "\\\\host\\share\\");
    assertUriRoundTripsCorrectly(PathType.windows(), "\\\\host\\share\\foo");
    assertUriRoundTripsCorrectly(PathType.windows(), "\\\\host\\share\\foo\\bar\\baz");
    assertUriRoundTripsCorrectly(PathType.windows(), "\\\\host\\share\\Users\\foo\\My Documents\\");
    assertUriRoundTripsCorrectly(PathType.windows(), "\\\\host\\share\\foo bar");
    assertUriRoundTripsCorrectly(PathType.windows(), "\\\\host\\share\\foo bar\\baz");
  }
}
