/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.addthis.hydra.query.aggregate;

import java.io.IOException;

import java.util.Map;

import com.addthis.hydra.data.query.QueryException;
import com.addthis.meshy.ChannelMaster;
import com.addthis.meshy.service.file.FileReference;
import com.addthis.meshy.service.stream.SourceInputStream;
import com.addthis.meshy.service.stream.StreamSource;

public class QueryTaskSourceOption {

    final FileReference queryReference;

    SourceInputStream sourceInputStream;

    public QueryTaskSourceOption(FileReference queryReference) {
        this.queryReference = queryReference;
    }

    public void activate(ChannelMaster meshy, Map<String, String> queryOptions) {
        StreamSource source = null;
        try {
            source = new StreamSource(meshy, queryReference.getHostUUID(),
                    queryReference.getHostUUID(), queryReference.name, queryOptions, 0);
        } catch (IOException e) {
            MeshSourceAggregator.log.warn("Error getting query handle for fileReference: {}/{}", queryReference.getHostUUID(), queryReference.name, e);
            throw new QueryException(e);
        }
        sourceInputStream = source.getInputStream();
    }

    public boolean isActive() {
        return sourceInputStream != null;
    }

    public void cancel(String message) {
        try {
            if (sourceInputStream != null) {
                sourceInputStream.close();
                sourceInputStream = null;
            }
        } catch (Exception e) {
            MeshSourceAggregator.log.warn("Exception closing sourceInputStream: ", e);
        }
    }
}
