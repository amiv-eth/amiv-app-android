package ch.amiv.android_app.events;

import android.graphics.drawable.AdaptiveIconDrawable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * A data class for storing a single additional Field for an event. See the SampleEventAdditFields file.
 * An instance of this class represents one variable.
 * Use an array of this type to generate a form for the event
 */
public class AdditField {
    //The types of fields we can have, according to the json schema, see SampleJsonSchemaAdditFields
    public static final class FieldType {
        public static final int ARRAY   = 0;
        public static final int BOOLEAN = 1;
        public static final int INTEGER = 2;
        public static final int NULL    = 3;
        public static final int NUMBER  = 4;
        public static final int OBJECT  = 5;
        public static final int STRING  = 6;
    }

    public int type;    //Use the FieldType constants, the currentValue should then be of that type
    public String name;
    public boolean required;

    public String[] possibleValues;
    public String currentValue;

    /**
     *
     * @param additional_fields The 'additional_fields' JsonObject from the event json
     * @return A parsed array of AdditFields. Empty array if we failed to parse
     */
    public static AdditField[] ParseFromJson(JSONObject additional_fields){
        ArrayList<AdditField> fields;
        JSONObject properties;
        List<String> requiredFields = null;

        //Get the properties json
        try {
            properties = additional_fields.getJSONObject("properties");
            fields = new ArrayList<>(properties.length());
        } catch (JSONException e) {
            e.printStackTrace();
            return new AdditField[0];//return empty array if we cannot parse
        }

        //parse which fields are required
        try {
            JSONArray requiredFieldsJson = additional_fields.getJSONArray("required");
            requiredFields = Arrays.asList(ParseStringArray(requiredFieldsJson));
        } catch (JSONException e) { }

        //If we have the data, parse it
        Iterator<String> iter = properties.keys();  //iterate over all items in the properties array, which each represent one AdditField
        while (iter.hasNext()) {
            String key = iter.next();
            try {
                JSONObject o = properties.getJSONObject(key);

                AdditField f = new AdditField();
                f.name = key;
                f.type = ParseFieldType(o.getString("type"));

                //Get possible values from enum
                if(o.has("enum"))
                    f.possibleValues = ParseStringArray(o.getJSONArray("enum"));

                //Check if this field is in the required list
                if(requiredFields != null && requiredFields.contains(f.name))
                    f.required = true;

                fields.add(f);
            }
            catch (JSONException e){
                e.printStackTrace();
            }
        }

        return (AdditField[]) fields.toArray();
    }

    /**
     * Will convert a type string from the json to a FieldType
     * @param type Type string from the AdditionalFields Json
     * @return FieldType
     */
    private static int ParseFieldType(String type){
        if(type == null) return FieldType.STRING;

        switch (type){
            case "array":
                return FieldType.ARRAY;
            case "boolean":
                return FieldType.BOOLEAN;
            case "integer":
                return FieldType.INTEGER;
            case "null":
                return FieldType.NULL;
            case "number":
                return FieldType.NUMBER;
            case "object":
                return FieldType.OBJECT;
            case "string":
                return FieldType.STRING;
            default:
                return FieldType.STRING;
        }
    }

    /**
     * Will parse a json String array to a java string array
     * @param array
     */
    private static String[] ParseStringArray(JSONArray array){
        String[] result = new String[array.length()];
        for (int i = 0; i < result.length; ++i){
            try {
                result[i] = array.getString(i);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return result;
    }
}
