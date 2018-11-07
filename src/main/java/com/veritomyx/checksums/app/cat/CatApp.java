/*
 * Copyright (c) 2018 Veritomyx, Inc.
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

package com.veritomyx.checksums.app.cat;

import com.veritomyx.checksums.ChecksumInputStream;
import com.veritomyx.checksums.ChecksumOutputStream;
import com.veritomyx.checksums.MissingChecksumException;

import java.io.*;
import java.security.NoSuchAlgorithmException;

public class CatApp {

    public static void main(String[] args) throws NoSuchAlgorithmException, IOException {

        final byte[] buffer = new byte[4096];

        try (BufferedOutputStream outputStream = new BufferedOutputStream((new ChecksumOutputStream(System.out)))) {

            for (String arg : args) {
                try (BufferedInputStream inputStream = new BufferedInputStream(new ChecksumInputStream(filenameToInputStream(arg)))) {
                    int len = inputStream.read(buffer);
                    while (len != -1) {
                        outputStream.write(buffer, 0, len);
                        len = inputStream.read(buffer);
                    }
                } catch (MissingChecksumException e) {
                    // swallow
                }
            }
        }
    }

    private static void readAndWriteFile(BufferedWriter writer, String arg) {

    }

    private static InputStream filenameToInputStream(String filename) throws FileNotFoundException {
        if (filename.equals("-")) {
            return System.in;
        }

        return new FileInputStream(filename);
    }
}
