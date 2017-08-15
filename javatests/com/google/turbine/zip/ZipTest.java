/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

package com.google.turbine.zip;

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.hash.Hashing;
import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link Zip}Test */
@RunWith(JUnit4.class)
public class ZipTest {

  @Rule public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void testEntries() throws IOException {
    testEntries(1000);
  }

  @Test
  public void zip64_testEntries() throws IOException {
    testEntries(70000);
  }

  @Test
  public void compression() throws IOException {
    Path path = temporaryFolder.newFile("test.jar").toPath();
    try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(path))) {
      for (int i = 0; i < 2; i++) {
        String name = "entry" + i;
        byte[] bytes = name.getBytes(UTF_8);
        jos.putNextEntry(new JarEntry(name));
        jos.write(bytes);
      }
    }
    assertThat(actual(path)).isEqualTo(expected(path));
  }

  private void testEntries(int entries) throws IOException {
    Path path = temporaryFolder.newFile("test.jar").toPath();
    try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(path))) {
      for (int i = 0; i < entries; i++) {
        String name = "entry" + i;
        byte[] bytes = name.getBytes(UTF_8);
        createEntry(jos, name, bytes);
      }
    }
    assertThat(actual(path)).isEqualTo(expected(path));
  }

  private static void createEntry(JarOutputStream jos, String name, byte[] bytes)
      throws IOException {
    JarEntry je = new JarEntry(name);
    je.setMethod(JarEntry.STORED);
    je.setSize(bytes.length);
    je.setCrc(Hashing.crc32().hashBytes(bytes).padToLong());
    jos.putNextEntry(je);
    jos.write(bytes);
  }

  private static Map<String, Long> actual(Path path) throws IOException {
    Map<String, Long> result = new LinkedHashMap<>();
    for (Zip.Entry e : new Zip.ZipIterable(path)) {
      result.put(e.name(), Hashing.goodFastHash(128).hashBytes(e.data()).padToLong());
    }
    return result;
  }

  private static Map<String, Long> expected(Path path) throws IOException {
    Map<String, Long> result = new LinkedHashMap<>();
    try (JarFile jf = new JarFile(path.toFile())) {
      Enumeration<JarEntry> entries = jf.entries();
      while (entries.hasMoreElements()) {
        JarEntry je = entries.nextElement();
        result.put(
            je.getName(),
            Hashing.goodFastHash(128)
                .hashBytes(ByteStreams.toByteArray(jf.getInputStream(je)))
                .padToLong());
      }
    }
    return result;
  }
}