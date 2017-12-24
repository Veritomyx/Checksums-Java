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

import org.apache.commons.codec.binary.Hex;

import java.io.BufferedOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ChecksumOutputStream extends FilterOutputStream {

    private final MessageDigest digest;
    private boolean isClosed;

    public ChecksumOutputStream(OutputStream out) throws NoSuchAlgorithmException {
        super(out);
        this.digest = MessageDigest.getInstance("SHA-1");
        this.isClosed = false;
    }

    /**
     * Convenience method to return a ChecksumOutputStream that wraps a
     * BufferedOutputStream around one returned from
     * {@link java.nio.file.Files}.
     *
     * @param path Path used for output file
     * @throws NoSuchAlgorithmException if SHA-1 is not found
     * @throws IOException if an I/O error occurs opening the file
     * @return A new instance of a ChecksumInputStream
     */
    public static OutputStream create(Path path) throws NoSuchAlgorithmException, IOException {
        return new ChecksumOutputStream(new BufferedOutputStream(Files.newOutputStream(path)));
    }

    @Override
    public void write(int b) throws IOException {
        digest.update((byte) b);
        out.write(b);
    }

    @Override
    public void close() throws IOException {
        if (isClosed) {
            return;
        }

        String checksum = "# checksum:" + Hex.encodeHexString(digest.digest()) + "\n";
        out.write(checksum.getBytes());
        out.close();
        isClosed = true;
    }
}
