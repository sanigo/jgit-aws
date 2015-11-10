/*
 * Copyright (c) 2015, Ravi Chodavarapu (rchodava@gmail.com)
 *
 * Parts of this are based on JGit, which has the following notes:
 *
 * Copyright (C) 2011, Google Inc.
 * and other copyright owners as documented in the project's IP log.
 *
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Distribution License v1.0 which
 * accompanies this distribution, is reproduced below, and is
 * available at http://www.eclipse.org/org/documents/edl-v10.php
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * - Neither the name of the Eclipse Foundation, Inc. nor the
 *   names of its contributors may be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.chodavarapu.jgitaws.jgit;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import org.chodavarapu.jgitaws.JGitAwsConfiguration;
import org.chodavarapu.jgitaws.aws.DynamoClient;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.internal.storage.dfs.DfsObjDatabase;
import org.eclipse.jgit.internal.storage.dfs.DfsRefDatabase;
import org.eclipse.jgit.internal.storage.dfs.DfsRepository;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryBuilder;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.ReflogReader;
import org.eclipse.jgit.lib.StoredConfig;

import java.io.IOException;
import java.text.MessageFormat;

/**
 * @author Ravi Chodavarapu (rchodava@gmail.com)
 */
public class AmazonRepository extends DfsRepository {
    private final StoredConfig config;
    private final DfsObjDatabase objectDatabase;
    private final DfsRefDatabase refDatabase;
    private AmazonRepository(Builder builder) {
        super(builder);

        DynamoClient client = new DynamoClient((AmazonDynamoDB) null);
        config = new DynamoStoredConfig(null, getRepositoryName());
        objectDatabase =
                new S3WithDynamoMetaDataObjDatabase(
                        this,
                        builder.getReaderOptions(),
                        new JGitAwsConfiguration(client, null));
        refDatabase = new DynamoRefDatabase(this);
    }

    public String getRepositoryName() {
        return getDescription().getRepositoryName();
    }

    @Override
    public void create(boolean bare) throws IOException {
        if (exists())
            throw new IOException(MessageFormat.format(
                    JGitText.get().repositoryAlreadyExists, "")); //$NON-NLS-1$

        String master = Constants.R_HEADS + Constants.MASTER;
        RefUpdate.Result result = updateRef(Constants.HEAD, true).link(master);
        if (result != RefUpdate.Result.NEW)
            throw new IOException(result.name());
    }

    public boolean exists() throws IOException {
//        return getRefDatabase().exists();
        return true;
    }

    @Override
    public StoredConfig getConfig() {
        return config;
    }

    @Override
    public DfsObjDatabase getObjectDatabase() {
        return objectDatabase;
    }

    @Override
    public DfsRefDatabase getRefDatabase() {
        return refDatabase;
    }

    @Override
    public ReflogReader getReflogReader(String refName) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void notifyIndexChanged() {

    }

    @Override
    public void scanForRepoChanges() throws IOException {

    }

    public static class Builder
            extends DfsRepositoryBuilder {
        @Override
        public Builder setup() throws IllegalArgumentException, IOException {
            return this;
        }

        @Override
        public AmazonRepository build() throws IOException {
            return new AmazonRepository(setup());
        }
    }

}
