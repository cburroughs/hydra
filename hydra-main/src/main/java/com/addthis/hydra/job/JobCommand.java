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
package com.addthis.hydra.job;

import java.util.Arrays;

import com.addthis.codec.Codec;
import com.addthis.codec.CodecJSON;
import com.addthis.maljson.JSONObject;

import com.google.common.base.Objects;

/**
 * command run on minion to start each task in a job
 */
public final class JobCommand implements Codec.Codable, Cloneable {

    @Codec.Set(codable = true)
    private String owner;
    @Codec.Set(codable = true)
    private String command[];
    @Codec.Set(codable = true)
    private int reqCPU;
    @Codec.Set(codable = true)
    private int reqMEM;
    @Codec.Set(codable = true)
    private int reqIO;

    public JobCommand() {
    }

    public JobCommand(String owner, String command[], int cpu, int mem, int io) {
        this.owner = owner;
        this.command = command;
        this.reqCPU = cpu;
        this.reqMEM = mem;
        this.reqIO = io;
    }

    public String getOwner() {
        return owner;
    }

    public String[] getCommand() {
        return command;
    }

    public int getReqCPU() {
        return reqCPU;
    }

    public int getReqMEM() {
        return reqMEM;
    }

    public int getReqIO() {
        return reqIO;
    }


    public JSONObject toJSON() throws Exception {
        return CodecJSON.encodeJSON(this);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("owner", owner)
                .add("command", Arrays.deepToString(command))
                .add("reqCPU", reqCPU)
                .add("reqMEM", reqMEM)
                .add("reqIO", reqIO)
                .toString();
    }
}
