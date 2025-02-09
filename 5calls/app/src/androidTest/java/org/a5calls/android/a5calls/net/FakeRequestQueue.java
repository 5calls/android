package org.a5calls.android.a5calls.net;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.NoCache;

public class FakeRequestQueue extends RequestQueue {
    protected Request mRequest = null;

    public FakeRequestQueue(BasicNetwork network) {
            super(new NoCache(), network);
        }

    @Override
    public Request add(Request request) {
        mRequest = request;
        super.add(request);
        return request;
    }
}
