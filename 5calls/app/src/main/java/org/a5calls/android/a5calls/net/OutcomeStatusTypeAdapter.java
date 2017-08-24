package org.a5calls.android.a5calls.net;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import org.a5calls.android.a5calls.model.Outcome;

import java.io.IOException;

public class OutcomeStatusTypeAdapter extends TypeAdapter<Outcome.Status> {
//    @Override
//    public Outcome.Status deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
//        Outcome.Status result = Outcome.Status.UNKNOWN;
//
//        for (Outcome.Status status : Outcome.Status.values()) {
//            if (status.toString().equals(json.getAsString())) {
//                result = status;
//            }
//        }
//
//        return result;
//    }
//
//    @Override
//    public JsonElement serialize(Outcome.Status src, Type typeOfSrc, JsonSerializationContext context) {
//        return new JsonPrimitive(src.toString());
//    }

    @Override
    public void write(JsonWriter out, Outcome.Status value) throws IOException {
        out.name("status").value(value.toString());
    }

    @Override
    public Outcome.Status read(JsonReader in) throws IOException {
        Outcome.Status result = Outcome.Status.UNKNOWN;
        String jsonString = in.nextString();

        for (Outcome.Status status : Outcome.Status.values()) {
            if (status.toString().equals(jsonString)) {
                result = status;
            }
        }

        return result;
    }
}
