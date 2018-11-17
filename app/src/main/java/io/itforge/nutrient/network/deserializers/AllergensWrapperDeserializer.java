
package io.itforge.nutrient.network.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.itforge.nutrient.models.AllergenResponse;
import io.itforge.nutrient.models.AllergensWrapper;

import java.io.IOException;
import java.util.*;


public class AllergensWrapperDeserializer extends StdDeserializer<AllergensWrapper> {


    private static final String NAMES_KEY = "name";
    private static final String WIKIDATA_KEY = "wikidata";

    public AllergensWrapperDeserializer() {
        super(AllergensWrapper.class);
    }

    @Override
    public AllergensWrapper deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        List<AllergenResponse> allergens = new ArrayList<>();

        JsonNode mainNode = jp.getCodec().readTree(jp);
        Iterator<Map.Entry<String, JsonNode>> mainNodeIterator = mainNode.fields();

        while (mainNodeIterator.hasNext()) {
            Map.Entry<String, JsonNode> subNode = mainNodeIterator.next();
            JsonNode namesNode = subNode.getValue().get(NAMES_KEY);
            Boolean isWikiNodePresent = subNode.getValue().has(WIKIDATA_KEY);

            if (namesNode != null) {
                Map<String, String> names =
                        new HashMap<>();  /* Entry<Language Code, Product Name> */
                Iterator<Map.Entry<String, JsonNode>> nameNodeIterator = namesNode.fields();
                while (nameNodeIterator.hasNext()) {
                    Map.Entry<String, JsonNode> nameNode = nameNodeIterator.next();
                    String name = nameNode.getValue().asText();
                    names.put(nameNode.getKey(), name);

                }

                if (isWikiNodePresent) {
                    allergens.add(new AllergenResponse(subNode.getKey(), names, subNode.getValue().get(WIKIDATA_KEY).toString()));
                } else {
                    allergens.add(new AllergenResponse(subNode.getKey(), names));
                }
            }
        }


        AllergensWrapper wrapper = new AllergensWrapper();
        wrapper.setAllergens(allergens);

        return wrapper;
    }
}