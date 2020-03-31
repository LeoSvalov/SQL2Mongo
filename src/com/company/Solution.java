package com.company;
import org.apache.commons.lang3.StringUtils;

public class Solution{
    String input;
    public Solution(String input) {
        this.input = input;
    }
    public String getSolution() {
        return parseSql2Mongo(this.input);
    }

    //  output for projection rows of the query
    private static String projection_output(String projections){
        if (!projections.equals("*")){
            String output = "{";
            String[] projection_rows = projections.split(",");
            output += projection_rows[0].trim() + ": 1";
            for (int i=1;i<projection_rows.length;i++){
                output+=", " + projection_rows[i].trim() + ": 1";
            }
            return ", " + output + "}";
        }
        return "";
    }

    //  parsing single logic expression
    private static String single_condition(String condition, Boolean isMultiple, Boolean FirstMult, Boolean LastMult){
        String output = "";
        if (isMultiple){
            if (FirstMult){
                output += " $and: [ {";
            }else{
                output += ", {";
            }
        }
        if (condition.contains(">") && !condition.contains("<>")) {
            output += '"' + StringUtils.substringBefore(condition, ">").trim() + '"' + ": {$gt: " + StringUtils.substringAfter(condition, ">").trim() + "}";
        }
        if (condition.contains("<") && !condition.contains("<>")) {
            output += '"' + StringUtils.substringBefore(condition, "<").trim() + '"' + ": {$lt: " + StringUtils.substringAfter(condition, "<").trim() + "}";
        }
        if (condition.contains("=")) {
            output += '"' + StringUtils.substringBefore(condition, "=").trim() + '"' + ": {$eq: " + StringUtils.substringAfter(condition, "=").trim() + "}";
        }
        if (condition.contains("<>")) {
            output += '"' + StringUtils.substringBefore(condition, "<>").trim() + '"' + ": {$ne: " + StringUtils.substringAfter(condition, "<>").trim() + "}";
        }
        if (isMultiple) output += "} ";
        if (LastMult) output += "] ";

        return output;
    }

    //  output condition part of the query
    private static String where_output(String condition){
        if (condition.isEmpty()) return "{}";
        String output = "{";
        if(!condition.contains("and")) {
            output += single_condition(condition,false,false,false);
        }else{
            output +=  single_condition(StringUtils.substringBefore(condition, "and").trim(),true,true,false);
            condition = StringUtils.substringAfter(condition, "and").trim();
            while(condition.contains("and")){
                output +=  single_condition(StringUtils.substringBefore(condition, "and").trim(),true,false,false);
                condition = StringUtils.substringAfter(condition, "and").trim();
            }
            output += single_condition(condition,true,false,true);
        }
        return output+ "}";
    }

    //  output for skip/limit part of the query
    private static String sl_output(String sl_part){
        String output  = ".";
        if (sl_part.contains("skip") && sl_part.contains("limit")){
            if (sl_part.indexOf("skip")<sl_part.indexOf("limit")){
                output+="skip(" + StringUtils.substringBetween(sl_part, "skip", "limit").trim()  + ")." +
                        "limit(" + StringUtils.substringAfter(sl_part, "limit").trim() + ");";
            }else{
                output+="limit("+ StringUtils.substringBetween(sl_part, "limit", "skip").trim()  + ")." +
                        "skip(" + StringUtils.substringAfter(sl_part, "skip").trim() + ");";
            }
        }else{
            if (sl_part.contains("limit")){
                output+="limit("+StringUtils.substringAfter(sl_part, "limit").trim()  + ");";
            }else{
                output+="skip("+StringUtils.substringAfter(sl_part, "skip").trim()  + ");";
            }
        }
        return output;
    }

    //  parsing condition part of the query -  (... where ... skip ... limit ...)
    private static String[] parse_conditions(String condition_part){
        String[] condition_output  = {"",""}; // 0 - for  where part, 1 - for skip and limit part
        if (condition_part.contains("where")){
            if (condition_part.contains("skip") && condition_part.contains("limit")){
                if (condition_part.indexOf("skip")<condition_part.indexOf("limit")){
                    condition_output[0] = where_output(StringUtils.substringBetween(condition_part,"where" ,"skip").trim());
                    condition_output[1] = sl_output("skip " + StringUtils.substringAfter(condition_part,"skip").trim());
                }else{
                    condition_output[0] = where_output(StringUtils.substringBetween(condition_part,"where" ,"limit").trim());
                    condition_output[1] = sl_output("limit " + StringUtils.substringAfter(condition_part,"limit").trim());
                }
            }else{
                if (condition_part.contains("limit")){
                    condition_output[0] = where_output(StringUtils.substringBetween(condition_part,"where","limit").trim());
                    condition_output[1] = sl_output("limit " + StringUtils.substringAfter(condition_part,"limit").trim());
                }else{
                    if (condition_part.contains("skip")){
                        condition_output[0] = where_output(StringUtils.substringBetween(condition_part,"where","skip").trim());
                        condition_output[1] = sl_output("skip " + StringUtils.substringAfter(condition_part,"skip").trim());
                    }else{
                        condition_output[0] = where_output(StringUtils.substringAfter(condition_part, "where").trim());
                    }
                }
            }
        }else{
            condition_output[0] = where_output("");
            condition_output[1] = sl_output(condition_part);
        }
        return condition_output;
    }

    //  main method for parsing sql query
    private static String parseSql2Mongo(String input){
        String normalized_input = StringUtils.normalizeSpace(input).toLowerCase();
        if(normalized_input.charAt(normalized_input.length()-1) == ';'){ // sql command usually ends with ;
            normalized_input = normalized_input.substring(0, normalized_input.length()-1);
        }
        String projections = StringUtils.substringBetween(normalized_input, "select", "from" ).trim();
        String projection_output = projection_output(projections);
        String rest = StringUtils.substringAfter(normalized_input, "from").trim();
        String s[] = rest.split(" ",2);
        String dbname= rest.split(" ",2)[0].trim();
        String condition_part = "";
        String[] condition_output = {"{}",""};
        if (s.length>1) {
            condition_part = rest.split(" ",2)[1].trim();
            condition_output = parse_conditions(condition_part);
        }
        String where_output = condition_output[0];
        String sl_output = condition_output[1];
        String final_output = "";
        if (sl_output.length()>0){
            final_output = "db." + dbname + ".find(" + where_output + projection_output +  ")" + sl_output;
        }else{
            final_output = "db." + dbname + ".find(" + where_output + projection_output +  ");";
        }
        return final_output;
    }
}