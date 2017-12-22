/*
 * Copyright (c) 2017 Veritomyx, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 *  Neither the name of PeakInvestigator-Java-SDK nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package com.veritomyx.checksums;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class ChecksumOutputStreamTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testSimple() throws IOException, NoSuchAlgorithmException {
        File file = folder.newFile();
        try (OutputStream stream = new ChecksumOutputStream(Files.newOutputStream(file.toPath()))) {
            stream.write('a');
        }

        StringBuilder builder = new StringBuilder();
        try (InputStream stream = new ChecksumInputStream(
                new BufferedInputStream(Files.newInputStream(file.toPath())))) {
            int val;
            while ((val = stream.read()) >= 0) {
                builder.append((char) val);
            }
        }

        assertThat(builder.toString(), equalTo("a"));
    }

    @Test
    public void testSentences() throws IOException, NoSuchAlgorithmException {
        List<String> text = new ArrayList<>();
        text.add("# this is a header");
        text.add("#");
        text.add("1234.5\t67.89");

        File file = folder.newFile();
        try (OutputStream stream = new ChecksumOutputStream(Files.newOutputStream(file.toPath()));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stream))) {

            for (String line : text) {
                writer.write(line);
                writer.newLine();
            }
        }

        List<String> lines = new ArrayList<>();
        try (InputStream stream = new ChecksumInputStream(new BufferedInputStream(Files.newInputStream(file.toPath())));
             BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }

        assertThat(lines, equalTo(text));
    }
}
