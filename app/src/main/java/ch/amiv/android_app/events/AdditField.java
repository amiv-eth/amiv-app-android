package ch.amiv.android_app.events;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import ch.amiv.android_app.R;

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

    public int type = FieldType.STRING;    //Use the FieldType constants, the currentValue should then be of that type
    private String name = "";
    private int resName = 0;
    public String title(Context context){   //For multi-lingual support on default additFields, use this function instead
        if(resName > 0)
            return context.getResources().getString(resName);
        else
            return name;
    }

    public boolean required = false;

    public String[] possibleValues = new String[0];
    public String currentValue = "";

    public AdditField (){ }

    public AdditField (int type_, String name_, boolean required_, String[] possibleValues_){
        type = type_;
        name = name_;
        required = required_;
        possibleValues = possibleValues_;
    }

    public AdditField (int type_, int resName_, boolean required_, String[] possibleValues_){
        type = type_;
        resName = resName_;
        required = required_;
        possibleValues = possibleValues_;
    }

    public static class Defaults {
        public static AdditField sbbAbo = new AdditField(FieldType.STRING, R.string.pref_sbb_title, false, new String[]{"GA", "Gleis 7", "Halbtax", "None"});
        public static AdditField food = new AdditField(FieldType.STRING, R.string.pref_food_title, false, new String[]{"Omnivore","Vegi","Vegan","Other"});
        public static AdditField specialFoodReq = new AdditField(FieldType.STRING, R.string.pref_special_food_title, false, new String[0]);
    }
    /**
     *
     * @param additional_fields The 'additional_fields' JsonObject from the event json
     * @return A parsed array of AdditFields. Empty array if we failed to parse
     */
    public static AdditField[] ParseFromJson(JSONObject additional_fields){
        List<AdditField> fields;
        JSONObject properties;
        List<String> requiredFields = null;

        //Get the properties json
        try {
            properties = new JSONObject(additional_fields.getString("properties"));
            fields = new ArrayList<>(properties.length());
        } catch (JSONException e) {
            e.printStackTrace();
            return new AdditField[0];//return empty array if we cannot parse
        }

        //parse which fields are required
        try {
            JSONArray requiredFieldsJson = new JSONArray(additional_fields.getString("required"));
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
                    f.possibleValues = ParseStringArray(new JSONArray(o.getString("enum")));

                //Check if this field is in the required list
                if(requiredFields != null && requiredFields.contains(f.name))
                    f.required = true;

                fields.add(f);
            }
            catch (JSONException e){
                e.printStackTrace();
            }
        }

        Object[] objects = fields.toArray();
        return Arrays.copyOf(objects, objects.length, AdditField[].class);
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
