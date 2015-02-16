/*
 * Copyright (c) 2014-2015. Vlad Ilyushchenko
 *
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

package org.nfsdb.examples.network.cluster;

import com.nfsdb.Journal;
import com.nfsdb.JournalKey;
import com.nfsdb.PartitionType;
import com.nfsdb.factory.JournalFactory;
import com.nfsdb.net.JournalClient;
import com.nfsdb.net.config.ClientConfig;
import com.nfsdb.tx.TxListener;
import org.nfsdb.examples.model.Price;

public class ClusterConsumerMain {
    public static void main(String[] args) throws Exception {
        JournalFactory factory = new JournalFactory(args[0]);
        final JournalClient client = new JournalClient(new ClientConfig("192.168.1.81:7080,192.168.1.81:7090") {{
            getReconnectPolicy().setRetryCount(6);
            getReconnectPolicy().setSleepBetweenRetriesMillis(1);
            getReconnectPolicy().setLoginRetryCount(2);
        }}, factory);

        final Journal<Price> reader = factory.bulkReader(new JournalKey<>(Price.class, "price-copy", PartitionType.NONE, 1000000000));
//        reader.transactions().print(new File(args[0], "client.csv"));


        client.subscribe(Price.class, null, "price-copy", 1000000000, new TxListener() {
            @Override
            public void onCommit() {
                int count = 0;
                long t = 0;
                for (Price p : reader.incrementBuffered()) {
                    if (count == 0) {
                        t = p.getNanos();
                    }
                    count++;
                }
                if (t == 0) {
                    System.out.println("no data received");
                } else {
                    System.out.println("took: " + (System.currentTimeMillis() - t) + ", count=" + count);
                }
            }
        });
        client.start();

        System.out.println("Client started");
    }
}
