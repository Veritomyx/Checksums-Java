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
import org.junit.rules.ExpectedException;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class ChecksumInputStreamTest {

    private final static String BASE_TEST_PATH = "/com/veritomyx/checksums/ChecksumInputStreamTestFiles/";

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void testSimple() throws NoSuchAlgorithmException, IOException {
        String text = "a";
        String checksum = "# checksum:86f7e437faa5a7fce15d1ddcb9eaeaea377667b8";
        String value = text + checksum + "\n";
        StringBuilder builder = new StringBuilder();

        try (InputStream stream = new ChecksumInputStream(new ByteArrayInputStream(value.getBytes()))) {
            int read;
            while ((read = stream.read()) >= 0) {
                builder.append((char) read);
            }
        }

        assertThat(builder.toString(), equalTo(text));
    }

    @Test
    public void testSentence() throws NoSuchAlgorithmException, IOException {
        String text = "Mary had a little lamb.";
        String checksum = "# checksum:4e07b8c7aaf2a4ed4ce39e76f65d2a04bdef5700";
        String value = text + checksum + "\n";
        StringBuilder builder = new StringBuilder();

        try (InputStream stream = new ChecksumInputStream(new ByteArrayInputStream(value.getBytes()))) {
            int read;
            while ((read = stream.read()) >= 0) {
                builder.append((char) read);
            }
        }

        assertThat(builder.toString(), equalTo(text));
    }

    @Test
    public void testComment() throws NoSuchAlgorithmException, IOException {
        String text = "# t";
        String checksum = "# checksum:4bf642f0aad16c948af58707d4bbc63745ca0100";
        String value = text + checksum + "\n";
        StringBuilder builder = new StringBuilder();

        try (InputStream stream = new ChecksumInputStream(new ByteArrayInputStream(value.getBytes()))) {
            int read;
            while ((read = stream.read()) >= 0) {
                builder.append((char) read);
            }
        }

        assertThat(builder.toString(), equalTo(text));
    }

    @Test
    public void testOk() throws URISyntaxException, IOException, NoSuchAlgorithmException {
        Path path = Paths.get(getResourceUri("valid.txt"));
        List<String> lines = new ArrayList<>();
        try (InputStream stream = new ChecksumInputStream(new BufferedInputStream(Files.newInputStream(path)));
             BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }

        assertThat(lines.size(), equalTo(3));
        assertThat(lines.get(2), equalTo("the end!"));
    }

    @Test
    public void testMissing() throws URISyntaxException, IOException, NoSuchAlgorithmException {
        exception.expect(MissingChecksumException.class);

        Path path = Paths.get(getResourceUri("missing.txt"));
        try (InputStream stream = new ChecksumInputStream(new BufferedInputStream(Files.newInputStream(path)));
             BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = reader.readLine()) != null) {
            }
        }
    }

    @Test
    public void testInvalid() throws URISyntaxException, IOException, NoSuchAlgorithmException {
        exception.expect(InvalidChecksumException.class);

        Path path = Paths.get(getResourceUri("invalid.txt"));
        List<String> lines = new ArrayList<>();
        try (InputStream stream = new ChecksumInputStream(new BufferedInputStream(Files.newInputStream(path)));
             BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
    }

    private static URI getResourceUri(String filename) throws URISyntaxException {
        URL resourceUrl = ChecksumInputStreamTest.class.getResource(BASE_TEST_PATH + filename);
        assertThat(resourceUrl, not(nullValue()));
        return resourceUrl.toURI();
    }
}
